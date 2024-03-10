package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.ToggleableKeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;
import java.util.function.BooleanSupplier;

/**
 * Use {@link KBPMod#newToggleableBuilder(String, BooleanSupplier)} if possible
 * as this implementation is not guaranteed to present in all version.
 */
@OnlyIn( Dist.CLIENT )
public class PatchedToggleableKeyBinding extends ToggleableKeyBinding implements IPatchedKeyBinding
{
	public PatchedToggleableKeyBinding(
		String description,
		int key_code,
		Iterator< Input > cmb_keys,
		String category,
		BooleanSupplier toggle_controller
	) {
		super( description, key_code, category, toggle_controller );
		
		final IKeyBinding kb = ( IKeyBinding ) this;
		kb.setDefaultCmbKeys( cmb_keys );
	}
	
	@Override
	public KeyBinding getKeyBinding() {
		return super.getKeyBinding();
	}
}
