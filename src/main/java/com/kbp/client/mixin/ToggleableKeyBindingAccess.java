package com.kbp.client.mixin;

import net.minecraft.client.settings.ToggleableKeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BooleanSupplier;

@Mixin( ToggleableKeyBinding.class )
public interface ToggleableKeyBindingAccess
{
	@Accessor
	BooleanSupplier getNeedsToggle();
}
