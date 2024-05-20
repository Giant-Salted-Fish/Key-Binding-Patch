package com.kbp.client;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Only used by internal shadow key bindings.
 */
@SideOnly( Side.CLIENT )
public class ShadowKeyBinding extends PatchedKeyBinding implements IKeyBinding
{
	public ShadowKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		int key,
		ImmutableSet< Integer > cmb_keys,
		String category
	) {
		super( description, key_conflict_context, key, cmb_keys, category );
	}
}
