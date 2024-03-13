package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.IKeyBinding;
import com.kbp.client.api.IPatchedKeyBinding;
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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Mixin( KeyBinding.class )
public abstract class KeyBindingMixin implements IKeyBinding
{
	// >>> Shadow fields and methods <<<
	@Shadow
	@Final
	private static Map< String, KeyBinding > KEYBIND_ARRAY;
	
	@Shadow
	@Final
	private static KeyBindingMap HASH;
	
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
	
	
	// >>> Unique fields <<<
	@Unique
	private static final HashMap< KeyModifier, ImmutableSet< Integer > >
		MODIFIER_2_CMB_KEYS = new HashMap<>();
	static
	{
		final BiConsumer< KeyModifier, Integer > adder = ( modifier, key_code ) ->
			MODIFIER_2_CMB_KEYS.put( modifier, ImmutableSet.of( key_code ) );
		adder.accept( KeyModifier.CONTROL, Keyboard.KEY_LCONTROL );
		adder.accept( KeyModifier.SHIFT, Keyboard.KEY_LSHIFT );
		adder.accept( KeyModifier.ALT, Keyboard.KEY_LMENU );
		MODIFIER_2_CMB_KEYS.put( KeyModifier.NONE, ImmutableSet.of() );
	}
	
	@Unique
	private static final HashMap< Integer, List< IKeyBinding > > UPDATE_TABLE = new HashMap<>();
	
	@Unique
	private static final HashSet< Integer > ACTIVE_KEYS = new HashSet<>();
	
	
	@Unique
	private ImmutableSet< Integer > default_cmb_keys = ImmutableSet.of();
	
	@Unique
	private ImmutableSet< Integer > current_cmb_keys = this.getDefaultCmbKeys();
	
	@Unique
	private final HashSet< Runnable > press_callbacks = new HashSet<>();
	
	@Unique
	private final HashSet< Runnable > release_callbacks = new HashSet<>();
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void onTick( int key )
	{
		UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
			.filter( kb -> kb.getKeyBinding().isKeyDown() )
			.forEach( IKeyBinding::incrPressTime );
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
				
				if ( ACTIVE_KEYS.containsAll( after_cmb_keys ) ) {
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
	public static void unPressAllKeys() {
		KEYBIND_ARRAY.values().forEach( kb -> ( ( IKeyBinding ) kb ).releaseKey() );
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
		KEYBIND_ARRAY.values().stream()
			.filter( kb -> kb.getKeyCode() != Keyboard.KEY_NONE )
			.forEach( KeyBindingMixin::__regisToUpdateTable );
	}
	
	private static void __regisToUpdateTable( KeyBinding kb )
	{
		final IKeyBinding ikb = ( IKeyBinding ) kb;
		UPDATE_TABLE.compute( kb.getKeyCode(), ( k, lst ) -> {
			if ( lst == null ) {
				lst = new ArrayList<>();
			}
			
			final List< Integer > priority_lst = lst.stream()
				.map( IPatchedKeyBinding::getCmbKeys )
				.map( AbstractCollection::size )
				.collect( Collectors.toList() );
			Collections.reverse( priority_lst );
			
			final int priority = ikb.getCmbKeys().size();
			final int idx = Collections.binarySearch( priority_lst, priority );
			final int insert_idx = lst.size() - ( idx < 0 ? -idx - 1 : idx );
			lst.add( insert_idx, ikb );
			return lst;
		} );
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
		CallbackInfo info
	) {
		this.setDefaultCmbKeys( MODIFIER_2_CMB_KEYS.get( keyModifier ).iterator() );
		this.setKeyAndCmbKeys( keyCode, this.getDefaultCmbKeys().iterator() );
		
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
		return this.pressed;
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
		this.setKeyAndCmbKeys( keyCode, MODIFIER_2_CMB_KEYS.get( keyModifier ).iterator() );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite( remap = false )
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getKeyCodeDefault(), this.getDefaultCmbKeys().iterator() );
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
		final String key = GameSettings.getKeyDisplayString( this.getKeyCode() );
		return (
			this.getCmbKeys().stream()
			.map( GameSettings::getKeyDisplayString )
			.reduce( ( s0, s1 ) -> s0 + " + " + s1 )
			.map( s -> s + " + " + key )
			.orElse( key )
		);
	}
	
	@Override
	public void pressKey()
	{
		if ( !this.pressed && this.getKeyConflictContext().isActive() )
		{
			this.pressed = true;
			this.press_callbacks.forEach( Runnable::run );
		}
	}
	
	@Override
	public void releaseKey()
	{
		if ( this.pressed )
		{
			this.pressed = false;
			this.release_callbacks.forEach( Runnable::run );
		}
	}
	
	@Override
	public final void incrPressTime() {
		this.pressTime += 1;
	}
	
	@Override
	public final void setDefaultCmbKeys( Iterator< Integer > cmb_keys ) {
		this.default_cmb_keys = ImmutableSet.copyOf( cmb_keys );
	}
	
	@Override
	public ImmutableSet< Integer > getDefaultCmbKeys() {
		return this.default_cmb_keys;
	}
	
	@Override
	public ImmutableSet< Integer > getCmbKeys() {
		return this.current_cmb_keys;
	}
	
	@Override
	public void setKeyAndCmbKeys( int key, Iterator< Integer > cmb_keys )
	{
		this.setKeyCode( key );
		this.current_cmb_keys = ImmutableSet.copyOf( cmb_keys );
	}
	
	@Override
	public void regisPressCallback( Runnable callback ) {
		this.press_callbacks.add( callback );
	}
	
	@Override
	public boolean removePressCallback( Runnable callback ) {
		return this.press_callbacks.remove( callback );
	}
	
	@Override
	public void regisReleaseCallback( Runnable callback ) {
		this.release_callbacks.add( callback );
	}
	
	@Override
	public boolean removeReleaseCallback( Runnable callback ) {
		return this.release_callbacks.remove( callback );
	}
	
	@Override
	public final KeyBinding getKeyBinding()
	{
		final Object o = this;
		return ( KeyBinding ) o;
	}
}
