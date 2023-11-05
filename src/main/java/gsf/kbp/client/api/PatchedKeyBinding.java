package gsf.kbp.client.api;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import gsf.kbp.client.IKeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Do not cast vanilla {@link KeyMapping} instance to this class! you will get a
 * bad cast exception if you did try. Cast it to {@link IPatchedKeyBinding} if
 * you do need to use the patched functionalities on vanilla key bindings. This
 * class only exists to make the creation of key binding with default
 * combinations more convenient.
 */
public class PatchedKeyBinding extends KeyMapping implements IPatchedKeyBinding
{
	public PatchedKeyBinding(
		String description,
		IKeyConflictContext key_conflict_context,
		Key key,
		Set< Key > combinations,
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
		
		protected Key key = InputConstants.UNKNOWN;
		
		protected Set< Key > combinations = Collections.emptySet();
		
		protected IKeyConflictContext conflict_context = KeyConflictContext.IN_GAME;
		
		public Builder( String description ) {
			this.description = description;
		}
		
		public Builder withCategory( String category )
		{
			this.category = category;
			return this;
		}
		
		public Builder withKey( Key key )
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
		
		public Builder withCombinations( Key... combinations )
		{
			this.combinations = new HashSet<>( Arrays.asList( combinations ) );
			return this;
		}
		
		public Builder withKeyboardCombinations( int... combinations )
		{
			this.combinations = new HashSet<>();
			for ( int code : combinations )
			{
				final Key input = Type.KEYSYM.getOrCreate( code );
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
