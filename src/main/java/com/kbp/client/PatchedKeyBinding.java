package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;

import java.util.Iterator;

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
		Iterator< Input > cmb_keys,
		String category
	) {
		super( description, key_conflict_context, key, category );
		
		final IKeyBinding kb = ( IKeyBinding ) this;
		kb.setDefaultCmbKeys( cmb_keys );
	}
	
	@Override
	public void setToDefault() {
		super.setToDefault();
	}
	
	@Override
	public KeyBinding getKeyBinding() {
		return super.getKeyBinding();
	}
}
