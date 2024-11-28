package com.kbp.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.AlertScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Objects;

@OnlyIn( Dist.CLIENT )
public final class KBPConfigScreen extends Screen
{
	private final Screen parent_screen;
	private ShadowCountList shadow_count_list;
	
	public KBPConfigScreen( Screen parent )
	{
		super( new TranslationTextComponent( "kbp.gui.config_title" ) );
		
		this.parent_screen = parent;
	}
	
	@Override
	protected void init()
	{
		final Button cancel_btn = new Button(
			this.width / 2 - 155, this.height - 29,
			150, 20,
			DialogTexts.GUI_CANCEL,
			btn -> Objects.requireNonNull( this.minecraft ).setScreen( this.parent_screen )
		);
		this.addButton( cancel_btn );
		
		final Button save_btn = new Button(
			this.width / 2 - 155 + 160, this.height - 29,
			150, 20,
			DialogTexts.GUI_DONE,
			btn -> {
				this.shadow_count_list._applyChanges();
				
				final Minecraft mc = Objects.requireNonNull( this.minecraft );
				final AlertScreen alert_screen = new AlertScreen(
					() -> mc.setScreen( this.parent_screen ),
					new TranslationTextComponent( "kbp.gui.alert_title" ),
					new TranslationTextComponent( "kbp.gui.alert_message" ),
					new TranslationTextComponent( "kbp.gui.alert_confirm" )
				);
				mc.setScreen( alert_screen );
			}
		);
		save_btn.active = false;
		this.addButton( save_btn );
		
		final ShadowCountList shadow_lst = new ShadowCountList( this, save_btn );
		this.shadow_count_list = shadow_lst;
		this.children.add( shadow_lst );
	}
	
	@Override
	public void render( @Nonnull MatrixStack matrix, int p_230430_2_, int p_230430_3_, float partial_ticks )
	{
		this.renderBackground( matrix );
		this.shadow_count_list.render( matrix, p_230430_2_, p_230430_3_, partial_ticks );
		drawCenteredString( matrix, this.font, this.title, this.width / 2, 8, RGB( 255, 255, 255 ) );
		
		super.render( matrix, p_230430_2_, p_230430_3_, partial_ticks );
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
