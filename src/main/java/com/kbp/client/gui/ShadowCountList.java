package com.kbp.client.gui;

import com.kbp.client.KBPModConfig;
import com.kbp.client.gui.ShadowCountList.Entry;
import com.kbp.client.impl.IKeyMappingImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn( Dist.CLIENT )
final class ShadowCountList extends ContainerObjectSelectionList< Entry >
{
	private static final int WHITE = Objects.requireNonNull( TextColor.fromLegacyFormat( ChatFormatting.WHITE ) ).getValue();
	
	private final Button save_all_btn;
	private final int max_label_width;
	
	private final Map< String, Integer > shadow_count = (
		KBPModConfig.SHADOW_KEY_MAPPINGS.get().stream()
		.collect( Collectors.groupingBy( Function.< String >identity(), Collectors.summingInt( o -> 1 ) ) )
	);
	
	/**
	 * {@link #shadow_count} = {previous count} - {shadow_change}
	 */
	private final HashMap< String, Integer > shadow_change = new HashMap<>();
	
	
	ShadowCountList( KBPConfigScreen parent, Button save_all_btn )
	{
		super( parent.getMinecraft(), parent.width + 45, parent.height, 20, parent.height - 32, 20 );
		
		this.save_all_btn = save_all_btn;
		
		final var km_arr = (
			Arrays.stream( this.minecraft.options.keyMappings )
			.filter( km -> !IKeyMappingImpl.isShadowKeyMapping( km ) )
			.toArray( KeyMapping[]::new )
		);
		
		final var grouped = Arrays.stream( km_arr ).collect( Collectors.groupingBy(
			KeyMapping::getCategory,
			Collectors.mapping( ShadowCountEntry::new, Collectors.toList() )
		) );
		
		Arrays.stream( km_arr )
			.map( KeyMapping::getCategory )
			.distinct()
			.forEachOrdered( category -> {
				final var label = Component.translatable( category );
				this.addEntry( new CategoryEntry( label ) );
				grouped.get( category ).stream()
					.sorted( Comparator.comparing( e -> e.label.getString() ) )
					.forEachOrdered( this::addEntry );
			} );
		
		this.max_label_width = (
			grouped.values().stream()
			.flatMap( Collection::stream )
			.map( e -> e.label )
			.mapToInt( this.minecraft.font::width )
			.max()
			.orElse( 0 )
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
		assert !this.shadow_change.isEmpty();
		KBPModConfig.SHADOW_KEY_MAPPINGS.set(
			this.shadow_count.entrySet().stream()
			.flatMap( e -> {
				final var name = e.getKey();
				final var count = e.getValue();
				return Stream.generate( () -> name ).limit( count );
			} )
			.toList()
		);
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
			final var font = mc.font;
			final var pos_x = ( screen.width - this.width ) / 2;
			final var pos_y = y + slot_height - font.lineHeight - 1;
			graphics.drawString( font, this.label, pos_x, pos_y, WHITE, false );
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
		private final String km_name;
		private final Component label;
		private final Button reduce_count_btn;
		private final Button increase_count_btn;
		private final Button count_field;
		
		private ShadowCountEntry( KeyMapping km )
		{
			final var name = km.getName();
			this.km_name = name;
			this.label = Component.translatable( name );
			
			final var count = this.__getShadowCount();
			final var count_field = (
				Button.builder( Component.literal( Integer.toString( count ) ), btn -> { } )
				.bounds( 0, 0, 20, 20 )
				.build()
			);
			count_field.active = false;
			this.count_field = count_field;
			
			final var rdc_btn = (
				Button.builder( Component.literal( "-" ), this::__handleReduceBtnClick )
				.bounds( 0, 0, 20, 20 )
				.build()
			);
			rdc_btn.active = count > 0;
			this.reduce_count_btn = rdc_btn;
			
			final var icr_btn = (
				Button.builder( Component.literal( "+" ), this::__handleIncreaseBtnClick )
				.bounds( 0, 0, 20, 20 )
				.build()
			);
			icr_btn.active = count < 5;
			this.increase_count_btn = icr_btn;
		}
		
		private void __handleReduceBtnClick( Button btn )
		{
			final var cnt = this.__shiftShadowCount( -1 );
			btn.active = cnt > 0;
			this.increase_count_btn.active = true;
		}
		
		private void __handleIncreaseBtnClick( Button btn )
		{
			final var cnt = this.__shiftShadowCount( 1 );
			btn.active = cnt < 5;
			this.reduce_count_btn.active = true;
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
			final var pos_y = y + ( slot_height - font.lineHeight ) / 2;
			graphics.drawString( font, this.label, pos_x, pos_y, WHITE, false );
			
			final var rcb = this.reduce_count_btn;
			rcb.setX( p_281373_ + 105 );
			rcb.setY( y );
			rcb.render( graphics, mouse_x, mouse_y, partial_ticks );
			
			final var cf = this.count_field;
			cf.setX( p_281373_ + 127 );
			cf.setY( y );
			cf.render( graphics, mouse_x, mouse_y, partial_ticks );
			
			final var icb = this.increase_count_btn;
			icb.setX( p_281373_ + 149 );
			icb.setY( y );
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
			return ShadowCountList.this.shadow_count.getOrDefault( this.km_name, 0 );
		}
		
		private int __shiftShadowCount( int delta )
		{
			final var count = this.__getShadowCount() + delta;
			final var text = Component.literal( Integer.toString( count ) );
			this.count_field.setMessage( text );
			
			final var kb = this.km_name;
			ShadowCountList.this.shadow_count.compute( kb, ( k, v ) -> count != 0 ? count : null );
			final var shadow_change = ShadowCountList.this.shadow_change;
			shadow_change.compute( kb, ( k, v ) -> {
				final var prev_delta = v != null ? v : 0;
				final var new_delta = prev_delta + delta;
				return new_delta != 0 ? new_delta : null;
			} );
			ShadowCountList.this.save_all_btn.active = !shadow_change.isEmpty();
			return count;
		}
	}
}
