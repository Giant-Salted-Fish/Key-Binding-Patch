package com.kbp.client;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.RequiresMcRestart;

@LangKey( "kbp.config" )
@Config( modid = KBPMod.MODID )
public final class ModConfig
{
	@Comment( "These shadow key bindings allow the duplication of " )
	@LangKey( "kbp.config.shadow_key_bindings" )
	@RequiresMcRestart
	public static String[] shadow_key_bindings = { };
	
	private ModConfig() { }
}
