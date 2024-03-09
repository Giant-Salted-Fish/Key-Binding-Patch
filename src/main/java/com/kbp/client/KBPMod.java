package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.api.KeyBindingBuilder;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;

@Mod( "key_binding_patch" )
public final class KBPMod
{
	public KBPMod()
	{
		// Make sure the mod being absent on the other network side does not \
		// cause the client to display the server as incompatible.
		ModLoadingContext.get().registerExtensionPoint(
			ExtensionPoint.DISPLAYTEST,
			() -> Pair.of(
				() -> "This is a client only mod.",
				( remote_version_string, network_bool ) -> network_bool
			)
		);
	}
	
	/**
	 * You should use this to retrieve {@link IPatchedKeyBinding} interface from
	 * {@link KeyBinding} instances because there is no guarantee that
	 * {@link KeyBinding} will always implement {@link IPatchedKeyBinding} in
	 * the future.
	 */
	public static IPatchedKeyBinding getPatched( KeyBinding key_binding ) {
		return ( IPatchedKeyBinding ) key_binding;
	}
	
	/**
	 * Convenient builder for creating normal key bindings.
	 *
	 * @see #newToggleableBuilder(String, BooleanSupplier)
	 */
	public static KeyBindingBuilder newBuilder( String description )
	{
		return new KeyBindingBuilder()
		{
			@Override
			public IPatchedKeyBinding build()
			{
				return new PatchedKeyBinding(
					description,
					this.conflict_context,
					this.key,
					this.cmb_keys,
					this.category
				);
			}
		};
	}
	
	/**
	 * Convenient builder for creating toggleable key bindings.
	 *
	 * @see #newBuilder(String)
	 */
	public static KeyBindingBuilder newToggleableBuilder(
		String description, BooleanSupplier toggle_controller
	) {
		return new KeyBindingBuilder()
		{
			@Override
			public IPatchedKeyBinding build()
			{
				final Input default_key = this.key;
				final PatchedToggleableKeyBinding tkb = new PatchedToggleableKeyBinding(
					description,
					InputMappings.UNKNOWN.getValue(),
					this.cmb_keys,
					this.category,
					toggle_controller
				) {
					@Nonnull
					@Override
					public Input getDefaultKey() {
						return default_key;
					}
				};
				tkb.setKeyConflictContext( this.conflict_context );
				return ( IKeyBinding ) tkb.getKeyBinding();
			}
		};
	}
}
