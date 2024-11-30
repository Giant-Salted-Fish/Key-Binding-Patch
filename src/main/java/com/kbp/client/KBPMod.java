package com.kbp.client;

import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.api.KeyMappingBuilder;
import com.kbp.client.gui.KBPConfigScreen;
import com.kbp.client.impl.PatchedKeyMapping;
import com.kbp.client.impl.PatchedToggleKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

@Mod( "key_binding_patch" )
public final class KBPMod
{
	/**
	 * Use should use this to retrieve {@link IPatchedKeyMapping} interface from
	 * {@link KeyMapping} instances because there is no guarantee that
	 * {@link KeyMapping} will always implement {@link IPatchedKeyMapping} in
	 * the future.
	 *
	 * @see #findByName(String)
	 */
	public static IPatchedKeyMapping getPatched( KeyMapping key_binding ) {
		return ( IPatchedKeyMapping ) key_binding;
	}
	
	/**
	 * @see #getPatched(KeyMapping)
	 */
	public static Optional< IPatchedKeyMapping > findByName( String name ) {
		return Optional.ofNullable( KeyMapping$ALL.get( name ) ).map( KBPMod::getPatched );
	}
	
	/**
	 * Convenient builder for creating normal key mappings.
	 *
	 * @see #newToggleableBuilder(String, BooleanSupplier)
	 */
	public static KeyMappingBuilder newBuilder( String description )
	{
		return new KeyMappingBuilder() {
			@Override
			@SuppressWarnings( "DataFlowIssue" )
			public IPatchedKeyMapping build()
			{
				return ( IPatchedKeyMapping ) new PatchedKeyMapping(
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
		return new KeyMappingBuilder() {
			@Override
			@SuppressWarnings( "DataFlowIssue" )
			public IPatchedKeyMapping build()
			{
				final var default_key = this.key;
				final PatchedToggleKeyMapping tkm = new PatchedToggleKeyMapping(
					description,
					InputConstants.UNKNOWN.getValue(),
					this.cmb_keys,
					this.category,
					toggle_controller
				) {
					@NotNull
					@Override
					public Key getDefaultKey() {
						return default_key;
					}
				};
				tkm.setKeyConflictContext( this.conflict_context );
				return ( IPatchedKeyMapping ) tkm;
			}
		};
	}
	
	
	// Internal implementations that should not be accessed by other mods.
	private static final Map< String, KeyMapping > KeyMapping$ALL = Objects.requireNonNull(
		ObfuscationReflectionHelper.getPrivateValue( KeyMapping.class, null, "f_90809_" )
	);
	
	public KBPMod()
	{
		// Make sure the mod being absent on the other network side does not
		// cause the client to display the server as incompatible.
		final var load_ctx = ModLoadingContext.get();
		load_ctx.registerExtensionPoint(
			DisplayTest.class,
			() -> new DisplayTest(
				() -> "This is a client only mod.",
				( remote_version_string, network_bool ) -> network_bool
			)
		);
		
		// Setup mod config settings.
		load_ctx.registerConfig( Type.CLIENT, KBPModConfig.CONFIG_SPEC );
		load_ctx.registerExtensionPoint(
			ConfigGuiFactory.class,
			() -> new ConfigGuiFactory( ( mc, screen ) -> new KBPConfigScreen( screen ) )
		);
	}
}
