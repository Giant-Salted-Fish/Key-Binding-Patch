package com.kbp.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.HashSet;

@SideOnly( Side.CLIENT )
public final class InputSignal
{
	private static final HashMap< String, InputSignal > SIGNAL_TABLE = new HashMap<>();
	
	public static InputSignal of( String signal ) {
		return SIGNAL_TABLE.computeIfAbsent( signal, key -> new InputSignal() );
	}
	
	
	public int active_count = 0;
	public int click_count = 0;
	
	public final HashSet< Runnable > press_callbacks = new HashSet<>();
	public final HashSet< Runnable > release_callbacks = new HashSet<>();
	
	public void increaseActiveCount()
	{
		final boolean flag = this.active_count == 0;
		this.active_count += 1;
		if ( flag ) {
			this.press_callbacks.forEach( Runnable::run );
		}
	}
	
	public void reduceActiveCount()
	{
		final boolean flag = this.active_count == 1;
		this.active_count -= 1;
		if ( flag ) {
			this.release_callbacks.forEach( Runnable::run );
		}
	}
}
