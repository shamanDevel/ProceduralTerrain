/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import org.shaman.terrain.Heightmap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@code HeightmapProcessor} that links multiple processors together.
 * @author Sebastian Weiss
 */
public class ChainProcessor extends ArrayList<HeightmapProcessor> implements HeightmapProcessor {

	public ChainProcessor() {
	}

	public ChainProcessor(Collection<? extends HeightmapProcessor> c) {
		super(c);
	}

	public ChainProcessor(int initialCapacity) {
		super(initialCapacity);
	}
	
	@Override
	public Heightmap apply(Heightmap map) {
		for (HeightmapProcessor p : this) {
			map = p.apply(map);
		}
		return map;
	}

	@Override
	public void reseed() {
		for (HeightmapProcessor p : this) {
			p.reseed();
		}
	}

	@Override
	public List<? extends PropItem> getProperties() {
		ArrayList<PropItem> items = new ArrayList<>();
		for (HeightmapProcessor p : this) {
			items.addAll(p.getProperties());
		}
		return items;
	}
}
