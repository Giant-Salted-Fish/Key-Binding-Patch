package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.impl.IKeyBindingImpl;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin( GameSettings.class )
public abstract class GameSettingsMixin
{
	@Shadow
	public KeyBinding[] keyBindings;
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Original implementation seems to be broken.
	 */
	@Overwrite
	public static boolean isKeyDown( KeyBinding key )
	{
		// Original implementation is very similar to KeyBinding#isActiveAndMatches(...),
		// except that it does not check the conflict context. Maybe it is a bug?
		final int key_code = key.getKeyCode();
		final ImmutableSet< Integer > cmb_keys = ( ( IPatchedKeyBinding ) key ).getCmbKeys();
		return (
			IKeyBindingImpl.isKeyDown( key_code )
			&& cmb_keys.stream().allMatch( IKeyBindingImpl::isKeyDown )
		);
	}
	
	@Redirect(
		method = "saveOptions",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/settings/GameSettings;keyBindings:[Lnet/minecraft/client/settings/KeyBinding;"
		)
	)
	private KeyBinding[] onSaveOptions$GetField( GameSettings self ) {
		return new KeyBinding[ 0 ];  // Skip vanilla key bindings save.
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
				final IPatchedKeyBinding ikb = ( IPatchedKeyBinding ) kb;
				final String save_key = "key_" + kb.getKeyDescription();
				final String key = Integer.toString( kb.getKeyCode() );
				final String modifier = kb.getKeyModifier().toString();
				final String cmb_keys = (
					ikb.getCmbKeys().stream()
					.map( Object::toString )
					.collect( Collectors.joining( "+" ) )
				);
				// This format is design to be compatible with vanilla key
				// saving, so that player can still have their key settings
				// after removing this mod.
				return String.join( ":", save_key, key, modifier, cmb_keys );
			} )
			.forEachOrdered( writer::println );
	}
	
	@Redirect(
		method = "loadOptions",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/settings/GameSettings;keyBindings:[Lnet/minecraft/client/settings/KeyBinding;"
		)
	)
	private KeyBinding[] onLoadOptions$GetField( GameSettings self ) {
		return new KeyBinding[ 0 ];  // Skip vanilla key bindings load.
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
		String s1,  // <key>
		String s2  // <value>
	) {
		final String save_key = s1.substring( 4 );
		Arrays.stream( this.keyBindings )
			.filter( kb -> kb.getKeyDescription().equals( save_key ) )
			.map( IPatchedKeyBinding.class::cast )
			.forEach( ikb -> {
				final String[] split = s2.split( ":" );
				final int key_code = Integer.parseInt( split[ 0 ] );
				final ImmutableSet< Integer > cmb_keys;
				if ( split.length > 2 )
				{
					cmb_keys = (
						Arrays.stream( split[ 2 ].split( "\\+" ) )
						.map( Integer::parseInt )
						.collect( ImmutableSet.toImmutableSet() )
					);
				}
				else {
					cmb_keys = ImmutableSet.of();
				}
				ikb.setKeyAndCmbKeys( key_code, cmb_keys );
			} );
	}
}
