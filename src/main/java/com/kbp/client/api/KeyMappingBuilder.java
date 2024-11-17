package com.kbp.client.api;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.KBPMod;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

/**
 * @see KBPMod#newBuilder(String)
 * @see KBPMod#newToggleableBuilder(String, BooleanSupplier)
 */
@OnlyIn( Dist.CLIENT )
public abstract class KeyMappingBuilder
{
	protected String category = "key.categories.gameplay";
	protected Key key = InputConstants.UNKNOWN;
	protected ImmutableSet< Key > cmb_keys = ImmutableSet.of();
	protected IKeyConflictContext conflict_context = KeyConflictContext.IN_GAME;
	
	/**
	 * @see KBPMod#newBuilder(String)
	 * @see KBPMod#newToggleableBuilder(String, BooleanSupplier)
	 */
	public KeyMappingBuilder() {
	}
	
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
		this.cmb_keys = ImmutableSet.copyOf( cmb_keys );
		return this;
	}
	
	public KeyMappingBuilder withKeyboardCmbKeys( int... cmb_keys )
	{
		this.cmb_keys = (
			Arrays.stream( cmb_keys )
			.mapToObj( Type.KEYSYM::getOrCreate )
			.collect( ImmutableSet.toImmutableSet() )
		);
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
		final var pkm = this.build();
		ClientRegistry.registerKeyBinding( pkm.getKeyMapping() );
		return pkm;
	}
}
