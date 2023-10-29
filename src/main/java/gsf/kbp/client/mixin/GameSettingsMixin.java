package gsf.kbp.client.mixin;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.nbt.CompoundNBT;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

@Mixin( GameSettings.class )
public abstract class GameSettingsMixin
{
	@Final
	@Shadow
	private static Logger LOGGER;
	
	@Final
	@Shadow
	private static Splitter OPTION_SPLITTER;
	
	@Shadow
	public KeyBinding[] keyMappings;
	
	@Final
	@Shadow
	private File optionsFile;
	
	@Shadow
	protected abstract CompoundNBT dataFix( CompoundNBT p_189988_1_ );
	
	
	@Inject( method = "load", at = @At( "RETURN" ) )
	public void onLoad( CallbackInfo ci )
	{
		try
		{
			if ( !this.optionsFile.exists() ) {
				return;
			}
			
			final CompoundNBT compoundnbt = new CompoundNBT();
			try ( BufferedReader bufferedreader = Files.newReader( this.optionsFile, Charsets.UTF_8 ) )
			{
				bufferedreader.lines().forEach( p_230004_1_ -> {
					try
					{
						final Iterator< String > iterator = OPTION_SPLITTER.split( p_230004_1_ ).iterator();
						compoundnbt.putString( iterator.next(), iterator.next() );
					}
					catch ( Exception ignored ) { }
				} );
			}
			
			final CompoundNBT compoundnbt1 = this.dataFix(compoundnbt);
			
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
			
			KeyBinding.resetMapping();
		}
		catch ( Exception exception1 ) {
			LOGGER.error( "Failed to load key binding options", exception1 );
		}
	}
}
