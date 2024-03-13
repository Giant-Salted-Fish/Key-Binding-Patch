package com.kbp.client.mixin;

import com.kbp.client.ActiveKeyTracker;
import com.kbp.client.IKeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;

@Mixin( GuiControls.class )
public abstract class GuiControlsMixin extends GuiScreen
{
	@Shadow
	@Final
	private GameSettings options;
	
	@Shadow
	public KeyBinding buttonId;
	
	@Shadow
	public long time;
	
	@Shadow
	private GuiKeyBindingList keyBindingList;
	
	
	@Unique
	private final ActiveKeyTracker key_tracker = new ActiveKeyTracker();
	
	
	@Override
	public void handleKeyboardInput() throws IOException
	{
		if ( this.buttonId == null )
		{
			super.handleKeyboardInput();
			return;
		}
		
		final int key_code = Keyboard.getEventKey();
		final char typed_char = Keyboard.getEventCharacter();
		// TODO: Do not quite understand this part. Copied from super.
		final boolean flag = key_code == Keyboard.KEY_NONE && typed_char >= ' ';
		final boolean is_key_typed = flag || Keyboard.getEventKeyState();
		if ( !is_key_typed )
		{
			this.__updateSelectedKeyBinding();
			this.key_tracker.resetTracking();
		}
		else
		{
			if ( key_code == Keyboard.KEY_ESCAPE )
			{
				this.key_tracker.resetTracking();
				this.__updateSelectedKeyBinding();
			}
			else if ( key_code != Keyboard.KEY_NONE ) {
				this.key_tracker.addActive( key_code );
			}
			else if ( typed_char > 0 ) {
				this.key_tracker.addActive( typed_char + 256 );
			}
			
			this.time = Minecraft.getSystemTime();
		}
		
		this.mc.dispatchKeypresses();
	}
	
	@Override
	protected void mouseClicked( int mouseX, int mouseY, int mouseButton ) throws IOException
	{
		if ( this.buttonId != null ) {
			this.key_tracker.addActive( mouseButton - 100 );
		}
		else if ( mouseButton != 0 || !this.keyBindingList.mouseClicked( mouseX, mouseY, mouseButton ) ) {
			super.mouseClicked( mouseX, mouseY, mouseButton );
		}
	}
	
	@Override
	protected void mouseReleased( int mouseX, int mouseY, int state )
	{
		final boolean is_select_click_release = this.key_tracker.noTrackingKey();
		if ( this.buttonId != null && !is_select_click_release ) {
			this.__updateSelectedKeyBinding();
		}
		else if ( state != 0 || !this.keyBindingList.mouseReleased( mouseX, mouseY, state ) ) {
			super.mouseReleased( mouseX, mouseY, state );
		}
	}
	
	@Unique
	private void __updateSelectedKeyBinding()
	{
		final IKeyBinding kb = ( IKeyBinding ) this.buttonId;
		final int key = this.key_tracker.getKey();
		kb.setKeyAndCmbKeys( key, this.key_tracker.getCmbKeys() );
		this.options.setOptionKeyBinding( this.buttonId, key );
		KeyBinding.resetKeyBindingArrayAndHash();
		this.buttonId = null;
	}
}
