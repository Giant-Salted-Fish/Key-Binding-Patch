package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.impl.IKeyMapping;
import com.kbp.client.impl.InputSignal;
import com.kbp.client.impl.ShadowKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.extensions.IForgeKeyMapping;
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin( KeyMapping.class )
public abstract class KeyMappingMixin implements IKeyMapping, IForgeKeyMapping
{
	// >>> Shadow fields and methods <<<
	@Shadow
	@Final
	private static Map< String, KeyMapping > ALL;
	
	@Shadow
	@Final
	private static KeyMappingLookup MAP;
	
	@Shadow
	boolean isDown;
	
	@Shadow( remap = false )
	private KeyModifier keyModifier;
	
	@Shadow( remap = false )
	private KeyModifier keyModifierDefault;
	
	@Shadow
	public abstract String getCategory();
	
	@Shadow
	public abstract String getName();
	
	@Shadow
	public abstract Key getDefaultKey();
	
	@Shadow
	public abstract void setKey( Key p_90849_ );
	
	
	// >>> Unique fields <<<
	@Unique
	private static final HashMap< KeyModifier, ImmutableSet< Key > >
		MODIFIER_2_CMB_KEYS = new HashMap<>();
	static
	{
		final BiConsumer< KeyModifier, Integer > adder = ( modifier, key_code ) ->
			MODIFIER_2_CMB_KEYS.put( modifier, ImmutableSet.of( Type.KEYSYM.getOrCreate( key_code ) ) );
		adder.accept( KeyModifier.CONTROL, GLFW.GLFW_KEY_LEFT_CONTROL );
		adder.accept( KeyModifier.SHIFT, GLFW.GLFW_KEY_LEFT_SHIFT );
		adder.accept( KeyModifier.ALT, GLFW.GLFW_KEY_LEFT_ALT );
		MODIFIER_2_CMB_KEYS.put( KeyModifier.NONE, ImmutableSet.of() );
	}
	
	@Unique
	private static final HashMap< Key, List< IKeyMapping > > UPDATE_TABLE = new HashMap<>();
	
	@Unique
	private static final HashSet< Key > ACTIVE_KEYS = new HashSet<>();
	
	
	@Unique
	private ImmutableSet< Key > default_cmb_keys;
	
	@Unique
	private ImmutableSet< Key > current_cmb_keys;
	
	@Unique
	private InputSignal input_signal;
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void click( Key key )
	{
		UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
			.filter( km -> km.getKeyMapping().isDown() )
			.forEachOrdered( IKeyMapping::incrClickCount );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void set( Key key, boolean is_down )
	{
		if ( !is_down )
		{
			ACTIVE_KEYS.remove( key );
			UPDATE_TABLE.getOrDefault( key, Collections.emptyList() )
				.forEach( km -> km.getKeyMapping().setDown( false ) );
			return;
		}
		
		final var is_already_active = !ACTIVE_KEYS.add( key );
		if ( is_already_active ) {
			return;
		}
		
		final var itr = UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).iterator();
		while ( itr.hasNext() )
		{
			final var km = itr.next();
			final var ctx = km.getKeyMapping().getKeyConflictContext();
			if ( !ctx.isActive() ) {
				continue;
			}
			
			final var cmb_keys = km.getCmbKeys();
			if ( !ACTIVE_KEYS.containsAll( cmb_keys ) ) {
				continue;
			}
			
			km.getKeyMapping().setDown( true );
			final var priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final var after_km = itr.next();
				final var after_cmb_keys = after_km.getCmbKeys();
				final var after_priority = after_cmb_keys.size();
				if ( after_priority != priority ) {
					break;
				}
				
				final var after_ctx = after_km.getKeyMapping().getKeyConflictContext();
				if ( after_ctx.isActive() && ACTIVE_KEYS.containsAll( after_cmb_keys ) ) {
					after_km.getKeyMapping().setDown( true );
				}
			}
			break;
		}
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void setAll()
	{
		// Copied from overwrite method. It seems that the original \
		// implementation only cares about the keyboard keys.
		final var mc = Minecraft.getInstance();
		final var window_handle = mc.getWindow().getWindow();
		ACTIVE_KEYS.removeIf( key -> {
			final var is_still_active = (
				key.getType() != Type.KEYSYM // && key != InputConstants.UNKNOWN
				&& InputConstants.isKeyDown( window_handle, key.getValue() )
			);
			if ( is_still_active ) {
				return false;
			}
			
			UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
				.map( IPatchedKeyMapping::getKeyMapping )
				.filter( KeyMapping::isDown )
				.forEachOrdered( km -> km.setDown( false ) );
			return true;
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void releaseAll() {
		UPDATE_TABLE.values().forEach( lst -> lst.forEach( IKeyMapping::resetKey ) );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	@SuppressWarnings( "ConstantValue" )
	public static void resetMapping()
	{
		MAP.clear();
		UPDATE_TABLE.clear();
		
		final var options = Minecraft.getInstance().options;
		// This will be called in GameSettings' constructor, hence it is \
		// possible that settings is null here. If it is null, then shadow \
		// key bindings have not been created yet, so safe to use #ALL.
		final var is_options_created = options != null;
		final var km_stream = (
			is_options_created
			? Arrays.stream( options.keyMappings )
			: ALL.values().stream()
		);
		km_stream.filter( km -> km.getKey() != InputConstants.UNKNOWN )
			.forEachOrdered( KeyMappingMixin::__regisToUpdateTable );
	}
	
	@Unique
	private static void __regisToUpdateTable( KeyMapping km )
	{
		final var ikm = ( IKeyMapping ) km;
		UPDATE_TABLE.compute( km.getKey(), ( k, lst ) -> {
			final var update_lst = lst != null ? lst : new ArrayList< IKeyMapping >();
			final var priority_lst = Lists.transform( update_lst, o -> o.getCmbKeys().size() );
			
			final var priority = ikm.getCmbKeys().size();
			final var idx = Collections.binarySearch( Lists.reverse( priority_lst ), priority );
			final var insert_idx = update_lst.size() - ( idx < 0 ? -idx - 1 : idx );
			update_lst.add( insert_idx, ikm );
			return update_lst;
		} );
	}
	
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String description,
		Type type,
		int keyCode,
		String category,
		CallbackInfo ci
	) {
		this.input_signal = InputSignal.of( description );
		
		final var cmb_keys = ImmutableSet.< Key >of();
		this.default_cmb_keys = cmb_keys;
		this.current_cmb_keys = cmb_keys;
	}
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;Lnet/minecraftforge/client/settings/KeyModifier;Lcom/mojang/blaze3d/platform/InputConstants$Key;Ljava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String description,
		IKeyConflictContext keyConflictContext,
		KeyModifier keyModifier,
		Key keyCode,
		String category,
		CallbackInfo info
	) {
		this.input_signal = InputSignal.of( description );
		
		final var cmb_keys = MODIFIER_2_CMB_KEYS.get( keyModifier );
		this.default_cmb_keys = cmb_keys;
		this.current_cmb_keys = cmb_keys;
		
		// Modifier will be ignored in the rest of the part.
		this.keyModifierDefault = KeyModifier.NONE;
		this.keyModifier = KeyModifier.NONE;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public boolean isDown() {
		return this.input_signal.active_count > 0;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public boolean consumeClick()
	{
		final var input_signal = this.input_signal;
		final var flag = input_signal.click_count > 0;
		input_signal.click_count -= flag ? 1 : 0;
		return flag;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	private void release()
	{
		this.setDown( false );
		final var input_signal = this.input_signal;
		input_signal.click_count -= input_signal.click_count > 0 ? 1 : 0;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
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
		
		final var other_ = ( IPatchedKeyMapping ) other;
		final var cmb0 = this.getCmbKeys();
		final var cmb1 = other_.getCmbKeys();
		final var key0 = this.getKey();
		final var key1 = other.getKey();
		return (
			cmb0.contains( key1 ) || cmb1.contains( key0 )
			|| key0.equals( key1 ) && cmb0.equals( cmb1 )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
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
	 * @reason Patch logic.
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
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public void setDown( boolean is_down )
	{
		// Although our implementation can guarantee the #setDown(boolean) \
		// will only be called when the active state of the key is changed, \
		// we still have to check before firing callbacks as #setDown(boolean) \
		// is a public method and can be called by other mods.
		if ( is_down )
		{
			if ( !this.isDown )
			{
				this.isDown = true;
				this.input_signal.increaseActiveCount();
			}
		}
		else
		{
			if ( this.isDown )
			{
				this.isDown = false;
				this.input_signal.reduceActiveCount();
			}
		}
	}
	
	@Override
	public boolean isActiveAndMatches( @Nonnull Key keyCode )
	{
		return (
			keyCode != InputConstants.UNKNOWN
			&& this.getKey().equals( keyCode )
			&& ACTIVE_KEYS.containsAll( this.getCmbKeys() )
			&& this.getKeyConflictContext().isActive()
		);
	}
	
	@Override
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getDefaultKey(), this.getDefaultCmbKeys() );
	}
	
	@Override
	public void setKeyModifierAndCode( @Nullable KeyModifier keyModifier, @NotNull Key keyCode )
	{
		// This part related to the modification by Forge in KeyBindsList.KeyEntry \
		// and it only presents in the production environment not in development \
		// environment. In general, it will clear the binding of the key when player \
		// click and select the key mapping in the controls screen.
		final var is_selected_in_key_binds_screen = keyModifier == null; // && keyCode == InputConstants.UNKNOWN;
		if ( !is_selected_in_key_binds_screen ) {
			this.setKeyAndCmbKeys( keyCode, MODIFIER_2_CMB_KEYS.get( keyModifier ) );
		}
	}
	
	@Override
	public boolean isConflictContextAndModifierActive()
	{
		return (
			this.getKeyConflictContext().isActive()
			&& ACTIVE_KEYS.containsAll( this.getCmbKeys() )
		);
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
		
		final var other_ = ( IPatchedKeyMapping ) other;
		final var cmb0 = this.getCmbKeys();
		final var cmb1 = other_.getCmbKeys();
		final var key0 = this.getKey();
		final var key1 = other.getKey();
		return cmb0.contains( key1 ) || cmb1.contains( key0 );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void incrClickCount() {
		this.input_signal.click_count += 1;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void initDefaultCmbKeys( ImmutableSet< Key > cmb_keys )
	{
		this.default_cmb_keys = cmb_keys;
		this.current_cmb_keys = cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void resetKey() {
		this.release();
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public String getSaveKey() {
		return this.getName();
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public boolean isShadowKeyMapping() {
		return false;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public KeyMapping createShadowCopy( int index )
	{
		return new ShadowKeyMapping(
			this.getName(),
			this.getKeyConflictContext(),
			InputConstants.UNKNOWN,
			ImmutableSet.of(),
			this.getCategory(),
			index
		);
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
		this.setKey( key );
		this.current_cmb_keys = cmb_keys;
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
	public final KeyMapping getKeyMapping()
	{
		final Object o = this;
		return ( KeyMapping ) o;
	}
}
