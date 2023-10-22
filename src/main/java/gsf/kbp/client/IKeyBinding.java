package gsf.kbp.client;

import gsf.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Only use {@link IPatchedKeyBinding} unless you know what you are doing. This
 * interface is used as an inner helper for the mixin implementation.
 *
 * @see gsf.kbp.client.mixin.KeyBindingMixin
 */
@OnlyIn( Dist.CLIENT )
public interface IKeyBinding extends IPatchedKeyBinding
{
	boolean _isActive();
	
	ArrayList< BooleanSupplier > _conditionList();
	
	void _incrClickCount();
	
	void _setDefaultCombinations( Set< Input > combinations );
	
	KeyBinding _cast();
}
