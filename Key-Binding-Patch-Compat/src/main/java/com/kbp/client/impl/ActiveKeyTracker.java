package com.kbp.client.impl;

import com.google.common.base.MoreObjects;
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
		return MoreObjects.firstNonNull( this.active_keys.peekFirst(), InputConstants.UNKNOWN );
	}
	
	public ImmutableSet< Key > getCmbKeys() {
		return this.active_keys.stream().skip( 1 ).collect( ImmutableSet.toImmutableSet() );
	}
}
