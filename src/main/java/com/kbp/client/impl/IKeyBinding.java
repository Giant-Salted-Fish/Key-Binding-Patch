package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Only use {@link IPatchedKeyBinding} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@SideOnly( Side.CLIENT )
public interface IKeyBinding extends IPatchedKeyBinding
{
	default void initDefaultCmbKeys( ImmutableSet< Integer > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	default String getSaveKey() {
		throw new UnsupportedOperationException();
	}
	
	default boolean isShadowKeyBinding() {
		throw new UnsupportedOperationException();
	}
}
