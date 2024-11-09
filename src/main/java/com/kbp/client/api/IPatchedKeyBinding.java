package com.kbp.client.api;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @see KBPMod#getPatched(KeyBinding)
 * @see KBPMod#findByName(String)
 */
@SideOnly( Side.CLIENT )
public interface IPatchedKeyBinding
{
	default ImmutableSet< Integer > getDefaultCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default ImmutableSet< Integer > getCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default void setKeyAndCmbKeys( int key, ImmutableSet< Integer > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	void addPressCallback( Runnable callback );
	
	boolean removePressCallback( Runnable callback );
	
	void addReleaseCallback( Runnable callback );
	
	boolean removeReleaseCallback( Runnable callback );
	
	default void pressKey() {
		throw new UnsupportedOperationException();
	}
	
	default void releaseKey() {
		throw new UnsupportedOperationException();
	}
	
	default KeyBinding getKeyBinding() {
		throw new UnsupportedOperationException();
	}
}
