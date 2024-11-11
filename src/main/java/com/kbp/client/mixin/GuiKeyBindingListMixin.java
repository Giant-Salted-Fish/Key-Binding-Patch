package com.kbp.client.mixin;

import com.kbp.client.impl.IKeyBindingImpl;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

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
			final KeyBinding kb = ( KeyBinding ) o;
			return IKeyBindingImpl.getShadowTarget( kb ).orElse( kb );
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
		final Optional< String > opt = IKeyBindingImpl.getShadowTarget( raw_key );
		final String localized = I18n.format( opt.orElse( raw_key ), args );
		return opt.isPresent() ? "*" + localized : localized;
	}
}
