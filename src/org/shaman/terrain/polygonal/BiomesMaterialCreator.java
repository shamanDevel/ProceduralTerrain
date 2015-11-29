/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.shaman.terrain.Vectorfield;
import org.shaman.terrain.Biome;

/**
 * Creates a terrain material out of biome information
 * @author Sebastian Weiss
 */
public class BiomesMaterialCreator {
	private static final Logger LOG = Logger.getLogger(BiomesMaterialCreator.class.getName());
	private static final String[] TEXTURES = {
		"org/shaman/terrain/textures/Beach.jpg",
		"org/shaman/terrain/textures/Desert.jpg",
		"org/shaman/terrain/textures/Grass1.jpg",
		"org/shaman/terrain/textures/Grass2.jpg",
		"org/shaman/terrain/textures/Leaves.jpg",
		"org/shaman/terrain/textures/Scorched.tga",
		"org/shaman/terrain/textures/Shrubland.tga",
		"org/shaman/terrain/textures/Snow.jpg",
		"org/shaman/terrain/textures/Stone.jpg",
		"org/shaman/terrain/textures/Tundra.jpg",
		"org/shaman/terrain/rock2.jpg"
	};
	private static final float[] TEXTURE_SCALES = {
		1/8f,
		1/8f,
		1/8f,
		1/8f,
		1/2f,
		1/8f,
		1/8f,
		1/8f,
		1/8f,
		1/8f,
		1/16f
	};
	private static final int[] BIOME_TO_TEXTURE = new int[Biome.values().length];
	static {
		BIOME_TO_TEXTURE[Biome.SNOW.ordinal()] = 7;
		BIOME_TO_TEXTURE[Biome.TUNDRA.ordinal()] = 9;
		BIOME_TO_TEXTURE[Biome.BARE.ordinal()] = 8;
		BIOME_TO_TEXTURE[Biome.SCORCHED.ordinal()] = 5;
		BIOME_TO_TEXTURE[Biome.TAIGA.ordinal()] = 9;
		BIOME_TO_TEXTURE[Biome.SHRUBLAND.ordinal()] = 6;
		BIOME_TO_TEXTURE[Biome.TEMPERATE_DESERT.ordinal()] = 1;
		BIOME_TO_TEXTURE[Biome.TEMPERATE_RAIN_FOREST.ordinal()] = 2;
		BIOME_TO_TEXTURE[Biome.TEMPERATE_DECIDUOUS_FOREST.ordinal()] = 4;
		BIOME_TO_TEXTURE[Biome.GRASSLAND.ordinal()] = 3;
		BIOME_TO_TEXTURE[Biome.TROPICAL_RAIN_FOREST.ordinal()] = 2;
		BIOME_TO_TEXTURE[Biome.TROPICAL_SEASONAL_FOREST.ordinal()] = 2;
		BIOME_TO_TEXTURE[Biome.SUBTROPICAL_DESERT.ordinal()] = 1;
		BIOME_TO_TEXTURE[Biome.BEACH.ordinal()] = 0;
		BIOME_TO_TEXTURE[Biome.LAKE.ordinal()] = 0;
		BIOME_TO_TEXTURE[Biome.OCEAN.ordinal()] = 0;
	}
	private static final float SLOPE_SCALE = 100f;
	private static final float SLOPE_POWER = 2f;
	
	public static Material createTerrainMaterial(Vectorfield biomes, AssetManager assetManager) {
		Material mat = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        mat.setBoolean("useTriPlanarMapping", true);
        mat.setFloat("Shininess", 0.0f);
		
		//assign textures
		for (int i=0; i<TEXTURES.length; ++i) {
			String nameSlot = i==0 ? "DiffuseMap" : ("DiffuseMap_"+i);
			String scaleSlot = "DiffuseMap_"+i+"_scale";
			Texture tex = assetManager.loadTexture(TEXTURES[i]);
			tex.setWrap(Texture.WrapMode.Repeat);
			tex.setAnisotropicFilter(4);
			mat.setTexture(nameSlot, tex);
			mat.setFloat(scaleSlot, TEXTURE_SCALES[i]);
		}
		
		//create alpha maps
		mat.setTexture("AlphaMap", createAlphaMap(biomes, 0, 1, 2, 3));
		mat.setTexture("AlphaMap_1", createAlphaMap(biomes, 4, 5, 6, 7));
		mat.setTexture("AlphaMap_2", createAlphaMap(biomes, 8, 9, -1, -1));
		
		LOG.info("biomes material created");
		
		return mat;
	}
	
	private static Texture createAlphaMap(Vectorfield biomes, int slot1, int slot2, int slot3, int slot4) {
		ByteBuffer data = BufferUtils.createByteBuffer(biomes.getSize()*biomes.getSize()*4);
		data.rewind();
		for (int x=0; x<biomes.getSize(); ++x) {
			for (int y=0; y<biomes.getSize(); ++y) {
				float[] v = biomes.getVectorAt(y, biomes.getSize()-x-1);
				float r=0, g=0, b=0, a=0;
				for (int i=0; i<Biome.values().length; ++i) {
					if (BIOME_TO_TEXTURE[i]==slot1) {
						r += v[i];
					} else if (BIOME_TO_TEXTURE[i]==slot2) {
						g += v[i];
					} else if (BIOME_TO_TEXTURE[i]==slot3) {
						b += v[i];
					} else if (BIOME_TO_TEXTURE[i]==slot4) {
						a += v[i];
					}
				}
				float sum = (float) Math.sqrt(r*r + g*g + b*b + a*a);
				if (sum != 0) {
					r /= sum;
					g /= sum;
					b /= sum;
					a /= sum;
				}
				data.put((byte) (255*r)).put((byte) (255*g)).put((byte) (255*b)).put((byte) (255*a));
			}
		}
		data.rewind();
		Image image = new Image(Image.Format.RGBA8, biomes.getSize(), biomes.getSize(), data, ColorSpace.Linear);
		Texture tex = new Texture2D(image);
		tex.setMagFilter(Texture.MagFilter.Bilinear);
		return tex;
	}
}
