package com.kbp.client.gui;

import com.kbp.client.KBPMod;
import com.kbp.client.KBPModConfig;
import com.kbp.client.api.IPatchedKeyMapping;
import com.kbp.client.gui.ShadowCountList.Entry;
import com.kbp.client.impl.IKeyMapping;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kbp.client.gui.KBPConfigScreen.RGB;

@OnlyIn( Dist.CLIENT )
final class ShadowCountList extends ContainerObjectSelectionList< Entry >
{
	private final Button save_all_btn;
	private final int max_label_width;
	
	private final Map< KeyMapping, Integer > shadow_count = (
		KBPModConfig.SHADOW_KEY_MAPPINGS.get().stream()
		.map( KBPMod::findByName )
		.filter( Optional::isPresent )
		.map( Optional::get )
		.map( IPatchedKeyMapping::getKeyMapping )
		.collect( Collectors.groupingBy( Function.identity(), Collectors.summingInt( o -> 1 ) ) )
	);
	
	// {shadow_count} - {shadow_change} = previous count.
	private final HashMap< KeyMapping, Integer > shadow_change = new HashMap<>();
	
	
	ShadowCountList( KBPConfigScreen parent, Button save_all_btn )
	{
		super( parent.getMinecraft(), parent.width + 45, parent.height, 20, parent.height - 32, 20 );
		
		this.save_all_btn = save_all_btn;
		
		final var p_lst = (
			Arrays.stream( this.minecraft.options.keyMappings )
			.filter( km -> !( ( IKeyMapping ) km ).isShadowKeyMapping() )
			.map( km -> Pair.of( km, Component.translatable( km.getName() ) ) )
			.sorted( Comparator.comparing( Pair::getFirst ) )
			.toList()
		);
		
		final var grouped = (
			p_lst.stream()
			.collect( Collectors.groupingBy( p -> p.getFirst().getCategory() ) )
		);
		
		p_lst.stream()
			.map( Pair::getFirst )
			.map( KeyMapping::getCategory )
			.distinct()
			.forEachOrdered( category -> {
				final var label = Component.translatable( category );
				this.addEntry( new CategoryEntry( label ) );
				
				grouped.get( category ).stream()
					.map( p -> new ShadowCountEntry( p.getFirst(), p.getSecond() ) )
					.forEachOrdered( this::addEntry );
			} );
		
		this.max_label_width = (
			p_lst.stream()
			.map( Pair::getSecond )
			.map( this.minecraft.font::width )
			.max( Integer::compare )
			.orElseThrow( RuntimeException::new )
		);
	}
	
	@Override
	protected int getScrollbarPosition() {
		return super.getScrollbarPosition() + 15 + 20;
	}
	
	@Override
	public int getRowWidth() {
		return super.getRowWidth() + 32;
	}
	
	void _applyChanges()
	{
		KBPModConfig.SHADOW_KEY_MAPPINGS.set(
			this.shadow_count.entrySet().stream()
			.flatMap( e -> {
				final var name = e.getKey().getName();
				final var count = e.getValue();
				return Stream.generate( () -> name ).limit( count );
			} )
			.toList()
		);
		
		KBPModConfig.SHADOW_KEY_MAPPINGS.save();
	}
	
	
	static abstract class Entry extends ContainerObjectSelectionList.Entry< Entry > {
		// Pass.
	}
	
	
	private final class CategoryEntry extends Entry
	{
		private final Component label;
		private final int width;
		
		private CategoryEntry( Component label )
		{
			this.label = label;
			this.width = ShadowCountList.this.minecraft.font.width( label );
		}
		
		@Override
		public void render(
			@NotNull GuiGraphics graphics,
			int x,
			int y,
			int p_281333_,
			int p_282287_,
			int slot_height,
			int mouse_x,
			int mouse_y,
			boolean is_selected,
			float partial_ticks
		) {
			final var mc = ShadowCountList.this.minecraft;
			final var screen = Objects.requireNonNull( mc.screen );
			final var pos_x = ( screen.width - this.width ) / 2;
			final var pos_y = y + slot_height - 9 - 1;
			graphics.drawString( mc.font, this.label, pos_x, pos_y, RGB( 255, 255, 255 ), false );
		}
		
		@Nullable
		@Override
		public ComponentPath nextFocusPath( @NotNull FocusNavigationEvent p_265672_ ) {
			return null;
		}
		
		@NotNull
		@Override
		public List< ? extends GuiEventListener > children() {
			return List.of();
		}
		
		@NotNull
		@Override
		public List< ? extends NarratableEntry > narratables() {
			return List.of();
		}
	}
	
	
	private final class ShadowCountEntry extends Entry
	{
		private final KeyMapping key_mapping;
		private final Component label;
		private final Button reduce_count_btn;
		private final Button increase_count_btn;
		private final Button count_field;
		
		private ShadowCountEntry( KeyMapping km, Component label )
		{
			this.key_mapping = km;
			this.label = label;
			this.reduce_count_btn = (
				Button.builder( Component.literal( "-" ), btn -> this.__shiftShadowCount( -1 ) )
				.bounds( 0, 0, 20, 20 )
				.build()
			);
			this.increase_count_btn = (
				Button.builder( Component.literal( "+" ), btn -> this.__shiftShadowCount( 1 ) )
				.bounds( 0, 0, 20, 20 )
				.build()
			);
			
			final var count = Integer.toString( this.__getShadowCount() );
			final var count_field = (
				Button.builder( Component.literal( count ), btn -> { } )
				.bounds( 0, 0, 20, 20 )
				.build()
			);
			count_field.active = false;
			this.count_field = count_field;
		}
		
		@Override
		public void render(
			@NotNull GuiGraphics graphics,
			int x,
			int y,
			int p_281373_,
			int p_283433_,
			int slot_height,
			int mouse_x,
			int mouse_y,
			boolean is_selected,
			float partial_ticks
		) {
			final var font = ShadowCountList.this.minecraft.font;
			final var pos_x = p_281373_ + 90 - ShadowCountList.this.max_label_width;
			final var pos_y = y + slot_height / 2 - 4;
			graphics.drawString( font, this.label, pos_x, pos_y, RGB( 255, 255, 255 ), false );
			
			final var count = this.__getShadowCount();
			final var rcb = this.reduce_count_btn;
			rcb.setX( p_281373_ + 105 );
			rcb.setY( y );
			rcb.active = count > 0;
			rcb.render( graphics, mouse_x, mouse_y, partial_ticks );
			
			final var cf = this.count_field;
			cf.setX( p_281373_ + 127 );
			cf.setY( y );
			cf.render( graphics, mouse_x, mouse_y, partial_ticks );
			
			final var icb = this.increase_count_btn;
			icb.setX( p_281373_ + 149 );
			icb.setY( y );
			icb.active = count < 5;
			icb.render( graphics, mouse_x, mouse_y, partial_ticks );
		}
		
		@NotNull
		@Override
		public List< ? extends GuiEventListener > children() {
			return List.of( this.reduce_count_btn, this.count_field, this.increase_count_btn );
		}
		
		@NotNull
		@Override
		public List< ? extends NarratableEntry > narratables() {
			return List.of();
		}
		
		private int __getShadowCount() {
			return ShadowCountList.this.shadow_count.getOrDefault( this.key_mapping, 0 );
		}
		
		private void __shiftShadowCount( int delta )
		{
			final var count = this.__getShadowCount() + delta;
			final var text = Component.literal( Integer.toString( count ) );
			this.count_field.setMessage( text );
			
			final var kb = this.key_mapping;
			ShadowCountList.this.shadow_count.compute( kb, ( k, v ) -> count != 0 ? count : null );
			final var shadow_change = ShadowCountList.this.shadow_change;
			shadow_change.compute( kb, ( k, v ) -> {
				final var prev_delta = v != null ? v : 0;
				final var new_delta = prev_delta + delta;
				return new_delta != 0 ? new_delta : null;
			} );
			ShadowCountList.this.save_all_btn.active = !shadow_change.isEmpty();
		}
	}
}
