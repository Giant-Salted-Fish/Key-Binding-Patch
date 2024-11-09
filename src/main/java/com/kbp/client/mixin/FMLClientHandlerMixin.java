package com.kbp.client.mixin;

import com.kbp.client.KBPMod;
import com.kbp.client.KBPModConfig;
import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.impl.ShadowKeyBinding;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Mixin( FMLClientHandler.class )
public abstract class FMLClientHandlerMixin
{
	@Inject(
		method = "finishMinecraftLoading",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/settings/GameSettings;loadOptions()V"
		)
	)
	private void onFinishMinecraftLoading( CallbackInfo ci )
	{
		// User may create multiple shadow copies for a single key binding, so
		// we need to count it and assign unique number for their save keys.
		Arrays.stream( KBPModConfig.shadow_key_bindings )
			.collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) )
			.entrySet().stream()
			.flatMap( e -> (
				KBPMod.findByName( e.getKey() )
				.map( IPatchedKeyBinding::getKeyBinding )
				.map( kb -> {
					final int cnt = e.getValue().intValue();
					final IntStream is = IntStream.range( 0, cnt );
					return is.mapToObj( i -> new ShadowKeyBinding( kb, i ) );
				} )
				.orElseGet( Stream::empty )
			) )
			.forEachOrdered( ClientRegistry::registerKeyBinding );
	}
}
