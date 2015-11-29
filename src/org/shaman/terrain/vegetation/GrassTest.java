/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

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
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.CustomFlyByCamera;
import org.shaman.terrain.Heightmap;
import se.fojob.forester.Forester;
import se.fojob.forester.RectBounds;
import se.fojob.forester.grass.GrassLayer;
import se.fojob.forester.grass.GrassLoader;
import se.fojob.forester.grass.GrassPage;
import se.fojob.forester.grass.algorithms.GPAUniform;
import se.fojob.forester.grass.algorithms.GrassPlantingAlgorithm;
import se.fojob.forester.grass.datagrids.MapGrid;
import se.fojob.forester.image.DensityMap;
import se.fojob.forester.image.FormatReader.Channel;
import se.fojob.forester.util.FastRandom;

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
	private Heightmap map;
	
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
		map = (Heightmap) loadedProperties.get(AbstractTerrainStep.KEY_HEIGHTMAP);
		terrain = new TerrainQuad("terrain", 65, map.getSize()+1, map.getJMEHeightmap(48));
		terrain.setMaterial(matTerrain);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
		terrain.setShadowMode(RenderQueue.ShadowMode.Receive);
		rootNode.attachChild(terrain);
//		terrain.setLocalTranslation(0, -48/2, 0);
        terrain.setLocalScale(16);
		
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
//		grid.addDensityMap(assetManager.loadTexture("Resources/Textures/Grass/noise.png"), 0, 0, 0);
		grid.addDensityMap(createDensityMap(map), 0, 0, 0);
		GrassLayer layer = grassLoader.addLayer(grassMat.clone(), GrassLayer.MeshType.CROSSQUADS);
		layer.setDensityTextureData(0, Channel.Green);
		layer.setDensityMultiplier(3f);
		layer.setMaxHeight(2f*16);
		layer.setMinHeight(1.f*16);
		layer.setMaxWidth(2.f*16);
		layer.setMinWidth(1.f*16);
//		layer.setPlantingAlgorithm(new GPAUniform(0.3f));
		layer.setPlantingAlgorithm(new GrassPlantingAlgorithmImpl());
		layer.setSwaying(true);
		layer.setWind(new Vector2f(16, 0));
		layer.setSwayingVariation(0.4f);
		layer.setSwayingFrequency(2f);
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
				float h = map.getHeightInterpolating(tx, ty);
				float d = h>0 ? 1 : 0;

				d *= d;

				if (rand.unitRandom() + 0.3 < d ) {
					grassData[iIt++] = (x + bounds.getxMin())*16;
					grassData[iIt++] = (z + bounds.getzMin())*16;
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
		camera.setMoveSpeed(200*16);
		forester.update(tpf);
	}

	@Override
	public void handleError(String errMsg, Throwable t) {
		LOG.log(Level.SEVERE, errMsg, t);
	}
	
	
}
