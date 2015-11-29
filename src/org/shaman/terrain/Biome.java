/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.math.ColorRGBA;

/**
 *
 * @author Sebastian Weiss
 */
public enum Biome {
	
	SNOW(new ColorRGBA(1, 1, 1, 1)),
	TUNDRA(new ColorRGBA(0.8f, 0.8f, 0.8f, 1)),
	BARE(new ColorRGBA(0.6f, 0.6f, 0.6f, 1)),
	SCORCHED(new ColorRGBA(0.4f, 0.4f, 0.4f, 1f)),
	TAIGA(new ColorRGBA(0.5f, 0.7f, 0.5f, 1)),
	SHRUBLAND(new ColorRGBA(0.7f, 0.7f, 0.5f, 1)),
	TEMPERATE_DESERT(new ColorRGBA(0.5f, 0.5f, 0.3f, 1)),
	TEMPERATE_RAIN_FOREST(new ColorRGBA(0.1f, 0.9f, 0.1f, 1)),
	TEMPERATE_DECIDUOUS_FOREST(new ColorRGBA(0.3f, 0.5f, 0, 1)),
	GRASSLAND(new ColorRGBA(0.6f, 0.8f, 0, 1)),
	TROPICAL_RAIN_FOREST(new ColorRGBA(0, 0.5f, 0, 1)),
	TROPICAL_SEASONAL_FOREST(new ColorRGBA(0.3f, 0.6f, 0, 1)),
	SUBTROPICAL_DESERT(new ColorRGBA(1, 0.6f, 0, 1)),
	
	BEACH(new ColorRGBA(0.9f, 0.9f, 0, 1)),
	LAKE(new ColorRGBA(0, 0.4f, 0.8f, 1)),
	OCEAN(new ColorRGBA(0, 0, 0.5f, 1));

	private Biome(ColorRGBA color) {
		this.color = color;
	}
	
	public final ColorRGBA color;
	
	/**
	 * Retrives the biome based on the temperature and moisture.
	 * The temperature ranges from 0 (cold) to 1 (hot).
	 * The moisture ranges from 0 (dry) to 1 (wet)
	 * @param temperature
	 * @param moisture
	 * @return the associated biome
	 * @throws IllegalArgumentException if temperature
	 * or moisture is outside the bounds [0,1].
	 */
	public static Biome getBiome(float temperature, float moisture) {
		if (temperature<0 || temperature>1 
				|| moisture<0 || moisture>1) {
			throw new IllegalArgumentException("arguments are out of bounds: temperature="+temperature+" moisture="+moisture);
		}
		//Temperature index, from 1 (warm) to 4 (cold)
		int temp = (int) ((1-temperature) * 4);
		temp = Math.min(4, Math.max(1, temp+1));
		
		//the moisture index, from 1 (dry) to 6 (wet)
		int moist = (int) ((1-moisture) * 6);
		moist = Math.min(6, Math.max(1, moist+1));
		
		switch (moist) {
			case 1:
				switch (temp) {
					case 1: return SUBTROPICAL_DESERT;
					case 2: return TEMPERATE_DESERT;
					case 3: return TEMPERATE_DESERT;
					case 4: return SCORCHED;
				} break;
			case 2:
				switch (temp) {
					case 1: return GRASSLAND;
					case 2: return GRASSLAND;
					case 3: return TEMPERATE_DESERT;
					case 4: return BARE;
				} break;
			case 3:
				switch (temp) {
					case 1: return TROPICAL_SEASONAL_FOREST;
					case 2: return GRASSLAND;
					case 3: return SHRUBLAND;
					case 4: return TUNDRA;
				} break;
			case 4:
				switch (temp) {
					case 1: return TROPICAL_SEASONAL_FOREST;
					case 2: return TEMPERATE_DECIDUOUS_FOREST;
					case 3: return SHRUBLAND;
					case 4: return SNOW;
				} break;
			case 5:
				switch (temp) {
					case 1: return TROPICAL_RAIN_FOREST;
					case 2: return TEMPERATE_DECIDUOUS_FOREST;
					case 3: return TAIGA;
					case 4: return SNOW;
				} break;
			case 6:
				switch (temp) {
					case 1: return TROPICAL_RAIN_FOREST;
					case 2: return TEMPERATE_RAIN_FOREST;
					case 3: return TAIGA;
					case 4: return SNOW;
				}
		}
		throw new IllegalArgumentException("unkown temperature-moisture combination: "
				+temp+"-"+moist);
	}
}
