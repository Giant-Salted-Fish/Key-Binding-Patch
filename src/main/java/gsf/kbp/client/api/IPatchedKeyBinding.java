package gsf.kbp.client.api;

import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.Set;

/**
 * You can cast {@link KeyMapping} instance to this interface to if you need to
 * use patched functionalities on it.
 */
@OnlyIn( Dist.CLIENT )
public interface IPatchedKeyBinding
{
	default Set< Key > combinations() {
		throw new RuntimeException();
	}
	
	default void setKeyAndCombinations( Key key, Set< Key > combinations ) {
		throw new RuntimeException();
	}
	
	default Set< Key > defaultCombinations() {
		throw new RuntimeException();
	}
	
	default String addPressCallback( Runnable callback ) {
		throw new RuntimeException();
	}
	
	default Optional< Runnable > regisPressCallback(
		String identifier, Runnable callback
	) { throw new RuntimeException(); }
	
	default Runnable unregisPressCallback( String identifier ) {
		throw new RuntimeException();
	}
	
	default String addReleaseCallback( Runnable callback ) {
		throw new RuntimeException();
	}
	
	default Optional< Runnable > regisReleaseCallback(
		String identifier, Runnable callback
	) { throw new RuntimeException(); }
	
	default Runnable unregisReleaseCallback( String identifier ) {
		throw new RuntimeException();
	}
}
