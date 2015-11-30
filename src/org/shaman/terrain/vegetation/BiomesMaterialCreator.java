/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Logger;
import org.shaman.terrain.Vectorfield;
import org.shaman.terrain.Biome;
import org.shaman.terrain.vegetation.BiomesMaterialCreator;

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
		1/16f,
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
		BIOME_TO_TEXTURE[Biome.TAIGA.ordinal()] = 3;
		BIOME_TO_TEXTURE[Biome.SHRUBLAND.ordinal()] = 6;
		BIOME_TO_TEXTURE[Biome.TEMPERATE_DESERT.ordinal()] = 1;
		BIOME_TO_TEXTURE[Biome.TEMPERATE_RAIN_FOREST.ordinal()] = 2;
		BIOME_TO_TEXTURE[Biome.TEMPERATE_DECIDUOUS_FOREST.ordinal()] = 4;
		BIOME_TO_TEXTURE[Biome.GRASSLAND.ordinal()] = 2;
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
	
	private Material texturedMat;
	private Texture alpha1Tex, alpha2Tex, alpha3Tex;
	private Image alpha1, alpha2, alpha3;
	private Material plainMat;
	private Texture plainTex;
	private Image plain;

	public BiomesMaterialCreator(AssetManager assetManager, Vectorfield biomes) {
		this.assetManager = assetManager;
		this.biomes = biomes;
		this.size = biomes.getSize();
		assert (Biome.values().length == biomes.getDimensions());
		
		//normalize biome vectorfield
		normalize();
		initTexturedMaterial();
		initPlainMaterial();
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
	private void initTexturedMaterial() {
		texturedMat = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        texturedMat.setBoolean("useTriPlanarMapping", true);
        texturedMat.setFloat("Shininess", 0.0f);
		
		//init alpha maps
		updateTexturedMaterial();
		
		//assign textures
		for (int i=0; i<TEXTURES.length; ++i) {
			String nameSlot = i==0 ? "DiffuseMap" : ("DiffuseMap_"+i);
			String scaleSlot = "DiffuseMap_"+i+"_scale";
			Texture tex = assetManager.loadTexture(TEXTURES[i]);
			tex.setWrap(Texture.WrapMode.Repeat);
			tex.setAnisotropicFilter(4);
			texturedMat.setTexture(nameSlot, tex);
			texturedMat.setFloat(scaleSlot, TEXTURE_SCALES[i]);
		}
		
	}
	private void initPlainMaterial() {
		plainMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		updatePlainMaterial();
	}
	
	public Material getMaterial(boolean textured) {
		if (textured) {
			return texturedMat;
		} else {
			return plainMat;
		}
	}
	
	public Vectorfield getBiomes() {
		return biomes;
	}
	
	public void updateBiomes(int x, int y, float[] biomes) {
		this.biomes.setVectorAt(x, y, biomes);
	}
	
	public void updateBiomes(int x, int y, Biome biome, float change) {
		float[] v = biomes.getVectorAt(x, y);
		int index = biome.ordinal();
		v[index] = Math.max(0, Math.min(1, v[index]+change));
		float sum = 0;
		for (int i=0; i<v.length; ++i) {
			if (i!=index) {
				sum += v[i]*v[i];
			}
		}
		if (sum>0 && sum<EPSILON) {
			for (int i=0; i<v.length; ++i) {
				if (i!=index) {
					v[i] = 0;
				}
			}
		} else if (sum>0) {
			float factor = FastMath.sqrt((1-v[index]*v[index]) / sum);
			for (int i=0; i<v.length; ++i) {
				if (i!=index) {
					v[i]*=factor;
				}
			}
		}
		for (int i=0; i<v.length; ++i) {
			v[i] = clamp(v[i]);
		}
		biomes.setVectorAt(x, y, v);
	}
	
	public void updateMaterial(boolean textured) {
		if (textured) {
			updateTexturedMaterial();
		} else {
			updatePlainMaterial();
		}
	}
	private void updateTexturedMaterial() {
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
					colors[BIOME_TO_TEXTURE[i]] += 1-(1-v[i])*(1-v[i]);
				}
				for (int i=0; i<colors.length; ++i) {
					colors[i] = Math.max(0, Math.min(1, colors[i]));
				}
				//TODO: rock at steep slopes
				buf1.put((byte) (colors[0]*255));
				buf1.put((byte) (colors[1]*255));
				buf1.put((byte) (colors[2]*255));
				buf1.put((byte) (colors[3]*255));
				buf2.put((byte) (colors[4]*255));
				buf2.put((byte) (colors[5]*255));
				buf2.put((byte) (colors[6]*255));
				buf2.put((byte) (colors[7]*255));
				buf3.put((byte) (colors[8]*255));
				buf3.put((byte) (colors[9]*255));
				buf3.put((byte) (colors[10]*255));
				buf3.put((byte) (colors[11]*255));
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
			alpha1Tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
			alpha2Tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
			alpha3Tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
		} else {
			alpha1.setUpdateNeeded();
			alpha2.setUpdateNeeded();
			alpha3.setUpdateNeeded();
		}
		texturedMat.setTexture("AlphaMap", alpha1Tex);
		texturedMat.setTexture("AlphaMap_1", alpha2Tex);
		texturedMat.setTexture("AlphaMap_2", alpha3Tex);
	}
	private void updatePlainMaterial() {
		ByteBuffer buf;
		if (plain==null) {
			buf = BufferUtils.createByteBuffer(size*size*4);
		} else {
			buf = plain.getData(0);
		}
		buf.rewind();
		float r,g,b,a;
		Biome[] bx = Biome.values();
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				r=g=b=0;
				a=1;
				float[] v = biomes.getVectorAt(y, size-x-1);
				for (int i=0; i<v.length; ++i) {
					r += v[i]*bx[i].color.r;
					g += v[i]*bx[i].color.g;
					b += v[i]*bx[i].color.b;
//					a += v[i]*bx[i].color.a;
				}
				r = clamp(r);
				g = clamp(g);
				b = clamp(b);
				buf.put((byte) (r*255));
				buf.put((byte) (g*255));
				buf.put((byte) (b*255));
				buf.put((byte) (a*255));
			}
		}
		buf.rewind();
		if (plain==null) {
			plain = new Image(Image.Format.RGBA8, size, size, buf, ColorSpace.Linear);
			plainTex = new Texture2D(plain);
			plainTex.setMagFilter(Texture.MagFilter.Bilinear);
			plainTex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
		} else {
			plain.setUpdateNeeded();
		}
		plainMat.setTexture("DiffuseMap", plainTex);
	}
	private static final float clamp(float v) {
		return Math.max(0, Math.min(1, v));
	}
}
