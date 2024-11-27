package com.kbp.client.gui;

import com.kbp.client.KBPMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiMessageDialog;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.PostConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

@SideOnly( Side.CLIENT )
final class GuiConfigScreen extends GuiScreen
{
	private final GuiScreen parent_screen;
	private String title;
	private GuiShadowCountList shadow_count_list;
	
	GuiConfigScreen( GuiScreen parent_screen ) {
		this.parent_screen = parent_screen;
	}
	
	@Override
	public void initGui()
	{
		this.title = I18n.format( "kbp.gui.config_title" );
		
		final GuiButton cancel_btn = new GuiButton(
			0,
			this.width / 2 - 155, this.height - 29,
			150, 20,
			I18n.format( "gui.cancel" )
		);
		this.addButton( cancel_btn );
		
		final GuiButton save_btn = new GuiButton(
			1,
			this.width / 2 - 155 + 160, this.height - 29,
			150, 20,
			I18n.format( "gui.done" )
		);
		save_btn.enabled = false;
		this.addButton( save_btn );
		
		this.shadow_count_list = new GuiShadowCountList( this, save_btn );
	}
	
	@Override
	public void handleMouseInput() throws IOException
	{
		super.handleMouseInput();
		this.shadow_count_list.handleMouseInput();
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		switch ( button.id )
		{
		case 0:
			this.mc.displayGuiScreen( this.parent_screen );
			break;
		case 1:
			this.shadow_count_list._applyChanges();
			
			final boolean is_world_running = this.mc.world != null;
			final OnConfigChangedEvent event = new OnConfigChangedEvent( KBPMod.MODID, null, is_world_running, true );
			MinecraftForge.EVENT_BUS.post( event );
			if ( event.getResult() != Result.DENY )
			{
				final PostConfigChangedEvent event1 = new PostConfigChangedEvent( KBPMod.MODID, null, is_world_running, true );
				MinecraftForge.EVENT_BUS.post( event1 );
			}
			
			final String title = "fml.configgui.gameRestartTitle";
			final TextComponentTranslation message = new TextComponentTranslation( "fml.configgui.gameRestartRequired" );
			final String btn_label = "fml.configgui.confirmRestartMessage";
			final GuiMessageDialog screen = new GuiMessageDialog( this.parent_screen, title, message, btn_label );
			this.mc.displayGuiScreen( screen );
			break;
		default:
			assert false: "Unknown button id: " + button.id;
		}
	}
	
	@Override
	protected void mouseClicked( int mouseX, int mouseY, int mouseButton ) throws IOException
	{
		if ( mouseButton != 0 || !this.shadow_count_list.mouseClicked( mouseX, mouseY, mouseButton ) ) {
			super.mouseClicked( mouseX, mouseY, mouseButton );
		}
	}
	
	@Override
	protected void mouseReleased( int mouseX, int mouseY, int state )
	{
		if ( state != 0 || !this.shadow_count_list.mouseReleased( mouseX, mouseY, state ) ) {
			super.mouseReleased( mouseX, mouseY, state );
		}
	}
	
	@Override
	public void drawScreen( int mouseX, int mouseY, float partialTicks )
	{
		this.drawDefaultBackground();
		this.shadow_count_list.drawScreen( mouseX, mouseY, partialTicks );
		this.drawCenteredString( this.fontRenderer, this.title, this.width / 2, 8, MathHelper.rgb( 255, 255, 255 ) );
		
		super.drawScreen( mouseX, mouseY, partialTicks );
	}
}
