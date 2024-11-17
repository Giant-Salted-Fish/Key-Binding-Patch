package com.kbp.client.impl;

import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.mixin.ToggleKeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Only used by internal shadow key bindings.
 */
@OnlyIn( Dist.CLIENT )
public final class ShadowToggleableKeyMapping
	extends ToggleKeyMapping
	implements IPatchedKeyMapping, IKeyMappingImpl
{
	public final KeyMapping target;
	
	public ShadowToggleableKeyMapping( KeyMapping target, int index )
	{
		super(
			String.format( "shadow#%s@%d", target.getName(), index ),
			GLFW.GLFW_KEY_UNKNOWN,
			target.getCategory(),
			( ( ToggleKeyMappingAccess ) target ).getNeedsToggle()
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
		final var ikb = ( IPatchedKeyMapping ) this.target;
		ikb.addPressCallback( callback );
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
