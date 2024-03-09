package com.kbp.client.mixin;

import com.kbp.client.IKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

@Mixin( GameSettings.class )
public class GameSettingsMixin
{
	@Shadow
	public KeyBinding[] keyMappings;
	
	@Final
	@Shadow
	private File optionsFile;
	
	
	@Inject( method = "load", at = @At( "HEAD" ) )
	public void onLoad( CallbackInfo ci )
	{
		if ( !this.optionsFile.exists() )
		{
			// Mapping reset would not be called if options file does not exist.
			// We need to call it manually here.
			KeyBinding.resetMapping();
		}
	}
	
	@Inject(
		method = "load",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/settings/KeyBinding;resetMapping()V"
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void onLoad(
		CallbackInfo ci,
		CompoundNBT compoundnbt,
		CompoundNBT compoundnbt1
	) {
		// Build a hashmap to speedup key binding lookup.
		final HashMap< String, IKeyBinding > lookup_table = new HashMap<>();
		Arrays.stream( this.keyMappings ).forEach( kb -> lookup_table.put( "key_" + kb.getName(), ( IKeyBinding ) kb ) );
		
		compoundnbt1.getAllKeys().stream()
		.filter( lookup_table::containsKey )
		.forEach( k -> {
			// This part is a little hacky. See KeyBindingMixin#saveString().
			final String save_data = compoundnbt1.getString( k );
			final String[] splits = save_data.split( ":" );
			if ( splits.length < 3 ) {
				return;
			}
			
			final String cmb_keys_data = splits[ 2 ];
			if ( cmb_keys_data.isEmpty() ) {
				return;
			}
			
			final IKeyBinding kb = lookup_table.get( k );
			final Iterator< Input > cmb_keys = Arrays
				.stream( cmb_keys_data.split( "\\+" ) )
				.map( InputMappings::getKey )
				.iterator();
			kb.setKeyAndCmbKeys( kb.getKey(), cmb_keys );
		} );
	}
}
