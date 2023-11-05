package gsf.kbp.client.mixin;

import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BooleanSupplier;

@Mixin( ToggleKeyMapping.class )
public abstract class ToggleKeyMappingMixin
	extends KeyMapping implements IKeyBinding
{
	@Final
	@Shadow
	private BooleanSupplier needsToggle;
	
	
	@Unique
	private boolean is_active;
	
	public ToggleKeyMappingMixin(
		String p_i45001_1_,
		int p_i45001_2_,
		String p_i45001_3_
	) { super( p_i45001_1_, p_i45001_2_, p_i45001_3_ ); }
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public void setDown( boolean is_down )
	{
		final boolean is_toggle_mode = this.needsToggle.getAsBoolean();
		if ( !is_toggle_mode ) {
			super.setDown( is_down );
		}
		else if ( !is_down ) {
			this.is_active = false;
		}
		else if ( !this.is_active && this.getKeyConflictContext().isActive() )
		{
			this.is_active = true;
			this.setDown( !this.isDown() );
		}
	}
	
	@Override
	public boolean _isActive() {
		return this.is_active;
	}
}
