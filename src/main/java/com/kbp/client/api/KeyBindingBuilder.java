package com.kbp.client.api;

import com.google.common.collect.Iterators;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

@OnlyIn( Dist.CLIENT )
public abstract class KeyBindingBuilder
{
	protected String category = "key.categories.gameplay";
	protected Input key = InputMappings.UNKNOWN;
	protected Iterator< Input > cmb_keys = Collections.emptyIterator();
	protected IKeyConflictContext conflict_context = KeyConflictContext.IN_GAME;
	
	public KeyBindingBuilder() { }
	
	public KeyBindingBuilder withCategory( String category )
	{
		this.category = category;
		return this;
	}
	
	public KeyBindingBuilder withKey( Input key )
	{
		this.key = key;
		return this;
	}
	
	public KeyBindingBuilder withKeyboardKey( int key_code )
	{
		this.key = Type.KEYSYM.getOrCreate( key_code );
		return this;
	}
	
	public KeyBindingBuilder withMouseButton( int button )
	{
		this.key = Type.MOUSE.getOrCreate( button );
		return this;
	}
	
	public KeyBindingBuilder withCmbKeys( Input... cmb_keys )
	{
		this.cmb_keys = Iterators.forArray( cmb_keys );
		return this;
	}
	
	public KeyBindingBuilder withKeyboardCmbKeys( int... cmb_keys )
	{
		this.cmb_keys = Arrays.stream( cmb_keys )
			.mapToObj( Type.KEYSYM::getOrCreate ).iterator();
		return this;
	}
	
	public KeyBindingBuilder withConflictContext( IKeyConflictContext context )
	{
		this.conflict_context = context;
		return this;
	}
	
	public abstract IPatchedKeyBinding build();
	
	public IPatchedKeyBinding buildAndRegis()
	{
		final IPatchedKeyBinding kb = this.build();
		ClientRegistry.registerKeyBinding( kb.getKeyBinding() );
		return kb;
	}
}
