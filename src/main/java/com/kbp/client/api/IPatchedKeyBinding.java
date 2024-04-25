package com.kbp.client.api;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;

/**
 * @see KBPMod#getPatched(KeyBinding)
 * @see KBPMod#findByName(String)
 */
@OnlyIn( Dist.CLIENT )
public interface IPatchedKeyBinding
{
	default ImmutableSet< Input > getDefaultCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default ImmutableSet< Input > getCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default void setKeyAndCmbKeys( Input key, Iterator< Input > cmb_keys ) {
		throw new UnsupportedOperationException();
	}
	
	default void addPressCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default boolean removePressCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default void addReleaseCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	default boolean removeReleaseCallback( Runnable callback ) {
		throw new UnsupportedOperationException();
	}
	
	KeyBinding getKeyBinding();
}
