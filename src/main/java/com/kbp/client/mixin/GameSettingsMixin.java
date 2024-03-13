package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kbp.client.IKeyBinding;
import com.kbp.client.api.IPatchedKeyBinding;
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
		method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V",
		at = @At( "RETURN" )
	)
	private void onNew( Minecraft mcIn, File mcDataDir, CallbackInfo ci ) {
		this.key_bindings_file = new File( mcDataDir, "key_bindings.json" );
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
		Arrays.stream( this.keyBindings ).forEach( kb -> {
			final IKeyBinding ikb = ( IKeyBinding ) kb;
			final JsonArray key_data = new JsonArray();
			key_data.add( kb.getKeyCode() );
			ikb.getCmbKeys().forEach( key_data::add );
			data.add( kb.getKeyDescription(), key_data );
		} );
		
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
		
		// Build a hashmap to speedup key binding lookup.
		// KeyBinding.class does have such table already, but it is private.
		final HashMap< String, IKeyBinding > lookup_table = new HashMap<>();
		Arrays.stream( this.keyBindings ).forEach( kb -> lookup_table.put( kb.getKeyDescription(), ( IKeyBinding ) kb ) );
		
		data.entrySet().stream()
		.filter( p -> lookup_table.containsKey( p.getKey() ) )
		.forEach( p -> {
			final IKeyBinding kb = lookup_table.get( p.getKey() );
			final JsonArray key_data = p.getValue().getAsJsonArray();
			final int key_code = key_data.get( 0 ).getAsInt();
			final Iterator< Integer > cmb_keys = IntStream.range( 1, key_data.size() )
				.mapToObj( key_data::get )
				.map( JsonElement::getAsInt )
				.iterator();
			kb.setKeyAndCmbKeys( key_code, cmb_keys );
		} );
	}
}
