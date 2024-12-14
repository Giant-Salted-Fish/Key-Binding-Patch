package com.kbp.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@OnlyIn( Dist.CLIENT )
public final class KBPConfigScreen extends Screen
{
	private static final int WHITE = Objects.requireNonNull( TextColor.fromLegacyFormat( ChatFormatting.WHITE ) ).getValue();
	
	private final Screen parent_screen;
	private ShadowCountList shadow_count_list;
	
	public KBPConfigScreen( Screen parent )
	{
		super( new TranslatableComponent( "kbp.gui.config_title" ) );
		
		this.parent_screen = parent;
	}
	
	@Override
	protected void init()
	{
		final var cancel_btn = new Button(
			this.width / 2 - 155, this.height - 29,
			150, 20,
			CommonComponents.GUI_CANCEL,
			btn -> Objects.requireNonNull( this.minecraft ).setScreen( this.parent_screen )
		);
		this.addRenderableWidget( cancel_btn );
		
		final var save_btn = new Button(
			this.width / 2 - 155 + 160, this.height - 29,
			150, 20,
			CommonComponents.GUI_DONE,
			btn -> {
				this.shadow_count_list._applyChanges();
				
				final var mc = Objects.requireNonNull( this.minecraft );
				final var alert_screen = new AlertScreen(
					() -> mc.setScreen( this.parent_screen ),
					new TranslatableComponent( "kbp.gui.alert_title" ),
					new TranslatableComponent( "kbp.gui.alert_message" ),
					new TranslatableComponent( "kbp.gui.alert_confirm" )
				);
				mc.setScreen( alert_screen );
			}
		);
		save_btn.active = false;
		this.addRenderableWidget( save_btn );
		
		final var shadow_lst = new ShadowCountList( this, save_btn );
		this.shadow_count_list = shadow_lst;
		this.addWidget( shadow_lst );
	}
	
	@Override
	public void render( @NotNull PoseStack pose, int p_96563_, int p_96564_, float partial_ticks )
	{
		this.renderBackground( pose );
		this.shadow_count_list.render( pose, p_96563_, p_96564_, partial_ticks );
		drawCenteredString( pose, this.font, this.title, this.width / 2, 8, WHITE );
		
		super.render( pose, p_96563_, p_96564_, partial_ticks );
	}
}
