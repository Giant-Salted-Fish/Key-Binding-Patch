package gsf.kbp.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.HashSet;

@Mixin( Options.class )
public abstract class OptionsMixin
{
	@Final
	@Shadow
	private File optionsFile;
	
	@Inject( method = "load", at = @At( "HEAD" ) )
	public void onLoad( CallbackInfo ci )
	{
		if ( !this.optionsFile.exists() )
		{
			// Mapping reset would not be called if options file does not exist.
			// We need to call it manually here.
			KeyMapping.resetMapping();
		}
	}
	
	@Inject(
		method = "processOptions",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/KeyMapping;setKeyModifierAndCode(Lnet/minecraftforge/client/settings/KeyModifier;Lcom/mojang/blaze3d/platform/InputConstants$Key;)V",
			shift = Shift.AFTER
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void onProcessOptions(
		@Coerce Object p_168428_,
		CallbackInfo ci, KeyMapping[] var2,
		int var3,
		int var4,
		KeyMapping keymapping,
		String s,
		String s1
	) {
		// This part is bit of hacky. See KeyBindingMixin#saveString().
		final String[] splits = s1.split( ":" );
		if ( splits.length < 3 ) {
			return;
		}

		final String combinations = splits[ 2 ];
		if ( combinations.isEmpty() ) {
			return;
		}

		final HashSet< Key > cmbs = new HashSet<>();
		for ( String ks : combinations.split( "\\+" ) ) {
			cmbs.add( InputConstants.getKey( ks ) );
		}

		final IKeyBinding kb = ( IKeyBinding ) keymapping;
		kb.setKeyAndCombinations( kb._cast().getKey(), cmbs );
	}
}
