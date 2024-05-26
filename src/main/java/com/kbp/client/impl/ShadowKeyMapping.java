package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;

/**
 * Only used by internal shadow key bindings.
 */
@OnlyIn( Dist.CLIENT )
public final class ShadowKeyMapping extends PatchedKeyMapping implements IKeyMapping
{
	private final int index;
	
	public ShadowKeyMapping(
		String description,
		IKeyConflictContext key_conflict_context,
		Key key,
		ImmutableSet< Key > cmb_keys,
		String category, int index
	) {
		super( description, key_conflict_context, key, cmb_keys, category );
		
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
