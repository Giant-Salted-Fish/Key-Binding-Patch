package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import net.minecraft.client.settings.ToggleableKeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;

/**
 * Use {@link KBPMod#newToggleableBuilder(String, BooleanSupplier)} if possible
 * as this implementation is not guaranteed to present in all version.
 */
@OnlyIn( Dist.CLIENT )
public class PatchedToggleableKeyBinding extends ToggleableKeyBinding
{
	public PatchedToggleableKeyBinding(
		String description,
		int key_code,
		ImmutableSet< Input > cmb_keys,
		String category,
		BooleanSupplier toggle_controller
	) {
		super( description, key_code, category, toggle_controller );
		
		final IKeyBindingImpl kb = ( IKeyBindingImpl ) this;
		kb.initDefaultCmbKeys( cmb_keys );
	}
}
