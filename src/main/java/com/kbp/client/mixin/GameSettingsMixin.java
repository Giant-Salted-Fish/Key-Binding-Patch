package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Arrays;
import java.util.Iterator;

@Mixin( GameSettings.class )
public class GameSettingsMixin
{
	@Shadow
	public KeyBinding[] keyMappings;
	
	
	@Redirect(
		method = "load",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/GameSettings;keyMappings:[Lnet/minecraft/client/settings/KeyBinding;"
		)
	)
	private KeyBinding[] onLoad$GetField( GameSettings self ) {
		return new KeyBinding[ 0 ];  // Skip vanilla key bindings load.
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
		String s,  // <key>
		String s1  // <data>
	) {
		final String save_key = s.substring( 4 );
		Arrays.stream( this.keyMappings )
			.filter( ikb -> ikb.getName().equals( save_key ) )
			.findAny()
			.map( IPatchedKeyBinding.class::cast )
			.ifPresent( ikb -> {
				final String[] split = s1.split( ":" );
				final Input key = InputMappings.getKey( split[ 0 ] );
				final ImmutableSet< Input > cmb_keys;
				if ( split.length > 2 )
				{
					cmb_keys = (
						Arrays.stream( split[ 2 ].split( "\\+" ) )
						.map( InputMappings::getKey )
						.collect( ImmutableSet.toImmutableSet() )
					);
				}
				else {
					cmb_keys = ImmutableSet.of();
				}
				ikb.setKeyAndCmbKeys( key, cmb_keys );
			} );
	}
}
