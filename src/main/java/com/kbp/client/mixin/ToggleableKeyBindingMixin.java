package com.kbp.client.mixin;

import com.google.common.collect.ImmutableSet;
import com.kbp.client.impl.IKeyBinding;
import com.kbp.client.impl.ShadowToggleableKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.ToggleableKeyBinding;
import net.minecraft.client.util.InputMappings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BooleanSupplier;

@Mixin( ToggleableKeyBinding.class )
public abstract class ToggleableKeyBindingMixin extends KeyBinding implements IKeyBinding
{
	@Shadow
	@Final
	private BooleanSupplier needsToggle;
	// TODO: Maybe move toggle predicate to InputSignal.
	
	
	public ToggleableKeyBindingMixin( String p_i45001_1_, int p_i45001_2_, String p_i45001_3_ ) {
		super( p_i45001_1_, p_i45001_2_, p_i45001_3_ );
	}
	
	@Override
	public void setDown( boolean is_down )
	{
		final boolean is_toggle_mode = this.needsToggle.getAsBoolean();
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
	public KeyBinding createShadowCopy( int index )
	{
		return new ShadowToggleableKeyBinding(
			this.getName(),
			InputMappings.UNKNOWN.getValue(),
			ImmutableSet.of(),
			this.getCategory(),
			this.needsToggle,
			index
		);
	}
	
	@Override
	@SuppressWarnings( "AddedMixinMembersNamePattern" )
	public KeyBinding getKeyBinding() {
		return this;
	}
}
