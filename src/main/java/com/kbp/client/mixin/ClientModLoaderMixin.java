package com.kbp.client.mixin;

import com.kbp.client.KBPMod;
import com.kbp.client.KBPModConfig;
import com.kbp.client.impl.ShadowKeyBinding;
import com.kbp.client.impl.ShadowToggleableKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.ToggleableKeyBinding;
import net.minecraftforge.client.extensions.IForgeKeybinding;
import net.minecraftforge.fml.ModWorkManager.DrivenExecutor;
import net.minecraftforge.fml.client.ClientModLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraftforge/fml/client/ClientModLoader;loading:Z"
		),
		remap = false
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
		KBPModConfig.SHADOW_KEY_BINDINGS.get().stream()
			.collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) )
			.entrySet().stream()
			.flatMap( e -> (
				KBPMod.findByName( e.getKey() )
				.map( IForgeKeybinding::getKeyBinding )
				.map( kb -> {
					final int cnt = e.getValue().intValue();
					final IntStream is = IntStream.range( 0, cnt );
					final IntFunction< KeyBinding > to_shadow = (
						kb instanceof ToggleableKeyBinding
						? i -> new ShadowToggleableKeyBinding( kb, i )
						: i -> new ShadowKeyBinding( kb, i )
					);
					return is.mapToObj( to_shadow );
				} )
				.orElseGet( Stream::empty )
			) )
			.forEachOrdered( ClientRegistry::registerKeyBinding );
	}
}
