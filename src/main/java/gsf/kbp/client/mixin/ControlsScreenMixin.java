package gsf.kbp.client.mixin;

import gsf.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.screen.ControlsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;

@Mixin( ControlsScreen.class )
public abstract class ControlsScreenMixin extends SettingsScreen
{
	@Shadow
	public KeyBinding selectedKey;
	
	@Shadow
	public long lastKeySelection;
	
	
	// It seems that the Forge will automatically set #selectedKey to null \
	// when key is released, so we have to manually save its reference to use.
	@Unique
	private KeyBinding shadow_selected_key;
	
	@Unique
	private Input last_active_key = InputMappings.UNKNOWN;
	
	@Unique
	private final HashSet< Input > combinations = new HashSet<>();
	
	public ControlsScreenMixin(
		Screen parent,
		GameSettings settings,
		ITextComponent title
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
			this.last_active_key = InputMappings.UNKNOWN;
			this.combinations.clear();
			this.__applyKeyAndCombinations();
		}
		else
		{
			final Input input = InputMappings.getKey( key, scan_code );
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
		final Input input = InputMappings.Type.MOUSE.getOrCreate( button );
		this.__appendActiveInput( input );
		return true;
	}
	
	@Override
	public boolean mouseReleased( double x, double y, int button )
	{
		if (
			this.selectedKey == null
			|| this.last_active_key == InputMappings.UNKNOWN
		) {
			return super.mouseReleased( x, y, button );
		}
		
		this.__applyKeyAndCombinations();
		return true;
	}
	
	@Unique
	private void __appendActiveInput( Input input )
	{
		// Skip if is repeated keys.
		if ( input.equals( this.last_active_key ) ) {
			return;
		}
		
		if ( this.last_active_key != InputMappings.UNKNOWN ) {
			this.combinations.add( this.last_active_key );
		}
		this.last_active_key = input;
	}
	
	@Unique
	private void __applyKeyAndCombinations()
	{
		final IPatchedKeyBinding kb = ( IPatchedKeyBinding ) this.shadow_selected_key;
		final HashSet< Input > combinations = new HashSet<>( this.combinations );
		kb.setKeyAndCombinations( this.last_active_key, combinations );
		
		this.options.setKey( this.shadow_selected_key, this.last_active_key );
		KeyBinding.resetMapping();
		
		this.last_active_key = InputMappings.UNKNOWN;
		this.combinations.clear();
		this.selectedKey = null;
		this.shadow_selected_key = null;
	}
}
