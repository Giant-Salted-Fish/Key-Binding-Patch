package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.impl.IKeyMapping;
import com.kbp.client.impl.ShadowToggleableKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BooleanSupplier;

@Mixin( ToggleKeyMapping.class )
public abstract class ToggleKeyMappingMixin extends KeyMapping implements IKeyMapping
{
	@Shadow
	@Final
	private BooleanSupplier needsToggle;
	
	
	public ToggleKeyMappingMixin( String p_90821_, int p_90822_, String p_90823_ ) {
		super( p_90821_, p_90822_, p_90823_ );
	}
	
	@Override
	public void setDown( boolean is_down )
	{
		final var is_toggle_mode = this.needsToggle.getAsBoolean();
		if ( !is_toggle_mode ) {
			super.setDown( is_down );
		}
		else if ( is_down && this.getKeyConflictContext().isActive() ) {
			super.setDown( !this.isDown() );
		}
	}
	
	@Override
	public boolean isDown() {
		return super.isDown();
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public KeyMapping createShadowCopy( int index )
	{
		return new ShadowToggleableKeyMapping(
			this.getName(),
			InputConstants.UNKNOWN.getValue(),
			ImmutableSet.of(),
			this.getCategory(),
			this.needsToggle,
			index
		);
	}
}
