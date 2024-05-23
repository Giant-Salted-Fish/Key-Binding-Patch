package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.LinkedList;

@SideOnly( Side.CLIENT )
public final class ActiveKeyTracker
{
	private final LinkedList< Integer > active_keys = new LinkedList<>();
	
	public void addActive( int key )
	{
		if ( !this.active_keys.contains( key ) ) {
			this.active_keys.addFirst( key );
		}
	}
	
	public boolean noTrackingKey() {
		return this.active_keys.isEmpty();
	}
	
	public void resetTracking() {
		this.active_keys.clear();
	}
	
	public int getKey() {
		return this.active_keys.isEmpty() ? Keyboard.KEY_NONE : this.active_keys.getFirst();
	}
	
	public ImmutableSet< Integer > getCmbKeys() {
		return ImmutableSet.copyOf( this.active_keys.stream().skip( 1 ).iterator() );
	}
}
