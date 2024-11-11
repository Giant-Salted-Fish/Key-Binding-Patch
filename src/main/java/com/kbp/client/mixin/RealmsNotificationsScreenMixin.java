package com.kbp.client.mixin;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( RealmsNotificationsScreen.class )
public abstract class RealmsNotificationsScreenMixin
{
	@Inject(
		method = "removed",
		at = @At( "HEAD" )
	)
	private void onRemoved( CallbackInfo ci ) {
		Keyboard.enableRepeatEvents( false );
	}
}
