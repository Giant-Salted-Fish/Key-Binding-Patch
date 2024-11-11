package com.kbp.client.gui;

import com.kbp.client.KBPMod;
import com.kbp.client.KBPModConfig;
import com.kbp.client.api.IPatchedKeyBinding;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SideOnly( Side.CLIENT )
final class GuiShadowCountList extends GuiListExtended
{
	private final ConfigGuiScreen parent_screen;
	private final GuiButton save_all_btn;
	private final IGuiListEntry[] list_entries;
	private final int max_label_width;
	
	private final Map< KeyBinding, Integer > shadow_count = (
		Arrays.stream( KBPModConfig.shadow_key_bindings )
		.map( KBPMod::findByName )
		.filter( Optional::isPresent )
		.map( Optional::get )
		.map( IPatchedKeyBinding::getKeyBinding )
		.collect( Collectors.groupingBy( Function.identity(), Collectors.summingInt( o -> 1 ) ) )
	);
	
	/**
	 * {previous count} = {shadow_count} - {shadow_change}
	 */
	private final HashMap< KeyBinding, Integer > shadow_change = new HashMap<>();
	
	
	GuiShadowCountList( ConfigGuiScreen parent, GuiButton save_all_btn )
	{
		super( parent.mc, parent.width + 45, parent.height, 23, parent.height - 32, 20 );
		
		this.parent_screen = parent;
		this.save_all_btn = save_all_btn;
		
		final KeyBinding[] kb_arr = (
			Arrays.stream( this.mc.gameSettings.keyBindings )
			.filter( kb -> !IKeyBindingImpl.isShadowKeyBinding( kb ) )
			.sorted()
			.toArray( KeyBinding[]::new )
		);
		
		final Map< String, List< KeyBinding > > grouped = (
			Arrays.stream( kb_arr )
			.collect( Collectors.groupingBy( KeyBinding::getKeyCategory ) )
		);
		
		this.list_entries = (
			Arrays.stream( kb_arr )
			.map( KeyBinding::getKeyCategory )
			.distinct()
			.flatMap( category -> Stream.concat(
				Stream.of( new CategoryEntry( category ) ),
				grouped.get( category ).stream().map( ShadowCountEntry::new )
			) )
			.toArray( IGuiListEntry[]::new )
		);
		
		this.max_label_width = (
			Arrays.stream( kb_arr )
			.map( KeyBinding::getKeyDescription )
			.map( I18n::format )
			.map( this.mc.fontRenderer::getStringWidth )
			.max( Integer::compare )
			.orElseThrow( IllegalStateException::new )
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
		KBPModConfig.shadow_key_bindings = (
			this.shadow_count.entrySet().stream()
			.flatMap( e -> {
				final String name = e.getKey().getKeyDescription();
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
		
		public void drawEntry(
			int slotIndex,
			int x,
			int y,
			int listWidth,
			int slotHeight,
			int mouseX,
			int mouseY,
			boolean isSelected,
			float partialTicks
		) {
			final Minecraft mc = GuiShadowCountList.this.mc;
			final FontRenderer font_renderer = mc.fontRenderer;
			final int pos_x = GuiShadowCountList.this.parent_screen.width / 2 - this.label_width / 2;
			final int pos_y = y + slotHeight - font_renderer.FONT_HEIGHT - 1;
			font_renderer.drawString( this.label_text, pos_x, pos_y, MathHelper.rgb( 255, 255, 255 ) );
		}
		
		@Override
		public boolean mousePressed(
			int slotIndex,
			int mouseX,
			int mouseY,
			int mouseEvent,
			int relativeX,
			int relativeY
		) {
			return false;
		}
		
		@Override
		public void mouseReleased(
			int slotIndex,
			int x,
			int y,
			int mouseEvent,
			int relativeX,
			int relativeY
		) { }
	}
	
	
	private final class ShadowCountEntry implements IGuiListEntry
	{
		private final KeyBinding key_binding;
		private final String label_text;
		private final GuiButton reduce_count_btn;
		private final GuiButton increase_count_btn;
		private final GuiButton count_field;
		
		private ShadowCountEntry( KeyBinding kb )
		{
			this.key_binding = kb;
			this.label_text = I18n.format( kb.getKeyDescription() );
			this.reduce_count_btn = new GuiButton( 0, 0, 0, 20, 20, "-" );
			this.increase_count_btn = new GuiButton( 0, 0, 0, 20, 20, "+" );
			
			final int count = this.__getShadowCount();
			final GuiButton count_field = new GuiButton( 0, 0, 0, 20, 20, Integer.toString( count ) );
			count_field.enabled = false;
			this.count_field = count_field;
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
			
			final int count = this.__getShadowCount();
			final GuiButton rcb = this.reduce_count_btn;
			rcb.x = x + 105;
			rcb.y = y;
			rcb.enabled = count > 0;
			rcb.drawButton( mc, mouseX, mouseY, partialTicks );
			
			final GuiButton cf = this.count_field;
			cf.x = x + 127;
			cf.y = y;
			cf.drawButton( mc, mouseX, mouseY, partialTicks );
			
			final GuiButton icb = this.increase_count_btn;
			icb.x = x + 149;
			icb.y = y;
			icb.enabled = count < 5;
			icb.drawButton( mc, mouseX, mouseY, partialTicks );
		}
		
		@Override
		public boolean mousePressed( int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY )
		{
			final Minecraft mc = GuiShadowCountList.this.mc;
			if ( this.reduce_count_btn.mousePressed( mc, mouseX, mouseY ) )
			{
				this.reduce_count_btn.playPressSound( mc.getSoundHandler() );
				this.__shiftShadowCount( -1 );
				return true;
			}
			
			if ( this.increase_count_btn.mousePressed( mc, mouseX, mouseY ) )
			{
				this.increase_count_btn.playPressSound( mc.getSoundHandler() );
				this.__shiftShadowCount( 1 );
				return true;
			}
			
			return false;
		}
		
		@Override
		public void mouseReleased( int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY )
		{
			this.reduce_count_btn.mouseReleased( x, y );
			this.increase_count_btn.mouseReleased( x, y );
		}
		
		private int __getShadowCount() {
			return GuiShadowCountList.this.shadow_count.getOrDefault( this.key_binding, 0 );
		}
		
		private void __shiftShadowCount( int delta )
		{
			final int count = this.__getShadowCount() + delta;
			this.count_field.displayString = Integer.toString( count );
			
			final KeyBinding kb = this.key_binding;
			GuiShadowCountList.this.shadow_count.compute( kb, ( k, v ) -> count != 0 ? count : null );
			final HashMap< KeyBinding, Integer > shadow_change = GuiShadowCountList.this.shadow_change;
			shadow_change.compute( kb, ( k, v ) -> {
				final int prev_delta = v != null ? v : 0;
				final int new_delta = prev_delta + delta;
				return new_delta != 0 ? new_delta : null;
			} );
			GuiShadowCountList.this.save_all_btn.enabled = !shadow_change.isEmpty();
		}
	}
}
