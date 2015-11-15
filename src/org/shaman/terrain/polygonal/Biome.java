/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

/**
 *
 * @author Sebastian Weiss
 */
public enum Biome {
	
	SNOW,
	TUNDRA,
	BARE,
	SCORCHED,
	TAIGA,
	SHRUBLAND,
	TEMPERATE_DESERT,
	TEMPERATE_RAIN_FOREST,
	TEMPERATE_DECIDUOUS_FOREST,
	GRASSLAND,
	TROPICAL_RAIN_FOREST,
	TROPICAL_SEASONAL_FOREST,
	SUBTROPICAL_DESERT,
	
	BEACH,
	WATER;
	
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
			throw new IllegalArgumentException("arguments are out of bounds");
		}
		//Temperature index, from 1 (warm) to 4 (cold)
		int temp = (int) ((1-temperature) * 4);
		temp = Math.max(4, temp+1);
		
		//the moisture index, from 1 (dry) to 6 (wet)
		int moist = (int) ((1-moisture) * 6);
		moist = Math.max(6, moist+1);
		
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
