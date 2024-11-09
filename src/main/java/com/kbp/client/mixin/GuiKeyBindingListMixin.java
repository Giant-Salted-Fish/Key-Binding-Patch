package com.kbp.client.mixin;

import com.kbp.client.impl.ShadowKeyBinding;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Comparator;

@Mixin( GuiKeyBindingList.class )
public abstract class GuiKeyBindingListMixin
{
	// TODO: Maybe correct KeyBinding#compareTo(...)?
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
			if ( o instanceof ShadowKeyBinding )
			{
				final ShadowKeyBinding skb = ( ShadowKeyBinding ) o;
				return skb.target;
			}
			else {
				return ( KeyBinding ) o;
			}
		} ) );
	}
	
	@Redirect(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/I18n;format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"
		)
	)
	private String onNew$Invoke( String raw_key, Object[] args )
	{
		return (
			ShadowKeyBinding.getRawDescription( raw_key )
			.map( key -> "*" + I18n.format( key, args ) )
			.orElseGet( () -> I18n.format( raw_key, args ) )
		);
	}
}
