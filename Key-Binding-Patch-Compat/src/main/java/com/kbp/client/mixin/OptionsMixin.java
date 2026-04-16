package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.Options.FieldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Optional;

@Mixin( Options.class )
public abstract class OptionsMixin
{
	@Shadow
	public KeyMapping[] keyMappings;
	
	
	@Redirect(
		method = "processOptionsForge",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;"
		)
	)
	private KeyMapping[] onProcessOptionsForge$GetField( Options self ) {
		return new KeyMapping[ 0 ];  // Cancel vanilla key mapping load/save.
	}
	
	@Inject(
		method = "processOptionsForge",
		remap = false,
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/sounds/SoundSource;values()[Lnet/minecraft/sounds/SoundSource;"
		)
	)
	private void onProcessOptionsForge( FieldAccess access, CallbackInfo ci )
	{
		Arrays.stream( this.keyMappings )
			.map( km -> {
				final var save_key = "key_" + km.getName();
				final var save_data = km.saveString();
				final var read_data = access.process( save_key, save_data );
				return (
					read_data.equals( save_data )
					? Optional.< Pair< KeyMapping, String > >empty()
					: Optional.of( Pair.of( km, read_data ) )
				);
			} )
			.filter( Optional::isPresent )
			.map( Optional::get )
			.forEachOrdered( p -> {
				final var data = p.getSecond();
				final var splits = data.split( ":" );
				final var key = InputConstants.getKey( splits[ 0 ] );
				final ImmutableSet< Key > cmb_keys;
				if ( splits.length > 2 )
				{
					cmb_keys = (
						Arrays.stream( splits[ 2 ].split( "\\+" ) )
						.map( InputConstants::getKey )
						.collect( ImmutableSet.toImmutableSet() )
					);
				}
				else {
					cmb_keys = ImmutableSet.of();
				}
				
				final var ikm = ( IPatchedKeyMapping ) p.getFirst();
				ikm.setKeyAndCmbKeys( key, cmb_keys );
			} );
	}
}
