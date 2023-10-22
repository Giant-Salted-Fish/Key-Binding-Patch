package gsf.kbp.client;

import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

@Mod( "key_binding_patch" )
public final class KBPMod
{
	public KBPMod()
	{
		// Make sure the mod being absent on the other network side does not \
		// cause the client to display the server as incompatible.
		ModLoadingContext.get().registerExtensionPoint(
			ExtensionPoint.DISPLAYTEST,
			()->Pair.of(
				() -> "This is a client only mod.",
				( remote_version_string, network_bool ) -> network_bool
			)
		);
	}
}
