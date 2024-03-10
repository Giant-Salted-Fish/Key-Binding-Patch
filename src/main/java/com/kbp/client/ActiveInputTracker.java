package com.kbp.client;

import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

@OnlyIn( Dist.CLIENT )
public final class ActiveInputTracker
{
	private final LinkedList< Input > active_inputs = new LinkedList<>();
	
	public void addActive( Input input )
	{
		if ( !this.active_inputs.contains( input ) ) {
			this.active_inputs.addFirst( input );
		}
	}
	
	public boolean noInputActive() {
		return this.active_inputs.isEmpty();
	}
	
	public void resetTracking() {
		this.active_inputs.clear();
	}
	
	public Input getKey() {
		return this.active_inputs.isEmpty() ? InputMappings.UNKNOWN : this.active_inputs.getFirst();
	}
	
	public Iterator< Input > getCmbKeys()
	{
		return (
			this.active_inputs.isEmpty()
			? Collections.emptyIterator()
			: this.active_inputs.stream().skip( 1 ).iterator()
		);
	}
}
