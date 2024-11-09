package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Use {@link KBPMod#newBuilder(String)} if possible as this implementation is
 * not guaranteed to present in all version.
 */
@SideOnly( Side.CLIENT )
public class PatchedKeyBinding extends KeyBinding
{
	public PatchedKeyBinding(
		String description,
		IKeyConflictContext conflict_context,
		int key_code,
		ImmutableSet< Integer > cmb_keys,
		String category
	) {
		super( description, conflict_context, key_code, category );
		
		final IKeyBindingImpl kb = ( IKeyBindingImpl ) this;
		kb.initDefaultCmbKeys( cmb_keys );
	}
}
