package gsf.kbp.client.api;

import com.mojang.blaze3d.platform.InputConstants.Key;
import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.ToggleKeyMapping;

import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Do not cast vanilla {@link ToggleKeyMapping} instance to this class! you
 * will get a bad cast exception if you did try. Cast it to
 * {@link IPatchedKeyBinding} if you do need to use the patched functionalities
 * on vanilla key bindings. This class only exists to make the creation of key
 * binding with default combinations more convenient.
 */
public class PatchedToggleableKeyBinding
	extends ToggleKeyMapping implements IPatchedKeyBinding
{
	public PatchedToggleableKeyBinding(
		String description,
		int key_code,
		Set< Key > combinations,
		String category,
		BooleanSupplier toggle_controller
	) {
		super( description, key_code, category, toggle_controller );
		
		final IKeyBinding ikb = ( IKeyBinding ) this;
		ikb._setDefaultCombinations( combinations );
	}
}
