package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Only used by internal shadow key bindings.
 */
@SideOnly( Side.CLIENT )
public final class ShadowKeyBinding extends PatchedKeyBinding implements IKeyBinding
{
	private final int index;
	
	public ShadowKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		int key,
		ImmutableSet< Integer > cmb_keys,
		String category,
		int index
	) {
		super( description, key_conflict_context, key, cmb_keys, category );
		
		this.index = index;
	}
	
	@Override
	public String getSaveKey() {
		return this.getKeyDescription() + "_" + index;
	}
	
	@Override
	public boolean isShadowKeyBinding() {
		return true;
	}
}
