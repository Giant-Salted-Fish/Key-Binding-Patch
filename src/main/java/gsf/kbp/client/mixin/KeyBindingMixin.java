package gsf.kbp.client.mixin;

import gsf.kbp.client.CombinationKey;
import gsf.kbp.client.IKeyBinding;
import gsf.kbp.client.api.IPatchedKeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.extensions.IForgeKeybinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;


@Mixin( KeyBinding.class )
public abstract class KeyBindingMixin implements IKeyBinding, IForgeKeybinding
{
	// Shadow fields and methods.
	@Final
	@Shadow
	private static Map< String, KeyBinding > ALL;
	
	@Final
	@Shadow
	private static KeyBindingMap MAP;
	static
	{
		// No longer use it, so clear it.
		// TODO: Fix this, this seems to have no effect.
		MAP.clearMap();
	}
	
	@Final
	@Shadow
	private Input defaultKey;
	
	@Shadow
	boolean isDown;
	
	@Shadow
	private int clickCount;
	
	@Shadow
	private KeyModifier keyModifierDefault;
	
	@Shadow
	private KeyModifier keyModifier;
	
	@Shadow
	public abstract void setKey( Input p_197979_1_ );
	
	
	// Added and fields and overwrite methods.
	@Unique
	private static final HashMap< Input, List< IKeyBinding > >
		UPDATE_TABLE = new HashMap<>();
	
	@Unique
	private static final HashMap< Input, CombinationKey >
		COMBINATION_TABLE = new HashMap<>();
	
	@Unique
	private static final HashSet< Input > ACTIVE_INPUTS = new HashSet<>();
	
	
	@Unique
	private Set< Input > default_combinations = Collections.emptySet();
	
	@Unique
	private Set< Input > combination_set = this.defaultCombinations();
	
	@Unique
	private final ArrayList< BooleanSupplier > condition_list = new ArrayList<>();
	
	@Unique
	private final HashMap< String, Runnable > press_callbacks = new HashMap<>();
	
	@Unique
	private final HashMap< String, Runnable > release_callbacks = new HashMap<>();
	
	@Inject(
		method = "<init>(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;Lnet/minecraftforge/client/settings/KeyModifier;Lnet/minecraft/client/util/InputMappings$Input;Ljava/lang/String;)V",
		at = @At( "RETURN" )
	)
	private void onNew(
		String description,
		IKeyConflictContext keyConflictContext,
		KeyModifier keyModifier,
		Input keyCode,
		String category,
		CallbackInfo info
	) {
		this.default_combinations = __getCombination( keyModifier );
		this.setKeyAndCombinations( keyCode, this.defaultCombinations() );
		
		this.keyModifierDefault = KeyModifier.NONE;
		this.keyModifier = KeyModifier.NONE;
	}
	
	@Override
	public Set< Input > defaultCombinations() {
		return this.default_combinations;
	}
	
	@Override
	public Set< Input > combinations() {
		return this.combination_set;
	}
	
	@Override
	public void setKeyAndCombinations( Input key, Set< Input > combinations )
	{
		this.setKey( key );
		this.combination_set = combinations;
	}
	
	@Override
	public String addPressCallback( Runnable callback )
	{
		String identifier = callback.toString();
		while ( this.press_callbacks.containsKey( identifier ) ) {
			identifier += "_";
		}
		
		this.regisPressCallback( identifier, callback );
		return identifier;
	}
	
	@Override
	public Optional< Runnable > regisPressCallback(
		String identifier, Runnable callback
	) {
		final Runnable old = this.press_callbacks.put( identifier, callback );
		return Optional.ofNullable( old );
	}
	
	@Override
	public Runnable unregisPressCallback( String identifier ) {
		return this.press_callbacks.remove( identifier );
	}
	
	@Override
	public String addReleaseCallback( Runnable callback )
	{
		String identifier = callback.toString();
		while ( this.release_callbacks.containsKey( identifier ) ) {
			identifier += "_";
		}
		
		this.regisPressCallback( identifier, callback );
		return identifier;
	}
	
	@Override
	public Optional< Runnable > regisReleaseCallback(
		String identifier, Runnable callback
	) {
		final Runnable old = this.release_callbacks.put( identifier, callback );
		return Optional.ofNullable( old );
	}
	
	@Override
	public Runnable unregisReleaseCallback( String identifier ) {
		return this.release_callbacks.remove( identifier );
	}
	
	@Override
	public boolean _isActive() {
		return this.isDown;
	}
	
	@Override
	public final ArrayList< BooleanSupplier > _conditionList() {
		return this.condition_list;
	}
	
	@Override
	public final void _incrClickCount() {
		this.clickCount += 1;
	}
	
	@Override
	public final void _setDefaultCombinations( Set< Input > combinations ) {
		this.default_combinations = combinations;
	}
	
	@Override
	public final KeyBinding _cast() {
		return ( KeyBinding ) ( Object ) this;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public boolean isDown() {
		return this.isDown;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public boolean same( KeyBinding other )
	{
		final IKeyConflictContext ctx0 = this.getKeyConflictContext();
		final IKeyConflictContext ctx1 = other.getKeyConflictContext();
		final boolean is_context_conflict = ctx0.conflicts( ctx1 );
		if ( !is_context_conflict ) {
			return this.getKey().equals( other.getKey() );
		}
		
		final IPatchedKeyBinding other_ = ( IPatchedKeyBinding ) other;
		final Set< Input > cmb0 = this.combinations();
		final Set< Input > cmb1 = other_.combinations();
		final Input key0 = this.getKey();
		final Input key1 = other.getKey();
		return(
			cmb0.contains( key1 ) || cmb1.contains( key0 )
			|| key0.equals( key1 ) && cmb0.equals( cmb1 )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public ITextComponent getTranslatedKeyMessage()
	{
		final StringTextComponent plus = new StringTextComponent( " + " );
		final String key = this.getKey().getDisplayName().getString();
		final String msg = this.combinations().stream()
			.map( Input::getDisplayName )
			.map( ITextComponent::getString )
			.reduce( ( k0, k1 ) -> k0 + " + " + k1 )
			.map( s -> s + " + " + key )
			.orElse( key );
		return new StringTextComponent( msg );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public boolean isDefault()
	{
		return(
			this.getKey().equals( this.defaultKey )
			&& this.combinations().equals( this.defaultCombinations() )
		);
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public String saveString()
	{
		// This is kind of hacky. See GameSettingsMixin.
		final String key = this.getKey().getName();
		final String modifier = KeyModifier.NONE.toString();
		final String combinations = this.combinations().stream()
			.map( Input::getName )
			.reduce( ( s0, s1 ) -> s0 + "+" + s1 )
			.orElse( "" );
		return String.format( "%s:%s:%s", key, modifier, combinations );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public void setDown( boolean is_down )
	{
		// Although our implementation can guarantee the #setDown(boolean) \
		// will only be called when the active state of the key is changed, \
		// we still have to check before firing callbacks as #setDown(boolean) \
		// is a public method and can be called by other mods.
		if ( is_down )
		{
			if ( !this.isDown && this.getKeyConflictContext().isActive() )
			{
				this.isDown = true;
				this.press_callbacks.values().forEach( Runnable::run );
			}
		}
		else
		{
			if ( this.isDown )
			{
				this.isDown = false;
				this.release_callbacks.values().forEach( Runnable::run );
			}
		}
	}
	
	@Override
	public void setKeyModifierAndCode( KeyModifier keyModifier, Input keyCode )
	{
		final Set< Input > combinations = __getCombination( keyModifier );
		this.setKeyAndCombinations( keyCode, combinations );
	}
	
	@Override
	public void setToDefault() {
		this.setKeyAndCombinations( this.defaultKey, this.defaultCombinations() );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void click( Input key )
	{
		final Iterator< IKeyBinding > itr = UPDATE_TABLE
			.getOrDefault( key, Collections.emptyList() ).iterator();
		while ( itr.hasNext() )
		{
			final IKeyBinding kb = itr.next();
			if ( !kb._isActive() ) {
				continue;
			}
			
			kb._incrClickCount();
			
			final int priority = kb.combinations().size();
			while ( itr.hasNext() )
			{
				final IKeyBinding inner_ikb = itr.next();
				final int inner_pri = inner_ikb.combinations().size();
				if ( inner_pri != priority ) {
					break;
				}
				
				inner_ikb._incrClickCount();
			}
			break;
		}
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void set( Input key, boolean is_down )
	{
		if ( !is_down ) {
			ACTIVE_INPUTS.remove( key );
		}
		else
		{
			final boolean is_already_active = !ACTIVE_INPUTS.add( key );
			if ( is_already_active ) {
				return;
			}
		}
		
		final CombinationKey ck = COMBINATION_TABLE.get( key );
		if ( ck != null ) {
			ck.is_down = is_down;
		}
		
		final List< IKeyBinding > lst =
			UPDATE_TABLE.getOrDefault( key, Collections.emptyList() );
		if ( is_down )
		{
			final Iterator< IKeyBinding > itr = lst.iterator();
			while ( itr.hasNext() )
			{
				final IKeyBinding kb = itr.next();
				if ( !__isCombinationActive( kb ) ) {
					continue;
				}
				
				kb._cast().setDown( true );
				if ( !kb._isActive() ) {
					continue;
				}
				
				final int priority = kb.combinations().size();
				while ( itr.hasNext() )
				{
					final IKeyBinding inner_kb = itr.next();
					final int inner_pri = inner_kb.combinations().size();
					if ( inner_pri != priority ) {
						break;
					}
					
					inner_kb._cast().setDown( true );
				}
				break;
			}
		}
		else {
			lst.forEach( kb -> kb._cast().setDown( false ) );
		}
	}
	
	@Unique
	private static boolean __isCombinationActive( IKeyBinding kb )
	{
		for ( BooleanSupplier condition : kb._conditionList() )
		{
			final boolean is_inactive = !condition.getAsBoolean();
			if ( is_inactive ) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void setAll()
	{
		// Copied from overwrite method. It seems that the original \
		// implementation only cares about the keyboard keys.
		final Minecraft mc = Minecraft.getInstance();
		final long window_handle = mc.getWindow().getWindow();
		final Function< Input, Boolean > check_active = input -> {
			final boolean is_keyboard_key = input.getType() == Type.KEYSYM;
			return(
				is_keyboard_key && input != InputMappings.UNKNOWN
				&& InputMappings.isKeyDown( window_handle, input.getValue() )
			);
		};
		
		// TODO: Figure out what is this for?
		ACTIVE_INPUTS.clear();
		
		COMBINATION_TABLE.forEach( ( key, ck ) -> {
			final boolean is_active = check_active.apply( key );
			if ( is_active ) {
				ACTIVE_INPUTS.add( key );
			}
			ck.is_down = is_active;
		} );
		
		ALL.values().forEach( kb -> {
			final Input key = kb.getKey();
			final boolean is_active = check_active.apply( key );
			if ( is_active ) {
				ACTIVE_INPUTS.add( key );
			}
			
			final IKeyBinding ikb = ( IKeyBinding ) kb;
			if ( __isCombinationActive( ikb ) && ikb._isActive() != is_active ) {
				kb.setDown( is_active );
			}
		} );
	}
	
	/**
	 * @author Giant_Salted_Fish
	 * @reason Patch logic.
	 */
	@Overwrite
	public static void resetMapping()
	{
		UPDATE_TABLE.clear();
		COMBINATION_TABLE.clear();
		
		ALL.values().stream().map( kb -> ( IKeyBinding ) kb ).forEach( kb -> {
			if ( kb._cast().getKey() == InputMappings.UNKNOWN ) {
				return;
			}
			
			__addToUpdateTable( kb );
			
			final List< BooleanSupplier > condition_lst = kb._conditionList();
			condition_lst.clear();
			kb.combinations().forEach(
				c -> condition_lst.add( __getCombinationKey( c ) )
			);
		} );
	}
	
	@Unique
	private static void __addToUpdateTable( IKeyBinding kb )
	{
		UPDATE_TABLE.compute( kb._cast().getKey(), ( k, lst ) -> {
			if ( lst == null ) {
				lst = new ArrayList<>();
			}
			
			final int priority = kb.combinations().size();
			
			// TODO: Maybe with binary search?
			int idx = 0;
			final int size = lst.size();
			while ( idx < size )
			{
				final IKeyBinding cur_ikb = lst.get( idx );
				final int cur_pri = cur_ikb.combinations().size();
				if ( priority > cur_pri ) {
					break;
				}
				
				idx += 1;
			}
			
			lst.add( idx, kb );
			return lst;
		} );
	}
	
	@Unique
	private static BooleanSupplier __getCombinationKey( Input key )
	{
		final CombinationKey ck_ = COMBINATION_TABLE.compute(
			key, ( k, ck ) -> ck != null ? ck : new CombinationKey() );
		return () -> ck_.is_down;
	}
	
	@Unique
	private static Set< Input > __getCombination( KeyModifier modifier )
	{
		// Transform modifier to corresponding combination key.
		final HashSet< Input > combinations = new HashSet<>();
		final IntConsumer modifier_processor = m -> {
			final Input input = Type.KEYSYM.getOrCreate( m );
			combinations.add( input );
		};
		
		switch( modifier )
		{
			case CONTROL:
				modifier_processor.accept( GLFW.GLFW_KEY_LEFT_CONTROL );
				break;
			case SHIFT:
				modifier_processor.accept( GLFW.GLFW_KEY_LEFT_SHIFT );
				break;
			case ALT:
				modifier_processor.accept( GLFW.GLFW_KEY_LEFT_ALT );
				break;
		}
		return combinations;
	}
}
