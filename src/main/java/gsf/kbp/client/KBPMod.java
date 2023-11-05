package gsf.kbp.client;

import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod( "key_binding_patch" )
public final class KBPMod
{
	public KBPMod()
	{
		// Make sure the mod being absent on the other network side does not \
		// cause the client to display the server as incompatible.
		ModLoadingContext.get().registerExtensionPoint(
			DisplayTest.class,
			() -> new DisplayTest(
				() -> "This is a client only mod.",
				( remote_version_string, network_bool ) -> network_bool
			)
		);
	}
}
