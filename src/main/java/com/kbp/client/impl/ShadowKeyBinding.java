package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;

/**
 * Only used by internal shadow key bindings.
 */
@OnlyIn( Dist.CLIENT )
public final class ShadowKeyBinding extends PatchedKeyBinding implements IKeyBinding
{
	private final int index;
	
	public ShadowKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		Input key,
		ImmutableSet< Input > cmb_keys,
		String category,
		int index
	) {
		super( description, key_conflict_context, key, cmb_keys, category );
		
		this.index = index;
	}
	
	@Override
	public String getSaveKey() {
		return this.getName() + "_" + this.index;
	}
	
	@Override
	public boolean isShadowKeyBinding() {
		return true;
	}
}
