package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Only use {@link IPatchedKeyBinding} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@OnlyIn( Dist.CLIENT )
public interface IKeyBinding extends IPatchedKeyBinding
{
	default void initDefaultCmbKeys( ImmutableSet< Input > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	default void incrClickCount() {
		throw new UnsupportedOperationException();
	}
	
	default void resetKey() {
		throw new UnsupportedOperationException();
	}
	
	default String getSaveKey() {
		throw new UnsupportedOperationException();
	}
	
	default boolean isShadowKeyBinding() {
		throw new UnsupportedOperationException();
	}
	
	default KeyBinding createShadowCopy( int index ) {
		throw new UnsupportedOperationException();
	}
}
