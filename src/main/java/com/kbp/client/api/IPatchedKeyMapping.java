package com.kbp.client.api;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;

/**
 * @see KBPMod#getPatched(KeyMapping)
 */
@OnlyIn( Dist.CLIENT )
public interface IPatchedKeyMapping
{
	default ImmutableSet< Key > getDefaultCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default ImmutableSet< Key > getCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default void setKeyAndCmbKeys( Key key, Iterator< Key > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	default void regisPressCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default boolean removePressCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default void regisReleaseCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default boolean removeReleaseCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default KeyMapping getKeyMapping() {
		throw new UnsupportedOperationException();
	}
}
