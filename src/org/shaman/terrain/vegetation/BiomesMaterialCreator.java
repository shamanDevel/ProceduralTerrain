/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import org.shaman.terrain.polygonal.*;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.shaman.terrain.Vectorfield;
import org.shaman.terrain.polygonal.Biome;

/**
 * Creates a terrain material out of biome information.
 * Extended copy of {@code org.shaman.terrain.polygonal.BiomesMaterialCreator}.
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
		"org/shaman/terrain/textures/Tundra.jpg"//,
		//"org/shaman/terrain/rock2.jpg"
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
		1/8f//,
		//1/16f
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
	private static final float EPSILON = 0.00001f;
	private static final float[] OCEAN_BIOME;
	static {
		OCEAN_BIOME = new float[Biome.values().length];
		OCEAN_BIOME[Biome.OCEAN.ordinal()] = 1;
	}
	
	private final AssetManager assetManager;
	private final Vectorfield biomes;
	private final int size;
	
	private Material mat;
	private Texture alpha1Tex, alpha2Tex, alpha3Tex;
	private Image alpha1, alpha2, alpha3;

	public BiomesMaterialCreator(AssetManager assetManager, Vectorfield biomes) {
		this.assetManager = assetManager;
		this.biomes = biomes;
		this.size = biomes.getSize();
		assert (Biome.values().length == biomes.getDimensions());
		
		//normalize biome vectorfield
		normalize();
		initMaterial();
	}
	private void normalize() {
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float[] v = biomes.getVectorAt(x, y);
				float sum = 0;
				for (int i=0; i<v.length; ++i) {
					sum += v[i]*v[i];
				}
				if (sum < EPSILON) {
					biomes.setVectorAt(x, y, OCEAN_BIOME);
				} else {
					sum = FastMath.sqrt(sum);
					for (int i=0; i<v.length; ++i) {
						v[i] /= sum;
					}
					biomes.setVectorAt(x, y, v);
				}
			}
		}
	}
	private void initMaterial() {
		mat = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
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
		
		//init textures
		updateMaterial();
	}
	
	public Material getMaterial() {
		return mat;
	}
	
	public Vectorfield getBiomes() {
		return biomes;
	}
	
	public void updateBiomes(Biome biome, float change) {
		
	}
	
	public void updateMaterial() {
		//get buffers
		ByteBuffer buf1, buf2, buf3;
		if (alpha1==null) {
			buf1 = BufferUtils.createByteBuffer(size*size*4);
			buf2 = BufferUtils.createByteBuffer(size*size*4);
			buf3 = BufferUtils.createByteBuffer(size*size*4);
		} else {
			buf1 = alpha1.getData(0);
			buf2 = alpha2.getData(0);
			buf3 = alpha3.getData(0);
		}
		
		//fill buffers
		buf1.rewind();
		buf2.rewind();
		buf3.rewind();
		float[] colors = new float[3*4];
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float[] v = biomes.getVectorAt(y, size-x-1);
				Arrays.fill(colors, 0);
				for (int i=0; i<v.length; ++i) {
					colors[BIOME_TO_TEXTURE[i]] += v[i];
				}
				//TODO: rock at steep slopes
				buf1.put((byte) (v[0]*255));
				buf1.put((byte) (v[1]*255));
				buf1.put((byte) (v[2]*255));
				buf1.put((byte) (v[3]*255));
				buf2.put((byte) (v[4]*255));
				buf2.put((byte) (v[5]*255));
				buf2.put((byte) (v[6]*255));
				buf2.put((byte) (v[7]*255));
				buf3.put((byte) (v[8]*255));
				buf3.put((byte) (v[9]*255));
				buf3.put((byte) (v[10]*255));
				buf3.put((byte) (v[11]*255));
			}
		}
		buf1.rewind();
		buf2.rewind();
		buf3.rewind();
		
		//send buffers to gpu
		if (alpha1==null) {
			alpha1 = new Image(Image.Format.RGBA8, size, size, buf1, ColorSpace.Linear);
			alpha2 = new Image(Image.Format.RGBA8, size, size, buf2, ColorSpace.Linear);
			alpha3 = new Image(Image.Format.RGBA8, size, size, buf3, ColorSpace.Linear);
			alpha1Tex = new Texture2D(alpha1);
			alpha2Tex = new Texture2D(alpha2);
			alpha3Tex = new Texture2D(alpha3);
			alpha1Tex.setMagFilter(Texture.MagFilter.Bilinear);
			alpha2Tex.setMagFilter(Texture.MagFilter.Bilinear);
			alpha3Tex.setMagFilter(Texture.MagFilter.Bilinear);
			mat.setTexture("AlphaMap", alpha1Tex);
			mat.setTexture("AlphaMap_1", alpha2Tex);
			mat.setTexture("AlphaMap_2", alpha3Tex);
		} else {
			alpha1.setData(buf1);
			alpha2.setData(buf2);
			alpha3.setData(buf3);
		}
	}

}
