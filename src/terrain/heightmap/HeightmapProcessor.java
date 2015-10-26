/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package terrain.heightmap;

import java.util.List;

/**
 * A <code>HeightmapProcessor</code> receives a {@link Heightmap} as input,
 * modifies it and returns the modified instance.
 * It is used to chain multiple heightmap modifications together.
 * @author Sebastian Weiss
 */
public interface HeightmapProcessor {
	/**
	 * Describes a property that is edited in the ui
	 */
	interface PropItem {
		/**
		 * The displayed text, including the current value
		 * @return 
		 */
		String getText();
		/**
		 * Changes the property.
		 * @param up {@code true} increase value, {@code false} decrease it
		 * @return {@code true} if the property has changed and the terrain should be regenerated
		 */
		boolean change(boolean up);
	}
	/**
	 * Applies the modification algorithm to the heightmap
	 * @param map the input heightmap
	 * @return the changed heightmap, the same instance
	 */
	Heightmap apply(Heightmap map);
	
	/**
	 * Sets a seed used for random generators.
	 * @param seed the new seed
	 */
	void setSeed(long seed);
	
	List<? extends PropItem> getProperties();
}
