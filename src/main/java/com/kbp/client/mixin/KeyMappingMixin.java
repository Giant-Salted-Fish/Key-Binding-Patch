package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.impl.IKeyMappingImpl;
import com.kbp.client.impl.InputSignal;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyMappingLookup;
import net.minecraftforge.client.settings.KeyModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin( KeyMapping.class )
public abstract class KeyMappingMixin implements IKeyMappingImpl, IPatchedKeyMapping
{
	// >>> Shadow Fields and Methods <<<
	@Shadow
	private static @Final Map< String, KeyMapping > ALL;
	
	@Shadow
	private static @Final KeyMappingLookup MAP;
	
	@Shadow
	boolean isDown;
	
	@Shadow
	private int clickCount;
	
	@Shadow( remap = false )
	private KeyModifier keyModifier;
	
	@Shadow( remap = false )
	private KeyModifier keyModifierDefault;
	
	@Shadow
	public abstract Key getDefaultKey();
	
	@Shadow
	public abstract void setKey( Key key );
	
	
	// >>> Unique Fields and Methods <<<
	@Unique
	private static final HashSet< Key > ACTIVE_KEYS = new HashSet<>();
	
	@Unique
	private boolean is_active;
	
	@Unique
	private ImmutableSet< Key > default_cmb_keys;
	
	@Unique
	private ImmutableSet< Key > current_cmb_keys;
	
	@Unique
	private InputSignal input_signal;
	
	@Unique
	private void __incrActiveCnt()
	{
		final var signal = this.input_signal;
		signal.active_count += 1;
		if ( signal.active_count == 1 )
		{
			this.isDown = true;
			signal.press_callbacks.forEach( Runnable::run );
		}
	}
	
	@Unique
	private void __decrActiveCnt()
	{
		final var signal = this.input_signal;
		signal.active_count -= 1;
		if ( signal.active_count == 0 )
		{
			this.isDown = false;
			signal.release_callbacks.forEach( Runnable::run );
		}
	}
	
	@Unique
	private static boolean __checkActive( Key key )
	{
		final var type = key.getType();
		if ( type == Type.SCANCODE ) {
			return ACTIVE_KEYS.contains( key );
		}
		else
		{
			final var mc = Minecraft.getInstance();
			final var window = mc.getWindow().getWindow();
			final var code = key.getValue();
			return switch ( type ) {
				case KEYSYM -> InputConstants.isKeyDown( window, code );
				case MOUSE -> GLFW.glfwGetMouseButton( window, code ) == GLFW.GLFW_PRESS;
				case SCANCODE -> throw new IllegalStateException();
			};
		}
	}
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Fix click only trigger one key maximum at each press.
	 */
	@Overwrite
	public static void click( Key key )
	{
		MAP.getAll( key ).stream()
			.filter( KeyMapping::isDown )
			.forEachOrdered( km -> {
				final var kmm = ( KeyMappingMixin ) ( Object ) km;
				final var delegate = ( KeyMappingMixin ) kmm.getDelegate();
				delegate.clickCount += 1;
			} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Add combo keys support and priority handling.
	 */
	@Overwrite
	@SuppressWarnings( "DataFlowIssue" )
	public static void set( Key key, boolean is_down )
	{
		if ( !is_down )
		{
			if ( ACTIVE_KEYS.remove( key ) ) {
				MAP.getAll( key ).forEach( km -> km.setDown( false ) );
			}
			return;
		}
		
		final var is_repeat_event = !ACTIVE_KEYS.add( key );
		if ( is_repeat_event ) {
			return;
		}
		
		final var itr = MAP.getAll( key ).iterator();
		while ( itr.hasNext() )
		{
			final var km = itr.next();
			final var ctx = km.getKeyConflictContext();
			if ( !ctx.isActive() ) {
				continue;
			}
			
			final var kmm = ( KeyMappingMixin ) ( Object ) km;
			final var cmb_keys = kmm.getCmbKeys();
			if ( !ACTIVE_KEYS.containsAll( cmb_keys ) ) {
				continue;
			}
			
			km.setDown( true );
			
			final var priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final var kmm1 = ( KeyMappingMixin ) ( Object ) itr.next();
				final var cmb_keys1 = kmm1.getCmbKeys();
				final var priority1 = cmb_keys1.size();
				if ( priority1 != priority ) {
					break;
				}
				
				final var ctx1 = kmm1.getKeyConflictContext();
				if ( ctx1.isActive() && ACTIVE_KEYS.containsAll( cmb_keys1 ) ) {
					kmm1.setDown( true );
				}
			}
			break;
		}
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Go through {@link #ACTIVE_KEYS} and update key bindings.
	 */
	@Overwrite
	public static void setAll()
	{
		// Copied from overwrite method. It seems that the original
		// implementation only cares about the keyboard keys.
		final var mc = Minecraft.getInstance();
		final var window_handle = mc.getWindow().getWindow();
		final var inactive_keys = (
			ACTIVE_KEYS.stream()
			.filter( key -> {
				final var is_still_active = (
					key.getType() != Type.KEYSYM // && key != InputConstants.UNKNOWN
					&& InputConstants.isKeyDown( window_handle, key.getValue() )
				);
				return !is_still_active;
			} )
			.toList()
		);
		
		inactive_keys.forEach( ACTIVE_KEYS::remove );
		inactive_keys.stream()
			.map( MAP::getAll )
			.flatMap( Collection::stream )
			.forEachOrdered( km -> km.setDown( false ) );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Clear {@link #ACTIVE_KEYS} and reset key mappings.
	 */
	@Overwrite
	@SuppressWarnings( "DataFlowIssue" )
	public static void releaseAll()
	{
		ACTIVE_KEYS.clear();
		ALL.values().forEach( km -> {
			final var kmm = ( KeyMappingMixin ) ( Object ) km;
			kmm.release();
		} );
	}
	
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String name,
		Type type,
		int keyCode,
		String category,
		CallbackInfo ci
	) {
		if ( !IKeyMappingImpl.isShadowKeyMapping( this.getKeyMapping() ) ) {
			this.input_signal = new InputSignal();
		}
		
		final var empty = ImmutableSet.< Key >of();
		this.default_cmb_keys = empty;
		this.current_cmb_keys = empty;
	}
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;Lnet/minecraftforge/client/settings/KeyModifier;Lcom/mojang/blaze3d/platform/InputConstants$Key;Ljava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String name,
		IKeyConflictContext conflict_context,
		KeyModifier modifier,
		Key key,
		String category,
		CallbackInfo ci
	) {
		this.input_signal = new InputSignal();
		
		final var resolved = this.keyModifier;
		final var cmb_keys = IKeyMappingImpl.toCmbKeySet( resolved );
		if ( !cmb_keys.isEmpty() )
		{
			// We have been added to the MAP when cmb keys is empty in super().
			// So redo MAP add after we set up cmb keys.
			final var self = this.getKeyMapping();
			MAP.remove( self );
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
			MAP.put( key, self );
		}
		else
		{
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
		}
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Proxy to delegate for shadow key mapping.
	 */
	@Overwrite
	private void release()
	{
		final var delegate = ( KeyMappingMixin ) this.getDelegate();
		delegate.clickCount = Math.max( 0, delegate.clickCount - 1 );
		this.setDown( false );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Need to check cmb keys for conflicts.
	 */
	@Overwrite
	public boolean same( KeyMapping other )
	{
		final var ctx0 = this.getKeyConflictContext();
		final var ctx1 = other.getKeyConflictContext();
		final var is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final var okmm = ( KeyMappingMixin ) ( Object ) other;
		final var cmb0 = this.getCmbKeys();
		final var cmb1 = okmm.getCmbKeys();
		final var key0 = this.getKey();
		final var key1 = other.getKey();
		return (
			cmb0.contains( key1 ) || cmb1.contains( key0 )
			|| ( key0.equals( key1 ) && cmb0.equals( cmb1 ) )
		);
	}
	
	/**
	 * @see #isActiveAndMatches(Key)
	 * @author Giant_Salted_Fish
	 * @reason Need to also check cmb keys.
	 */
	@Overwrite
	public boolean matches( int key_code, int scancode )
	{
		// TODO: Should we check context?
		final var key = this.getKey();
		final var type = key.getType();
		final var value = key.getValue();
		final var key_match = (
			key_code == InputConstants.UNKNOWN.getValue()
			? type == Type.SCANCODE && value == scancode
			: type == Type.KEYSYM && value == key_code
		);
		return key_match && this.getCmbKeys().stream().allMatch( KeyMappingMixin::__checkActive );
	}
	
	/**
	 * @see #isActiveAndMatches(Key)
	 * @author Giant_Salted_Fish
	 * @reason Need to also check cmb keys.
	 */
	@Overwrite
	public boolean matchesMouse( int button )
	{
		final var key = this.getKey();
		return (
			key.getType() == Type.MOUSE
			&& key.getValue() == button
			&& this.getCmbKeys().stream().allMatch( KeyMappingMixin::__checkActive )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Display cmb keys rather than the modifier.
	 */
	@Overwrite
	public Component getTranslatedKeyMessage()
	{
		return Component.literal(
			Stream.concat( this.getCmbKeys().stream(), Stream.of( this.getKey() ) )
			.map( Key::getDisplayName )
			.map( Component::getString )
			.collect( Collectors.joining( " + " ) )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Check cmb keys as well.
	 */
	@Overwrite
	public boolean isDefault()
	{
		return (
			this.getKey().equals( this.getDefaultKey() )
			&& this.getCmbKeys().equals( this.getDefaultCmbKeys() )
		);
	}
	
	/**
	 * This format is design to be compatible with vanilla key saving, so that
	 * player can still have their key settings after removing this mod.
	 *
	 * @author Giant_Salted_Fish
	 * @reason Set cmb keys as well.
	 */
	@Overwrite
	public String saveString()
	{
		final var key = this.getKey().getName();
		final var modifier = this.getKeyModifier().toString();
		final var cmb_keys = this.getCmbKeys().stream().map( Key::getName ).collect( Collectors.joining("+") );
		return String.join( ":", key, modifier, cmb_keys );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Proxy to delegate for shadow key bindings.
	 */
	@Overwrite
	public void setDown( boolean is_down )
	{
		if ( is_down )
		{
			if ( !this.is_active )
			{
				this.is_active = true;
				final var delegate = ( KeyMappingMixin ) this.getDelegate();
				delegate.__incrActiveCnt();
			}
		}
		else
		{
			if ( this.is_active )
			{
				this.is_active = false;
				final var delegate = ( KeyMappingMixin ) this.getDelegate();
				delegate.__decrActiveCnt();
			}
		}
	}
	
	/**
	 * This method is mainly being used in GUI codes where key bindings are not
	 * being updated by {@link #set(Key, boolean)}.
	 */
	@Override
	public boolean isActiveAndMatches( @NotNull Key key )
	{
		return (
			key != InputConstants.UNKNOWN
			&& key.equals( this.getKey() )
			&& this.getCmbKeys().stream().allMatch( KeyMappingMixin::__checkActive )
			&& this.getKeyConflictContext().isActive()
		);
	}
	
	@Override
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getDefaultKey(), this.getDefaultCmbKeys() );
	}
	
	@Override
	public void setKeyModifierAndCode( @Nullable KeyModifier modifier, @NotNull Key key )
	{
		// This part related to the modification by Forge in KeyBindsList.KeyEntry
		// and it only presents in the production environment not in development
		// environment. In general, it will clear the binding of the key when player
		// click and select the key mapping in the controls screen.
		final var is_selected_in_key_binds_screen = modifier == null; // && keyCode == InputConstants.UNKNOWN;
		if ( !is_selected_in_key_binds_screen )
		{
			final var resolved = modifier.matches( key ) ? KeyModifier.NONE : modifier;
			this.setKeyAndCmbKeys( key, IKeyMappingImpl.toCmbKeySet( resolved ) );
		}
	}
	
	@Override
	public boolean hasKeyModifierConflict( KeyMapping other )
	{
		final var ctx0 = this.getKeyConflictContext();
		final var ctx1 = other.getKeyConflictContext();
		final var is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final var okmm = ( KeyMappingMixin ) ( Object ) other;
		final var cmb0 = this.getCmbKeys();
		final var cmb1 = okmm.getCmbKeys();
		final var key0 = this.getKey();
		final var key1 = other.getKey();
		return cmb0.contains( key1 ) || cmb1.contains( key0 );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void initDefaultCmbKeys( ImmutableSet< Key > cmb_keys )
	{
		if ( !cmb_keys.isEmpty() )
		{
			assert !cmb_keys.contains( this.getKey() );
			final var self = this.getKeyMapping();
			MAP.remove( self );
			
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
			
			final var modifier = IKeyMappingImpl.toModifier( cmb_keys );
			this.keyModifierDefault = modifier;
			this.keyModifier = modifier;
			
			MAP.put( this.getKey(), self );
		}
	}
	
	@Override
	@SuppressWarnings("AddedMixinMembersNamePattern")
	public Object getDelegate() {
		return this;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public ImmutableSet< Key > getDefaultCmbKeys() {
		return this.default_cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public ImmutableSet< Key > getCmbKeys() {
		return this.current_cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void setKeyAndCmbKeys( Key key, ImmutableSet< Key > cmb_keys )
	{
		assert !cmb_keys.contains( key );
		this.setKey( key );
		this.current_cmb_keys = cmb_keys;
		this.keyModifier = IKeyMappingImpl.toModifier( cmb_keys );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void addPressCallback( Runnable callback ) {
		this.input_signal.press_callbacks.add( callback );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public boolean removePressCallback( Runnable callback ) {
		return this.input_signal.press_callbacks.remove( callback );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void addReleaseCallback( Runnable callback ) {
		this.input_signal.release_callbacks.add( callback );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public boolean removeReleaseCallback( Runnable callback ) {
		return this.input_signal.release_callbacks.remove( callback );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final KeyMapping getKeyMapping() {
		return ( KeyMapping ) ( Object ) this;
	}
}
