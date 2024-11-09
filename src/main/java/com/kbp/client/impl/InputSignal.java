package com.kbp.client.impl;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;

@SideOnly( Side.CLIENT )
public final class InputSignal
{
	public int active_count = 0;
	public final HashSet< Runnable > press_callbacks = new HashSet<>();
	public final HashSet< Runnable > release_callbacks = new HashSet<>();
}
