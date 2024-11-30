package com.kbp.client.gui;

import com.kbp.client.KBPModConfig;
import com.kbp.client.impl.IKeyBindingImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SideOnly( Side.CLIENT )
final class GuiShadowCountList extends GuiListExtended
{
	private final GuiConfigScreen parent_screen;
	private final GuiButton save_all_btn;
	private final IGuiListEntry[] list_entries;
	private final int max_label_width;
	
	private final Map< String, Integer > shadow_count = (
		Arrays.stream( KBPModConfig.shadow_key_bindings )
		.collect( Collectors.groupingBy( Function.identity(), Collectors.summingInt( o -> 1 ) ) )
	);
	
	/**
	 * {@link #shadow_count} = {previous count} - {shadow_change}
	 */
	private final HashMap< String, Integer > shadow_change = new HashMap<>();
	
	
	GuiShadowCountList( GuiConfigScreen parent, GuiButton save_all_btn )
	{
		super( parent.mc, parent.width + 45, parent.height, 23, parent.height - 32, 20 );
		
		this.parent_screen = parent;
		this.save_all_btn = save_all_btn;
		
		final KeyBinding[] kb_arr = (
			Arrays.stream( this.mc.gameSettings.keyBindings )
			.filter( kb -> !IKeyBindingImpl.isShadowKeyBinding( kb ) )
			.toArray( KeyBinding[]::new )
		);
		
		final Map< String, List< ShadowCountEntry > > grouped = Arrays.stream( kb_arr ).collect(
			Collectors.groupingBy(
				KeyBinding::getKeyCategory,
				Collectors.mapping( ShadowCountEntry::new, Collectors.toList() )
			)
		);
		
		this.list_entries = (
			Arrays.stream( kb_arr )
			.map( KeyBinding::getKeyCategory )
			.distinct()
			.flatMap( category -> Stream.concat(
				Stream.of( new CategoryEntry( category ) ),
				grouped.get( category ).stream().sorted( Comparator.comparing( e -> e.label_text ) )
			) )
			.toArray( IGuiListEntry[]::new )
		);
		
		this.max_label_width = (
			grouped.values().stream()
			.flatMap( Collection::parallelStream )
			.map( e -> e.label_text )
			.mapToInt( this.mc.fontRenderer::getStringWidth )
			.max()
			.orElse( 0 )
		);
	}
	
	@Override
	protected int getSize() {
		return this.list_entries.length;
	}
	
	@Nonnull
	@Override
	public IGuiListEntry getListEntry( int index ) {
		return this.list_entries[ index ];
	}
	
	@Override
	protected int getScrollBarX() {
		return super.getScrollBarX() + 35;
	}
	
	@Override
	public int getListWidth() {
		return super.getListWidth() + 32;
	}
	
	void _applyChanges()
	{
		assert !this.shadow_change.isEmpty();
		KBPModConfig.shadow_key_bindings = (
			this.shadow_count.entrySet().stream()
			.flatMap( e -> {
				final String name = e.getKey();
				final int cnt = e.getValue();
				return Stream.generate( () -> name ).limit( cnt );
			} )
			.toArray( String[]::new )
		);
	}
	
	
	private final class CategoryEntry implements IGuiListEntry
	{
		private final String label_text;
		private final int label_width;
		
		private CategoryEntry( String name )
		{
			this.label_text = I18n.format( name );
			
			final Minecraft mc = GuiShadowCountList.this.mc;
			this.label_width = mc.fontRenderer.getStringWidth( this.label_text );
		}
		
		@Override
		public void updatePosition( int slotIndex, int x, int y, float partialTicks ) {
		}
		
		public void drawEntry( int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks )
		{
			final Minecraft mc = GuiShadowCountList.this.mc;
			final FontRenderer font_renderer = mc.fontRenderer;
			final int pos_x = GuiShadowCountList.this.parent_screen.width / 2 - this.label_width / 2;
			final int pos_y = y + slotHeight - font_renderer.FONT_HEIGHT - 1;
			font_renderer.drawString( this.label_text, pos_x, pos_y, MathHelper.rgb( 255, 255, 255 ) );
		}
		
		@Override
		public boolean mousePressed( int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY ) {
			return false;
		}
		
		@Override
		public void mouseReleased( int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY ) {
		}
	}
	
	
	private final class ShadowCountEntry implements IGuiListEntry
	{
		private final String kb_name;
		private final String label_text;
		private final GuiButton reduce_count_btn;
		private final GuiButton increase_count_btn;
		private final GuiButton count_field;
		
		private ShadowCountEntry( KeyBinding kb )
		{
			final String name = kb.getKeyDescription();
			this.kb_name = name;
			this.label_text = I18n.format( name );
			
			final int count = this.__getShadowCount();
			final GuiButton count_field = new GuiButton( 0, 0, 0, 20, 20, Integer.toString( count ) );
			count_field.enabled = false;
			this.count_field = count_field;
			
			final GuiButton rdc_btn = new GuiButton( 0, 0, 0, 20, 20, "-" );
			rdc_btn.enabled = count > 0;
			this.reduce_count_btn = rdc_btn;
			
			final GuiButton icr_btn = new GuiButton( 0, 0, 0, 20, 20, "+" );
			icr_btn.enabled = count < 5;
			this.increase_count_btn = icr_btn;
		}
		
		@Override
		public void updatePosition( int slotIndex, int x, int y, float partialTicks ) {
		}
		
		@Override
		public void drawEntry( int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks )
		{
			final Minecraft mc = GuiShadowCountList.this.mc;
			final int pos_x = x + 90 - GuiShadowCountList.this.max_label_width;
			final int pos_y = y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2;
			mc.fontRenderer.drawString( this.label_text, pos_x, pos_y, MathHelper.rgb( 255, 255, 255 ) );
			
			final GuiButton rcb = this.reduce_count_btn;
			rcb.x = x + 105;
			rcb.y = y;
			rcb.drawButton( mc, mouseX, mouseY, partialTicks );
			
			final GuiButton cf = this.count_field;
			cf.x = x + 127;
			cf.y = y;
			cf.drawButton( mc, mouseX, mouseY, partialTicks );
			
			final GuiButton icb = this.increase_count_btn;
			icb.x = x + 149;
			icb.y = y;
			icb.drawButton( mc, mouseX, mouseY, partialTicks );
		}
		
		@Override
		public boolean mousePressed( int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY )
		{
			final Minecraft mc = GuiShadowCountList.this.mc;
			if ( this.reduce_count_btn.mousePressed( mc, mouseX, mouseY ) )
			{
				this.reduce_count_btn.playPressSound( mc.getSoundHandler() );
				final int cnt = this.__shiftShadowCount( -1 );
				this.reduce_count_btn.enabled = cnt > 0;
				this.increase_count_btn.enabled = true;
				return true;
			}
			else if ( this.increase_count_btn.mousePressed( mc, mouseX, mouseY ) )
			{
				this.increase_count_btn.playPressSound( mc.getSoundHandler() );
				final int cnt = this.__shiftShadowCount( 1 );
				this.increase_count_btn.enabled = cnt < 5;
				this.reduce_count_btn.enabled = true;
				return true;
			}
			else {
				return false;
			}
		}
		
		@Override
		public void mouseReleased( int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY )
		{
			this.reduce_count_btn.mouseReleased( x, y );
			this.increase_count_btn.mouseReleased( x, y );
		}
		
		private int __getShadowCount() {
			return GuiShadowCountList.this.shadow_count.getOrDefault( this.kb_name, 0 );
		}
		
		private int __shiftShadowCount( int delta )
		{
			final int count = this.__getShadowCount() + delta;
			this.count_field.displayString = Integer.toString( count );
			
			final String kb = this.kb_name;
			GuiShadowCountList.this.shadow_count.compute( kb, ( k, v ) -> count != 0 ? count : null );
			final HashMap< String, Integer > shadow_change = GuiShadowCountList.this.shadow_change;
			shadow_change.compute( kb, ( k, v ) -> {
				final int prev_delta = v != null ? v : 0;
				final int new_delta = prev_delta + delta;
				return new_delta != 0 ? new_delta : null;
			} );
			GuiShadowCountList.this.save_all_btn.enabled = !shadow_change.isEmpty();
			return count;
		}
	}
}
