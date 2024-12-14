package com.kbp.client.gui;

import com.google.common.collect.ImmutableList;
import com.kbp.client.KBPModConfig;
import com.kbp.client.impl.IKeyBindingImpl;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.AbstractOptionList;
import net.minecraft.client.gui.widget.list.KeyBindingList;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn( Dist.CLIENT )
final class ShadowCountList extends AbstractOptionList< KeyBindingList.Entry >
{
	private static final int WHITE = Objects.requireNonNull( Color.fromLegacyFormat( TextFormatting.WHITE ) ).getValue();
	
	private final Button save_all_btn;
	private final int max_label_width;
	
	private final Map< String, Integer > shadow_count = (
		KBPModConfig.SHADOW_KEY_BINDINGS.get().stream()
		.collect( Collectors.groupingBy( Function.identity(), Collectors.summingInt( o -> 1 ) ) )
	);
	
	/**
	 * {@link #shadow_count} = {previous count} - {shadow_change}
	 */
	private final HashMap< String, Integer > shadow_change = new HashMap<>();
	
	
	ShadowCountList( KBPConfigScreen parent, Button save_all_btn )
	{
		super( parent.getMinecraft(), parent.width + 45, parent.height, 23, parent.height - 32, 20 );
		
		this.save_all_btn = save_all_btn;
		
		final KeyBinding[] kb_arr = (
			Arrays.stream( this.minecraft.options.keyMappings )
			.filter( kb -> !IKeyBindingImpl.isShadowKeyBinding( kb ) )
			.toArray( KeyBinding[]::new )
		);
		
		final Map< String, List< ShadowCountEntry > > grouped = Arrays.stream( kb_arr ).collect( Collectors.groupingBy(
			KeyBinding::getCategory,
			Collectors.mapping( ShadowCountEntry::new, Collectors.toList() )
		) );
		
		Arrays.stream( kb_arr )
			.map( KeyBinding::getCategory )
			.distinct()
			.forEachOrdered( category -> {
				final ITextComponent label = new TranslationTextComponent( category );
				this.addEntry( new CategoryEntry( label ) );
				grouped.get( category ).stream()
					.sorted( Comparator.comparing( e -> e.label_text.getString() ) )
					.forEachOrdered( this::addEntry );
			} );
		
		this.max_label_width = (
			grouped.values().stream()
			.flatMap( Collection::stream )
			.map( e -> e.label_text )
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
		KBPModConfig.SHADOW_KEY_BINDINGS.set(
			this.shadow_count.entrySet().stream()
			.flatMap( e -> {
				final String name = e.getKey();
				final int cnt = e.getValue();
				return Stream.generate( () -> name ).limit( cnt );
			} )
			.collect( Collectors.toList() )
		);
		
		KBPModConfig.SHADOW_KEY_BINDINGS.save();
	}
	
	
	private final class CategoryEntry extends KeyBindingList.Entry
	{
		private final ITextComponent label;
		private final int width;
		
		private CategoryEntry( ITextComponent label )
		{
			this.label = label;
			this.width = ShadowCountList.this.minecraft.font.width( label );
		}
		
		@Override
		public void render(
			@Nonnull MatrixStack matrix,
			int x,
			int y,
			int p_230432_4_,
			int p_230432_5_,
			int slot_height,
			int mouse_x,
			int mouse_y,
			boolean is_selected,
			float partial_ticks
		) {
			final Minecraft mc = ShadowCountList.this.minecraft;
			final Screen parent = Objects.requireNonNull( mc.screen );
			final FontRenderer font = mc.font;
			final float pos_x = ( parent.width - this.width ) * 0.5F;
			final float pos_y = y + slot_height - font.lineHeight - 1;
			font.draw( matrix, this.label, pos_x, pos_y, WHITE );
		}
		
		@Override
		public boolean changeFocus( boolean p_231049_1_ ) {
			return false;
		}
		
		@Nonnull
		@Override
		public List< ? extends IGuiEventListener > children() {
			return Collections.emptyList();
		}
	}
	
	private final class ShadowCountEntry extends KeyBindingList.Entry
	{
		private final String kb_name;
		private final ITextComponent label_text;
		private final Button reduce_count_btn;
		private final Button increase_count_btn;
		private final Button count_field;
		
		private ShadowCountEntry( KeyBinding kb )
		{
			final String name = kb.getName();
			this.kb_name = name;
			this.label_text = new TranslationTextComponent( name );
			
			final int count = this.__getShadowCount();
			final ITextComponent text = new StringTextComponent( Integer.toString( count ) );
			final Button count_field = new Button( 0, 0, 20, 20, text, btn -> { } );
			count_field.active = false;
			this.count_field = count_field;
			
			final Button rdc_btn = new Button(
				0, 0,
				20, 20,
				new StringTextComponent( "-" ),
				this::__handleReduceBtnClick
			);
			rdc_btn.active = count > 0;
			this.reduce_count_btn = rdc_btn;
			
			final Button icr_btn = new Button(
				0, 0,
				20, 20,
				new StringTextComponent( "+" ),
				this::__handleIncreaseBtnClick
			);
			icr_btn.active = count < 5;
			this.increase_count_btn = icr_btn;
		}
		
		private void __handleReduceBtnClick( Button btn )
		{
			final int cnt = this.__shiftShadowCount( -1 );
			btn.active = cnt > 0;
			this.increase_count_btn.active = true;
		}
		
		private void __handleIncreaseBtnClick( Button btn )
		{
			final int cnt = this.__shiftShadowCount( 1 );
			btn.active = cnt < 5;
			this.reduce_count_btn.active = true;
		}
		
		@Override
		public void render(
			@Nonnull MatrixStack matrix,
			int x,
			int y,
			int p_230432_4_,
			int p_230432_5_,
			int slot_height,
			int mouse_x,
			int mouse_y,
			boolean is_selected,
			float partial_ticks
		) {
			final FontRenderer font = ShadowCountList.this.minecraft.font;
			final float pos_x = p_230432_4_ + 90 - ShadowCountList.this.max_label_width;
			final float pos_y = y + ( slot_height - font.lineHeight ) * 0.5F;
			font.draw( matrix, this.label_text, pos_x, pos_y, WHITE );
			
			final Button rcb = this.reduce_count_btn;
			rcb.x = p_230432_4_ + 105;
			rcb.y = y;
			rcb.render( matrix, mouse_x, mouse_y, partial_ticks );
			
			final Button cf = this.count_field;
			cf.x = p_230432_4_ + 127;
			cf.y = y;
			cf.render( matrix, mouse_x, mouse_y, partial_ticks );
			
			final Button icb = this.increase_count_btn;
			icb.x = p_230432_4_ + 149;
			icb.y = y;
			icb.render( matrix, mouse_x, mouse_y, partial_ticks );
		}
		
		@Nonnull
		@Override
		public List< ? extends IGuiEventListener > children() {
			return ImmutableList.of( this.reduce_count_btn, this.count_field, this.increase_count_btn );
		}
		
		@Override
		public boolean mouseClicked( double p_231044_1_, double p_231044_3_, int p_231044_5_ )
		{
			return (
				this.reduce_count_btn.mouseClicked( p_231044_1_, p_231044_3_, p_231044_5_ )
				|| this.increase_count_btn.mouseClicked( p_231044_1_, p_231044_3_, p_231044_5_ )
			);
		}
		
		@Override
		public boolean mouseReleased( double p_231048_1_, double p_231048_3_, int p_231048_5_ )
		{
			return (
				this.reduce_count_btn.mouseReleased( p_231048_1_, p_231048_3_, p_231048_5_ )
				|| this.increase_count_btn.mouseReleased( p_231048_1_, p_231048_3_, p_231048_5_ )
			);
		}
		
		private int __getShadowCount() {
			return ShadowCountList.this.shadow_count.getOrDefault( this.kb_name, 0 );
		}
		
		private int __shiftShadowCount( int delta )
		{
			final int count = this.__getShadowCount() + delta;
			final ITextComponent text = new StringTextComponent( Integer.toString( count ) );
			this.count_field.setMessage( text );
			
			final String kb = this.kb_name;
			ShadowCountList.this.shadow_count.compute( kb, ( k, v ) -> count != 0 ? count : null );
			final HashMap< String, Integer > shadow_change = ShadowCountList.this.shadow_change;
			shadow_change.compute( kb, ( k, v ) -> {
				final int prev_delta = v != null ? v : 0;
				final int new_delta = prev_delta + delta;
				return new_delta != 0 ? new_delta : null;
			} );
			ShadowCountList.this.save_all_btn.active = !shadow_change.isEmpty();
			return count;
		}
	}
}
