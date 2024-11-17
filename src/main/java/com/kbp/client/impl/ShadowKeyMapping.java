package com.kbp.client.impl;

import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;
import org.jetbrains.annotations.NotNull;

/**
 * Only used by internal shadow key bindings.
 */
@OnlyIn( Dist.CLIENT )
public final class ShadowKeyMapping extends KeyMapping implements IPatchedKeyMapping, IKeyMappingImpl
{
	public final KeyMapping target;
	
	public ShadowKeyMapping( KeyMapping target, int index )
	{
		super(
			String.format( "shadow#%s@%d", target.getName(), index ),
			target.getKeyConflictContext(),
			InputConstants.UNKNOWN,
			target.getCategory()
		);
		
		this.target = target;
	}
	
	@Override
	public boolean isDown() {
		return this.target.isDown();
	}
	
	@Override
	public boolean consumeClick() {
		return this.target.consumeClick();
	}
	
	@Override
	public void setKeyConflictContext( @NotNull IKeyConflictContext context ) {
		this.target.setKeyConflictContext( context );
	}
	
	@NotNull
	@Override
	public IKeyConflictContext getKeyConflictContext() {
		return this.target.getKeyConflictContext();
	}
	
	@Override
	public void addPressCallback( Runnable callback )
	{
		final var delegate = ( IPatchedKeyMapping ) this.getDelegate();
		delegate.addPressCallback( callback );
	}
	
	@Override
	public boolean removePressCallback( Runnable callback )
	{
		final var ikb = ( IPatchedKeyMapping ) this.target;
		return ikb.removePressCallback( callback );
	}
	
	@Override
	public void addReleaseCallback( Runnable callback )
	{
		final var ikb = ( IPatchedKeyMapping ) this.target;
		ikb.addReleaseCallback( callback );
	}
	
	@Override
	public boolean removeReleaseCallback( Runnable callback )
	{
		final var ikb = ( IPatchedKeyMapping ) this.target;
		return ikb.removeReleaseCallback( callback );
	}
	
	@Override
	public Object getDelegate() {
		return this.target;
	}
}
