package gsf.kbp.client.api;

import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Do not cast vanilla {@link KeyBinding} instance to this class! you will get a
 * bad cast exception if you did try. Cast it to {@link IPatchedKeyBinding} if
 * you do need to use the patched functionalities on vanilla key bindings. This
 * class only exists to make the creation of key binding with default
 * combinations more convenient.
 */
public class PatchedKeyBinding extends KeyBinding implements IPatchedKeyBinding
{
	public PatchedKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		Input key,
		Set< Input > combinations,
		String category
	) {
		super( description, key_conflict_context, key, category );
		
		final IKeyBinding ikb = ( IKeyBinding ) this;
		ikb._setDefaultCombinations( combinations );
	}
	
	public static class Builder
	{
		protected final String description;
		
		protected String category = "key.categories.gameplay";
		
		protected Input key = InputMappings.UNKNOWN;
		
		protected Set< Input > combinations = Collections.emptySet();
		
		protected IKeyConflictContext conflict_context = KeyConflictContext.IN_GAME;
		
		public Builder( String description ) {
			this.description = description;
		}
		
		public Builder withCategory( String category )
		{
			this.category = category;
			return this;
		}
		
		public Builder withKey( Input key )
		{
			this.key = key;
			return this;
		}
		
		public Builder withKeyboardKey( int key_code )
		{
			this.key = Type.KEYSYM.getOrCreate( key_code );
			return this;
		}
		
		public Builder withMouseButton( int button )
		{
			this.key = Type.MOUSE.getOrCreate( button );
			return this;
		}
		
		public Builder withCombinations( Input... combinations )
		{
			this.combinations = new HashSet<>( Arrays.asList( combinations ) );
			return this;
		}
		
		public Builder withKeyboardCombinations( int... combinations )
		{
			this.combinations = new HashSet<>();
			for ( int code : combinations )
			{
				final Input input = Type.KEYSYM.getOrCreate( code );
				this.combinations.add( input );
			}
			return this;
		}
		
		public Builder withConflictContext( IKeyConflictContext context )
		{
			this.conflict_context = context;
			return this;
		}
		
		public PatchedKeyBinding build()
		{
			return new PatchedKeyBinding(
				this.description,
				this.conflict_context,
				this.key,
				this.combinations,
				this.category
			);
		}
		
		public PatchedKeyBinding buildAndRegis()
		{
			final PatchedKeyBinding kb = this.build();
			ClientRegistry.registerKeyBinding( kb );
			return kb;
		}
	}
}
