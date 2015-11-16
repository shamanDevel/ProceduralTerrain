/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import org.shaman.terrain.Heightmap;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class PerlinNoiseProcessor implements HeightmapProcessor {
	private int index;
	private Noise noise;
	private double frequency;
	private double amplitude;

	public PerlinNoiseProcessor() {
		noise = new Noise(new Random().nextInt());
	}

	public PerlinNoiseProcessor(int index, double frequency, double amplitude) {
		this();
		this.index = index;
		this.frequency = frequency;
		this.amplitude = amplitude;
	}
	
	public double getFrequency() {
		return frequency;
	}

	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	public double getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(double amplitude) {
		this.amplitude = amplitude;
	}

	@Override
	public Heightmap apply(Heightmap map) {
		double scale = frequency / map.getSize();
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float v = map.getHeightAt(x, y);
				v += noise.noise(x*scale, y*scale)*amplitude;
				map.setHeightAt(x, y, v);
			}
		}
		return map;
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
				return "Perlin "+index+": Frequency="+format.format(frequency);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					frequency *= 1.5f;
				} else {
					frequency /= 1.5f;
				}
				return true;
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Perlin "+index+": Amplitude="+format.format(amplitude);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					amplitude *= 1.5f;
				} else {
					amplitude /= 1.5f;
				}
				return true;
			}
		}
		
		);
	}
	
}
