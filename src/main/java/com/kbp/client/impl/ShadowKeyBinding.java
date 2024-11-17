package com.kbp.client.impl;

import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;

import javax.annotation.Nonnull;

/**
 * Only used by internal shadow key bindings.
 */
@OnlyIn( Dist.CLIENT )
public final class ShadowKeyBinding extends KeyBinding implements IPatchedKeyBinding, IKeyBindingImpl
{
	public final KeyBinding target;
	
	public ShadowKeyBinding( KeyBinding target, int index )
	{
		super(
			String.format( "shadow#%s@%d", target.getName(), index ),
			target.getKeyConflictContext(),
			InputMappings.UNKNOWN,
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
	public void setKeyConflictContext( @Nonnull IKeyConflictContext context ) {
		this.target.setKeyConflictContext( context );
	}
	
	@Nonnull
	@Override
	public IKeyConflictContext getKeyConflictContext() {
		return this.target.getKeyConflictContext();
	}
	
	@Override
	public void addPressCallback( Runnable callback )
	{
		final IPatchedKeyBinding ikb = ( IPatchedKeyBinding ) this.target;
		ikb.addPressCallback( callback );
	}
	
	@Override
	public boolean removePressCallback( Runnable callback )
	{
		final IPatchedKeyBinding ikb = ( IPatchedKeyBinding ) this.target;
		return ikb.removePressCallback( callback );
	}
	
	@Override
	public void addReleaseCallback( Runnable callback )
	{
		final IPatchedKeyBinding ikb = ( IPatchedKeyBinding ) this.target;
		ikb.addReleaseCallback( callback );
	}
	
	@Override
	public boolean removeReleaseCallback( Runnable callback )
	{
		final IPatchedKeyBinding ikb = ( IPatchedKeyBinding ) this.target;
		return ikb.removeReleaseCallback( callback );
	}
	
	@Override
	public Object getDelegate() {
		return this.target;
	}
}
