package com.kbp.client;

import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.api.KeyMappingBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

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
			DisplayTest.class,
			() -> new DisplayTest(
				() -> "This is a client only mod.",
				( remote_version_string, network_bool ) -> network_bool
			)
		);
	}
	
	/**
	 * Use should use this to retrieve {@link IPatchedKeyMapping} interface from
	 * {@link KeyMapping} instances because there is no guarantee that
	 * {@link KeyMapping} will always implement {@link IPatchedKeyMapping} in
	 * the future.
	 */
	public static IPatchedKeyMapping getPatched( KeyMapping key_binding ) {
		return ( IPatchedKeyMapping ) key_binding;
	}
	
	/**
	 * Convenient builder for creating normal key mappings.
	 *
	 * @see #newToggleableBuilder(String, BooleanSupplier)
	 */
	public static KeyMappingBuilder newBuilder( String description )
	{
		return new KeyMappingBuilder()
		{
			@Override
			public IPatchedKeyMapping build()
			{
				return new PatchedKeyMapping(
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
	 * Convenient builder for creating toggleable key mappings.
	 *
	 * @see #newBuilder(String)
	 */
	public static KeyMappingBuilder newToggleableBuilder(
		String description, BooleanSupplier toggle_controller
	) {
		return new KeyMappingBuilder()
		{
			@Override
			public IPatchedKeyMapping build()
			{
				final Key default_key = this.key;
				final PatchedToggleableKeyMapping tkm = new PatchedToggleableKeyMapping(
					description,
					InputConstants.UNKNOWN.getValue(),
					this.cmb_keys,
					this.category,
					toggle_controller
				) {
					@Override
					@Nonnull
					public Key getDefaultKey() {
						return default_key;
					}
				};
				tkm.setKeyConflictContext( this.conflict_context );
				return tkm;
			}
		};
	}
}
