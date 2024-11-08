package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.kbp.client.KBPMod;
import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.impl.IKeyBinding;
import com.kbp.client.impl.InputSignal;
import net.minecraft.client.Minecraft;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin( KeyBinding.class )
public abstract class KeyBindingMixin implements IKeyBinding
{
	// >>> Shadow Fields and Methods <<<
	@Shadow
	@Final
	private static Map< String, KeyBinding > KEYBIND_ARRAY;
	
	@Shadow
	@Final
	private static KeyBindingMap HASH;
	
	@Shadow
	private boolean pressed;
	
	@Shadow( remap = false )
	private KeyModifier keyModifierDefault;
	
	@Shadow( remap = false )
	private KeyModifier keyModifier;
	
	@Shadow
	public abstract String getKeyDescription();
	
	@Shadow
	public abstract int getKeyCode();
	
	@Shadow
	public abstract void setKeyCode( int keyCode );
	
	@Shadow
	public abstract int getKeyCodeDefault();
	
	@Shadow( remap = false )
	public abstract IKeyConflictContext getKeyConflictContext();
	
	
	// >>> Unique fields <<<
	@Unique
	private static final HashMap< Integer, List< IKeyBinding > > UPDATE_TABLE = new HashMap<>();
	
	@Unique
	private static final HashSet< Integer > ACTIVE_KEYS = new HashSet<>();
	
	
	@Unique
	private ImmutableSet< Integer > current_cmb_keys;
	
	@Unique
	private ImmutableSet< Integer > default_cmb_keys;
	
	@Unique
	private InputSignal input_signal;
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void onTick( int key )
	{
		UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).forEach( ikb -> {
			final KeyBindingMixin kb = ( KeyBindingMixin ) ikb;
			if ( kb.isKeyDown() ) {
				kb.input_signal.click_count += 1;
			}
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void setKeyBindState( int key, boolean is_down )
	{
		if ( !is_down )
		{
			ACTIVE_KEYS.remove( key );
			UPDATE_TABLE.getOrDefault( key, Collections.emptyList() )
				.forEach( IPatchedKeyBinding::releaseKey );
			return;
		}
		
		final boolean is_already_active = !ACTIVE_KEYS.add( key );
		if ( is_already_active ) {
			return;
		}
		
		final Iterator< IKeyBinding > itr = UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).iterator();
		while ( itr.hasNext() )
		{
			final IKeyBinding kb = itr.next();
			final IKeyConflictContext ctx = kb.getKeyBinding().getKeyConflictContext();
			if ( !ctx.isActive() ) {
				continue;
			}
			
			final ImmutableSet< Integer > cmb_keys = kb.getCmbKeys();
			if ( !ACTIVE_KEYS.containsAll( cmb_keys ) ) {
				continue;
			}
			
			kb.pressKey();
			final int priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final IKeyBinding after_kb = itr.next();
				final ImmutableSet< Integer > after_cmb_keys = after_kb.getCmbKeys();
				final int after_priority = after_cmb_keys.size();
				if ( after_priority != priority ) {
					break;
				}
				
				final IKeyConflictContext after_ctx = after_kb.getKeyBinding().getKeyConflictContext();
				if ( after_ctx.isActive() && ACTIVE_KEYS.containsAll( after_cmb_keys ) ) {
					after_kb.pressKey();
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
	public static void updateKeyBindState()
	{
		ACTIVE_KEYS.removeIf( key -> {
			final boolean is_still_active = key > 0 && key < 256 && Keyboard.isKeyDown( key );
			if ( is_still_active ) {
				return false;
			}
			
			UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
				.filter( kb -> kb.getKeyBinding().isKeyDown() )
				.forEachOrdered( IPatchedKeyBinding::releaseKey );
			return true;
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void unPressAllKeys()
	{
		UPDATE_TABLE.values().stream().flatMap( List::stream ).forEachOrdered( ikb -> {
			final KeyBindingMixin kb = ( KeyBindingMixin ) ikb;
			kb.unpressKey();
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void resetKeyBindingArrayAndHash()
	{
		HASH.clearMap();
		UPDATE_TABLE.clear();
		
		final Minecraft mc = Minecraft.getMinecraft();
		final GameSettings settings = mc.gameSettings;
		// This will be called in GameSettings' constructor, hence it is \
		// possible that settings is null here. If it is null, then shadow \
		// key bindings have not been created yet, so safe to use #KEYBIND_ARRAY.
		final boolean is_settings_created = settings != null;
		final Stream< KeyBinding > kb_stream = (
			is_settings_created
			? Arrays.stream( settings.keyBindings )
			: KEYBIND_ARRAY.values().stream()
		);
		kb_stream.filter( kb -> kb.getKeyCode() != Keyboard.KEY_NONE )
			.forEachOrdered( KeyBindingMixin::__regisToUpdateTable );
	}
	
	@Unique
	private static void __regisToUpdateTable( KeyBinding kb )
	{
		final IKeyBinding ikb = ( IKeyBinding ) kb;
		UPDATE_TABLE.compute( kb.getKeyCode(), ( k, lst ) -> {
			final List< IKeyBinding > update_lst = lst != null ? lst : new ArrayList<>();
			final List< Integer > priority_lst = Lists.transform( update_lst, o -> o.getCmbKeys().size() );
			
			final int priority = ikb.getCmbKeys().size();
			final int idx = Collections.binarySearch( Lists.reverse( priority_lst ), priority );
			final int insert_idx = update_lst.size() - ( idx < 0 ? -idx - 1 : idx );
			update_lst.add( insert_idx, ikb );
			return update_lst;
		} );
	}
	
	
	@Inject(
		method = "<init>(Ljava/lang/String;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew( String description, int keyCode, String category, CallbackInfo ci )
	{
		this.input_signal = InputSignal.of( description );
		
		final ImmutableSet< Integer > cmb_keys = ImmutableSet.of();
		this.default_cmb_keys = cmb_keys;
		this.current_cmb_keys = cmb_keys;
	}
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;Lnet/minecraftforge/client/settings/KeyModifier;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String description,
		IKeyConflictContext keyConflictContext,
		KeyModifier keyModifier,
		int keyCode,
		String category,
		CallbackInfo ci
	) {
		this.input_signal = InputSignal.of( description );
		
		final ImmutableSet< Integer > cmb_keys = KBPMod.getCmbKeySet( keyModifier );
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
	public boolean isKeyDown() {
		return this.input_signal.active_count > 0;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public boolean isPressed()
	{
		final InputSignal input_signal = this.input_signal;
		final boolean flag = input_signal.click_count > 0;
		input_signal.click_count -= flag ? 1 : 0;
		return flag;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	private void unpressKey()
	{
		this.releaseKey();
		final InputSignal input_signal = this.input_signal;
		input_signal.click_count -= input_signal.click_count > 0 ? 1 : 0;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite( remap = false )
	public boolean isActiveAndMatches( int keyCode )
	{
		return (
			keyCode != Keyboard.KEY_NONE
			&& keyCode == this.getKeyCode()
			&& ACTIVE_KEYS.containsAll( this.getCmbKeys() )
			&& this.getKeyConflictContext().isActive()
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite( remap = false )
	public void setKeyModifierAndCode( KeyModifier keyModifier, int keyCode ) {
		this.setKeyAndCmbKeys( keyCode, KBPMod.getCmbKeySet( keyModifier ) );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite( remap = false )
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getKeyCodeDefault(), this.getDefaultCmbKeys() );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
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
	 * @reason Patch logic.
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
			|| key0 == key1 && cmb0.equals( cmb1 )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite( remap = false )
	public boolean hasKeyCodeModifierConflict(KeyBinding other)
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
	 * @reason Patch logic.
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
		// Although our implementation can guarantee the #pressKey() will only \
		// be called when the active state of the key is changed, we still \
		// have to check before firing callbacks as #pressKey() is a public \
		// method and can be called by any other mods.
		if ( !this.pressed )
		{
			this.pressed = true;
			this.input_signal.increaseActiveCount();
		}
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public void releaseKey()
	{
		if ( this.pressed )
		{
			this.pressed = false;
			this.input_signal.reduceActiveCount();
		}
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void initDefaultCmbKeys( ImmutableSet< Integer > cmb_keys )
	{
		this.default_cmb_keys = cmb_keys;
		this.current_cmb_keys = cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public String getSaveKey() {
		return this.getKeyDescription();
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public boolean isShadowKeyBinding() {
		return false;
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
	public final KeyBinding getKeyBinding()
	{
		final Object o = this;
		return ( KeyBinding ) o;
	}
}
