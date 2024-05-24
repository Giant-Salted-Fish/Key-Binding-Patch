package com.kbp.client.gui;

import com.google.common.collect.ImmutableList;
import com.kbp.client.KBPMod;
import com.kbp.client.KBPModConfig;
import com.kbp.client.api.IPatchedKeyBinding;
import com.kbp.client.impl.IKeyBinding;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.AbstractOptionList;
import net.minecraft.client.gui.widget.list.KeyBindingList;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
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
final class ShadowCountList extends AbstractOptionList< KeyBindingList.Entry >
{
	private final Button save_all_btn;
	private final int max_label_width;
	
	private final Map< KeyBinding, Integer > shadow_count = (
		KBPModConfig.SHADOW_KEY_BINDINGS.get().stream()
		.map( KBPMod::findByName )
		.filter( Optional::isPresent )
		.map( Optional::get )
		.map( IPatchedKeyBinding::getKeyBinding )
		.collect( Collectors.groupingBy( Function.identity(), Collectors.summingInt( o -> 1 ) ) )
	);
	
	// {shadow_count} - {shadow_change} = previous count.
	private final HashMap< KeyBinding, Integer > shadow_change = new HashMap<>();
	
	
	ShadowCountList( KBPConfigScreen parent, Button save_all_btn )
	{
		super( parent.getMinecraft(), parent.width + 45, parent.height, 23, parent.height - 32, 20 );
		
		this.save_all_btn = save_all_btn;
		
		final List< Pair< KeyBinding, TranslationTextComponent > > p_lst = (
			Arrays.stream( this.minecraft.options.keyMappings )
			.filter( kb -> !( ( IKeyBinding ) kb ).isShadowKeyBinding() )
			.map( kb -> Pair.of( kb, new TranslationTextComponent( kb.getName() ) ) )
			.sorted( Comparator.comparing( Pair::getFirst ) )
			.collect( Collectors.toList() )
		);
		
		final Map< String, List< Pair< KeyBinding, TranslationTextComponent > > > grouped = (
			p_lst.stream()
			.collect( Collectors.groupingBy( p -> p.getFirst().getCategory() ) )
		);
		
		p_lst.stream()
			.map( Pair::getFirst )
			.map( KeyBinding::getCategory )
			.distinct()
			.forEachOrdered( category -> {
				final ITextComponent label = new TranslationTextComponent( category );
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
		KBPModConfig.SHADOW_KEY_BINDINGS.set(
			this.shadow_count.entrySet().stream()
			.flatMap( e -> {
				final String name = e.getKey().getName();
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
			final Screen screen = Objects.requireNonNull( mc.screen );
			final float pos_x = ( screen.width - this.width ) * 0.5F;
			final float pos_y = y + slot_height - 9 - 1;
			mc.font.draw( matrix, this.label, pos_x, pos_y, RGB( 255, 255, 255 ) );
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
		private final KeyBinding key_binding;
		private final ITextComponent label_text;
		private final Button reduce_count_btn;
		private final Button increase_count_btn;
		private final Button count_field;
		
		private ShadowCountEntry( KeyBinding kb, ITextComponent label )
		{
			this.key_binding = kb;
			this.label_text = label;
			this.reduce_count_btn = new Button(
				0, 0,
				20, 20,
				new StringTextComponent( "-" ),
				btn -> this.__shiftShadowCount( -1 )
			);
			this.increase_count_btn = new Button(
				0, 0,
				20, 20,
				new StringTextComponent( "+" ),
				btn -> this.__shiftShadowCount( 1 )
			);
			
			final String count = Integer.toString( this.__getShadowCount() );
			final ITextComponent text = new StringTextComponent( count );
			final Button count_field = new Button( 0, 0, 20, 20, text, btn -> { } );
			count_field.active = false;
			this.count_field = count_field;
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
			final float pos_y = y + ( slot_height - 9 ) * 0.5F;
			font.draw( matrix, this.label_text, pos_x, pos_y, RGB( 255, 255, 255 ) );
			
			final int count = this.__getShadowCount();
			final Button rcb = this.reduce_count_btn;
			rcb.x = p_230432_4_ + 105;
			rcb.y = y;
			rcb.active = count > 0;
			rcb.render( matrix, mouse_x, mouse_y, partial_ticks );
			
			final Button cf = this.count_field;
			cf.x = p_230432_4_ + 127;
			cf.y = y;
			cf.render( matrix, mouse_x, mouse_y, partial_ticks );
			
			final Button icb = this.increase_count_btn;
			icb.x = p_230432_4_ + 149;
			icb.y = y;
			icb.active = count < 5;
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
			return ShadowCountList.this.shadow_count.getOrDefault( this.key_binding, 0 );
		}
		
		private void __shiftShadowCount( int delta )
		{
			final int count = this.__getShadowCount() + delta;
			final ITextComponent text = new StringTextComponent( Integer.toString( count ) );
			this.count_field.setMessage( text );
			
			final KeyBinding kb = this.key_binding;
			ShadowCountList.this.shadow_count.compute( kb, ( k, v ) -> count != 0 ? count : null );
			final HashMap< KeyBinding, Integer > shadow_change = ShadowCountList.this.shadow_change;
			shadow_change.compute( kb, ( k, v ) -> {
				final int prev_delta = v != null ? v : 0;
				final int new_delta = prev_delta + delta;
				return new_delta != 0 ? new_delta : null;
			} );
			ShadowCountList.this.save_all_btn.active = !shadow_change.isEmpty();
		}
	}
}
