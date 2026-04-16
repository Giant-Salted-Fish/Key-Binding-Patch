package com.kbp.client.mixin;

import com.kbp.client.impl.IKeyMappingImpl;
import net.minecraft.client.gui.screens.controls.KeyBindsList.KeyEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin( KeyEntry.class )
public abstract class KeyEntryMixin
{
	@Redirect(
		method = "refreshEntry",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
			ordinal = 0
		)
	)
	private MutableComponent onRefreshEntry$Invoke( String key )
	{
		final var opt = IKeyMappingImpl.getShadowTarget( key );
		final var comp = Component.translatable( opt.orElse( key ) );
		return opt.isPresent() ? Component.literal( "*" ).append( comp ) : comp;
	}
}
