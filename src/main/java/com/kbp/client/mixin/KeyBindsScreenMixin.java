package com.kbp.client.mixin;

import com.kbp.client.ActiveKeyTracker;
import com.kbp.client.IKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
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

@Mixin( KeyBindsScreen.class )
public abstract class KeyBindsScreenMixin extends OptionsSubScreen
{
	@Shadow
	public KeyMapping selectedKey;
	
	@Shadow
	public long lastKeySelection;
	
	
	// It turns out that Forge will automatically set #selectedKey to null in
	// certain circumstances when keyboard key is released, so we have to
	// manually copy the reference to use.
	@Unique
	private KeyMapping shadow_selected_key;
	
	@Unique
	private final ActiveKeyTracker key_tracker = new ActiveKeyTracker();
	
	
	public KeyBindsScreenMixin( Screen parent, Options settings, Component title ) {
		super( parent, settings, title );
	}
	
	@Override
	public boolean keyPressed( int key, int scan_code, int modifier )
	{
		if ( this.selectedKey == null ) {
			return super.keyPressed( key, scan_code, modifier );
		}
		
		// Copy reference so that we can use it on key release.
		// See KeyboardHandler#keyPress(...).
		this.shadow_selected_key = this.selectedKey;
		
		if ( key == GLFW.GLFW_KEY_ESCAPE )
		{
			this.key_tracker.resetTracking();
			this.__updateSelectedKeyBinding();
		}
		else
		{
			final Key active_key = InputConstants.getKey( key, scan_code );
			this.key_tracker.addActive( active_key );
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
		this.key_tracker.resetTracking();
		return true;
	}
	
	@Override
	public boolean mouseClicked( double x, double y, int button )
	{
		if ( this.selectedKey == null ) {
			return super.mouseClicked( x, y, button );
		}
		
		this.shadow_selected_key = this.selectedKey;
		final Key key = InputConstants.Type.MOUSE.getOrCreate( button );
		this.key_tracker.addActive( key );
		return true;
	}
	
	@Override
	public boolean mouseReleased( double x, double y, int button )
	{
		final boolean is_select_click_release = this.key_tracker.noKeyActive();
		if ( this.shadow_selected_key == null || is_select_click_release ) {
			return super.mouseReleased( x, y, button );
		}
		
		this.__updateSelectedKeyBinding();
		this.key_tracker.resetTracking();
		return true;
	}
	
	@Unique
	private void __updateSelectedKeyBinding()
	{
		final IKeyMapping km = ( IKeyMapping ) this.shadow_selected_key;
		final Key key = this.key_tracker.getKey();
		km.setKeyAndCmbKeys( key, this.key_tracker.getCmbKeys() );
		this.options.setKey( this.shadow_selected_key, key );
		
		this.shadow_selected_key = null;
		this.selectedKey = null;
		KeyMapping.resetMapping();
	}
}
