package com.kbp.client.mixin;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.kbp.client.impl.IKeyBinding;
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
import java.util.LinkedList;

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
	private final LinkedList< Integer > active_keys = new LinkedList<>();
	
	
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
			this.active_keys.clear();
		}
		else
		{
			if ( key_code == Keyboard.KEY_ESCAPE )
			{
				this.active_keys.clear();
				this.__updateSelectedKeyBinding();
			}
			else if ( key_code != Keyboard.KEY_NONE ) {
				this.active_keys.add( key_code );
			}
			else if ( typed_char > 0 ) {
				this.active_keys.add( typed_char + 256 );
			}
			
			this.time = Minecraft.getSystemTime();
		}
		
		this.mc.dispatchKeypresses();
	}
	
	@Override
	protected void mouseClicked( int mouseX, int mouseY, int mouseButton ) throws IOException
	{
		if ( this.buttonId != null ) {
			this.active_keys.add( mouseButton - 100 );
		}
		else if ( mouseButton != 0 || !this.keyBindingList.mouseClicked( mouseX, mouseY, mouseButton ) ) {
			super.mouseClicked( mouseX, mouseY, mouseButton );
		}
	}
	
	@Override
	protected void mouseReleased( int mouseX, int mouseY, int state )
	{
		final boolean is_select_click_release = this.active_keys.isEmpty();
		if ( this.buttonId != null && !is_select_click_release )
		{
			this.__updateSelectedKeyBinding();
			this.active_keys.clear();
		}
		else if ( state != 0 || !this.keyBindingList.mouseReleased( mouseX, mouseY, state ) ) {
			super.mouseReleased( mouseX, mouseY, state );
		}
	}
	
	@Unique
	private void __updateSelectedKeyBinding()
	{
		final IKeyBinding ikb = ( IKeyBinding ) this.buttonId;
		final int key = MoreObjects.firstNonNull( this.active_keys.peek(), Keyboard.KEY_NONE );
		final ImmutableSet< Integer > cmb_keys = ImmutableSet.copyOf(
			this.active_keys.stream().skip( 1 ).iterator()
		);
		ikb.setKeyAndCmbKeys( key, cmb_keys );
		this.options.setOptionKeyBinding( this.buttonId, key );
		KeyBinding.resetKeyBindingArrayAndHash();
		this.buttonId = null;
	}
}
