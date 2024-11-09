package com.kbp.client.mixin;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.IntHashMap;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

@Mixin( KeyBindingMap.class )
public abstract class KeyBindingMapMixin
{
	@Shadow( remap = false )
	private static @Final EnumMap< KeyModifier, IntHashMap< Collection< KeyBinding > > > map;
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Prioritize key bindings with their combo keys.
	 */
	@Overwrite( remap = false )
	public void addKey( int key_code, KeyBinding kb )
	{
		final KeyModifier modifier = kb.getKeyModifier();
		final IntHashMap< Collection< KeyBinding > > mapper = map.get( modifier );
		final Collection< KeyBinding > lookup = mapper.lookup( key_code );
		if ( lookup == null )
		{
			final ArrayList< KeyBinding > lst = new ArrayList<>();
			lst.add( kb );
			mapper.addKey( key_code, lst );
		}
		else
		{
			final List< KeyBinding > update_lst = ( List< KeyBinding > ) lookup;
			final List< Integer > prio_lst = Lists.transform( update_lst, o -> {
				final IPatchedKeyBinding ikb = ( IPatchedKeyBinding ) o;
				return ikb.getCmbKeys().size();
			} );
			
			final IPatchedKeyBinding ikb = ( IPatchedKeyBinding ) kb;
			final int priority = MoreObjects.firstNonNull( ikb.getCmbKeys(), ImmutableSet.of() ).size();
			final int result = Collections.binarySearch( Lists.reverse( prio_lst ), priority );
			final int index = result < 0 ? -result - 1 : result;
			update_lst.add( update_lst.size() - index, kb );
		}
	}
}
