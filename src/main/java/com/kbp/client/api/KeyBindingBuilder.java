package com.kbp.client.api;

import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

@SideOnly( Side.CLIENT )
public abstract class KeyBindingBuilder
{
	protected String category = "key.categories.gameplay";
	protected int key = Keyboard.KEY_NONE;
	protected Iterator< Integer > cmb_keys = Collections.emptyIterator();
	protected IKeyConflictContext conflict_context = KeyConflictContext.IN_GAME;
	
	public KeyBindingBuilder() { }
	
	public KeyBindingBuilder withCategory( String category )
	{
		this.category = category;
		return this;
	}
	
	public KeyBindingBuilder withKey( int key )
	{
		this.key = key;
		return this;
	}
	
	public KeyBindingBuilder withMouseButton( int button )
	{
		this.key = button - 100;
		return this;
	}
	
	public KeyBindingBuilder withCmbKeys( int... cmb_keys )
	{
		this.cmb_keys = Arrays.stream( cmb_keys ).iterator();
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
