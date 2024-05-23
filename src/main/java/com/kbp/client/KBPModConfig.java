package com.kbp.client;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RequiresMcRestart;

@Config( modid = KBPMod.MODID )
public final class KBPModConfig
{
	@Comment( {
		"Shadow key bindings are replications of the specified key binding.",
		"This brings the ability to have multiple key setups for a single functionality."
	} )
	@RequiresMcRestart
	public static String[] shadow_key_bindings = { };
	
	private KBPModConfig() { }
}
