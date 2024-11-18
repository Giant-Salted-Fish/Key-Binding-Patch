package com.kbp.client.mixin;

import com.kbp.client.impl.IKeyMappingImpl;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Comparator;

@Mixin( KeyBindsList.class )
public abstract class KeyBindsListMixin
{
	@Redirect(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/Arrays;sort([Ljava/lang/Object;)V"
		)
	)
	private void onNew$Invoke( Object[] array )
	{
		Arrays.sort( array, Comparator.comparing( o -> {
			final var km = ( KeyMapping ) o;
			return IKeyMappingImpl.getShadowTarget( km ).orElse( km );
		} ) );
	}
	
	@Redirect(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
			ordinal = 1
		)
	)
	private MutableComponent onNew$Invoke( String name )
	{
		final var opt = IKeyMappingImpl.getShadowTarget( name );
		final var comp = Component.translatable( opt.orElse( name ) );
		return opt.isPresent() ? Component.literal( "*" ).append( comp ) : comp;
	}
}
