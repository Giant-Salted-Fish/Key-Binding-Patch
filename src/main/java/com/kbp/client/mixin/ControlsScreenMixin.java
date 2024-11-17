package com.kbp.client.mixin;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.GameSettings;
import net.minecraft.client.KeyboardListener;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;

@Mixin( ControlsScreen.class )
public abstract class ControlsScreenMixin extends SettingsScreen
{
	@Shadow
	public KeyBinding selectedKey;
	
	@Shadow
	public long lastKeySelection;
	
	
	/**
	 * It turns out that Forge will automatically set {@link #selectedKey} to
	 * {@code null} in {@link KeyboardListener#keyPress(long, int, int, int, int)}
	 * under certain circumstances when keyboard key is released, so we have to
	 * manually copy the reference to use.
	 */
	@Unique
	private KeyBinding shadow_selected_key;
	
	@Unique
	private final LinkedList< Input > active_inputs = new LinkedList<>();
	
	
	public ControlsScreenMixin( Screen parent, GameSettings settings, ITextComponent title ) {
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
		// See KeyboardListener#keyPress(...).
		this.shadow_selected_key = this.selectedKey;
		
		if ( key == GLFW.GLFW_KEY_ESCAPE )
		{
			this.active_inputs.clear();
			this.__updateSelectedKeyBinding();
		}
		else
		{
			final Input input = InputMappings.getKey( key, scan_code );
			this.active_inputs.addFirst( input );
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
		this.active_inputs.clear();
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
		this.active_inputs.addFirst( input );
		return true;
	}
	
	@Override
	public boolean mouseReleased( double x, double y, int button )
	{
		final boolean is_select_click_release = this.active_inputs.isEmpty();
		if ( this.shadow_selected_key == null || is_select_click_release ) {
			return super.mouseReleased( x, y, button );
		}
		
		this.__updateSelectedKeyBinding();
		this.active_inputs.clear();
		return true;
	}
	
	@Unique
	private void __updateSelectedKeyBinding()
	{
		final IPatchedKeyBinding kb = ( IPatchedKeyBinding ) this.shadow_selected_key;
		final Input key = MoreObjects.firstNonNull( this.active_inputs.peekFirst(), InputMappings.UNKNOWN );
		final ImmutableSet< Input > cmb_keys = this.active_inputs.stream().skip( 1 ).collect( ImmutableSet.toImmutableSet() );
		kb.setKeyAndCmbKeys( key, cmb_keys );
		this.options.setKey( this.shadow_selected_key, key );
		
		this.shadow_selected_key = null;
		this.selectedKey = null;
		KeyBinding.resetMapping();
	}
}
