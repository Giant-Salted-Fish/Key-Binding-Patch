package com.kbp.client;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@MCVersion( "1.12.2" )
public final class EarlyMixinInit implements IFMLLoadingPlugin, IEarlyMixinLoader
{
	@Override
	public String[] getASMTransformerClass() {
		return new String[ 0 ];
	}
	
	@Override
	public String getModContainerClass() {
		return null;
	}
	
	@Nullable
	@Override
	public String getSetupClass() {
		return null;
	}
	
	@Override
	public void injectData( Map< String, Object > data ) {
	
	}
	
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
	
	@Override
	public List< String > getMixinConfigs() {
		return ImmutableList.of( "mixins.kbp.json" );
	}
}
