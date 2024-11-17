package com.kbp.client.mixin;

import com.kbp.client.impl.IKeyBindingImpl;
import net.minecraft.client.gui.widget.list.KeyBindingList;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Mixin( KeyBindingList.class )
public abstract class KeyBindingListMixin
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
			final KeyBinding kb = ( KeyBinding ) o;
			return IKeyBindingImpl.getShadowTarget( kb ).orElse( kb );
		} ) );
	}
	
	@Redirect(
		method = "<init>",
		at = @At(
			value = "NEW",
			target = "(Ljava/lang/String;)Lnet/minecraft/util/text/TranslationTextComponent;",
			ordinal = 1
		)
	)
	private TranslationTextComponent onNew$Invoke( String name )
	{
		return (
			IKeyBindingImpl.getShadowTarget( name )
			.map( raw -> ( TranslationTextComponent ) new TranslationTextComponent( raw ) {
				private final ITextProperties prefix = ITextProperties.of( "*" );
				
				@Nonnull
				@Override
				public < T > Optional< T > visitSelf( @Nonnull ITextAcceptor< T > visitor )
				{
					final Optional< T > result = this.prefix.visit( visitor );
					return result.isPresent() ? result : super.visitSelf( visitor );
				}
				
				@Nonnull
				@Override
				public < T > Optional< T > visitSelf(
					@Nonnull IStyledTextAcceptor< T > visitor,
					@Nonnull Style style
				) {
					final Optional< T > result = this.prefix.visit( visitor, style );
					return result.isPresent() ? result : super.visitSelf( visitor, style );
				}
			} )
			.orElseGet( () -> new TranslationTextComponent( name ) )
		);
	}
}
