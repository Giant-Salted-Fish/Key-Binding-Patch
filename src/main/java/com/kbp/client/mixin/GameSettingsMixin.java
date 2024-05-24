package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.impl.IKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

@Mixin( GameSettings.class )
public class GameSettingsMixin
{
	@Shadow
	public KeyBinding[] keyMappings;
	
	@Shadow
	@Final
	private File optionsFile;
	
	
	@Unique
	private static final KeyBinding[] EMPTY_KEY_BINDINGS = { };
	
	
	@Redirect(
		method = "save",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/GameSettings;keyMappings:[Lnet/minecraft/client/settings/KeyBinding;"
		)
	)
	private KeyBinding[] onSave$GetField( GameSettings self ) {
		return EMPTY_KEY_BINDINGS;  // Skip vanilla key bindings save.
	}
	
	@Inject(
		method = "save",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/SoundCategory;values()[Lnet/minecraft/util/SoundCategory;"
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onSave( CallbackInfo ci, PrintWriter writer )
	{
		Arrays.stream( this.keyMappings )
			.map( kb -> {
				final IKeyBinding ikb = ( IKeyBinding ) kb;
				final String save_key = "key_" + ikb.getSaveKey();
				final String key = kb.getKey().getName();
				final String modifier = KeyModifier.NONE.toString();
				final String cmb_keys = (
					ikb.getCmbKeys().stream()
					.map( Input::getName )
					.collect( Collectors.joining( "+" ) )
				);
				// This format is design to be compatible with vanilla key \
				// saving, so that player can still have their key settings \
				// after removing this mod.
				return String.join( ":", save_key, key, modifier, cmb_keys );
			} )
			.forEachOrdered( writer::println );
	}
	
	@Inject( method = "load", at = @At( "HEAD" ) )
	private void onLoad( CallbackInfo ci )
	{
		// Mapping reset would not be called if options file does not exist. \
		// So we need to manually check and call it here.
		if ( !this.optionsFile.exists() ) {
			KeyBinding.resetMapping();
		}
	}
	
	@Redirect(
		method = "load",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/GameSettings;keyMappings:[Lnet/minecraft/client/settings/KeyBinding;"
		)
	)
	private KeyBinding[] onLoad$GetField( GameSettings self ) {
		return EMPTY_KEY_BINDINGS;  // Skip vanilla key bindings load.
	}
	
	@Inject(
		method = "load",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/SoundCategory;values()[Lnet/minecraft/util/SoundCategory;"
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onLoad(
		CallbackInfo ci,
		CompoundNBT compoundnbt,
		CompoundNBT compoundnbt1,
		Iterator< String > var3,
		String s,
		String s1
	) {
		final String save_key = s.substring( 4 );
		Arrays.stream( this.keyMappings )
			.map( IKeyBinding.class::cast )
			.filter( ikb -> ikb.getSaveKey().equals( save_key ) )
			.forEach( ikb -> {
				final String[] split = s1.split( ":" );
				final Input key = InputMappings.getKey( split[ 0 ] );
				final ImmutableSet< Input > cmb_keys = (
					split.length > 2
					? Arrays.stream( split[ 2 ].split( "\\+" ) )
						.map( InputMappings::getKey )
						.collect( ImmutableSet.toImmutableSet() )
					: ImmutableSet.of()
				);
				ikb.setKeyAndCmbKeys( key, cmb_keys );
			} );
	}
}
