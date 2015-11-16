/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import org.shaman.terrain.Heightmap;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Sebastian Weiss
 */
public class ThermalErosionProcessor implements HeightmapProcessor {
	private static final int[][] NEIGHBORS = new int[][]{
		{1, -1}, {1, 0}, {1, 1},
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, 1}, {0, -1}
	};
	private float c = 0.25f;
	private float T = 2f;
	private int iterations = 5;

	public ThermalErosionProcessor() {
	}

	public ThermalErosionProcessor(float c, float T, int iterations) {
		this.c = c;
		this.T = T;
		this.iterations = iterations;
	}

	@Override
	public Heightmap apply(Heightmap map) {
		for (int i=0; i<iterations; ++i) {
			map = oneIteration(map);
		}
		return map;
	}
	private Heightmap oneIteration(Heightmap map) {
		Heightmap m = map.clone();
		float t = T / map.getSize();
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float h = map.getHeightAt(x, y);
				float[] hi = new float[NEIGHBORS.length];
				float[] di = new float[NEIGHBORS.length];
				float dmax = 0;
				float dtotal = 0;
				//compute slopes
				for (int i=0; i<NEIGHBORS.length; ++i) {
					hi[i] = map.getHeightAtClamping(x + NEIGHBORS[i][0], y + NEIGHBORS[i][1]);
					di[i] = h-hi[i];
					dmax = Math.max(dmax, di[i]);
					if (di[i] > t) {
						dtotal+=di[i];
					}
				}
				//move terrain
				float vsum = 0;
				for (int i=0; i<NEIGHBORS.length; ++i) {
					if (di[i] > t) {
						float v = c*(dmax-t)*di[i]/dtotal;
						m.adjustHeightAt(x + NEIGHBORS[i][0], y + NEIGHBORS[i][1], v);
						vsum+=v;
					}
				}
				m.adjustHeightAt(x, y, -vsum);
			}
		}
		return m;
	}

	@Override
	public void reseed() {
		//no randomness
	}

	@Override
	public List<? extends PropItem> getProperties() {
		final DecimalFormat format = new DecimalFormat("0.000");
		return Arrays.asList(
		new PropItem() {

			@Override
			public String getText() {
				return "Thermal Erosion: Iterations="+iterations;
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					iterations+=5;
					return true;
				} else {
					if (iterations==0) return false;
					iterations = Math.max(0, iterations-5);
					return true;
				}
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Thermal Erosion: c="+format.format(c);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					c+=0.01f;
				} else {
					if (c<=0) return false;
					c = Math.max(0, c-0.01f);
				}
				return true;
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Thermal Erosion: T="+format.format(T);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					T+=0.1f;
				} else {
					if (T<=0) return false;
					T = Math.max(0, T-0.1f);
				}
				return true;
			}
		}
		);
	}
	
}
