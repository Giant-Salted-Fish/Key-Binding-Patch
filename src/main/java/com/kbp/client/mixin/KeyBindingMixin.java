package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.impl.IKeyBindingImpl;
import com.kbp.client.impl.InputSignal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyModifier;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin( KeyBinding.class )
public abstract class KeyBindingMixin implements IKeyBindingImpl, IPatchedKeyBinding
{
	// >>> Shadow Fields and Methods <<<
	@Shadow
	private static @Final Map< String, KeyBinding > ALL;
	
	@Shadow
	private static @Final KeyBindingMap MAP;
	
	@Shadow
	boolean isDown;
	
	@Shadow
	private int clickCount;
	
	@Shadow( remap = false )
	private KeyModifier keyModifierDefault;
	
	@Shadow( remap = false )
	private KeyModifier keyModifier;
	
	@Shadow
	public abstract Input getDefaultKey();
	
	@Shadow
	public abstract void setKey( Input key );
	
	
	// >>> Unique Fields and Methods <<<
	/**
	 * {@link #set(Input, boolean)} will also be called on {@link GLFW#GLFW_REPEAT},
	 * so we need to keep track of the active inputs to avoid repeat activation.
	 */
	@Unique
	private static final HashSet< Input > ACTIVE_INPUTS = new HashSet<>();
	
	
	@Unique
	private boolean is_active;
	
	@Unique
	private ImmutableSet< Input > default_cmb_keys;
	
	@Unique
	private ImmutableSet< Input > current_cmb_keys;
	
	@Unique
	private InputSignal input_signal;
	
	@Unique
	private void __incrActiveCnt()
	{
		final InputSignal signal = this.input_signal;
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
		final InputSignal signal = this.input_signal;
		signal.active_count -= 1;
		if ( signal.active_count == 0 )
		{
			this.isDown = false;
			signal.release_callbacks.forEach( Runnable::run );
		}
	}
	
	@Unique
	private static boolean __checkActive( Input input )
	{
		final Type type = input.getType();
		if ( type == Type.SCANCODE ) {
			return ACTIVE_INPUTS.contains( input );
		}
		else
		{
			final Minecraft mc = Minecraft.getInstance();
			final long window = mc.getWindow().getWindow();
			final int code = input.getValue();
			switch ( type )
			{
			case KEYSYM:
				return InputMappings.isKeyDown( window, code );
			case MOUSE:
				return GLFW.glfwGetMouseButton( window, code ) == GLFW.GLFW_PRESS;
			default:
				throw new IllegalStateException();
			}
		}
	}
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Fix click only trigger one key maximum at each press.
	 */
	@Overwrite
	public static void click( Input key )
	{
		MAP.lookupAll( key ).stream()
			.filter( KeyBinding::isDown )
			.forEachOrdered( kb -> {
				final KeyBindingMixin kbm = ( KeyBindingMixin ) ( Object ) kb;
				final KeyBindingMixin delegate = ( KeyBindingMixin ) kbm.getDelegate();
				delegate.clickCount += 1;
			} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Add combo keys support and priority handling.
	 */
	@Overwrite
	@SuppressWarnings( "DataFlowIssue" )
	public static void set( Input key, boolean is_down )
	{
		if ( !is_down )
		{
			if ( ACTIVE_INPUTS.remove( key ) ) {
				MAP.lookupAll( key ).forEach( kb -> kb.setDown( false ) );
			}
			return;
		}
		
		final boolean is_repeat_event = !ACTIVE_INPUTS.add( key );
		if ( is_repeat_event ) {
			return;
		}
		
		final Iterator< KeyBinding > itr = MAP.lookupAll( key ).iterator();
		while ( itr.hasNext() )
		{
			final KeyBindingMixin kbm = ( KeyBindingMixin ) ( Object ) itr.next();
			final ImmutableSet< Input > cmb_keys = kbm.getCmbKeys();
			if ( !ACTIVE_INPUTS.containsAll( cmb_keys ) ) {
				continue;
			}
			
			kbm.setDown( true );
			
			final int priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final KeyBindingMixin kbm1 = ( KeyBindingMixin ) ( Object ) itr.next();
				final ImmutableSet< Input > cmb_keys1 = kbm1.getCmbKeys();
				final int priority1 = cmb_keys1.size();
				if ( priority1 != priority ) {
					break;
				}
				
				if ( ACTIVE_INPUTS.containsAll( cmb_keys1 ) ) {
					kbm1.setDown( true );
				}
			}
			break;
		}
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Go through {@link #ACTIVE_INPUTS} and update key bindings.
	 */
	@Overwrite
	public static void setAll()
	{
		// Copied from overwrite method. It seems that the original
		// implementation only cares about the keyboard keys.
		final Minecraft mc = Minecraft.getInstance();
		final long window_handle = mc.getWindow().getWindow();
		final List< Input > inactive_inputs = (
			ACTIVE_INPUTS.stream()
			.filter( input -> {
				final boolean is_still_active = (
					input.getType() == Type.KEYSYM // && input != InputMappings.UNKNOWN
					&& InputMappings.isKeyDown( window_handle, input.getValue() )
				);
				return !is_still_active;
			} )
			.collect( Collectors.toList() )
		);
		
		inactive_inputs.forEach( ACTIVE_INPUTS::remove );
		inactive_inputs.stream()
			.map( MAP::lookupAll )
			.flatMap( Collection::stream )
			.forEachOrdered( kb -> kb.setDown( false ) );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Clear {@link #ACTIVE_INPUTS} and reset key bindings.
	 */
	@Overwrite
	@SuppressWarnings( "DataFlowIssue" )
	public static void releaseAll()
	{
		ACTIVE_INPUTS.clear();
		ALL.values().forEach(  kb -> {
			final KeyBindingMixin kbm = ( KeyBindingMixin ) ( Object ) kb;
			kbm.release();
		} );
	}
	
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraft/client/util/InputMappings$Type;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String name,
		Type input_type,
		int key_code,
		String category,
		CallbackInfo ci
	) {
		if ( !IKeyBindingImpl.isShadowKeyBinding( this.getKeyBinding() ) ) {
			this.input_signal = new InputSignal();
		}
		
		final ImmutableSet< Input > empty = ImmutableSet.of();
		this.default_cmb_keys = empty;
		this.current_cmb_keys = empty;
	}
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;Lnet/minecraftforge/client/settings/KeyModifier;Lnet/minecraft/client/util/InputMappings$Input;Ljava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String name,
		IKeyConflictContext conflict_context,
		KeyModifier modifier,
		Input input,
		String category,
		CallbackInfo ci
	) {
		this.input_signal = new InputSignal();
		
		final KeyModifier resolved = this.keyModifier;
		final ImmutableSet< Input > cmb_keys = IKeyBindingImpl.toCmbKeySet( resolved );
		if ( !cmb_keys.isEmpty() )
		{
			// We have been added to the MAP when cmb keys is empty in super().
			// So redo MAP add after we set up cmb keys.
			final KeyBinding self = this.getKeyBinding();
			MAP.removeKey( self );
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
			MAP.addKey( input, self );
		}
		else
		{
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
		}
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Proxy to delegate for shadow key binding.
	 */
	@Overwrite
	private void release()
	{
		final KeyBindingMixin delegate = ( KeyBindingMixin ) this.getDelegate();
		delegate.clickCount = Math.max( 0, delegate.clickCount - 1 );
		this.setDown( false );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Need to check cmb keys for conflicts.
	 */
	@Overwrite
	public boolean same( KeyBinding other )
	{
		final IKeyConflictContext ctx0 = this.getKeyConflictContext();
		final IKeyConflictContext ctx1 = other.getKeyConflictContext();
		final boolean is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final KeyBindingMixin okbm = ( KeyBindingMixin ) ( Object ) other;
		final ImmutableSet< Input > cmb0 = this.getCmbKeys();
		final ImmutableSet< Input > cmb1 = okbm.getCmbKeys();
		final Input key0 = this.getKey();
		final Input key1 = other.getKey();
		return (
			cmb0.contains( key1 ) || cmb1.contains( key0 )
			|| ( key0.equals( key1 ) && cmb0.equals( cmb1 ) )
		);
	}
	
	/**
	 * @see #isActiveAndMatches(Input)
	 * @author Giant_Salted_Fish
	 * @reason Need to also check cmb keys.
	 */
	@Overwrite
	public boolean matches( int key_code, int scancode )
	{
		// TODO: Should we check context?
		final Input key = this.getKey();
		final Type type = key.getType();
		final int value = key.getValue();
		final boolean key_match = (
			key_code == InputMappings.UNKNOWN.getValue()
			? type == Type.SCANCODE && value == scancode
			: type == Type.KEYSYM && value == key_code
		);
		return key_match && this.getCmbKeys().stream().allMatch( KeyBindingMixin::__checkActive );
	}
	
	/**
	 * @see #isActiveAndMatches(Input)
	 * @author Giant_Salted_Fish
	 * @reason Need to also check cmb keys.
	 */
	@Overwrite
	public boolean matchesMouse( int button )
	{
		final Input key = this.getKey();
		return (
			key.getType() == Type.MOUSE
			&& key.getValue() == button
			&& this.getCmbKeys().stream().allMatch( KeyBindingMixin::__checkActive )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Display cmb keys rather than the modifier.
	 */
	@Overwrite
	public ITextComponent getTranslatedKeyMessage()
	{
		return new StringTextComponent(
			Stream.concat( this.getCmbKeys().stream(), Stream.of( this.getKey() ) )
			.map( Input::getDisplayName )
			.map( ITextComponent::getString )
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
		final String key = this.getKey().getName();
		final String modifier = this.getKeyModifier().toString();
		final String cmb_keys = this.getCmbKeys().stream().map( Input::getName ).collect( Collectors.joining( "+" ) );
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
				final KeyBindingMixin delegate = ( KeyBindingMixin ) this.getDelegate();
				delegate.__incrActiveCnt();
			}
		}
		else
		{
			if ( this.is_active )
			{
				this.is_active = false;
				final KeyBindingMixin delegate = ( KeyBindingMixin ) this.getDelegate();
				delegate.__decrActiveCnt();
			}
		}
	}
	
	/**
	 * This method is mainly being used in GUI codes where key bindings are not
	 * being updated by {@link #set(Input, boolean)}.
	 */
	@Override
	public boolean isActiveAndMatches( Input key )
	{
		return (
			key != InputMappings.UNKNOWN
			&& key.equals( this.getKey() )
			&& this.getCmbKeys().stream().allMatch( KeyBindingMixin::__checkActive )
			&& this.getKeyConflictContext().isActive()
		);
	}
	
	@Override
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getDefaultKey(), this.getDefaultCmbKeys() );
	}
	
	@Override
	public void setKeyModifierAndCode( KeyModifier modifier, Input key )
	{
		final KeyModifier resolved = modifier.matches( key ) ? KeyModifier.NONE : modifier;
		this.setKeyAndCmbKeys( key, IKeyBindingImpl.toCmbKeySet( resolved ) );
	}
	
	@Override
	public boolean hasKeyCodeModifierConflict( KeyBinding other )
	{
		final IKeyConflictContext ctx0 = this.getKeyConflictContext();
		final IKeyConflictContext ctx1 = other.getKeyConflictContext();
		final boolean is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final KeyBindingMixin okbm = ( KeyBindingMixin ) ( Object ) other;
		final ImmutableSet< Input > cmb0 = this.getCmbKeys();
		final ImmutableSet< Input > cmb1 = okbm.getCmbKeys();
		final Input key0 = this.getKey();
		final Input key1 = other.getKey();
		return cmb0.contains( key1 ) || cmb1.contains( key0 );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void initDefaultCmbKeys( ImmutableSet< Input > cmb_keys )
	{
		if ( !cmb_keys.isEmpty() )
		{
			assert !cmb_keys.contains( this.getKey() );
			final KeyBinding self = this.getKeyBinding();
			MAP.removeKey( self );
			
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
			
			final KeyModifier modifier = IKeyBindingImpl.toModifier( cmb_keys );
			this.keyModifierDefault = modifier;
			this.keyModifier = modifier;
			
			MAP.addKey( this.getKey(), self );
		}
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public Object getDelegate() {
		return this;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public ImmutableSet< Input > getDefaultCmbKeys() {
		return this.default_cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public ImmutableSet< Input > getCmbKeys() {
		return this.current_cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void setKeyAndCmbKeys( Input key, ImmutableSet< Input > cmb_keys )
	{
		assert !cmb_keys.contains( key );
		this.setKey( key );
		this.current_cmb_keys = cmb_keys;
		this.keyModifier = IKeyBindingImpl.toModifier( cmb_keys );
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
}
