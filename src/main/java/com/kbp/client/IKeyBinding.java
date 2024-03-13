package com.kbp.client;

import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;

/**
 * Only use {@link IPatchedKeyBinding} unless you know what you are doing. This
 * interface is not guarantee to be stable between versions.
 */
@SideOnly( Side.CLIENT )
public interface IKeyBinding extends IPatchedKeyBinding
{
	void incrPressTime();
	
	void setDefaultCmbKeys( Iterator< Integer > cmb_keys );
}
