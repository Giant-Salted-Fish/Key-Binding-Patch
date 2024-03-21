package com.kbp.client.mixin;

import com.kbp.client.IKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

@Mixin( Options.class )
public abstract class OptionsMixin
{
	@Shadow
	@Final
	private File optionsFile;
	
	
	@Inject( method = "load", at = @At( "HEAD" ) )
	private void onLoad( CallbackInfo ci )
	{
		if ( !this.optionsFile.exists() )
		{
			// Mapping reset would not be called if options file does not exist.
			// We need to call it manually here.
			KeyMapping.resetMapping();
		}
	}
	
	@Inject(
		method = "processOptions",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/KeyMapping;setKeyModifierAndCode(Lnet/minecraftforge/client/settings/KeyModifier;Lcom/mojang/blaze3d/platform/InputConstants$Key;)V",
			remap = false,
			shift = Shift.AFTER
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onProcessOptions(
		@Coerce Object p_168428_,
		CallbackInfo ci, KeyMapping[] var2,
		int var3,
		int var4,
		KeyMapping keymapping,
		String s,
		String s1
	) {
		// This part is bit of hacky. See KeyMappingMixin#saveString().
		final String[] splits = s1.split( ":" );
		if ( splits.length < 3 ) {
			return;
		}
		
		final String cmb_keys_data = splits[ 2 ];
		if ( cmb_keys_data.isEmpty() ) {
			return;
		}
		
		final IKeyMapping km = ( IKeyMapping ) keymapping;
		final Iterator< Key > cmb_keys = Arrays
			.stream( cmb_keys_data.split( "\\+" ) )
			.map( InputConstants::getKey )
			.iterator();
		km.setKeyAndCmbKeys( km.getKeyMapping().getKey(), cmb_keys );
	}
}
