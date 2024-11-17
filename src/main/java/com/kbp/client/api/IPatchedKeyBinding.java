package com.kbp.client.api;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.IForgeKeybinding;

/**
 * @see KBPMod#getPatched(KeyBinding)
 * @see KBPMod#findByName(String)
 */
@OnlyIn( Dist.CLIENT )
public interface IPatchedKeyBinding extends IForgeKeybinding
{
	default ImmutableSet< Input > getDefaultCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default ImmutableSet< Input > getCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default void setKeyAndCmbKeys( Input key, ImmutableSet< Input > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	void addPressCallback( Runnable callback );
	
	boolean removePressCallback( Runnable callback );
	
	void addReleaseCallback( Runnable callback );
	
	boolean removeReleaseCallback( Runnable callback );
}
