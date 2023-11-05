package gsf.kbp.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import gsf.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;

@Mixin( KeyBindsScreen.class )
public abstract class KeyBindsScreenMixin extends OptionsSubScreen
{
	@Shadow
	public KeyMapping selectedKey;
	
	@Shadow
	public long lastKeySelection;
	
	
	// It seems that the Forge will automatically set #selectedKey to null \
	// when key is released, so we have to manually save its reference to use.
	@Unique
	private KeyMapping shadow_selected_key;
	
	@Unique
	private Key last_active_key = InputConstants.UNKNOWN;
	
	@Unique
	private final HashSet< Key > combinations = new HashSet<>();
	
	public KeyBindsScreenMixin(
		Screen parent,
		Options settings,
		Component title
	) { super( parent, settings, title ); }
	
	@Override
	public boolean keyPressed( int key, int scan_code, int modifier )
	{
		if ( this.selectedKey == null ) {
			return super.keyPressed( key, scan_code, modifier );
		}
		
		// Copy reference so that we can use it on key release.
		// See KeyboardListener#keyPress(...).
		this.shadow_selected_key = this.selectedKey;
		if ( key == GLFW.GLFW_KEY_ESCAPE )
		{
			this.last_active_key = InputConstants.UNKNOWN;
			this.combinations.clear();
			this.__applyKeyAndCombinations();
		}
		else
		{
			final Key input = InputConstants.getKey( key, scan_code );
			this.__appendActiveInput( input );
		}
		
		this.lastKeySelection = Util.getMillis();
		return true;
	}
	
	@Override
	public boolean keyReleased( int key, int scan_code, int modifier )
	{
		if ( this.shadow_selected_key == null ) {
			return super.keyReleased( key, scan_code, modifier );
		}
		
		this.__applyKeyAndCombinations();
		return true;
	}
	
	@Override
	public boolean mouseClicked( double x, double y, int button )
	{
		if ( this.selectedKey == null ) {
			return super.mouseClicked( x, y, button );
		}
		
		this.shadow_selected_key = this.selectedKey;
		final Key input = InputConstants.Type.MOUSE.getOrCreate( button );
		this.__appendActiveInput( input );
		return true;
	}
	
	@Override
	public boolean mouseReleased( double x, double y, int button )
	{
		if (
			this.selectedKey == null
			|| this.last_active_key == InputConstants.UNKNOWN
		) {
			return super.mouseReleased( x, y, button );
		}
		
		this.__applyKeyAndCombinations();
		return true;
	}
	
	@Unique
	private void __appendActiveInput( Key input )
	{
		// Skip if is repeated keys.
		if ( input.equals( this.last_active_key ) ) {
			return;
		}
		
		if ( this.last_active_key != InputConstants.UNKNOWN ) {
			this.combinations.add( this.last_active_key );
		}
		this.last_active_key = input;
	}
	
	@Unique
	private void __applyKeyAndCombinations()
	{
		final IPatchedKeyBinding kb = ( IPatchedKeyBinding ) this.shadow_selected_key;
		final HashSet< Key > combinations = new HashSet<>( this.combinations );
		kb.setKeyAndCombinations( this.last_active_key, combinations );
		
		this.options.setKey( this.shadow_selected_key, this.last_active_key );
		KeyMapping.resetMapping();
		
		this.last_active_key = InputConstants.UNKNOWN;
		this.combinations.clear();
		this.selectedKey = null;
		this.shadow_selected_key = null;
	}
}
