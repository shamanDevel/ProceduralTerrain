/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import java.util.Collections;
import java.util.List;

/**
 * A processor that normalizes the heightmap to be in the range from 0 to 1.
 * @author Sebastian Weiss
 */
public class NormalizationProcessor implements HeightmapProcessor {

	@Override
	public Heightmap apply(Heightmap map) {
		float min = Float.MAX_VALUE;
		float max = -Float.MIN_VALUE;
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float v = map.getHeightAt(x, y);
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
		}
		float factor = 1f / (max-min);
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float v = map.getHeightAt(x, y);
				v -= min;
				v *= factor;
				map.setHeightAt(x, y, v);
			}
		}
		return map;
	}

	@Override
	public void reseed() {}

	@Override
	public List<? extends PropItem> getProperties() {
		return Collections.emptyList();
	}
	
}
