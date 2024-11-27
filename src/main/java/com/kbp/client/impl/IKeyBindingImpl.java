package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Optional;
import java.util.Set;

/**
 * Only use {@link IPatchedKeyBinding} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@SideOnly( Side.CLIENT )
public interface IKeyBindingImpl
{
	default void initDefaultCmbKeys( ImmutableSet< Integer > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	Object getDelegate();
	
	
	static boolean isKeyDown( int key_code ) {
		return key_code < 0 ? Mouse.isButtonDown( key_code + 100 ) : Keyboard.isKeyDown( key_code );
	}
	
	ImmutableSet< Integer > CMB_CTRL = ImmutableSet.of( Keyboard.KEY_LCONTROL );
	ImmutableSet< Integer > CMB_META = ImmutableSet.of( Keyboard.KEY_LMETA );
	ImmutableSet< Integer > CMB_SHIFT = ImmutableSet.of( Keyboard.KEY_LSHIFT );
	ImmutableSet< Integer > CMB_ALT = ImmutableSet.of( Keyboard.KEY_LMENU );
	static ImmutableSet< Integer > toCmbKeySet( KeyModifier modifier )
	{
		switch ( modifier )
		{
		case CONTROL:
			return Minecraft.IS_RUNNING_ON_MAC ? CMB_META : CMB_CTRL;
		case SHIFT:
			return CMB_SHIFT;
		case ALT:
			return CMB_ALT;
		default:
			return ImmutableSet.of();
		}
	}
	
	static KeyModifier toModifier( Set< Integer > cmb_keys )
	{
		if ( cmb_keys.isEmpty() ) {
			return KeyModifier.NONE;
		}
		
		final boolean contains_ctrl = (
			Minecraft.IS_RUNNING_ON_MAC
			? cmb_keys.contains( Keyboard.KEY_LMETA ) || cmb_keys.contains( Keyboard.KEY_RMETA )
			: cmb_keys.contains( Keyboard.KEY_LCONTROL ) || cmb_keys.contains( Keyboard.KEY_RCONTROL )
		);
		if ( contains_ctrl ) {
			return KeyModifier.CONTROL;
		}
		
		if ( cmb_keys.contains( Keyboard.KEY_LSHIFT ) || cmb_keys.contains( Keyboard.KEY_RSHIFT ) ) {
			return KeyModifier.SHIFT;
		}
		
		if ( cmb_keys.contains( Keyboard.KEY_LMENU ) || cmb_keys.contains( Keyboard.KEY_RMENU ) ) {
			return KeyModifier.ALT;
		}
		
		return KeyModifier.NONE;
	}
	
	static boolean isShadowKeyBinding( KeyBinding kb ) {
		return kb instanceof ShadowKeyBinding;
	}
	
	static Optional< KeyBinding > getShadowTarget( KeyBinding kb )
	{
		if ( kb instanceof ShadowKeyBinding )
		{
			final ShadowKeyBinding skb = ( ShadowKeyBinding ) kb;
			return Optional.of( skb.target );
		}
		else {
			return Optional.empty();
		}
	}
	
	static Optional< String > getShadowTarget( String description )
	{
		if ( description.startsWith( "shadow#" ) )
		{
			final int suffix = description.indexOf( '@' );
			return Optional.of( description.substring( 7, suffix ) );
		}
		else {
			return Optional.empty();
		}
	}
}
