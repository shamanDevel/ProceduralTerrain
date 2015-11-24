/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.fojob.forester.Forester;
import se.fojob.forester.grass.GrassLayer;
import se.fojob.forester.grass.GrassLoader;
import se.fojob.forester.grass.algorithms.GPAUniform;
import se.fojob.forester.grass.datagrids.MapGrid;
import se.fojob.forester.image.FormatReader.Channel;

/**
 *
 * @author Sebastian Weiss
 */
public class GrassTest extends SimpleApplication {
	private static final Logger LOG = Logger.getLogger(GrassTest.class.getName());
	private TerrainQuad terrain;
    private Material matTerrain;
	private CustomFlyByCamera camera;
	private Forester forester;
	
	public static void main(String[] args) {
		GrassTest app = new GrassTest();
		app.getStateManager().detach(app.getStateManager().getState(FlyCamAppState.class));
		AppSettings settings = new AppSettings(true);
		settings.setUseJoysticks(true);
		settings.setResolution(1280, 800);
		settings.setVSync(true);
		app.setSettings(settings);
		app.setShowSettings(true);
		app.start();
	}

	@Override
	public void simpleInitApp() {
		cam.setFrustumFar(10000);
		camera = new CustomFlyByCamera(cam);
		camera.registerWithInput(inputManager);
//		camera.setDragToRotate(true);
		inputManager.setCursorVisible(true);
		cam.setLocation(new Vector3f(0, 1000, 1000));
        cam.lookAtDirection(new Vector3f(0, -1.5f, -1).normalizeLocal(), Vector3f.UNIT_Y);
		
		DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.1f, -0.1f, -0.1f)).normalize());
        rootNode.addLight(light);
		AmbientLight ambientLight = new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1));
		rootNode.addLight(ambientLight);
		DirectionalLightShadowRenderer shadowRenderer = new DirectionalLightShadowRenderer(assetManager, 512, 4);
		shadowRenderer.setLight(light);
		viewPort.addProcessor(shadowRenderer);
		
		Texture grass = assetManager.loadTexture("org/shaman/terrain/grass.jpg");
		matTerrain = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		matTerrain.setTexture("DiffuseMap", grass);
		
		String file = "C:\\Users\\Sebastian\\Documents\\Java\\ProceduralTerrain\\saves\\Auto WaterErosionSimulation 325_20_47_28.save";
		Map<Object, Object> loadedProperties = null;
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			Class loadedStep = (Class<? extends AbstractTerrainStep>) in.readObject();
			loadedProperties = (Map<Object, Object>) in.readObject();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "unable to load save file", ex);
		}
		
		//Heightmap map = new Heightmap(256);
		Heightmap map = (Heightmap) loadedProperties.get(AbstractTerrainStep.KEY_HEIGHTMAP);
		terrain = new TerrainQuad("terrain", 65, map.getSize()+1, map.getJMEHeightmap(48));
		terrain.setMaterial(matTerrain);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
		terrain.setShadowMode(RenderQueue.ShadowMode.Receive);
		rootNode.attachChild(terrain);
//		terrain.setLocalTranslation(0, -48/2, 0);
//        terrain.setLocalScale(16);
		
		//init grass
		Material grassMat = new Material(assetManager, "Resources/MatDefs/Grass/grassBase.j3md");
		grassMat.setTexture("ColorMap", assetManager.loadTexture("Resources/Textures/Grass/grass.png"));
		grassMat.setTexture("AlphaNoiseMap", assetManager.loadTexture("Resources/Textures/Grass/noise.png"));
		grassMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		grassMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		forester = Forester.getInstance();
		Node foresterNode = new Node();
		forester.initialize(foresterNode, cam, terrain, this);
		rootNode.attachChild(foresterNode);
//		foresterNode.setLocalTranslation(0, -48/2, 0);
//		foresterNode.setLocalScale(16);
		//first layer
		GrassLoader grassLoader = forester.createGrassLoader(map.getSize(), 4, 4000, 2000);
		MapGrid grid = grassLoader.createMapGrid();
		//grid.addDensityMap(assetManager.loadTexture("Resources/Textures/Grass/noise.png"), 0, 0, 0);
		grid.addDensityMap(createDensityMap(map), 0, 0, 0);
		GrassLayer layer = grassLoader.addLayer(grassMat.clone(), GrassLayer.MeshType.CROSSQUADS);
		layer.setDensityTextureData(0, Channel.Green);
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

	@Override
	public void simpleUpdate(float tpf) {
		camera.setMoveSpeed(200);
		forester.update(tpf);
	}

	@Override
	public void handleError(String errMsg, Throwable t) {
		LOG.log(Level.SEVERE, errMsg, t);
	}
	
	
}
