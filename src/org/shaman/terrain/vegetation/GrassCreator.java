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
import se.fojob.forester.Forester;
import se.fojob.forester.grass.GrassLayer;
import se.fojob.forester.grass.GrassLoader;
import se.fojob.forester.grass.algorithms.GPAUniform;
import se.fojob.forester.grass.datagrids.MapGrid;
import se.fojob.forester.image.FormatReader;

/**
 *
 * @author Sebastian Weiss
 */
public class GrassCreator {
	private static final Logger LOG = Logger.getLogger(GrassCreator.class.getName());
	private final TerrainHeighmapCreator app;
	private final Heightmap map;
	private final Vectorfield biomes;
	private final Node sceneNode;
	private Forester forester;
	private GrassLayer layer;
	private float size;
	private boolean enabled;

	public GrassCreator(TerrainHeighmapCreator app, Heightmap map, Vectorfield biomes, Node sceneNode) {
		this.app = app;
		this.map = map;
		this.biomes = biomes;
		this.sceneNode = new Node("grass");
		sceneNode.attachChild(this.sceneNode);
	}
	
	public void update(float tpf) {
		if (forester != null && enabled) {
			forester.update(tpf);
		}
	}
	
	public void setGrassSize(float size) {
		this.size = size;
		LOG.log(Level.INFO, "change grass size to {0}", size);
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
			GrassLoader grassLoader = forester.createGrassLoader(map.getSize(), 4, 400, 200);
			MapGrid grid = grassLoader.createMapGrid();
			grid.addDensityMap(createDensityMap(map), 0, 0, 0);
			layer = grassLoader.addLayer(grassMat.clone(), GrassLayer.MeshType.CROSSQUADS);
			layer.setDensityTextureData(0, FormatReader.Channel.Green);
			layer.setDensityMultiplier(3f);
			layer.setMaxHeight(2f);
			layer.setMinHeight(1.f);
			layer.setMaxWidth(2.f);
			layer.setMinWidth(1.f);
			layer.setPlantingAlgorithm(new GPAUniform(0.3f));
			layer.setSwaying(true);
			layer.setWind(new Vector2f(1, 0));
			layer.setSwayingVariation(0.4f);
			layer.setSwayingFrequency(2f);
			enabled = true;
		} else {
			sceneNode.setCullHint(show ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
			enabled = show;
		}
	}
	private Texture createDensityMap(Heightmap map) {
		ByteBuffer data = BufferUtils.createByteBuffer(map.getSize() * map.getSize() * 4);
		data.rewind();
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float h = map.getHeightAt(y, map.getSize()-x-1);
				float v = h>0 ? 1 : 0;
				data.put((byte) (255*v)).put((byte) (255*v)).put((byte) (255*v)).put((byte) (255*v));
			}
		}
		data.rewind();
		Image img = new Image(Image.Format.RGBA8, map.getSize(), map.getSize(), data, ColorSpace.Linear);
		return new Texture2D(img);
	}
}
