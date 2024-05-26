package com.kbp.client.mixin;

import com.kbp.client.KBPMod;
import com.kbp.client.KBPModConfig;
import com.kbp.client.impl.IKeyBinding;
import net.minecraftforge.fml.ModWorkManager.DrivenExecutor;
import net.minecraftforge.fml.client.ClientModLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mixin( ClientModLoader.class )
public abstract class ClientModLoaderMixin
{
	@Unique
	private static boolean is_shadow_created = false;
	
	@Inject(
		method = "finishModLoading",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraftforge/fml/ModWorkManager$DrivenExecutor;execute(Ljava/lang/Runnable;)V"
		),
		remap = false
	)
	private static void onFinishModLoading(
		DrivenExecutor syncExecutor,
		Executor parallelExecutor,
		CallbackInfo ci
	) {
		if ( is_shadow_created ) {
			return;
		}
		
		syncExecutor.execute( ClientModLoaderMixin::__createShadowKeyBindings );
		is_shadow_created = true;
	}
	
	@Unique
	private static void __createShadowKeyBindings()
	{
		// User may create multiple shadow copies for a single key binding, \
		// so we need to count it and assign unique number for their save keys.
		KBPModConfig.SHADOW_KEY_BINDINGS.get().stream()
			.map( KBPMod::findByName )
			.filter( Optional::isPresent )
			.map( Optional::get )
			.map( IKeyBinding.class::cast )
			.collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) )
			.entrySet().stream()
			.flatMap( e -> {
				final IKeyBinding ikb = e.getKey();
				final int cnt = e.getValue().intValue();
				return IntStream.range( 0, cnt ).mapToObj( ikb::createShadowCopy );
			} )
			.forEachOrdered( ClientRegistry::registerKeyBinding );
	}
}
