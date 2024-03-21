package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.IKeyMapping;
import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.extensions.IForgeKeyMapping;
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

@Mixin( KeyMapping.class )
public abstract class KeyMappingMixin implements IKeyMapping, IForgeKeyMapping
{
	// >>> Shadow fields and methods <<<
	@Shadow
	@Final
	private static Map< String, KeyMapping > ALL;
	
	@Shadow
	@Final
	private static KeyBindingMap MAP;
	
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
	private ImmutableSet< Key > default_cmb_keys = ImmutableSet.of();
	
	@Unique
	private ImmutableSet< Key > current_cmb_keys = this.getDefaultCmbKeys();
	
	@Unique
	private final HashSet< Runnable > press_callbacks = new HashSet<>();
	
	@Unique
	private final HashSet< Runnable > release_callbacks = new HashSet<>();
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void click( Key key )
	{
		UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
			.filter( km -> km.getKeyMapping().isDown() )
			.forEach( IKeyMapping::incrClickCount );
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
		
		final boolean is_already_active = !ACTIVE_KEYS.add( key );
		if ( is_already_active ) {
			return;
		}
		
		final Iterator< IKeyMapping > itr = UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).iterator();
		while ( itr.hasNext() )
		{
			final IKeyMapping km = itr.next();
			final ImmutableSet< Key > cmb_keys = km.getCmbKeys();
			if ( !ACTIVE_KEYS.containsAll( cmb_keys ) ) {
				continue;
			}
			
			km.getKeyMapping().setDown( true );
			final int priority = cmb_keys.size();
			while ( itr.hasNext() )
			{
				final IKeyMapping after_km = itr.next();
				final ImmutableSet< Key > after_cmb_keys = after_km.getCmbKeys();
				final int after_priority = after_cmb_keys.size();
				if ( after_priority != priority ) {
					break;
				}
				
				if ( ACTIVE_KEYS.containsAll( after_cmb_keys ) ) {
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
		final Minecraft mc = Minecraft.getInstance();
		final long window_handle = mc.getWindow().getWindow();
		ACTIVE_KEYS.removeIf( key -> {
			final boolean is_still_active = (
				key.getType() != Type.KEYSYM // && key != InputConstants.UNKNOWN
				&& InputConstants.isKeyDown( window_handle, key.getValue() )
			);
			if ( is_still_active ) {
				return false;
			}
			
			UPDATE_TABLE.getOrDefault( key, Collections.emptyList() ).stream()
				.map( IPatchedKeyMapping::getKeyMapping )
				.filter( KeyMapping::isDown )
				.forEach( km -> km.setDown( false ) );
			return true;
		} );
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
		ALL.values().stream()
			.filter( km -> km.getKey() != InputConstants.UNKNOWN )
			.forEach( KeyMappingMixin::__regisToUpdateTable );
	}
	
	private static void __regisToUpdateTable( KeyMapping km )
	{
		final IKeyMapping ikm = ( IKeyMapping ) km;
		UPDATE_TABLE.compute( km.getKey(), ( k, lst ) -> {
			final List< IKeyMapping > update_lst = lst != null ? lst : new ArrayList<>();
			final List< Integer > priority_lst = update_lst.stream()
				.map( IPatchedKeyMapping::getCmbKeys )
				.map( AbstractCollection::size )
				.collect( Collectors.toList() );
			Collections.reverse( priority_lst );
			
			final int priority = ikm.getCmbKeys().size();
			final int idx = Collections.binarySearch( priority_lst, priority );
			final int insert_idx = update_lst.size() - ( idx < 0 ? -idx - 1 : idx );
			update_lst.add( insert_idx, ikm );
			return update_lst;
		} );
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
	public boolean isDown() {
		return this.isDown;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public boolean same( KeyMapping other )
	{
		final IKeyConflictContext ctx0 = this.getKeyConflictContext();
		final IKeyConflictContext ctx1 = other.getKeyConflictContext();
		final boolean is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final IPatchedKeyMapping other_ = ( IPatchedKeyMapping ) other;
		final ImmutableSet< Key > cmb0 = this.getCmbKeys();
		final ImmutableSet< Key > cmb1 = other_.getCmbKeys();
		final Key key0 = this.getKey();
		final Key key1 = other.getKey();
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
		final String key = this.getKey().getDisplayName().getString();
		final String msg = this.getCmbKeys().stream()
			.map( Key::getDisplayName )
			.map( Component::getString )
			.reduce( ( k0, k1 ) -> k0 + " + " + k1 )
			.map( s -> s + " + " + key )
			.orElse( key );
		return new TextComponent( msg );
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
		// This is kind of hacky. See OptionsMixin.
		final String key = this.getKey().getName();
		final String modifier = KeyModifier.NONE.toString();
		final String cmb_keys = this.getCmbKeys().stream()
			.map( Key::getName )
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
		// is a public method and can be called by other mods.
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
	
	@Override
	public void setKeyModifierAndCode( KeyModifier keyModifier, Key keyCode ) {
		this.setKeyAndCmbKeys( keyCode, MODIFIER_2_CMB_KEYS.get( keyModifier ).iterator() );
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
		final IKeyConflictContext ctx0 = this.getKeyConflictContext();
		final IKeyConflictContext ctx1 = other.getKeyConflictContext();
		final boolean is_ctx_conflict = ctx0.conflicts( ctx1 ) || ctx1.conflicts( ctx0 );
		if ( !is_ctx_conflict ) {
			return false;
		}
		
		final IPatchedKeyMapping other_ = ( IPatchedKeyMapping ) other;
		final ImmutableSet< Key > cmb0 = this.getCmbKeys();
		final ImmutableSet< Key > cmb1 = other_.getCmbKeys();
		final Key key0 = this.getKey();
		final Key key1 = other.getKey();
		return cmb0.contains( key1 ) || cmb1.contains( key0 );
	}
	
	@Override
	public final void incrClickCount() {
		this.clickCount += 1;
	}
	
	@Override
	public final void setDefaultCmbKeys( Iterator< Key > cmb_keys ) {
		this.default_cmb_keys = ImmutableSet.copyOf( cmb_keys );
	}
	
	@Override
	public void setToDefault() {
		this.setKeyAndCmbKeys( this.getDefaultKey(), this.getDefaultCmbKeys().iterator() );
	}
	
	@Override
	public ImmutableSet< Key > getDefaultCmbKeys() {
		return this.default_cmb_keys;
	}
	
	@Override
	public ImmutableSet< Key > getCmbKeys() {
		return this.current_cmb_keys;
	}
	
	@Override
	public void setKeyAndCmbKeys( Key key, Iterator< Key > cmb_keys )
	{
		this.setKey( key );
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
	public final KeyMapping getKeyMapping()
	{
		final Object o = this;
		return ( KeyMapping ) o;
	}
}
