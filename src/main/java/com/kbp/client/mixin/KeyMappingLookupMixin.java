package com.kbp.client.mixin;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.kbp.client.api.IPatchedKeyMapping;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyMappingLookup;
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
import java.util.Map;
import java.util.Objects;

@Mixin( KeyMappingLookup.class )
public abstract class KeyMappingLookupMixin
{
	@Shadow( remap = false )
	private static @Final EnumMap< KeyModifier, Map< Key, Collection< KeyMapping > > > map;
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Prioritize key mappings with their combo keys.
	 */
	@Overwrite( remap = false )
	public void put( Key key, KeyMapping km )
	{
		final var modifier = km.getKeyModifier();
		final var mapper = map.get( modifier );
		mapper.compute( key, ( k, v ) -> {
			if ( v == null )
			{
				final var lst = new ArrayList< KeyMapping >();
				lst.add( km );
				return lst;
			}
			else
			{
				final var update_lst = ( List< KeyMapping > ) v;
				final var prio_lst = Lists.transform( update_lst, o -> {
					final var ikm = ( IPatchedKeyMapping ) o;
					return ikm.getCmbKeys().size();
				} );
				
				final var ikm = ( IPatchedKeyMapping ) km;
				final var priority = MoreObjects.firstNonNull( ikm.getCmbKeys(), ImmutableSet.of() ).size();
				final var result = Collections.binarySearch( Lists.reverse( prio_lst ), priority );
				final var index = result < 0 ? -result - 1 : result;
				update_lst.add( update_lst.size() - index, km );
				return update_lst;
			}
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason The implementation of this method has been modified in production environment.
	 */
	@Overwrite( remap = false )
	public List< KeyMapping > getAll( Key key )
	{
		return (
			map.values().stream()
			.map( m -> m.get( key ) )
			.filter( Objects::nonNull )
			.flatMap( Collection::stream )
			.toList()
		);
	}
}
