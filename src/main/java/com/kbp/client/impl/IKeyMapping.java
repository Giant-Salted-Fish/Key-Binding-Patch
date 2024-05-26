package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Only use {@link IPatchedKeyMapping} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@OnlyIn( Dist.CLIENT )
public interface IKeyMapping extends IPatchedKeyMapping
{
	default void incrClickCount() {
		throw new UnsupportedOperationException();
	}
	
	default void initDefaultCmbKeys( ImmutableSet< Key > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	default void resetKey() {
		throw new UnsupportedOperationException();
	}
	
	default String getSaveKey() {
		throw new UnsupportedOperationException();
	}
	
	default boolean isShadowKeyMapping() {
		throw new UnsupportedOperationException();
	}
	
	default KeyMapping createShadowCopy( int index ) {
		throw new UnsupportedOperationException();
	}
}
