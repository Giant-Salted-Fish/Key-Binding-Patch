package gsf.kbp.client.mixin;

import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

@Mixin( GameSettings.class )
public abstract class GameSettingsMixin
{
	@Shadow
	public KeyBinding[] keyMappings;
	
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
			KeyBinding.resetMapping();
		}
	}
	
	@Inject(
		method = "load",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/settings/KeyBinding;resetMapping()V"
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void onLoad(
		CallbackInfo ci,
		CompoundNBT compoundnbt,
		CompoundNBT compoundnbt1
	) {
		// Build a hashmap to speedup key binding lookup.
		final HashMap< String, IKeyBinding >
			lookup_table = new HashMap<>();
		for ( KeyBinding kb : this.keyMappings ) {
			lookup_table.put( "key_" + kb.getName(), ( IKeyBinding ) kb );
		}
		
		for ( String key : compoundnbt1.getAllKeys() )
		{
			lookup_table.computeIfPresent( key, ( k, kb ) -> {
				// This part is bit of hacky. See KeyBindingMixin#saveString().
				final String value = compoundnbt1.getString( k );
				final String[] splits = value.split( ":" );
				if ( splits.length < 3 ) {
					return kb;
				}
				
				final String combinations = splits[ 2 ];
				if ( combinations.isEmpty() ) {
					return kb;
				}
				
				final HashSet< Input > cmbs = new HashSet<>();
				for ( String s : combinations.split( "\\+" ) ) {
					cmbs.add( InputMappings.getKey( s ) );
				}
				
				kb.setKeyAndCombinations( kb._cast().getKey(), cmbs );
				return kb;
			} );
		}
	}
}
