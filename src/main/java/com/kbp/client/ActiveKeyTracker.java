package com.kbp.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.Iterator;
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
	
	public Iterator< Integer > getCmbKeys()
	{
		return (
			this.active_keys.isEmpty()
			? Collections.emptyIterator()
			: this.active_keys.stream().skip( 1 ).iterator()
		);
	}
}
