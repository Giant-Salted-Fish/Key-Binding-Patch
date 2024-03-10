package com.kbp.client.api;

import com.google.common.collect.Iterators;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

@OnlyIn( Dist.CLIENT )
public abstract class KeyMappingBuilder
{
	protected String category = "key.categories.gameplay";
	protected Key key = InputConstants.UNKNOWN;
	protected Iterator< Key > cmb_keys = Collections.emptyIterator();
	protected IKeyConflictContext conflict_context = KeyConflictContext.IN_GAME;
	
	public KeyMappingBuilder() { }
	
	public KeyMappingBuilder withCategory( String category )
	{
		this.category = category;
		return this;
	}
	
	public KeyMappingBuilder withKey( Key key )
	{
		this.key = key;
		return this;
	}
	
	public KeyMappingBuilder withKeyboardKey( int key_code )
	{
		this.key = Type.KEYSYM.getOrCreate( key_code );
		return this;
	}
	
	public KeyMappingBuilder withMouseButton( int button )
	{
		this.key = Type.MOUSE.getOrCreate( button );
		return this;
	}
	
	public KeyMappingBuilder withCmbKeys( Key... cmb_keys )
	{
		this.cmb_keys = Iterators.forArray( cmb_keys );
		return this;
	}
	
	public KeyMappingBuilder withKeyboardCmbKeys( int... cmb_keys )
	{
		this.cmb_keys = Arrays.stream( cmb_keys )
			.mapToObj( Type.KEYSYM::getOrCreate ).iterator();
		return this;
	}
	
	public KeyMappingBuilder withConflictContext( IKeyConflictContext context )
	{
		this.conflict_context = context;
		return this;
	}
	
	public abstract IPatchedKeyMapping build();
	
	public IPatchedKeyMapping buildAndRegis()
	{
		final IPatchedKeyMapping km = this.build();
		ClientRegistry.registerKeyBinding( km.getKeyMapping() );
		return km;
	}
}
