package com.kbp.client.mixin;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.ToggleableKeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BooleanSupplier;

@Mixin( ToggleableKeyBinding.class )
public abstract class ToggleableKeyBindingMixin extends KeyBinding
{
	@Shadow
	@Final
	private BooleanSupplier needsToggle;
	
	
	public ToggleableKeyBindingMixin(
		String p_i45001_1_,
		int p_i45001_2_,
		String p_i45001_3_
	) { super( p_i45001_1_, p_i45001_2_, p_i45001_3_ ); }
	
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
}
