package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;

/**
 * Use {@link KBPMod#newBuilder(String)} if possible as this implementation is
 * not guaranteed to present in all version.
 */
@SideOnly( Side.CLIENT )
public class PatchedKeyBinding extends KeyBinding implements IPatchedKeyBinding
{
	public PatchedKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		int key,
		Iterator< Integer > cmb_keys,
		String category
	) {
		super( description, key_conflict_context, key, category );
		
		final IKeyBinding kb = ( IKeyBinding ) this;
		kb.setDefaultCmbKeys( cmb_keys );
	}
}
