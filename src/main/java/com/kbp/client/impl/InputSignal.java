package com.kbp.client.impl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;

@OnlyIn( Dist.CLIENT )
public final class InputSignal
{
	public int active_count = 0;
	public final HashSet< Runnable > press_callbacks = new HashSet<>();
	public final HashSet< Runnable > release_callbacks = new HashSet<>();
}
