package com.kbp.client.mixin;

import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.impl.ActiveKeyTracker;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( KeyBindsScreen.class )
public abstract class KeyBindsScreenMixin extends OptionsSubScreen
{
	@Shadow
	public KeyMapping selectedKey;
	
	@Shadow
	public long lastKeySelection;
	
	
	/**
	 * It turns out that Forge will automatically set {@link #selectedKey} to
	 * {@code null} in {@link KeyboardHandler#keyPress(long, int, int, int, int)}
	 * under certain circumstances when keyboard key is released, so we have to
	 * manually copy the reference to use.
	 */
	@Unique
	private KeyMapping shadow_selected_key;
	
	@Unique
	private final ActiveKeyTracker key_tracker = new ActiveKeyTracker();
	
	
	public KeyBindsScreenMixin( Screen parent, Options settings, Component title ) {
		super( parent, settings, title );
	}
	
	@Inject( method = "init", at = @At( "HEAD" ) )
	private void onInit( CallbackInfo ci )
	{
		assert this.minecraft != null;
		this.minecraft.keyboardHandler.setSendRepeatsToGui( false );
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
		final var key = InputConstants.Type.MOUSE.getOrCreate( button );
		this.key_tracker.addActive( key );
		return true;
	}
	
	@Override
	public boolean mouseReleased( double x, double y, int button )
	{
		final var is_select_click_release = this.key_tracker.noKeyActive();
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
		final var ikm = ( IPatchedKeyMapping ) this.shadow_selected_key;
		final var key = this.key_tracker.getKey();
		ikm.setKeyAndCmbKeys( key, this.key_tracker.getCmbKeys() );
		this.options.setKey( this.shadow_selected_key, key );
		
		this.shadow_selected_key = null;
		this.selectedKey = null;
		KeyMapping.resetMapping();
	}
}
