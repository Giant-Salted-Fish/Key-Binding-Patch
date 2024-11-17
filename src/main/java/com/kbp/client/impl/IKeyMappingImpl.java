package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

/**
 * Only use {@link IPatchedKeyMapping} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@OnlyIn( Dist.CLIENT )
public interface IKeyMappingImpl
{
	default void initDefaultCmbKeys( ImmutableSet< Key > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	Object getDelegate();
	
	
	ImmutableSet< Key > CMB_CTRL = ImmutableSet.of( Type.KEYSYM.getOrCreate( GLFW.GLFW_KEY_LEFT_CONTROL ) );
	ImmutableSet< Key > CMB_SUPER = ImmutableSet.of( Type.KEYSYM.getOrCreate( GLFW.GLFW_KEY_LEFT_SUPER ) );
	ImmutableSet< Key > CMB_SHIFT = ImmutableSet.of( Type.KEYSYM.getOrCreate( GLFW.GLFW_KEY_LEFT_SHIFT ) );
	ImmutableSet< Key > CMB_ALT = ImmutableSet.of( Type.KEYSYM.getOrCreate( GLFW.GLFW_KEY_LEFT_ALT ) );
	static ImmutableSet< Key > toCmbKeySet( KeyModifier modifier )
	{
		return switch ( modifier ) {
			case CONTROL -> Minecraft.ON_OSX ? CMB_SUPER : CMB_CTRL;
			case SHIFT -> CMB_SHIFT;
			case ALT -> CMB_ALT;
			case NONE -> ImmutableSet.of();
		};
	}
	
	static KeyModifier toModifier( ImmutableSet< Key > cmb_keys )
	{
		if ( cmb_keys.isEmpty() ) {
			return KeyModifier.NONE;
		}
		
		final var type = Type.KEYSYM;
		if ( Minecraft.ON_OSX )
		{
			final var lsuper = type.getOrCreate( GLFW.GLFW_KEY_LEFT_SUPER );
			final var rsuper = type.getOrCreate( GLFW.GLFW_KEY_RIGHT_SUPER );
			if ( cmb_keys.contains( lsuper ) && cmb_keys.contains( rsuper ) ) {
				return KeyModifier.CONTROL;
			}
		}
		else
		{
			final var lctrl = type.getOrCreate( GLFW.GLFW_KEY_LEFT_CONTROL );
			final var rctrl = type.getOrCreate( GLFW.GLFW_KEY_RIGHT_CONTROL );
			if ( cmb_keys.contains( lctrl ) && cmb_keys.contains( rctrl ) ) {
				return KeyModifier.CONTROL;
			}
		}
		
		final var lshift = type.getOrCreate( GLFW.GLFW_KEY_LEFT_SHIFT );
		final var rshift = type.getOrCreate( GLFW.GLFW_KEY_RIGHT_SHIFT );
		if ( cmb_keys.contains( lshift ) && cmb_keys.contains( rshift ) ) {
			return KeyModifier.SHIFT;
		}
		
		final var lalt = type.getOrCreate( GLFW.GLFW_KEY_LEFT_ALT );
		final var ralt = type.getOrCreate( GLFW.GLFW_KEY_RIGHT_ALT );
		if ( cmb_keys.contains( lalt ) && cmb_keys.contains( ralt ) ) {
			return KeyModifier.ALT;
		}
		
		return KeyModifier.NONE;
	}
	
	static boolean isShadowKeyMapping( KeyMapping km ) {
		return km instanceof ShadowKeyMapping || km instanceof ShadowToggleableKeyMapping;
	}
	
	static Optional< KeyMapping > getShadowTarget( KeyMapping km )
	{
		if ( km instanceof ShadowKeyMapping skm ) {
			return Optional.of( skm.target );
		}
		else if ( km instanceof ShadowToggleableKeyMapping skm ) {
			return Optional.of( skm.target );
		}
		else {
			return Optional.empty();
		}
	}
	
	static Optional< String > getShadowTarget( String name )
	{
		if( name.startsWith( "shadow#" ) )
		{
			final int suffix = name.indexOf( '@' );
			return Optional.of( name.substring( 7, suffix ) );
		}
		else {
			return Optional.empty();
		}
	}
}
