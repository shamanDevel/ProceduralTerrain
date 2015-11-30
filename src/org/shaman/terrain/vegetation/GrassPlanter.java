/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.Vectorfield;
import org.shaman.terrain.Biome;
import se.fojob.forester.Forester;
import se.fojob.forester.RectBounds;
import se.fojob.forester.grass.GrassLayer;
import se.fojob.forester.grass.GrassLoader;
import se.fojob.forester.grass.GrassPage;
import se.fojob.forester.grass.algorithms.GPAUniform;
import se.fojob.forester.grass.algorithms.GrassPlantingAlgorithm;
import se.fojob.forester.grass.datagrids.MapGrid;
import se.fojob.forester.image.DensityMap;
import se.fojob.forester.image.FormatReader;
import se.fojob.forester.util.FastRandom;

/**
 *
 * @author Sebastian Weiss
 */
public class GrassPlanter {
	private static final Logger LOG = Logger.getLogger(GrassPlanter.class.getName());
	private final TerrainHeighmapCreator app;
	private final Heightmap map;
	private final Vectorfield biomes;
	private final Node sceneNode;
	private final float scaleFactor;
	private Forester forester;
	private GrassLayer layer;
	private float size;
	private boolean enabled;

	public GrassPlanter(TerrainHeighmapCreator app, Heightmap map, Vectorfield biomes, 
			Node sceneNode, float scaleFactor, float grassSize) {
		this.app = app;
		this.map = map;
		this.biomes = biomes;
		this.sceneNode = new Node("grass");
		this.scaleFactor = scaleFactor;
		this.size = grassSize;
		sceneNode.attachChild(this.sceneNode);
	}
	
	public void update(float tpf) {
		if (forester != null && enabled) {
			forester.update(tpf);
		}
	}
	
	public void showGrass(boolean show) {
		if (forester==null && show) {
			//init grass
			Material grassMat = new Material(app.getAssetManager(), "Resources/MatDefs/Grass/grassBase.j3md");
			grassMat.setTexture("ColorMap", app.getAssetManager().loadTexture("Resources/Textures/Grass/grass.png"));
			grassMat.setTexture("AlphaNoiseMap", app.getAssetManager().loadTexture("Resources/Textures/Grass/noise.png"));
			grassMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
			grassMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
			forester = Forester.getInstance();
			forester.initialize(sceneNode, app.getCamera(), app.getHeightmapSpatial(), app);
			GrassLoader grassLoader = forester.createGrassLoader(map.getSize(), 8, 
					400*TerrainHeighmapCreator.TERRAIN_SCALE*scaleFactor, 200*TerrainHeighmapCreator.TERRAIN_SCALE*scaleFactor);
			MapGrid grid = grassLoader.createMapGrid();
			grid.addDensityMap(createDensityMap(map), 0, 0, 0);
			layer = grassLoader.addLayer(grassMat.clone(), GrassLayer.MeshType.CROSSQUADS);
			layer.setDensityTextureData(0, FormatReader.Channel.Green);
			layer.setDensityMultiplier(3f);
			layer.setMaxHeight(0.5f*TerrainHeighmapCreator.TERRAIN_SCALE*size);
			layer.setMinHeight(0.3f*TerrainHeighmapCreator.TERRAIN_SCALE*size);
			layer.setMaxWidth(0.5f*TerrainHeighmapCreator.TERRAIN_SCALE*size);
			layer.setMinWidth(0.3f*TerrainHeighmapCreator.TERRAIN_SCALE*size);
//			layer.setPlantingAlgorithm(new GPAUniform(0.3f));
			layer.setPlantingAlgorithm(new GrassPlantingAlgorithmImpl());
			layer.setSwaying(true);
			layer.setWind(new Vector2f(0.5f*TerrainHeighmapCreator.TERRAIN_SCALE, 0));
			layer.setSwayingVariation(0.4f);
			layer.setSwayingFrequency(2f);
			enabled = true;
		} else {
			sceneNode.setCullHint(show ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
			enabled = show;
		}
	}
	private class GrassPlantingAlgorithmImpl implements GrassPlantingAlgorithm {

		@Override
		public int generateGrassData(GrassPage page, GrassLayer layer, DensityMap densityMap, float[] grassData, int grassCount) {
			RectBounds bounds = page.getBounds();
			//Populating the array of locations (and also getting the total amount
			//of quads).
			FastRandom rand = new FastRandom();
			float width = bounds.getWidth();
			//Dens is size width * width.
			System.out.println("page: "+page+", bounds: "+page.getBounds());
//			float[] dens = densityMap.getDensityUnfiltered(page, layer.getDmChannel());
			//Iterator
			int iIt = 0;

			float minX = Float.POSITIVE_INFINITY;
			float maxX = Float.NEGATIVE_INFINITY;
			float v[] = null;
			for (int i = 0; i < grassCount; i++) {
				float x = rand.unitRandom() * (bounds.getWidth() - 0.01f);
				float z = rand.unitRandom() * (bounds.getWidth() - 0.01f);
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);

//				float d = dens[(int)x + (int)width * (int)z];
//				d=1;
//				float h = map.getHeightInterpolating(x/map.getSize(), z/map.getSize());
//				d = h>0 ? 0.7f : 0;
//				float d = 1;
				float tx = ((x+bounds.getxMin())+(map.getSize()/2));
				float ty = ((z+bounds.getzMin())+(map.getSize()/2));
//				float h = map.getHeightInterpolating(tx, ty);
//				float d = h>0 ? 1 : 0;
				v = biomes.getVectorInterpolating(tx, ty, v);
				float d = v[Biome.GRASSLAND.ordinal()] + 0.02f*v[Biome.TUNDRA.ordinal()];

//				d *= d;

				if (rand.unitRandom() < d ) {
					grassData[iIt++] = (x + bounds.getxMin())*TerrainHeighmapCreator.TERRAIN_SCALE*scaleFactor;
					grassData[iIt++] = (z + bounds.getzMin())*TerrainHeighmapCreator.TERRAIN_SCALE*scaleFactor;
					grassData[iIt++] = rand.unitRandom();
					//-pi/2 -> pi/2
					grassData[iIt++] = (-0.5f + rand.unitRandom())*3.141593f;
				}
			}
			System.out.println("minx="+minX+" maxx="+maxX);
			//The iterator divided by four is the grass-count.
			return iIt/4;
		}
		
	}
	private Texture createDensityMap(Heightmap map) {
		ByteBuffer data = BufferUtils.createByteBuffer(map.getSize() * map.getSize() * 4);
		data.rewind();
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float h = map.getHeightAt(y, map.getSize()-x-1);
				float[] b = biomes.getVectorAt(x, y);
				float v = h>0 ? 1 : 0;
				v *= (b[Biome.GRASSLAND.ordinal()]*1 + b[Biome.TUNDRA.ordinal()]*0.2f
						+ b[Biome.TAIGA.ordinal()]*0.2f);
				data.put((byte) (255*v)).put((byte) (255*v)).put((byte) (255*v)).put((byte) (255*v));
			}
		}
		data.rewind();
		Image img = new Image(Image.Format.RGBA8, map.getSize(), map.getSize(), data, ColorSpace.Linear);
		return new Texture2D(img);
	}
}
