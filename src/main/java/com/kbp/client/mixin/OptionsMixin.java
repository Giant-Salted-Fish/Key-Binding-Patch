package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.impl.IKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.Options.FieldAccess;
import net.minecraftforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin( Options.class )
public abstract class OptionsMixin
{
	@Shadow
	@Final
	private File optionsFile;
	
	@Shadow
	public KeyMapping[] keyMappings;
	
	
	@Unique
	private static final KeyMapping[] EMPTY_KEY_MAPPINGS = { };
	
	
	@Inject( method = "load(Z)V", remap = false, at = @At( "HEAD" ) )
	private void onLoad( CallbackInfo ci )
	{
		// Mapping reset would not be called if options file does not exist.
		// We need to call it manually here.
		if ( !this.optionsFile.exists() ) {
			KeyMapping.resetMapping();
		}
	}
	
	@Redirect(
		method = "processOptionsForge",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;"
		)
	)
	private KeyMapping[] onProcessOptionsForge$GetField( Options self ) {
		return EMPTY_KEY_MAPPINGS;  // Cancel vanilla key mapping load/save.
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
			.map( IKeyMapping.class::cast )
			.map( ikm -> {
				final var km = ikm.getKeyMapping();
				final var key = km.getKey().getName();
				final var modifier = KeyModifier.NONE.toString();
				final var cmb_keys = (
					ikm.getCmbKeys().stream()
					.map( Key::getName )
					.collect( Collectors.joining( "+" ) )
				);
				// This format is design to be compatible with vanilla key saving, \
				// so that player can still have their key settings after removing \
				// this mod.
				final var data = String.join( ":", key, modifier, cmb_keys );
				return Pair.of( ikm, data );
			} )
			.map( p -> {
				final var ikm = p.getFirst();
				final var save_key = "key_" + ikm.getSaveKey();
				final var save_data = p.getSecond();
				final var read_data = access.process( save_key, save_data );
				final var data = (
					read_data.equals( save_data )
					? Optional.< String >empty()
					: Optional.of( read_data )
				);
				return Pair.of( ikm, data );
			} )
			.filter( p -> p.getSecond().isPresent() )
			.forEachOrdered( p -> {
				final var ikm = p.getFirst();
				final var data = p.getSecond().get();
				final var splits = data.split( ":" );
				final var key = InputConstants.getKey( splits[ 0 ] );
				final var cmb_keys = (
					splits.length > 2
					? Arrays.stream( splits[ 2 ].split( "\\+" ) )
						.map( InputConstants::getKey )
						.collect( ImmutableSet.toImmutableSet() )
					: ImmutableSet.< Key >of()
				);
				ikm.setKeyAndCmbKeys( key, cmb_keys );
			} );
	}
}
