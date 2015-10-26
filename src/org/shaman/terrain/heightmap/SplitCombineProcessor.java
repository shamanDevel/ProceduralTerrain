/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code HeightmapProcessor} that splits the computation up into multiple
 * processors and combines the results then with given factors.
 * @author Sebastian Weiss
 */
public class SplitCombineProcessor implements HeightmapProcessor {
	private HeightmapProcessor[] processors;
	private float[] factors;

	public SplitCombineProcessor() {
		processors = new HeightmapProcessor[0];
		factors = new float[0];
	}

	public SplitCombineProcessor(HeightmapProcessor[] processors, float[] factors) {
		if (processors.length != factors.length) {
			throw new IllegalArgumentException("processors and factors must be of equal length");
		}
		this.processors = processors;
		this.factors = factors;
	}
	
	public void setProcessors(HeightmapProcessor[] processors, float[] factors) {
		if (processors.length != factors.length) {
			throw new IllegalArgumentException("processors and factors must be of equal length");
		}
		this.processors = processors;
		this.factors = factors;
	}

	@Override
	public Heightmap apply(Heightmap map) {
		for (int i=0; i<processors.length; ++i) {
			Heightmap m = map.clone();
			m = processors[i].apply(m);
			combine(map, factors[i], m);
		}
		return map;
	}
	private void combine(Heightmap target, float factor, Heightmap map) {
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				target.adjustHeightAt(x, y, factor * map.getHeightAt(x, y));
			}
		}
	}

	@Override
	public void reseed() {
		for (HeightmapProcessor p : processors) {
			p.reseed();
		}
	}

	@Override
	public List<? extends PropItem> getProperties() {
		final DecimalFormat format = new DecimalFormat("0.000");
		ArrayList<PropItem> items = new ArrayList<>();
		for (int i=0; i<processors.length; ++i) {
			HeightmapProcessor p = processors[i];
			final int index = i;
			items.add(new PropItem() {

				@Override
				public String getText() {
					return "CHAIN "+(index+1)+": weight="+format.format(factors[index]);
				}

				@Override
				public boolean change(boolean up) {
					if (up) {
						factors[index] += 0.1f;
						return true;
					} else {
						if (factors[index]==0) {
							return false;
						}
						factors[index] = Math.max(0, factors[index]-0.1f);
						return true;
					}
				}
			});
			items.addAll(p.getProperties());
		}
		return items;
	}
	
}
