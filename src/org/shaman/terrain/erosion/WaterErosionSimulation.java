/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.erosion;

import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.logging.Logger;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.Vectorfield;
import org.shaman.terrain.polygonal.PolygonalMapGenerator;

/**
 *
 * @author Sebastian Weiss
 */
public class WaterErosionSimulation extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(WaterErosionSimulation.class.getName());
	private static final double BRUSH_STRENGTH = 0.03;
	
	//GUI and settings
	private WaterErosionScreenController screenController;
	private int displayMode = 0;
	private float brushSize = 0;

	//maps
	private Heightmap map;
	private Heightmap scaledMap;
	private Heightmap originalMap;
	private Vector3f originalMapScale;
	private Heightmap moisture;
	private Heightmap originalMoisture;
	private Heightmap temperature;
	private Heightmap originalTemperature;
	private float scaleFactor = 1;
	
	//textures / materials
	private Texture temperatureTexture;
	private Material temperatureMaterial;
	private Texture moistureTexture;
	private Material moistureMaterial;
	
	//brush
	private Geometry brushSphere;
	private ListenerImpl listener;
	
	//solver
	private ErosionSolver solver;
	private Texture solverAlphaTexture;
	private Texture solverHeightTexture;
	private Material solverMaterial;
	private int iteration = 0;

	@Override
	protected void enable() {
		app.enableWater(0);
		app.setSkyEnabled(true);
		app.setCameraEnabled(true);
		
		map = (Heightmap) properties.get(KEY_HEIGHTMAP);
		originalMap = map.clone();
		scaledMap = originalMap;
		app.setTerrain(map);
		originalMapScale = app.getHeightmapSpatial().getLocalScale().clone();
		originalTemperature = (Heightmap) properties.get(KEY_TEMPERATURE);
		temperature = originalTemperature.clone();
		originalMoisture = (Heightmap) properties.get(KEY_MOISTURE);
		moisture = originalMoisture.clone();
		
		Nifty nifty = app.getNifty();
		screenController = new WaterErosionScreenController(this, originalMap.getSize());
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/erosion/WaterErosionScreen.xml");
		nifty.gotoScreen("WaterErosion");
		
		temperatureMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		moistureMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		updateTextures();
		
		brushSphere = new Geometry("brush", new Sphere(32, 32, 1));
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(1, 1, 1, 0.5f));
		mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		brushSphere.setMaterial(mat);
		brushSphere.setQueueBucket(RenderQueue.Bucket.Transparent);
		sceneNode.attachChild(brushSphere);
		brushSphere.setCullHint(Spatial.CullHint.Always);
		brushSphere.setLocalScale(brushSize*TerrainHeighmapCreator.TERRAIN_SCALE);
		
		solverMaterial = new Material(app.getAssetManager(), "org/shaman/terrain/shader/TerrainLightingExt.j3md");
        solverMaterial.setBoolean("useTriPlanarMapping", true);
        solverMaterial.setFloat("Shininess", 0.0f);
		Texture darkRock = app.getAssetManager().loadTexture("org/shaman/terrain/rock2.jpg");
        darkRock.setWrap(Texture.WrapMode.Repeat);
        solverMaterial.setTexture("DiffuseMap", darkRock);
        solverMaterial.setFloat("DiffuseMap_0_scale", 1/16f);
		Texture grass = app.getAssetManager().loadTexture("org/shaman/terrain/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        solverMaterial.setTexture("DiffuseMap_1", grass);
        solverMaterial.setFloat("DiffuseMap_1_scale", 1/8f);
		Texture water = app.getAssetManager().loadTexture("org/shaman/terrain/textures/Water.jpg");
        water.setWrap(Texture.WrapMode.Repeat);
        solverMaterial.setTexture("DiffuseMap_2", water);
        solverMaterial.setFloat("DiffuseMap_2_scale", 1/8f);
		
		registerListener();
	}

	@Override
	protected void disable() {
		app.getHeightmapSpatial().setLocalScale(originalMapScale);
		app.forceTerrainMaterial(null);
		unregisterListener();
	}

	@Override
	public void update(float tpf) {
		if (solver != null) {
			solverUpdate();
		}
	}
	
	private void mouseMoved(float mouseX, float mouseY, int clicked, float tpf) {
		//Create ray
		Vector3f dir = app.getCamera().getWorldCoordinates(new Vector2f(mouseX, mouseY), 1);
		dir.subtractLocal(app.getCamera().getLocation());
		dir.normalizeLocal();
		Ray ray = new Ray(app.getCamera().getLocation(), dir);
		CollisionResults results = new CollisionResults();
		//shoot ray at the terrain
		app.getHeightmapSpatial().collideWith(ray, results);
		if (results.size()==0) {
			brushSphere.setCullHint(Spatial.CullHint.Always);
		} else {
			brushSphere.setCullHint(displayMode==0 ? Spatial.CullHint.Always : Spatial.CullHint.Never);
			Vector3f point = results.getClosestCollision().getContactPoint();
			brushSphere.setLocalTranslation(point);
			point.x *= scaleFactor;
			point.z *= scaleFactor;
			Vector3f mapPoint = app.mapWorldToHeightmap(point);
			//apply brush
			if (clicked==0) {return;}
			int direction = clicked==1 ? 1 : -1;
			float radius = brushSize;
			float cx = mapPoint.x;
			float cy = mapPoint.y;
			for (int x = Math.max(0, (int) (cx - radius)); x<Math.min(map.getSize(), (int) (cx + radius + 1)); ++x) {
				for (int y = Math.max(0, (int) (cy - radius)); y<Math.min(map.getSize(), (int) (cy + radius + 1)); ++y) {
					double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
					if (dist<radius) {
						dist = dist/radius;
						dist = Math.cos(dist*dist*Math.PI/2);
						if (displayMode==1) {
							temperature.adjustHeightAt(x, y, (float) (dist*direction*BRUSH_STRENGTH));
						} else {
							moisture.adjustHeightAt(x, y, (float) (dist*direction*BRUSH_STRENGTH));
						}
					}
				}
			}
			if (displayMode==1) {
				updateTemperatureTexture();
			} else {
				updateMoistureTexture();
			}
		}
	}
	
	private void updateTextures() {
		int size = map.getSize();
		
		temperatureTexture = new Texture2D(size, size, Image.Format.ABGR8);
		updateTemperatureTexture();
		
		moistureTexture = new Texture2D(size, size, Image.Format.ABGR8);
		updateMoistureTexture();
	}
	private void updateTemperatureTexture() {		
		Image image = temperatureTexture.getImage();
		ByteBuffer data = image.getData(0);
		int size = map.getSize();
		if (data == null) {
			data = BufferUtils.createByteBuffer(size*size*4);
		}
		data.rewind();
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float v = temperature.getHeightAt(y, size-x-1);
				float r = Math.max(0, Math.min(1, v));
				float g = 0;
				float b = Math.max(0, Math.min(1, 1-v));
				data.put((byte) (255*r)).put((byte) (255*g)).put((byte) (255*b)).put((byte) 0);
			}
		}
		data.rewind();
		image.setFormat(Image.Format.RGBA8);
		image.setWidth(size);
		image.setHeight(size);
		image.setData(0, data);
		temperatureTexture.setMagFilter(Texture.MagFilter.Bilinear);
		temperatureMaterial.setTexture("DiffuseMap", temperatureTexture);
	}
	private void updateMoistureTexture() {		
		Image image = moistureTexture.getImage();
		ByteBuffer data = image.getData(0);
		int size = map.getSize();
		if (data == null) {
			data = BufferUtils.createByteBuffer(size*size*4);
		}
		data.rewind();
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float v = moisture.getHeightAt(y, size-x-1);
				float r = Math.max(0, Math.min(1, 1-v));
				float g = Math.max(0, Math.min(1, 0.5f-v/2));
				float b = Math.max(0, Math.min(1, v));
				data.put((byte) (255*r)).put((byte) (255*g)).put((byte) (255*b)).put((byte) 0);
			}
		}
		data.rewind();
		image.setFormat(Image.Format.RGBA8);
		image.setWidth(size);
		image.setHeight(size);
		image.setData(0, data);
		moistureTexture.setMagFilter(Texture.MagFilter.Bilinear);
		moistureMaterial.setTexture("AlphaMap", moistureTexture);
	}
	private void updateSolverTexture() {		
		Image image = solverAlphaTexture.getImage();
		ByteBuffer data = image.getData(0);
		int size = map.getSize();
		if (data == null) {
			data = BufferUtils.createByteBuffer(size*size*4);
		}
		data.rewind();
		int zeroWaterCounter = 0;
		int negativeWaterCounter = 0;
		int positiveWaterCounter = 0;
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float slope = solver.getTerrainHeight().getSlopeAt(y, size-x-1);
				slope = (float) Math.pow(slope * TerrainHeighmapCreator.SLOPE_SCALE, TerrainHeighmapCreator.SLOPE_POWER);
				float g = Math.max(0, 1-slope);
				float r = 1-g;
				float wh = solver.getWaterHeight().getHeightAt(y, size-x-1);
				float b = Math.min(1, wh*32);
				g = Math.max(0, g-b/2);
				r = Math.max(0, r-b/2);
				data.put((byte) (255*r)).put((byte) (255*g)).put((byte) (255*b)).put((byte) 0);
				//Debug
				if (wh==0) {
					zeroWaterCounter++;
				} else if (wh>0) {
					positiveWaterCounter++;
				} else {
					negativeWaterCounter++;
				}
			}
		}
		System.out.println("nuber of times water is =0:"+zeroWaterCounter+", >0:"+positiveWaterCounter+", <0:"+negativeWaterCounter);		
		data.rewind();
		image.setFormat(Image.Format.RGBA8);
		image.setWidth(size);
		image.setHeight(size);
		image.setData(0, data);
		solverAlphaTexture.setMagFilter(Texture.MagFilter.Bilinear);
		solverMaterial.setTexture("AlphaMap", solverAlphaTexture);
	}
	private void updateSolverHeightmap() {
		Image image = solverHeightTexture.getImage();
		ByteBuffer data = image.getData(0);
		int size = map.getSize();
		if (data == null) {
			data = BufferUtils.createByteBuffer(size*size*2);
		}
		data.rewind();
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float h = solver.getTerrainHeight().getHeightAt(y, size-x-1) 
						+ solver.getWaterHeight().getHeightAt(y, size-x-1);
				h = (h/4)+0.5f;
				h = Math.min(1, Math.max(0, h));
				h *= 1<<16;
				data.putShort((short) (int) h);
//				data.putShort((short) (Short.MAX_VALUE+10));
			}
		}	
		data.rewind();
		image.setFormat(Image.Format.Depth16);
		image.setWidth(size);
		image.setHeight(size);
		image.setData(0, data);
		solverHeightTexture.setMagFilter(Texture.MagFilter.Bilinear);
		solverMaterial.setTexture("HeightMap", solverHeightTexture);
		solverMaterial.setFloat("HeightMapOffset", -0.5f);
		solverMaterial.setFloat("HeightMapScale", 4 * TerrainHeighmapCreator.HEIGHMAP_HEIGHT_SCALE);
	}
	
	void guiUpscaleMap(int power) {
		int factor = 1 << power;
		if (originalMap.getSize() * factor == scaledMap.getSize()) {
			return; //no change
		}
		LOG.info("scale map from "+originalMap.getSize()+" to "+(originalMap.getSize()*factor));
		scaledMap = new Heightmap(originalMap.getSize() * factor);
		temperature = new Heightmap(originalMap.getSize() * factor);
		moisture = new Heightmap(originalMap.getSize() * factor);
		float invFactor = 1f / factor;
		scaleFactor = factor;
		for (int x=0; x<scaledMap.getSize(); ++x) {
			for (int y=0; y<scaledMap.getSize(); ++y) {
				scaledMap.setHeightAt(x, y, originalMap.getHeightInterpolating(x*invFactor, y*invFactor));
				temperature.setHeightAt(x, y, originalTemperature.getHeightInterpolating(x*invFactor, y*invFactor));
				moisture.setHeightAt(x, y, originalMoisture.getHeightInterpolating(x*invFactor, y*invFactor));
			}
		}
		map = scaledMap.clone();
		app.setTerrain(map);
		app.getHeightmapSpatial().setLocalScale(originalMapScale.x * invFactor, originalMapScale.y, originalMapScale.z * invFactor);
		updateTextures();
	}
	/**
	 * Sets the display mode: 0=none, 1=show+edit temperature, 2=show+edit moisture
	 * @param mode 
	 */
	void guiDisplayMode(int mode) {
		LOG.info("switch to display mode "+mode);
		this.displayMode = mode;
		switch (mode) {
			case 0: app.forceTerrainMaterial(null); break;
			case 1: app.forceTerrainMaterial(temperatureMaterial); break;
			case 2: app.forceTerrainMaterial(moistureMaterial); break;
		}
		brushSphere.setCullHint(mode==0 ? Spatial.CullHint.Always : Spatial.CullHint.Never);
	}
	void guiBrushSizeChanged(float brushSize) {
		this.brushSize = brushSize;
		if (brushSphere != null) {
			brushSphere.setLocalScale(brushSize*TerrainHeighmapCreator.TERRAIN_SCALE);
		}
	}
	/**
	 * Sets the river editing mode: 0=none, 1=add river sources, 2=edit river sources
	 * @param mode 
	 */
	void guiRiverMode(int mode) {
		
	}
	void guiDeleteRiverSource() {
		
	}
	void guiRiverSourceRadiusChanged(float radius) {
		
	}
	void guiRiverSourceIntensityChanged(float intensity) {
		
	}
	
	void guiRun() {
		screenController.setSolving(true);
		int size = map.getSize();
		solver = new ErosionSolver(temperature, moisture, map);
		solverAlphaTexture = new Texture2D(size, size, Image.Format.ABGR8);
		solverHeightTexture = new Texture2D(size, size, Image.Format.Depth16);
		updateSolverTexture();
		updateSolverHeightmap();
		app.forceTerrainMaterial(solverMaterial);
		iteration = 0;
		LOG.info("start solving");
	}
	private void solverUpdate() {
		solver.oneIteration();
		updateSolverTexture();
		updateSolverHeightmap();
		iteration++;
		screenController.setIteration(iteration);
	}
	void guiStop() {
		screenController.setSolving(false);
		solver = null;
		LOG.info("solving stopped");
	}
	void guiReset() {
		
	}
	
	private void registerListener() {
		if (listener == null) {
			listener = new ListenerImpl();
			app.getInputManager().addMapping("ErosionMouseX+", new MouseAxisTrigger(MouseInput.AXIS_X, false));
			app.getInputManager().addMapping("ErosionMouseX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));
			app.getInputManager().addMapping("ErosionMouseY+", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
			app.getInputManager().addMapping("ErosionMouseY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
			app.getInputManager().addMapping("ErosionMouseLeft", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
			app.getInputManager().addMapping("ErosionMouseRight", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
		}
		app.getInputManager().addListener(listener, "ErosionMouseX+", "ErosionMouseX-", "ErosionMouseY+", "ErosionMouseY-", "ErosionMouseLeft", "ErosionMouseRight");
	}
	private void unregisterListener() {
		app.getInputManager().removeListener(listener);
	}
	private class ListenerImpl implements ActionListener, AnalogListener {
		private float mouseX, mouseY;
		private boolean left, right;
		
		private void update(float tpf) {
			int mouse;
			if (left && right) {
				mouse = 0;
			} else if (left) {
				mouse = 1;
			} else if (right) {
				mouse = 2;
			} else {
				mouse = 0;
			}
			mouseMoved(mouseX, mouseY, mouse, tpf);
		}

		@Override
		public void onAction(String name, boolean isPressed, float tpf) {
			if ("ErosionMouseLeft".equals(name)) {
				left = isPressed;
				update(tpf);
			} else if ("ErosionMouseRight".equals(name)) {
				right = isPressed;
				update(tpf);
			}
		}

		@Override
		public void onAnalog(String name, float value, float tpf) {
			mouseX = app.getInputManager().getCursorPosition().x;
			mouseY = app.getInputManager().getCursorPosition().y;
			update(tpf);
		}
		
	}
	
	public static class ErosionSolver {
		//Settings
		private static final int RAINDROPS_PER_ITERATION = 500;
		private static final float RAINDROP_WATER = 1f;
		private static final float DELTA_T = 0.02f;
		private static final float A = 1; //tube area
		private static final float L = 1; //cell distances
		private static final float G = 20; //graviation
		//Input
		private final int size;
		private final Heightmap originalHeight;
		private final Heightmap temperature;
		private final Heightmap moisture;
		private final Random rand = new Random();
		private int iteration = 0;
		//maps
		private Heightmap terrainHeight;
		private Heightmap waterHeight;
		private Heightmap sediment;
		/**
		 * Dimensions: 0=left/-x, 1=right,+x, 2=down/-y, 3=up/+y
		 */
		private Vectorfield outflowFlux;
		private Vectorfield velocity;
		private static final float[] NULL_FLUX = {0,0,0,0};
		private static final float[] NULL_VELOCITY = {0,0};

		public ErosionSolver(Heightmap temperature, Heightmap moisture, Heightmap height) {
			this.size = height.getSize();
			this.temperature = temperature;
			this.moisture = moisture;
			this.originalHeight = height;
			this.terrainHeight = height.clone();
			
			this.waterHeight = new Heightmap(size);
			this.sediment = new Heightmap(size);
			this.outflowFlux = new Vectorfield(size, 4);
			this.velocity = new Vectorfield(size, 2);
		}
		
		public void oneIteration() {
			iteration++;
			if (iteration<50) {
				addWater();
			}
			computeFlow();
		}
		
		private void addWater() {
			//create raindrops
			int n = 0;
			while (n<RAINDROPS_PER_ITERATION) {
				int x = rand.nextInt(size);
				int y = rand.nextInt(size);
				if (terrainHeight.getHeightAt(x, y)<=0) {
					continue; //no rain in the ocean
				}
				float v = rand.nextFloat();
				if (v<moisture.getHeightAt(x, y)) {
					//add drop
					n++;
					waterHeight.adjustHeightAt(x, y, DELTA_T * RAINDROP_WATER);
				}
			}
		}
		private void computeFlow() {
			//compute flow
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					float deltaH;
					//left
					deltaH = terrainHeight.getHeightAt(x, y) + waterHeight.getHeightAt(x, y)
							- terrainHeight.getHeightAtClamping(x-1, y) - waterHeight.getHeightAtClamping(x-1, y);
					outflowFlux.setScalarAt(x, y, 0, Math.max(0, outflowFlux.getScalarAt(x, y, 0)
							+ DELTA_T * A * G * deltaH / L));
					//right
					deltaH = terrainHeight.getHeightAt(x, y) + waterHeight.getHeightAt(x, y)
							- terrainHeight.getHeightAtClamping(x+1, y) - waterHeight.getHeightAtClamping(x+1, y);
					outflowFlux.setScalarAt(x, y, 1, Math.max(0, outflowFlux.getScalarAt(x, y, 1)
							+ DELTA_T * A * G * deltaH / L));
					//down
					deltaH = terrainHeight.getHeightAt(x, y) + waterHeight.getHeightAt(x, y)
							- terrainHeight.getHeightAtClamping(x, y-1) - waterHeight.getHeightAtClamping(x, y-1);
					outflowFlux.setScalarAt(x, y, 2, Math.max(0, outflowFlux.getScalarAt(x, y, 2)
							+ DELTA_T * A * G * deltaH / L));
					//up
					deltaH = terrainHeight.getHeightAt(x, y) + waterHeight.getHeightAt(x, y)
							- terrainHeight.getHeightAtClamping(x, y+1) - waterHeight.getHeightAtClamping(x, y+1);
					outflowFlux.setScalarAt(x, y, 3, Math.max(0, outflowFlux.getScalarAt(x, y, 3)
							+ DELTA_T * A * G * deltaH / L));
					//scale
					float sum = outflowFlux.getScalarAt(x, y, 0)+outflowFlux.getScalarAt(x, y, 1)
							  +outflowFlux.getScalarAt(x, y, 2)+outflowFlux.getScalarAt(x, y, 3);
					float K;
					if (sum==0) {
						K = 1;
					} else {
						K = Math.min(1, waterHeight.getHeightAt(x, y) * L * L /	(sum * DELTA_T) );
					}
					outflowFlux.setScalarAt(x, y, 0, K*outflowFlux.getScalarAt(x, y, 0));
					outflowFlux.setScalarAt(x, y, 1, K*outflowFlux.getScalarAt(x, y, 1));
					outflowFlux.setScalarAt(x, y, 2, K*outflowFlux.getScalarAt(x, y, 2));
					outflowFlux.setScalarAt(x, y, 3, K*outflowFlux.getScalarAt(x, y, 3));
				}
			}
			//compute water volume change and velocity field
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					//water volume
					float deltaV = DELTA_T * (
							outflowFlux.getScalarAtClamping(x-1, y, 1) + outflowFlux.getScalarAtClamping(x+1, y, 0)
							+ outflowFlux.getScalarAtClamping(x, y-1, 3) + outflowFlux.getScalarAtClamping(x, y+1, 2)
							- outflowFlux.getScalarAt(x, y, 0) - outflowFlux.getScalarAt(x, y, 1)
							- outflowFlux.getScalarAt(x, y, 2) - outflowFlux.getScalarAt(x, y, 3)
						);
					float d1 = waterHeight.getHeightAt(x, y);
//					float d2 = Math.max(0, d1 + deltaV / (L*L));
					float d2 = d1 + deltaV / (L*L);
					waterHeight.setHeightAt(x, y, d2);
					float averageD = (d1 + d2)/2;
					//velocity field
					float deltaWx = (outflowFlux.getScalarAtClamping(x-1, y, 1) - outflowFlux.getScalarAt(x, y, 0)
							+ outflowFlux.getScalarAt(x, y, 1) - outflowFlux.getScalarAtClamping(x+1, y, 0));
					velocity.setScalarAt(x, y, 0, deltaWx / L / averageD);
					float deltaWy = (outflowFlux.getScalarAtClamping(x, y-1, 3) - outflowFlux.getScalarAt(x, y, 2)
							+ outflowFlux.getScalarAt(x, y, 3) - outflowFlux.getScalarAtClamping(x, y+1, 2));
					velocity.setScalarAt(x, y, 1, deltaWy / L / averageD);
				}
			}
			//oceans act like endless sinks
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					if (originalHeight.getHeightAt(x, y)<=0) {
						waterHeight.setHeightAt(x, y, 0);
						outflowFlux.setVectorAt(x, y, NULL_FLUX);
						velocity.setVectorAt(x, y, NULL_VELOCITY);
					}
				}
			}
		}
		
		public Heightmap getTerrainHeight() {
			return terrainHeight;
		}
		public Heightmap getWaterHeight() {
			return waterHeight;
		}
	}
}
