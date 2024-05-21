package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import com.kbp.client.ModConfig;
import com.kbp.client.ShadowKeyBinding;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.IntStream;

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
		// User may create multiple shadow copies for a single key binding, so \
		// we need to count it and assign unique number for their save keys.
		final HashMap< KeyBinding, Integer > shadow_map = new HashMap<>();
		Arrays.stream( ModConfig.shadow_key_bindings )
			.map( KBPMod::findByName )
			.filter( Optional::isPresent )
			.map( Optional::get )
			.map( IPatchedKeyBinding::getKeyBinding )
			.forEachOrdered( kb -> shadow_map.compute( kb, ( k, v ) -> v != null ? v + 1 : 1 ) );
		
		shadow_map.forEach( ( kb, cnt ) -> IntStream.range( 0, cnt )
			.mapToObj( i -> (
				new ShadowKeyBinding(
					kb.getKeyDescription(),
					kb.getKeyConflictContext(),
					Keyboard.KEY_NONE,
					ImmutableSet.of(),
					kb.getKeyCategory()
				) {
					@Override
					public String getSaveKey() {
						return super.getSaveKey() + "_" + i;
					}
				}
			) )
			.forEachOrdered( ClientRegistry::registerKeyBinding )
		);
	}
}
