package com.kbp.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.Collections;
import java.util.List;

@OnlyIn( Dist.CLIENT )  // TODO: Test crash on server side?
public final class KBPModConfig
{
	static final ForgeConfigSpec CONFIG_SPEC;
	public static final ConfigValue< List< ? extends String > > SHADOW_KEY_BINDINGS;
	
	static
	{
		final Builder builder = new Builder();
		SHADOW_KEY_BINDINGS = (
			builder.comment(
				"Shadow key bindings are replications of the specified key binding.",
				"This brings the ability to have multiple key setups for a single functionality."
			)
			.defineList( "shadow_key_bindings", Collections.emptyList(), String.class::isInstance )
		);
		CONFIG_SPEC = builder.build();
	}
	
	private KBPModConfig() {
	}
}
