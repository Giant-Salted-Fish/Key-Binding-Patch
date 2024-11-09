package com.kbp.client.mixin;

import com.kbp.client.impl.ShadowKeyBinding;
import net.minecraft.client.gui.GuiKeyBindingList.KeyEntry;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
		return (
			ShadowKeyBinding.getRawDescription( raw_key )
			.map( key -> "*" + I18n.format( key, args ) )
			.orElseGet( () -> I18n.format( raw_key, args ) )
		);
	}
}
