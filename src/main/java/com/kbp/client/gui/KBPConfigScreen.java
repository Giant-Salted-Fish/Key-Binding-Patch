package com.kbp.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@OnlyIn( Dist.CLIENT )
public final class KBPConfigScreen extends Screen
{
	private final Screen parent_screen;
	private ShadowCountList shadow_count_list;
	
	public KBPConfigScreen( Screen parent )
	{
		super( Component.translatable( "kbp.gui.config_title" ) );
		
		this.parent_screen = parent;
	}
	
	@Override
	protected void init()
	{
		final var cancel_btn = (
			Button.builder(
				CommonComponents.GUI_CANCEL,
				btn -> Objects.requireNonNull( this.minecraft ).setScreen( this.parent_screen )
			)
			.bounds( this.width / 2 - 155, this.height - 29, 150, 20 )
			.build()
		);
		this.addRenderableWidget( cancel_btn );
		
		final var save_btn = (
			Button.builder(
				CommonComponents.GUI_DONE,
				btn -> {
					this.shadow_count_list._applyChanges();
					
					final var mc = Objects.requireNonNull( this.minecraft );
					final var alert_screen = new AlertScreen(
						() -> mc.setScreen( this.parent_screen ),
						Component.translatable( "kbp.gui.alert_title" ),
						Component.translatable( "kbp.gui.alert_message" ),
						CommonComponents.GUI_ACKNOWLEDGE,
						false
					);
					mc.setScreen( alert_screen );
				}
			)
			.bounds( this.width / 2 - 155 + 160, this.height - 29, 150, 20 )
			.build()
		);
		save_btn.active = false;
		this.addRenderableWidget( save_btn );
		
		final var shadow_lst = new ShadowCountList( this, save_btn );
		this.shadow_count_list = shadow_lst;
		this.addWidget( shadow_lst );
	}
	
	@Override
	public void render( @NotNull GuiGraphics graphics, int p_281550_, int p_282878_, float partial_ticks )
	{
		this.renderBackground( graphics );
		this.shadow_count_list.render( graphics, p_281550_, p_282878_, partial_ticks );
		graphics.drawCenteredString( this.font, this.title, this.width / 2, 8, RGB( 255, 255, 255 ) );
		
		super.render( graphics, p_281550_, p_282878_, partial_ticks );
	}
	
	
	// >>> Utility Function <<<
	static int RGB( int red, int green, int blue )
	{
		assert red >= 0 && red <= 255;
		assert green >= 0 && green <= 255;
		assert blue >= 0 && blue <= 255;
		return ( red << 16 ) | ( green << 8 ) | blue;
	}
}
