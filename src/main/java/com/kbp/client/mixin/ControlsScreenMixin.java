package com.kbp.client.mixin;

import com.kbp.client.ActiveInputTracker;
import com.kbp.client.IKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.screen.ControlsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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
	private final ActiveInputTracker input_tracker = new ActiveInputTracker();
	
	
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
		
		if ( key == GLFW.GLFW_KEY_ESCAPE )
		{
			this.input_tracker.resetTracking();
			this.__updateSelectedKeyBinding();
			this.shadow_selected_key = null;
			this.selectedKey = null;
		}
		else
		{
			// Copy reference so that we can use it on key release.
			// See KeyboardListener#keyPress(...).
			this.shadow_selected_key = this.selectedKey;
			
			final Input input = InputMappings.getKey( key, scan_code );
			this.input_tracker.addActive( input );
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
		
		this.__updateSelectedKeyBinding();
		this.input_tracker.resetTracking();
		this.shadow_selected_key = null;
		return true;
	}
	
	@Override
	public boolean mouseClicked( double x, double y, int button )
	{
		if ( this.selectedKey == null ) {
			return super.mouseClicked( x, y, button );
		}
		
		this.shadow_selected_key = this.selectedKey;
		final Input input = Type.MOUSE.getOrCreate( button );
		this.input_tracker.addActive( input );
		return true;
	}
	
	@Override
	public boolean mouseReleased( double x, double y, int button )
	{
		final boolean is_select_click_release = this.input_tracker.noInputActive();
		if ( this.selectedKey == null || is_select_click_release ) {
			return super.mouseReleased( x, y, button );
		}
		
		this.__updateSelectedKeyBinding();
		this.input_tracker.resetTracking();
		this.shadow_selected_key = null;
		this.selectedKey = null;
		return true;
	}
	
	@Unique
	private void __updateSelectedKeyBinding()
	{
		final IKeyBinding kb = ( IKeyBinding ) this.shadow_selected_key;
		final Input key = this.input_tracker.getKey();
		kb.setKeyAndCmbKeys( key, this.input_tracker.getCmbKeys() );
		this.options.setKey( this.shadow_selected_key, key );
		KeyBinding.resetMapping();
	}
}
