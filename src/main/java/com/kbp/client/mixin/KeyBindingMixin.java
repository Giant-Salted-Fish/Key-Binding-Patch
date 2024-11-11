package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.impl.IKeyBindingImpl;
import com.kbp.client.impl.InputSignal;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin( KeyBinding.class )
public abstract class KeyBindingMixin implements IKeyBindingImpl, IPatchedKeyBinding
{
	// >>> Shadow Fields and Methods <<<
	@Shadow
	private static @Final KeyBindingMap HASH;
	
	@Shadow
	private boolean pressed;
	
	@Shadow
	private int pressTime;
	
	@Shadow( remap = false )
	private KeyModifier keyModifierDefault;
	
	@Shadow( remap = false )
	private KeyModifier keyModifier;
	
	@Shadow
	public abstract int getKeyCode();
	
	@Shadow
	public abstract void setKeyCode( int keyCode );
	
	@Shadow
	public abstract int getKeyCodeDefault();
	
	@Shadow( remap = false )
	public abstract IKeyConflictContext getKeyConflictContext();
	
	@Shadow( remap = false )
	public abstract KeyModifier getKeyModifierDefault();
	
	
	// >>> Unique Fields and Methods <<<
	/**
	 * Use this to track activation state as {@link #pressed} may be changed by
	 * other mods via reflection.
	 */
	@Unique
	private boolean is_active;
	
	@Unique
	private ImmutableSet< Integer > current_cmb_keys;
	
	@Unique
	private ImmutableSet< Integer > default_cmb_keys;
	
	@Unique
	private InputSignal input_signal;
	
	@Unique
	private void __incrActiveCnt()
	{
		final InputSignal signal = this.input_signal;
		signal.active_count += 1;
		if ( signal.active_count == 1 )
		{
			this.pressed = true;
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
			this.pressed = false;
			signal.release_callbacks.forEach( Runnable::run );
		}
	}
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Fix tick only trigger one key maximum at each press.
	 */
	@Overwrite
	public static void onTick( int key )
	{
		HASH.lookupAll( key ).stream()
			.filter( KeyBinding::isKeyDown )
			.forEachOrdered( kb -> {
				final KeyBindingMixin kbm = ( KeyBindingMixin ) ( Object ) kb;
				final KeyBindingMixin delegate = ( KeyBindingMixin ) kbm.getDelegate();
				delegate.pressTime += 1;
			} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Add combo keys support and priority handling.
	 */
	@Overwrite
	@SuppressWarnings( "DataFlowIssue" )
	public static void setKeyBindState( int key, boolean is_down )
	{
		if ( !is_down )
		{
			HASH.lookupAll( key ).forEach( kb -> {
				final KeyBindingMixin kbm = ( KeyBindingMixin ) ( Object ) kb;
				kbm.releaseKey();
			} );
			return;
		}
		
		final Iterator< KeyBinding > itr = HASH.lookupAll( key ).iterator();
		while ( itr.hasNext() )
		{
			final KeyBinding kb = itr.next();
			final IKeyConflictContext ctx = kb.getKeyConflictContext();
			if ( !ctx.isActive() ) {
				continue;
			}
			
			final KeyBindingMixin kbm = ( KeyBindingMixin ) ( Object ) kb;
			final ImmutableSet< Integer > cmb_keys = kbm.getCmbKeys();
			if ( !cmb_keys.stream().allMatch( IKeyBindingImpl::isKeyDown ) ) {
				continue;
			}
			
			kbm.pressKey();
			
			final int priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final KeyBindingMixin kbm1 = ( KeyBindingMixin ) ( Object ) itr.next();
				final ImmutableSet< Integer > cmb_keys1 = kbm1.getCmbKeys();
				final int after_priority = cmb_keys1.size();
				if ( after_priority != priority ) {
					break;
				}
				
				final IKeyConflictContext ctx1 = kbm1.getKeyConflictContext();
				if ( ctx1.isActive() && cmb_keys1.stream().allMatch( IKeyBindingImpl::isKeyDown ) ) {
					kbm1.pressKey();
				}
			}
			break;
		}
	}
	
	
	@Inject(
		method = "<init>(Ljava/lang/String;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew( String description, int key_code, String category, CallbackInfo ci )
	{
		if ( !IKeyBindingImpl.isShadowKeyBinding( this.getKeyBinding() ) ) {
			this.input_signal = new InputSignal();
		}
		
		final ImmutableSet< Integer > empty = ImmutableSet.of();
		this.default_cmb_keys = empty;
		this.current_cmb_keys = empty;
	}
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;Lnet/minecraftforge/client/settings/KeyModifier;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String description,
		IKeyConflictContext conflict_context,
		KeyModifier modifier,
		int key_code,
		String category,
		CallbackInfo ci
	) {
		this.input_signal = new InputSignal();
		
		final ImmutableSet< Integer > cmb_keys = IKeyBindingImpl.toCmbKeySet( modifier );
		if ( !cmb_keys.isEmpty() )
		{
			// We have been added to the HASH when cmb keys is empty in super().
			// So redo HASH add after we set up cmb keys.
			final KeyBinding self = this.getKeyBinding();
			HASH.removeKey( self );
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
			HASH.addKey( key_code, self );
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
	private void unpressKey()
	{
		final KeyBindingMixin delegate = ( KeyBindingMixin ) this.getDelegate();
		delegate.pressTime = Math.max( 0, delegate.pressTime - 1 );
		this.releaseKey();
	}
	
	/**
	 * This method is mainly being used in GUI codes where key bindings are not
	 * being updated by {@link #setKeyBindState(int, boolean)}.
	 *
	 * @author Giant_Salted_Fish
	 * @reason Need to also check the cmb keys.
	 */
	@Overwrite( remap = false )
	public boolean isActiveAndMatches( int key_code )
	{
		return (
			key_code != Keyboard.KEY_NONE
			&& key_code == this.getKeyCode()
			&& this.getCmbKeys().stream().allMatch( IKeyBindingImpl::isKeyDown )
			&& this.getKeyConflictContext().isActive()
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Set cmb keys as well.
	 */
	@Overwrite( remap = false )
	public void setKeyModifierAndCode( KeyModifier modifier, int key_code )
	{
		this.setKeyAndCmbKeys( key_code, IKeyBindingImpl.toCmbKeySet( modifier ) );
		this.keyModifier = modifier.matches( key_code ) ? KeyModifier.NONE : modifier;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Set cmb keys as well.
	 */
	@Overwrite( remap = false )
	public void setToDefault()
	{
		this.setKeyAndCmbKeys( this.getKeyCodeDefault(), this.getDefaultCmbKeys() );
		this.keyModifier = this.getKeyModifierDefault();
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Check cmb keys as well.
	 */
	@Overwrite( remap = false )
	public boolean isSetToDefaultValue()
	{
		return (
			this.getKeyCode() == this.getKeyCodeDefault()
			&& this.getCmbKeys().equals( this.getDefaultCmbKeys() )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Need to check cmb keys for conflicts.
	 */
	@Overwrite( remap = false )
	public boolean conflicts( KeyBinding other )
	{
		final IKeyConflictContext ctx0 = this.getKeyConflictContext();
		final IKeyConflictContext ctx1 = other.getKeyConflictContext();
		final boolean is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final IPatchedKeyBinding other_ = ( IPatchedKeyBinding ) other;
		final ImmutableSet< Integer > cmb0 = this.getCmbKeys();
		final ImmutableSet< Integer > cmb1 = other_.getCmbKeys();
		final int key0 = this.getKeyCode();
		final int key1 = other.getKeyCode();
		return (
			cmb0.contains( key1 ) || cmb1.contains( key0 )
			|| ( key0 == key1 && cmb0.equals( cmb1 ) )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Need to check cmb keys for conflicts.
	 */
	@Overwrite( remap = false )
	public boolean hasKeyCodeModifierConflict( KeyBinding other )
	{
		final IKeyConflictContext ctx0 = this.getKeyConflictContext();
		final IKeyConflictContext ctx1 = other.getKeyConflictContext();
		final boolean is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final IPatchedKeyBinding other_ = ( IPatchedKeyBinding ) other;
		final ImmutableSet< Integer > cmb0 = this.getCmbKeys();
		final ImmutableSet< Integer > cmb1 = other_.getCmbKeys();
		final int key0 = this.getKeyCode();
		final int key1 = other.getKeyCode();
		return cmb0.contains( key1 ) || cmb1.contains( key0 );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Display cmb keys rather than the modifier.
	 */
	@Overwrite( remap = false )
	public String getDisplayName()
	{
		return (
			Stream.concat( this.getCmbKeys().stream(), Stream.of( this.getKeyCode() ) )
			.map( GameSettings::getKeyDisplayString )
			.collect( Collectors.joining( " + " ) )
		);
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void pressKey()
	{
		if ( !this.is_active )
		{
			this.is_active = true;
			final KeyBindingMixin delegate = ( KeyBindingMixin ) this.getDelegate();
			delegate.__incrActiveCnt();
		}
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void releaseKey()
	{
		if ( this.is_active )
		{
			this.is_active = false;
			final KeyBindingMixin delegate = ( KeyBindingMixin ) this.getDelegate();
			delegate.__decrActiveCnt();
		}
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void initDefaultCmbKeys( ImmutableSet< Integer > cmb_keys )
	{
		if ( !cmb_keys.isEmpty() )
		{
			final KeyBinding self = this.getKeyBinding();
			HASH.removeKey( self );
			
			this.default_cmb_keys = cmb_keys;
			this.current_cmb_keys = cmb_keys;
			
			final KeyModifier modifier = IKeyBindingImpl.toModifier( cmb_keys );
			this.keyModifierDefault = modifier;
			this.keyModifier = modifier;
			
			HASH.addKey( self.getKeyCode(), self );
		}
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public Object getDelegate() {
		return this;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public ImmutableSet< Integer > getDefaultCmbKeys() {
		return this.default_cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public ImmutableSet< Integer > getCmbKeys() {
		return this.current_cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void setKeyAndCmbKeys( int key, ImmutableSet< Integer > cmb_keys )
	{
		this.setKeyCode( key );
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
	public final KeyBinding getKeyBinding() {
		return ( KeyBinding ) ( Object ) this;
	}
}
