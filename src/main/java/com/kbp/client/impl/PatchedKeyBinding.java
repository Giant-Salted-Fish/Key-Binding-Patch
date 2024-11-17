package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;

/**
 * Use {@link KBPMod#newBuilder(String)} if possible as this implementation is
 * not guaranteed to present in all version.
 */
@OnlyIn( Dist.CLIENT )
public class PatchedKeyBinding extends KeyBinding
{
	public PatchedKeyBinding(
		String description,
		IKeyConflictContext conflict_context,
		Input key,
		ImmutableSet< Input > cmb_keys,
		String category
	) {
		super( description, conflict_context, key, category );
		
		final IKeyBindingImpl kb = ( IKeyBindingImpl ) this;
		kb.initDefaultCmbKeys( cmb_keys );
	}
}
