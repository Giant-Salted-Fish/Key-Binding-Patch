package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.api.KeyBindingBuilder;
import com.kbp.client.impl.PatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.Optional;

@Mod(
	modid = KBPMod.MODID,
	version = "1.3.3.2",
	clientSideOnly = true,
	updateJSON = "https://raw.githubusercontent.com/Giant-Salted-Fish/Key-Binding-Patch/1.16.X/update.json",
	acceptedMinecraftVersions = "[1.12,1.13)",
	guiFactory = "com.kbp.client.gui.ConfigGuiFactory",
	dependencies = "required:mixinbooter@[8.0,);"
)
@EventBusSubscriber
public final class KBPMod
{
	public static final String MODID = "key_binding_patch";
	
	/**
	 * You should use this to retrieve {@link IPatchedKeyBinding} interface from
	 * {@link KeyBinding} instances because there is no guarantee that
	 * {@link KeyBinding} will always implement {@link IPatchedKeyBinding} in
	 * the future.
	 *
	 * @see #findByName(String)
	 */
	public static IPatchedKeyBinding getPatched( KeyBinding key_binding ) {
		return ( IPatchedKeyBinding ) key_binding;
	}
	
	/**
	 * @see #getPatched(KeyBinding)
	 */
	public static Optional< IPatchedKeyBinding > findByName( String name ) {
		return Optional.ofNullable( KeyBinding$KEYBIND_ARRAY.get( name ) ).map( KBPMod::getPatched );
	}
	
	/**
	 * Convenient builder for creating key bindings.
	 */
	public static KeyBindingBuilder newBuilder( String description )
	{
		return new KeyBindingBuilder() {
			@Override
			@SuppressWarnings( "DataFlowIssue" )
			public IPatchedKeyBinding build()
			{
				return ( IPatchedKeyBinding ) new PatchedKeyBinding(
					description,
					this.conflict_context,
					this.key,
					this.cmb_keys,
					this.category
				);
			}
		};
	}
	
	
	// Internal implementations that should not be accessed by other mods.
	private static final Map< String, KeyBinding >
		KeyBinding$KEYBIND_ARRAY = ObfuscationReflectionHelper.getPrivateValue( KeyBinding.class, null, "KEYBIND_ARRAY" );
	
	@SubscribeEvent
	static void _onConfigChanged( OnConfigChangedEvent evt )
	{
		if ( evt.getModID().equals( MODID ) ) {
			ConfigManager.sync( MODID, Config.Type.INSTANCE );
		}
	}
	
	@Mod.InstanceFactory
	@SuppressWarnings( "InstantiationOfUtilityClass" )
	private static KBPMod __create() {
		return new KBPMod();
	}
	
	private KBPMod() {
	}
}
