package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.IKeyBinding;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin( GameSettings.class )
public abstract class GameSettingsMixin
{
	@Shadow
	public KeyBinding[] keyBindings;
	
	@Shadow
	private File optionsFile;
	
	
	@Unique
	private static final KeyBinding[] EMPTY_KEY_BINDINGS = { };
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static boolean isKeyDown( KeyBinding key )
	{
		// TODO: May need to double check this.
		final Predicate< Integer > pressing = code -> {
			final boolean is_valid_key = code != Keyboard.KEY_NONE && code < 256;
			return is_valid_key && ( code < 0 ? Mouse.isButtonDown( code + 100 ) : Keyboard.isKeyDown( code ) );
		};
		
		final int key_code = key.getKeyCode();
		final ImmutableSet< Integer > cmb_keys = ( ( IPatchedKeyBinding ) key ).getCmbKeys();
		return pressing.test( key_code ) && cmb_keys.stream().allMatch( pressing );
	}
	
	@Redirect(
		method = "saveOptions",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/settings/GameSettings;keyBindings:[Lnet/minecraft/client/settings/KeyBinding;"
		)
	)
	private KeyBinding[] onSaveOptions$GetField( GameSettings self ) {
		return EMPTY_KEY_BINDINGS;  // Skip vanilla key bindings save.
	}
	
	@Inject(
		method = "saveOptions",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/SoundCategory;values()[Lnet/minecraft/util/SoundCategory;"
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onSaveOptions( CallbackInfo info, PrintWriter writer )
	{
		Arrays.stream( this.keyBindings )
			.map( kb -> {
				final IKeyBinding ikb = ( IKeyBinding ) kb;
				final String save_key = "key_" + ikb.getSaveKey();
				final String key = Integer.toString( kb.getKeyCode() );
				final String modifier = KeyModifier.NONE.toString();
				final String cmb_keys = (
					ikb.getCmbKeys().stream()
					.map( Object::toString )
					.collect( Collectors.joining( "+" ) )
				);
				// This format is design to be compatible with vanilla key \
				// saving, so that player can still have their key settings \
				// after removing this mod.
				return String.join( ":", save_key, key, modifier, cmb_keys );
			} )
			.forEachOrdered( writer::println );
	}
	
	@Inject( method = "loadOptions", at = @At( "HEAD" ) )
	private void onLoadOptions( CallbackInfo ci )
	{
		// Mapping reset would not be called if options file does not exist. \
		// So we need to manually check and call it here.
		if ( !this.optionsFile.exists() ) {
			KeyBinding.resetKeyBindingArrayAndHash();
		}
	}
	
	@Redirect(
		method = "loadOptions",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/settings/GameSettings;keyBindings:[Lnet/minecraft/client/settings/KeyBinding;"
		)
	)
	private KeyBinding[] onLoadOptions$GetField( GameSettings self ) {
		return EMPTY_KEY_BINDINGS;  // Skip vanilla key bindings load.
	}
	
	@Inject(
		method = "loadOptions",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/SoundCategory;values()[Lnet/minecraft/util/SoundCategory;"
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onLoadOptions(
		CallbackInfo info,
		FileInputStream fileInputStream,
		List< String > list,
		NBTTagCompound nbttagcompound,
		Iterator< String > var4,
		String s1,
		String s2
	) {
		final String save_key = s1.substring( 4 );
		Arrays.stream( this.keyBindings )
			.map( kb -> ( IKeyBinding ) kb )
			.filter( ikb -> ikb.getSaveKey().equals( save_key ) )
			.forEach( ikb -> {
				final String[] split = s2.split( ":" );
				final int key_code = Integer.parseInt( split[ 0 ] );
				final ImmutableSet< Integer > cmb_keys = (
					split.length > 2
					? Arrays.stream( split[ 2 ].split( "\\+" ) )
						.map( Integer::parseInt )
						.collect( ImmutableSet.toImmutableSet() )
					: ImmutableSet.of()
				);
				ikb.setKeyAndCmbKeys( key_code, cmb_keys );
			} );
	}
}
