package com.kbp.client.api;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;

/**
 * @see com.kbp.client.KBPMod#getPatched(KeyBinding)
 * @see com.kbp.client.KBPMod#newBuilder(String)
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
	
	default void setKeyAndCmbKeys( int key, Iterator< Integer > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	default void regisPressCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default boolean removePressCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default void regisReleaseCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default boolean removeReleaseCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
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
