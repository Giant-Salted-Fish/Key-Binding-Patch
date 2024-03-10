package com.kbp.client;

import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;

/**
 * Only use {@link IPatchedKeyMapping} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@OnlyIn( Dist.CLIENT )
public interface IKeyMapping extends IPatchedKeyMapping
{
	void incrClickCount();
	
	void setDefaultCmbKeys( Iterator< Key > cmb_keys );
}
