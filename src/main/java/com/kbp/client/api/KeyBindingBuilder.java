package com.kbp.client.api;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * @see KBPMod#newBuilder(String)
 */
@SideOnly( Side.CLIENT )
public abstract class KeyBindingBuilder
{
	protected String category = "key.categories.gameplay";
	protected int key = Keyboard.KEY_NONE;
	protected ImmutableSet< Integer > cmb_keys = ImmutableSet.of();
	protected IKeyConflictContext conflict_context = KeyConflictContext.IN_GAME;
	
	/**
	 * @see KBPMod#newBuilder(String)
	 */
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
	
	public KeyBindingBuilder withCmbKeys( Integer... cmb_keys )
	{
		this.cmb_keys = ImmutableSet.copyOf( cmb_keys );
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
