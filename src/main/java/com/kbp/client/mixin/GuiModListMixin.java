package com.kbp.client.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.GuiModList;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( GuiModList.class )
public abstract class GuiModListMixin extends GuiScreen
{
	@Inject( method = "initGui", at = @At( "RETURN" ) )
	private void onInitGui( CallbackInfo ci ) {
		Keyboard.enableRepeatEvents( true );  // For mod search input.
	}
	
	@Override
	public void onGuiClosed()
	{
		super.onGuiClosed();
		
		Keyboard.enableRepeatEvents( false );
	}
}
