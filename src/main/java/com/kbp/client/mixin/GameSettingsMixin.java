package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kbp.client.IKeyBinding;
import com.kbp.client.api.IPatchedKeyBinding;
import com.mojang.realmsclient.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.IntStream;

@Mixin( GameSettings.class )
public abstract class GameSettingsMixin
{
	@Shadow
	@Final
	private static Gson GSON;
	
	@Shadow
	public KeyBinding[] keyBindings;
	
	
	@Unique
	private File key_bindings_file;
	
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static boolean isKeyDown( KeyBinding key )
	{
		// TODO: May need to double check this.
		final int key_code = key.getKeyCode();
		final ImmutableSet< Integer > cmb_keys = ( ( IPatchedKeyBinding ) key ).getCmbKeys();
		
		final Predicate< Integer > pressing = code -> {
			final boolean is_valid_key = code != Keyboard.KEY_NONE && code < 256;
			return is_valid_key && ( code < 0 ? Mouse.isButtonDown( code + 100 ) : Keyboard.isKeyDown( code ) );
		};
		return pressing.test( key_code ) && cmb_keys.stream().allMatch( pressing );
	}
	
	@Inject(
		method = "saveOptions",
		at = @At(
			value = "INVOKE",
			target = "Lorg/apache/commons/io/IOUtils;closeQuietly(Ljava/io/Writer;)V",
			remap = false
		)
	)
	private void onSaveOptions( CallbackInfo info )
	{
		// It is really hard to inject original logic for saving the key \
		// bindings. So instead, we do it in a separate file.
		final JsonObject data = new JsonObject();
		Arrays.stream( this.keyBindings )
			.map( kb -> {
				final IKeyBinding ikb = ( IKeyBinding ) kb;
				final JsonArray key_data = new JsonArray();
				key_data.add( kb.getKeyCode() );
				ikb.getCmbKeys().forEach( key_data::add );
				return Pair.of( ikb.getSaveKey(), key_data );
			} )
			.forEachOrdered( p -> data.add( p.first(), p.second() ) );
		
		final String json_str = GSON.toJson( data );
		try ( FileWriter out = new FileWriter( this.key_bindings_file ) ) {
			out.write( json_str );
		}
		catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}
	
	@Inject(
		method = "loadOptions",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/settings/KeyBinding;resetKeyBindingArrayAndHash()V"
		)
	)
	private void onLoadOptions( CallbackInfo info )
	{
		// Because #loadOptions(...) is called in constructor, we can not \
		// init #key_bindings_file in the constructor.
		if ( this.key_bindings_file == null )
		{
			final File mc_data_dir = Minecraft.getMinecraft().gameDir;
			this.key_bindings_file = new File( mc_data_dir, "key_bindings.json" );
		}
		
		if ( !this.key_bindings_file.exists() ) {
			return;
		}
		
		JsonObject data;
		try ( FileReader in = new FileReader( this.key_bindings_file ) )
		{
			// TODO: Handle mal-formatted json data?
			data = GSON.fromJson( in, JsonObject.class );
		}
		catch ( IOException e ) {
			throw new RuntimeException( e );
		}
		
		Arrays.stream( this.keyBindings ).forEach( kb -> {
			final IKeyBinding ikb = ( IKeyBinding ) kb;
			final JsonArray key_arr = data.getAsJsonArray( ikb.getSaveKey() );
			if ( key_arr == null )
			{
				// For newly created shadow key bindings, they will not have \
				// any corresponding save data, but vanilla will set them with \
				// they target key binding's data, hence we need to create it \
				// here to ensure correctness.
				kb.setToDefault();
				return;
			}
			
			final Iterator< JsonElement > key_itr = key_arr.iterator();
			final int key_code = key_itr.next().getAsInt();
			final Iterator< Integer > cmb_keys = Iterators.transform( key_itr, JsonElement::getAsInt );
			ikb.setKeyAndCmbKeys( key_code, ImmutableSet.copyOf( cmb_keys ) );
		} );
	}
}
