package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;

/**
 * Only used by internal shadow key bindings.
 */
@OnlyIn( Dist.CLIENT )
public final class ShadowToggleableKeyBinding extends PatchedToggleableKeyBinding implements IKeyBinding
{
	private final int index;
	
	public ShadowToggleableKeyBinding(
		String description,
		int key_code,
		ImmutableSet< Input > cmb_keys,
		String category,
		BooleanSupplier toggle_controller,
		int index
	) {
		super( description, key_code, cmb_keys, category, toggle_controller );
		
		this.index = index;
	}
	
	@Override
	public String getSaveKey() {
		return this.getName() + "_" + index;
	}
	
	@Override
	public boolean isShadowKeyBinding() {
		return true;
	}
}
