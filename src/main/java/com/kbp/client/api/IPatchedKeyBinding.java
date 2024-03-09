package com.kbp.client.api;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * @see com.kbp.client.KBPMod#getPatched(KeyBinding)
 * @see com.kbp.client.KBPMod#newBuilder(String)
 * @see com.kbp.client.KBPMod#newToggleableBuilder(String, BooleanSupplier)
 */
@OnlyIn( Dist.CLIENT )
public interface IPatchedKeyBinding
{
	default Set< Input > defaultCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default Set< Input > currentCmbKeys() {
		throw new UnsupportedOperationException();
	}
	
	default void setKeyAndCmbKeys( Input key, Iterator< Input > cmb_keys ) {
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
	
	Input getDefaultKey();
	
	Input getKey();
	
	boolean isDefault();
	
	void setToDefault();
	
	boolean isDown();
	
	void setDown( boolean down );
	
	boolean consumeClick();
	
	/**
	 * In most cases you should directly use methods in this interface rather
	 * than ones with similar functionalities in {@link KeyBinding}.
	 */
	KeyBinding getKeyBinding();
}