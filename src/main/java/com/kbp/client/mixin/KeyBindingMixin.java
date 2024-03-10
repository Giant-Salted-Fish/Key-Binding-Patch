package com.kbp.client.mixin;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.kbp.client.IKeyBinding;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.extensions.IForgeKeybinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mixin( KeyBinding.class )
public abstract class KeyBindingMixin implements IKeyBinding, IForgeKeybinding
{
	// >>> Shadow fields and methods <<<
	@Shadow
	@Final
	private static Map< String, KeyBinding > ALL;
	
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
	
	
	// >>> Unique fields <<<
	@Unique
	private static final HashMap< KeyModifier, Supplier< Iterator< Input > > >
		MODIFIER_2_CMBS = new HashMap<>();
	static
	{
		final BiConsumer< KeyModifier, Integer > adder = ( modifier, key_code ) ->
			MODIFIER_2_CMBS.put( modifier, () -> Iterators.forArray( Type.KEYSYM.getOrCreate( key_code ) ) );
		adder.accept( KeyModifier.CONTROL, GLFW.GLFW_KEY_LEFT_CONTROL );
		adder.accept( KeyModifier.SHIFT, GLFW.GLFW_KEY_LEFT_SHIFT );
		adder.accept( KeyModifier.ALT, GLFW.GLFW_KEY_LEFT_ALT );
		MODIFIER_2_CMBS.put( KeyModifier.NONE, Collections::emptyIterator );
	}
	
	@Unique
	private static final HashMap< Input, List< IKeyBinding > > UPDATE_TABLE = new HashMap<>();
	
	@Unique
	private static final HashSet< Input > ACTIVE_INPUTS = new HashSet<>();
	
	
	@Unique
	private Set< Input > default_cmb_keys = Collections.emptySet();
	
	@Unique
	private Set< Input > current_cmb_keys = this.getDefaultCmbKeys();
	
	@Unique
	private final HashSet< Runnable > press_callbacks = new HashSet<>();
	
	@Unique
	private final HashSet< Runnable > release_callbacks = new HashSet<>();
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void click( Input key )
	{
		UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
			.filter( kb -> kb.getKeyBinding().isDown() )
			.forEach( IKeyBinding::incrClickCount );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void set( Input key, boolean is_down )
	{
		if ( !is_down )
		{
			ACTIVE_INPUTS.remove( key );
			UPDATE_TABLE.getOrDefault( key, Collections.emptyList() )
				.forEach( kb -> kb.getKeyBinding().setDown( false ) );
			return;
		}
		
		final boolean is_already_active = !ACTIVE_INPUTS.add( key );
		if ( is_already_active ) {
			return;
		}
		
		final Iterator< IKeyBinding > itr = UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).iterator();
		while ( itr.hasNext() )
		{
			final IKeyBinding kb = itr.next();
			final Set< Input > cmb_keys = kb.getCmbKeys();
			if ( !ACTIVE_INPUTS.containsAll( cmb_keys ) ) {
				continue;
			}
			
			kb.getKeyBinding().setDown( true );
			final int priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final IKeyBinding after_kb = itr.next();
				final Set< Input > after_cmb_keys = after_kb.getCmbKeys();
				final int after_priority = after_cmb_keys.size();
				if ( after_priority != priority ) {
					break;
				}
				
				if ( ACTIVE_INPUTS.containsAll( after_cmb_keys ) ) {
					after_kb.getKeyBinding().setDown( true );
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
		final Minecraft mc = Minecraft.getInstance();
		final long window_handle = mc.getWindow().getWindow();
		ACTIVE_INPUTS.removeIf( input -> {
			final boolean not_keyboard_key = input.getType() != Type.KEYSYM;
			return (
				not_keyboard_key || input == InputMappings.UNKNOWN
				|| !InputMappings.isKeyDown( window_handle, input.getValue() )
			);
		} );
		
		UPDATE_TABLE.forEach( ( key, lst ) -> {
			if ( !ACTIVE_INPUTS.contains( key ) )
			{
				lst.stream()
					.map( IPatchedKeyBinding::getKeyBinding )
					.filter( KeyBinding::isDown )
					.forEach( kb -> kb.setDown( false ) );
			}
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void resetMapping()
	{
		UPDATE_TABLE.clear();
		ALL.values().stream()
			.filter( kb -> kb.getKey() != InputMappings.UNKNOWN )
			.map( kb -> ( IKeyBinding ) kb )
			.forEach( KeyBindingMixin::__regisToUpdateTable );
	}
	
	private static void __regisToUpdateTable( IKeyBinding kb )
	{
		UPDATE_TABLE.compute( kb.getKeyBinding().getKey(), ( k, lst ) -> {
			if ( lst == null ) {
				lst = new ArrayList<>();
			}
			
			final List< Integer > priority_lst = lst.stream()
				.map( IPatchedKeyBinding::getCmbKeys )
				.map( Set::size )
				.collect( Collectors.toList() );
			Collections.reverse( priority_lst );
			
			final int priority = kb.getCmbKeys().size();
			final int idx = Collections.binarySearch( priority_lst, priority );
			final int insert_idx = lst.size() - ( idx < 0 ? -idx - 1 : idx );
			lst.add( insert_idx, kb );
			return lst;
		} );
	}
	
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;Lnet/minecraftforge/client/settings/KeyModifier;Lnet/minecraft/client/util/InputMappings$Input;Ljava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String description,
		IKeyConflictContext keyConflictContext,
		KeyModifier keyModifier,
		Input keyCode,
		String category,
		CallbackInfo info
	) {
		this.setDefaultCmbKeys( MODIFIER_2_CMBS.get( keyModifier ).get() );
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
	public boolean isDown() {
		return this.isDown;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
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
		
		final IPatchedKeyBinding other_ = ( IPatchedKeyBinding ) other;
		final Set< Input > cmb0 = this.getCmbKeys();
		final Set< Input > cmb1 = other_.getCmbKeys();
		final Input key0 = this.getKey();
		final Input key1 = other.getKey();
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
	public ITextComponent getTranslatedKeyMessage()
	{
		final String key = this.getKey().getDisplayName().getString();
		final String msg = this.getCmbKeys().stream()
			.map( Input::getDisplayName )
			.map( ITextComponent::getString )
			.reduce( ( k0, k1 ) -> k0 + " + " + k1 )
			.map( s -> s + " + " + key )
			.orElse( key );
		return new StringTextComponent( msg );
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
	public String saveString()
	{
		// This is kind of hacky. See GameSettingsMixin.
		final String key = this.getKey().getName();
		final String modifier = KeyModifier.NONE.toString();
		final String cmb_keys = this.getCmbKeys().stream()
			.map( Input::getName )
			.reduce( ( s0, s1 ) -> s0 + "+" + s1 )
			.orElse( "" );
		return String.format( "%s:%s:%s", key, modifier, cmb_keys );
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
		// is a public method and can be called by any other mods.
		if ( is_down )
		{
			if ( !this.isDown && this.getKeyConflictContext().isActive() )
			{
				this.isDown = true;
				this.press_callbacks.forEach( Runnable::run );
			}
		}
		else
		{
			if ( this.isDown )
			{
				this.isDown = false;
				this.release_callbacks.forEach( Runnable::run );
			}
		}
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite( remap = false )
	public void setKeyModifierAndCode( KeyModifier keyModifier, Input keyCode ) {
		this.setKeyAndCmbKeys( keyCode, MODIFIER_2_CMBS.get( keyModifier ).get() );
	}
	
	@Override
	public final void incrClickCount() {
		this.clickCount += 1;
	}
	
	@Override
	public final void setDefaultCmbKeys( Iterator< Input > cmb_keys ) {
		this.default_cmb_keys = Sets.newHashSet( cmb_keys );
	}
	
	@Override
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getDefaultKey(), this.getDefaultCmbKeys().iterator() );
	}
	
	@Override
	public Set< Input > getDefaultCmbKeys() {
		return Collections.unmodifiableSet( this.default_cmb_keys );
	}
	
	@Override
	public Set< Input > getCmbKeys() {
		return Collections.unmodifiableSet( this.current_cmb_keys );
	}
	
	@Override
	public void setKeyAndCmbKeys( Input key, Iterator< Input > cmb_keys )
	{
		this.setKey( key );
		this.current_cmb_keys = Sets.newHashSet( cmb_keys );
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
	public final KeyBinding getKeyBinding() {
		return IForgeKeybinding.super.getKeyBinding();
	}
}
