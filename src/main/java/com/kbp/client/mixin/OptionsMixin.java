package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.impl.IKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Mixin( Options.class )
public abstract class OptionsMixin
{
	@Shadow
	public KeyMapping[] keyMappings;
	
	@Shadow
	@Final
	private File optionsFile;
	
	
	@Unique
	private static final KeyMapping[] EMPTY_KEY_MAPPINGS = { };
	
	
	@Inject( method = "load", at = @At( "HEAD" ) )
	private void onLoad( CallbackInfo ci )
	{
		// Mapping reset would not be called if options file does not exist. \
		// We need to call it manually here.
		if ( !this.optionsFile.exists() ) {
			KeyMapping.resetMapping();
		}
	}
	
	@Redirect(
		method = "processOptions",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;"
		)
	)
	private KeyMapping[] onProcessOptions$GetField( Options self ) {
		return EMPTY_KEY_MAPPINGS;  // Skip vanilla key mappings load/save.
	}
	
	@Inject(
		method = "save",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Options;processOptions(Lnet/minecraft/client/Options$FieldAccess;)V",
			shift = Shift.AFTER
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onSave( CallbackInfo ci, PrintWriter writer )
	{
		Arrays.stream( this.keyMappings )
			.map( km -> {
				final var ikm = ( IKeyMapping ) km;
				final var save_key = "key_" + ikm.getSaveKey();
				final var key = km.getKey().getName();
				final var modifier = KeyModifier.NONE.toString();
				final var cmb_keys = (
					ikm.getCmbKeys().stream()
					.map( Key::getName )
					.collect( Collectors.joining( "+" ) )
				);
				// This format is design to be compatible with vanilla key \
				// saving, so that player can still have their key settings \
				// after removing this mod.
				return String.join( ":", save_key, key, modifier, cmb_keys );
			} )
			.forEachOrdered( writer::println );
	}
	
	@Inject(
		method = "load",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Options;processOptions(Lnet/minecraft/client/Options$FieldAccess;)V",
			shift = Shift.AFTER
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onLoad(
		CallbackInfo ci,
		CompoundTag compoundtag,
		BufferedReader bufferedreader,
		CompoundTag compoundtag1
	) {
		Arrays.stream( this.keyMappings )
			.map( IKeyMapping.class::cast )
			.filter( ikm -> compoundtag1.contains( "key_" + ikm.getSaveKey() ) )
			.forEach( ikm -> {
				final var data = compoundtag1.getString( "key_" + ikm.getSaveKey() );
				final var split = data.split( ":" );
				final var key = InputConstants.getKey( split[ 0 ] );
				final var cmb_keys = (
					split.length > 2
					? Arrays.stream( split[ 2 ].split( "\\+" ) )
						.map( InputConstants::getKey )
						.collect( ImmutableSet.toImmutableSet() )
					: ImmutableSet.< Key >of()
				);
				ikm.setKeyAndCmbKeys( key, cmb_keys );
			} );
	}
}
