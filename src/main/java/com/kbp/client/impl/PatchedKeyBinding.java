package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import com.kbp.client.api.IPatchedKeyBinding;
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
public class PatchedKeyBinding extends KeyBinding implements IPatchedKeyBinding
{
	public PatchedKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		Input key,
		ImmutableSet< Input > cmb_keys,
		String category
	) {
		super( description, key_conflict_context, key, category );
		
		final IKeyBinding kb = ( IKeyBinding ) this;
		kb.initDefaultCmbKeys( cmb_keys );
	}
	
	@Override
	public KeyBinding getKeyBinding() {
		return this;
	}
}
