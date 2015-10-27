/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Sebastian Weiss
 */
public class DistortionProcessor implements HeightmapProcessor {
	private Noise noise;
	private float distortion;
	private float frequency;

	public DistortionProcessor(float distortion, float frequency) {
		this.distortion = distortion;
		this.frequency = frequency;
		noise = new Noise(new Random().nextLong());
	}

	public DistortionProcessor() {
		this(0.05f, 8);
	}

	public float getDistortion() {
		return distortion;
	}

	public void setDistortion(float distortion) {
		this.distortion = distortion;
	}

	public float getFrequency() {
		return frequency;
	}

	public void setFrequency(float frequency) {
		this.frequency = frequency;
	}
	
	private double snoise(double x, double y, double z) {
		return 2*noise.noise(x, y, z);
	}

	@Override
	public Heightmap apply(Heightmap map) {
		Heightmap m = new Heightmap(map.getSize());
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float s = x/(float)map.getSize();
				float t = y/(float)map.getSize();
				float ss = (float) (s + distortion * snoise(s*frequency, t*frequency, 0));
				float tt = (float) (t + distortion * snoise(s*frequency, t*frequency, 3.4));
				float v = map.getHeightInterpolating(ss*map.getSize(), tt*map.getSize());
				m.setHeightAt(x, y, v);
			}
		}
		return m;
	}

	@Override
	public void reseed() {
		noise = new Noise(new Random().nextLong());
	}

	@Override
	public List<? extends PropItem> getProperties() {
		final DecimalFormat format = new DecimalFormat("0.000");
		return Arrays.asList(
		new PropItem() {

			@Override
			public String getText() {
				return "Distortion: Strength="+format.format(distortion);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					distortion += 0.01f;
					return true;
				} else {
					if (distortion <=0 ) return false;
					distortion = Math.max(0, distortion - 0.01f);
					return true;
				}
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Distortion: Frequency="+format.format(frequency);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					frequency += 0.5f;
					return true;
				} else {
					if (frequency <=0 ) return false;
					frequency = Math.max(0, frequency - 0.5f);
					return true;
				}
			}
		}
		);
	}
	
}
