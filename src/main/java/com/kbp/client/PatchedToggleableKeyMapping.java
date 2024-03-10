package com.kbp.client;

import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;
import java.util.function.BooleanSupplier;

/**
 * Use {@link KBPMod#newToggleableBuilder(String, BooleanSupplier)} if possible
 * as this implementation is not guaranteed to present in all version.
 */
@OnlyIn( Dist.CLIENT )
public class PatchedToggleableKeyMapping extends ToggleKeyMapping implements IPatchedKeyMapping
{
	public PatchedToggleableKeyMapping(
		String description,
		int key_code,
		Iterator< Key > cmb_keys,
		String category,
		BooleanSupplier toggle_controller
	) {
		super( description, key_code, category, toggle_controller );
		
		final IKeyMapping km = ( IKeyMapping ) this;
		km.setDefaultCmbKeys( cmb_keys );
	}
}
