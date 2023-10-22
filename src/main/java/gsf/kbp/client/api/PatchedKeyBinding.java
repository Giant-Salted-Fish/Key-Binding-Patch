package gsf.kbp.client.api;

import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.client.settings.IKeyConflictContext;

import java.util.Set;

/**
 * Do not cast vanilla {@link KeyBinding} instance to this class! you will get a
 * bad cast exception if you did try. Cast it to {@link IPatchedKeyBinding} if
 * you do need to use the patched functionalities on vanilla key bindings. This
 * class only exists to make the creation of key binding with default
 * combinations more convenient.
 */
public class PatchedKeyBinding extends KeyBinding implements IPatchedKeyBinding
{
	public PatchedKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		Input key,
		Set< Input > combinations,
		String category
	) {
		super( description, key_conflict_context, key, category );
		
		final IKeyBinding ikb = ( IKeyBinding ) this;
		ikb._setDefaultCombinations( combinations );
	}
}
