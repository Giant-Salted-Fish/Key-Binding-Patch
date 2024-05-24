package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.impl.IKeyBinding;
import com.kbp.client.impl.InputSignal;
import com.kbp.client.impl.ShadowKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.extensions.IForgeKeybinding;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin( KeyBinding.class )
public abstract class KeyBindingMixin implements IKeyBinding, IForgeKeybinding
{
	// >>> Shadow fields and methods <<<
	@Shadow
	@Final
	private static Map< String, KeyBinding > ALL;
	
	@Shadow
	@Final
	private static KeyBindingMap MAP;
	
	@Shadow
	boolean isDown;
	
	@Shadow( remap = false )
	private KeyModifier keyModifierDefault;
	
	@Shadow( remap = false )
	private KeyModifier keyModifier;
	
	@Shadow
	public abstract String getCategory();
	
	@Shadow
	public abstract String getName();
	
	@Shadow
	public abstract Input getDefaultKey();
	
	@Shadow
	public abstract void setKey( Input key );
	
	
	// >>> Unique fields <<<
	@Unique
	private static final HashMap< KeyModifier, ImmutableSet< Input > >
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
	private static final HashMap< Input, List< IKeyBinding > > UPDATE_TABLE = new HashMap<>();
	
	@Unique
	private static final HashSet< Input > ACTIVE_INPUTS = new HashSet<>();
	
	
	@Unique
	private ImmutableSet< Input > default_cmb_keys;
	
	@Unique
	private ImmutableSet< Input > current_cmb_keys;
	
	@Unique
	private InputSignal input_signal;
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void click( Input key )
	{
		UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
			.filter( kb -> kb.getKeyBinding().isDown() )
			.forEachOrdered( IKeyBinding::incrClickCount );
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
			final IKeyConflictContext ctx = kb.getKeyBinding().getKeyConflictContext();
			if ( !ctx.isActive() ) {
				continue;
			}
			
			final ImmutableSet< Input > cmb_keys = kb.getCmbKeys();
			if ( !ACTIVE_INPUTS.containsAll( cmb_keys ) ) {
				continue;
			}
			
			kb.getKeyBinding().setDown( true );
			final int priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final IKeyBinding after_kb = itr.next();
				final ImmutableSet< Input > after_cmb_keys = after_kb.getCmbKeys();
				final int after_priority = after_cmb_keys.size();
				if ( after_priority != priority ) {
					break;
				}
				
				final IKeyConflictContext after_ctx = after_kb.getKeyBinding().getKeyConflictContext();
				if ( after_ctx.isActive() && ACTIVE_INPUTS.containsAll( after_cmb_keys ) ) {
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
			final boolean is_still_active = (
				input.getType() == Type.KEYSYM // && input != InputMappings.UNKNOWN
				&& InputMappings.isKeyDown( window_handle, input.getValue() )
			);
			if ( is_still_active ) {
				return false;
			}
			
			UPDATE_TABLE.getOrDefault( input, Collections.emptyList() ).stream()
				.map( IPatchedKeyBinding::getKeyBinding )
				.filter( KeyBinding::isDown )
				.forEachOrdered( kb -> kb.setDown( false ) );
			return true;
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void releaseAll() {
		UPDATE_TABLE.values().forEach( lst -> lst.forEach( IKeyBinding::resetKey ) );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void resetMapping()
	{
		MAP.clearMap();
		UPDATE_TABLE.clear();
		
		final GameSettings options = Minecraft.getInstance().options;
		// This will be called in GameSettings' constructor, hence it is \
		// possible that settings is null here. If it is null, then shadow \
		// key bindings have not been created yet, so safe to use #ALL.
		final boolean is_options_created = options != null;
		final Stream< KeyBinding > kb_stream = (
			is_options_created
			? Arrays.stream( options.keyMappings )
			: ALL.values().stream()
		);
		kb_stream.filter( kb -> kb.getKey() != InputMappings.UNKNOWN )
			.forEachOrdered( KeyBindingMixin::__regisToUpdateTable );
	}
	
	@Unique
	private static void __regisToUpdateTable( KeyBinding kb )
	{
		final IKeyBinding ikb = ( IKeyBinding ) kb;
		UPDATE_TABLE.compute( kb.getKeyBinding().getKey(), ( k, lst ) -> {
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
		method = "<init>(Ljava/lang/String;Lnet/minecraft/client/util/InputMappings$Type;ILjava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String p_i47675_1_,
		Type p_i47675_2_,
		int p_i47675_3_,
		String p_i47675_4_,
		CallbackInfo ci
	) {
		this.input_signal = InputSignal.of( p_i47675_1_ );
		
		final ImmutableSet< Input > cmb_keys = ImmutableSet.of();
		this.default_cmb_keys = cmb_keys;
		this.current_cmb_keys = cmb_keys;
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
		CallbackInfo ci
	) {
		this.input_signal = InputSignal.of( description );
		
		final ImmutableSet< Input > cmb_keys = MODIFIER_2_CMB_KEYS.get( keyModifier );
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
	private void release()
	{
		this.setDown( false );
		final InputSignal input_signal = this.input_signal;
		input_signal.click_count -= input_signal.click_count > 0 ? 1 : 0;
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
		final ImmutableSet< Input > cmb0 = this.getCmbKeys();
		final ImmutableSet< Input > cmb1 = other_.getCmbKeys();
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
		return new StringTextComponent(
			Stream.concat( this.getCmbKeys().stream(), Stream.of( this.getKey() ) )
			.map( Input::getDisplayName )
			.map( ITextComponent::getString )
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
		// is a public method and can be called by any other mods.
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
	public boolean isActiveAndMatches( Input keyCode )
	{
		return (
			keyCode != InputMappings.UNKNOWN
			&& this.getKey().equals( keyCode )
			&& ACTIVE_INPUTS.containsAll( this.getCmbKeys() )
			&& this.getKeyConflictContext().isActive()
		);
	}
	
	@Override
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getDefaultKey(), this.getDefaultCmbKeys() );
	}
	
	@Override
	public void setKeyModifierAndCode( KeyModifier keyModifier, Input keyCode ) {
		this.setKeyAndCmbKeys( keyCode, MODIFIER_2_CMB_KEYS.get( keyModifier ) );
	}
	
	@Override
	public boolean isConflictContextAndModifierActive()
	{
		return (
			this.getKeyConflictContext().isActive()
			&& ACTIVE_INPUTS.containsAll( this.getCmbKeys() )
		);
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
		
		final IPatchedKeyBinding other_ = ( IPatchedKeyBinding ) other;
		final ImmutableSet< Input > cmb0 = this.getCmbKeys();
		final ImmutableSet< Input > cmb1 = other_.getCmbKeys();
		final Input key0 = this.getKey();
		final Input key1 = other.getKey();
		return cmb0.contains( key1 ) || cmb1.contains( key0 );
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void initDefaultCmbKeys( ImmutableSet< Input > cmb_keys )
	{
		this.default_cmb_keys = cmb_keys;
		this.current_cmb_keys = cmb_keys;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public final void incrClickCount() {
		this.input_signal.click_count += 1;
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
	public boolean isShadowKeyBinding() {
		return false;
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public KeyBinding createShadowCopy( int index )
	{
		return new ShadowKeyBinding(
			this.getName(),
			this.getKeyConflictContext(),
			InputMappings.UNKNOWN,
			ImmutableSet.of(),
			this.getCategory(),
			index
		);
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
	public KeyBinding getKeyBinding()
	{
		final Object o = this;
		return ( KeyBinding ) o;
	}
}
