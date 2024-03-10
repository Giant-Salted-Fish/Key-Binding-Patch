package com.kbp.client;

import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;

import java.util.Iterator;

/**
 * Use {@link KBPMod#newBuilder(String)} if possible as this implementation is
 * not guaranteed to present in all version.
 */
@OnlyIn( Dist.CLIENT )
public class PatchedKeyMapping extends KeyMapping implements IPatchedKeyMapping
{
	public PatchedKeyMapping(
		String description,
		IKeyConflictContext key_conflict_context,
		Key key,
		Iterator< Key > cmb_keys,
		String category
	) {
		super( description, key_conflict_context, key, category );
		
		final IKeyMapping ikb = ( IKeyMapping ) this;
		ikb.setDefaultCmbKeys( cmb_keys );
	}
}
