package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;

/**
 * Only use {@link IPatchedKeyBinding} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@OnlyIn( Dist.CLIENT )
public interface IKeyBinding extends IPatchedKeyBinding
{
	void incrClickCount();
	
	void setDefaultCmbKeys( Iterator< Input > cmb_keys );
}
