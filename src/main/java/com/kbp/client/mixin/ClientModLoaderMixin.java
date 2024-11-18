package com.kbp.client.mixin;

import com.kbp.client.KBPMod;
import com.kbp.client.KBPModConfig;
import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.impl.ShadowKeyMapping;
import com.kbp.client.impl.ShadowToggleKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraftforge.client.loading.ClientModLoader;
import net.minecraftforge.fml.ModWorkManager.DrivenExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Mixin( ClientModLoader.class )
public abstract class ClientModLoaderMixin
{
	@Shadow( remap = false )
	private static boolean loadingComplete;
	
	@Inject(
		method = "finishModLoading",
		remap = false,
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraftforge/client/loading/ClientModLoader;loading:Z"
		)
	)
	private static void onFinishModLoading(
		DrivenExecutor syncExecutor,
		Executor parallelExecutor,
		CallbackInfo ci
	) {
		if ( !loadingComplete ) {
			syncExecutor.execute( ClientModLoaderMixin::__createShadowKeyBindings );
		}
	}
	
	@Unique
	private static void __createShadowKeyBindings()
	{
		// User may create multiple shadow copies for a single key binding,
		// so we need to count it and assign unique number for their save keys.
		final var shadow_copies = (
			KBPModConfig.SHADOW_KEY_MAPPINGS.get().stream()
			.collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) )
			.entrySet().stream()
			.flatMap( e -> (
				KBPMod.findByName( e.getKey() )
				.map( IPatchedKeyMapping::getKeyMapping )
				.map( km -> {
					final var cnt = e.getValue().intValue();
					final var is = IntStream.range( 0, cnt );
					final IntFunction< KeyMapping > to_shadow = (
						km instanceof ToggleKeyMapping
						? i -> new ShadowToggleKeyMapping( km, i )
						: i -> new ShadowKeyMapping( km, i )
					);
					return is.mapToObj( to_shadow );
				} )
				.orElseGet( Stream::empty )
			) )
		);
		
		final var options = Minecraft.getInstance().options;
		options.keyMappings = (
			Stream.concat( Arrays.stream( options.keyMappings ), shadow_copies )
			.toArray( KeyMapping[]::new )
		);
	}
}
