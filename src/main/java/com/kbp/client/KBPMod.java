package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.api.KeyBindingBuilder;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;

@Mod(
	modid = "key_binding_patch",
	version = "1.12.2-1.2.0.0",
	clientSideOnly = true,
	updateJSON = "https://raw.githubusercontent.com/Giant-Salted-Fish/Key-Binding-Patch/1.16.X/update.json",
	acceptedMinecraftVersions = "[1.12,1.13)",
	dependencies = "required:mixinbooter@[8.0,);"
)
public final class KBPMod
{
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
	 * Convenient builder for creating key bindings.
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
}
