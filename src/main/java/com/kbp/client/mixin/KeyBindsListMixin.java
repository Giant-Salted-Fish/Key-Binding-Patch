package com.kbp.client.mixin;

import com.kbp.client.impl.IKeyMappingImpl;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

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
			value = "NEW",
			target = "(Ljava/lang/String;)Lnet/minecraft/network/chat/TranslatableComponent;"
		)
	)
	private TranslatableComponent onNew$Invoke( String name )
	{
		return (
			IKeyMappingImpl.getShadowTarget( name )
			.map( raw -> ( TranslatableComponent ) new TranslatableComponent( raw ) {
				@NotNull
				@Override
				public < T > Optional< T > visitSelf( @NotNull ContentConsumer< T > visitor )
				{
					final var result = visitor.accept( "*" );
					return result.isPresent() ? result : super.visitSelf( visitor );
				}
				
				@NotNull
				@Override
				public < T > Optional< T > visitSelf( @NotNull StyledContentConsumer< T > visitor, @NotNull Style style )
				{
					final var result = visitor.accept( style, "*" );
					return result.isPresent() ? result : super.visitSelf( visitor, style );
				}
			} )
			.orElseGet( () -> new TranslatableComponent( name ) )
		);
	}
}
