package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;

/**
 * Only used by internal shadow key bindings.
 */
@OnlyIn( Dist.CLIENT )
public final class ShadowToggleableKeyMapping extends PatchedToggleableKeyMapping implements IKeyMapping
{
	private final int index;
	
	public ShadowToggleableKeyMapping(
		String description,
		int key_code,
		ImmutableSet< Key > cmb_keys,
		String category,
		BooleanSupplier toggle_controller,
		int index
	) {
		super( description, key_code, cmb_keys, category, toggle_controller );
		
		this.index = index;
	}
	
	@Override
	public String getSaveKey() {
		return this.getName() + "_" + this.index;
	}
	
	@Override
	public boolean isShadowKeyMapping() {
		return true;
	}
}
