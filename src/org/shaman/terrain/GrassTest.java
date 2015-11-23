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
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.texture.Texture;
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
		cam.setFrustumFar(1000);
		camera = new CustomFlyByCamera(cam);
		camera.registerWithInput(inputManager);
//		camera.setDragToRotate(true);
		inputManager.setCursorVisible(true);
		cam.setLocation(new Vector3f(0, 100, 100));
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
		
		Heightmap map = new Heightmap(256);
		terrain = new TerrainQuad("terrain", 65, 257, map.getJMEHeightmap(1));
		terrain.setMaterial(matTerrain);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
		terrain.setShadowMode(RenderQueue.ShadowMode.Receive);
		rootNode.attachChild(terrain);
		
		//init grass
		forester = Forester.getInstance();
		forester.initialize(rootNode, cam, terrain, this);
		GrassLoader grassLoader = forester.createGrassLoader(256, 4, 400, 50);
		MapGrid grid = grassLoader.createMapGrid();
		grid.addDensityMap(assetManager.loadTexture("Resources/Textures/Grass/noise.png"), 0, 0, 0);
		Material grassMat = new Material(assetManager, "Resources/MatDefs/Grass/grassBase.j3md");
		grassMat.setTexture("ColorMap", assetManager.loadTexture("Resources/Textures/Grass/grass.png"));
		grassMat.setTexture("AlphaNoiseMap", assetManager.loadTexture("Resources/Textures/Grass/noise.png"));
		grassMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		grassMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		GrassLayer layer = grassLoader.addLayer(grassMat, GrassLayer.MeshType.CROSSQUADS);
		layer.setDensityTextureData(0, Channel.Green);
		layer.setDensityMultiplier(2f);
		layer.setMaxHeight(2f);
		layer.setMinHeight(1.f);
		layer.setMaxWidth(2.4f);
		layer.setMinWidth(1.f);
		layer.setPlantingAlgorithm(new GPAUniform(0.3f));
		layer.setSwaying(true);
		layer.setWind(new Vector2f(1, 0));
		layer.setSwayingVariation(0.4f);
		layer.setSwayingFrequency(2f);
//		layer.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
	}

	@Override
	public void simpleUpdate(float tpf) {
		camera.setMoveSpeed(200);
		forester.update(tpf);
	}
	
	
}
