package com.kbp.client.impl;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedList;

@OnlyIn( Dist.CLIENT )
public final class ActiveKeyTracker
{
	private final LinkedList< Key > active_keys = new LinkedList<>();
	
	public void addActive( Key input )
	{
		if ( !this.active_keys.contains( input ) ) {
			this.active_keys.addFirst( input );
		}
	}
	
	public boolean noKeyActive() {
		return this.active_keys.isEmpty();
	}
	
	public void resetTracking() {
		this.active_keys.clear();
	}
	
	public Key getKey() {
		return this.active_keys.isEmpty() ? InputConstants.UNKNOWN : this.active_keys.getFirst();
	}
	
	public ImmutableSet< Key > getCmbKeys() {
		return ImmutableSet.copyOf( this.active_keys.stream().skip( 1 ).iterator() );
	}
}
