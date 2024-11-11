package com.kbp.client.mixin;

import com.kbp.client.impl.IKeyBindingImpl;
import net.minecraft.client.gui.GuiKeyBindingList.KeyEntry;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin( KeyEntry.class )
public abstract class KeyEntryMixin
{
	@Redirect(
		method = "<init>(Lnet/minecraft/client/gui/GuiKeyBindingList;Lnet/minecraft/client/settings/KeyBinding;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/I18n;format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"
		)
	)
	private String onInit$Invoke( String raw_key, Object[] args )
	{
		final Optional< String > opt = IKeyBindingImpl.getShadowTarget( raw_key );
		final String localized = I18n.format( opt.orElse( raw_key ), args );
		return opt.isPresent() ? "*" + localized : localized;
	}
}
