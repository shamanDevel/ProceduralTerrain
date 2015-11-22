/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.erosion;

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.Vectorfield;
import org.shaman.terrain.polygonal.PolygonalMapGenerator;
import org.shaman.terrain.vegetation.VegetationGenerator;

/**
 *
 * @author Sebastian Weiss
 */
public class WaterErosionSimulation extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(WaterErosionSimulation.class.getName());
	private static final Class<? extends AbstractTerrainStep> NEXT_STEP = VegetationGenerator.class;
	private static final double BRUSH_STRENGTH = 0.03;
	private static final float WATER_COLOR_FACTOR = 64;
	private static final float HEIGHT_DIFF_FACTOR = 2;
	private static final ColorRGBA BRUSH_COLOR_TERRAIN = new ColorRGBA(1, 1, 1, 0.5f);
	private static final ColorRGBA BRUSH_COLOR_WATER = new ColorRGBA(0.3f, 0.3f, 1, 0.5f);
	private static final ColorRGBA COLOR_RIVER_SOURCE = new ColorRGBA(0, 0, 0.7f, 0.5f);
	private static final ColorRGBA COLOR_RIVER_SOURCE_SELECTED = new ColorRGBA(0, 0, 0, 0.5f);
	
	//GUI and settings
	private WaterErosionScreenController screenController;
	private int displayMode = 0;
	private float brushSize = 0;
	private boolean cameraLocked = false;
	private boolean recording = false;

	//maps
	private Heightmap map;
	private Heightmap scaledMap;
	private Heightmap originalMap;
	private Heightmap newMap;
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
	
	//rivers
	private final ArrayList<RiverSource> riverSources = new ArrayList<>();
	private Node riverSourceNode;
	private int riverEditMode;
	private int selectedRiver = -1;
	
	//solver
	private ErosionSolver solver;
	private Texture solverAlphaTexture;
	private Texture solverHeightTexture;
	private Material solverMaterial;
	private Texture heightDiffTexture;
	private Material heightDiffMaterial;
	private int iteration = 0;
	private boolean solving = false;

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
		if (originalTemperature == null) {
			originalTemperature = new Heightmap(originalMap.getSize());
			originalTemperature.fillHeight(0.5f);
		}
		temperature = originalTemperature.clone();
		originalMoisture = (Heightmap) properties.get(KEY_MOISTURE);
		if (originalMoisture == null) {
			originalMoisture = new Heightmap(originalMap.getSize());
			originalMoisture.fillHeight(0.5f);
		}
		moisture = originalMoisture.clone();
		
		Nifty nifty = app.getNifty();
		screenController = new WaterErosionScreenController(this, originalMap.getSize());
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/erosion/WaterErosionScreen.xml");
		nifty.gotoScreen("WaterErosion");
		
		temperatureMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		moistureMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		heightDiffMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
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
		
		riverSourceNode = new Node("river sources");
		sceneNode.attachChild(riverSourceNode);
	}

	@Override
	protected void disable() {
		app.getHeightmapSpatial().setLocalScale(originalMapScale);
		app.forceTerrainMaterial(null);
		unregisterListener();
	}

	@Override
	public void update(float tpf) {
		if (solving) {
			solverUpdate();
		}
		if (recording) {
			app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();
		}
	}
	
	private void mouseMoved(float mouseX, float mouseY, int clicked, float tpf, boolean mouseMoved) {
		//Create ray
		Vector3f dir = app.getCamera().getWorldCoordinates(new Vector2f(mouseX, mouseY), 1);
		dir.subtractLocal(app.getCamera().getLocation());
		dir.normalizeLocal();
		Ray ray = new Ray(app.getCamera().getLocation(), dir);
		if (riverEditMode==2) {
			pickRiverSources(ray, clicked, mouseMoved);
		} else if (riverEditMode>0 || displayMode>0) {
			editTerrain(ray, clicked, mouseMoved);
		}
	}
	private void editTerrain(Ray ray, int clicked, boolean mouseMoved) {
		CollisionResults results = new CollisionResults();
		//shoot ray at the terrain
		app.getHeightmapSpatial().collideWith(ray, results);
		if (results.size()==0) {
			brushSphere.setCullHint(Spatial.CullHint.Always);
		} else {
			brushSphere.setCullHint(displayMode==0 ? Spatial.CullHint.Always : Spatial.CullHint.Never);
			if (displayMode>0) {
				brushSphere.setCullHint(Spatial.CullHint.Never);
				brushSphere.getMaterial().setColor("Color", BRUSH_COLOR_TERRAIN);
			} else if (riverEditMode==1) {
				brushSphere.setCullHint(Spatial.CullHint.Never);
				brushSphere.getMaterial().setColor("Color", BRUSH_COLOR_WATER);
			} else {
				brushSphere.setCullHint(Spatial.CullHint.Always);
				return;
			}
			Vector3f point = results.getClosestCollision().getContactPoint();
			brushSphere.setLocalTranslation(point);
			point.x *= scaleFactor;
			point.z *= scaleFactor;
			Vector3f mapPoint = app.mapWorldToHeightmap(point);
			//apply brush
			if (clicked==0) {return;}
			if (displayMode>0) {
				int direction = clicked==1 ? 1 : -1;
				float radius = brushSize*scaleFactor;
				float cx = mapPoint.x;
				float cy = mapPoint.y;
				for (int x = Math.max(0, (int) (cx - radius)); x<Math.min(map.getSize(), (int) (cx + radius + 1)); ++x) {
					for (int y = Math.max(0, (int) (cy - radius)); y<Math.min(map.getSize(), (int) (cy + radius + 1)); ++y) {
						double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
						if (dist<radius) {
							dist /= radius;
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
			} else if (riverEditMode==1 && clicked==1 && !mouseMoved) {
				RiverSource source = new RiverSource();
				source.x = (int) mapPoint.x;
				source.y = (int) mapPoint.y;
				source.radius = brushSize*scaleFactor;
				source.intensity = 0.5f;
				Geometry geom = new Geometry("river source", new Sphere(16, 16, 1));
				geom.setLocalScale(brushSize*TerrainHeighmapCreator.TERRAIN_SCALE);
				geom.setLocalTranslation(brushSphere.getLocalTranslation());
				Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				mat.setColor("Color", COLOR_RIVER_SOURCE);
				mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
				geom.setMaterial(mat);
				geom.setQueueBucket(RenderQueue.Bucket.Transparent);
				riverSourceNode.attachChild(geom);
				source.geom = geom;
				riverSources.add(source);
				LOG.info("river source added");
			}
		}
	}
	private void pickRiverSources(Ray ray, int clicked, boolean mouseMoved) {
		CollisionResults results = new CollisionResults();
		//pick river source
		if (riverEditMode==2 && clicked==1) {
			results.clear();
			riverSourceNode.collideWith(ray, results);
			if (selectedRiver>=0) {
				Material mat = riverSources.get(selectedRiver).geom.getMaterial();
				mat.setColor("Color", COLOR_RIVER_SOURCE);
			}
			selectedRiver = -1;
			if (results.size()>0) {
				Geometry geom = results.getClosestCollision().getGeometry();
				for (int i=0; i<riverSources.size(); ++i) {
					if (riverSources.get(i).geom==geom) {
						selectedRiver = i;
						break;
					}
				}
				if (selectedRiver>=0) {
					Material mat = riverSources.get(selectedRiver).geom.getMaterial();
					mat.setColor("Color", COLOR_RIVER_SOURCE_SELECTED);
				}
			}
			LOG.log(Level.INFO, "river source selected: {0}", selectedRiver);
		}
	}
	
	private void updateTextures() {
		int size = map.getSize();
		
		temperatureTexture = new Texture2D(size, size, Image.Format.ABGR8);
		updateTemperatureTexture();
		
		moistureTexture = new Texture2D(size, size, Image.Format.ABGR8);
		updateMoistureTexture();
		
		heightDiffTexture = new Texture2D(size, size, Image.Format.ABGR8);
		updateHeightDiffTexture();
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
		moistureMaterial.setTexture("DiffuseMap", moistureTexture);
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
				float b = Math.min(1, wh*WATER_COLOR_FACTOR);
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
	private void updateHeightDiffTexture() {
		if (newMap==null) {
			return;
		}
		Image image = heightDiffTexture.getImage();
		ByteBuffer data = image.getData(0);
		int size = map.getSize();
		if (data == null) {
			data = BufferUtils.createByteBuffer(size*size*4);
		}
		data.rewind();
		float minDiff = Float.POSITIVE_INFINITY;
		float maxDiff = Float.NEGATIVE_INFINITY;
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float v = newMap.getHeightAt(y, size-x-1) - scaledMap.getHeightAt(y, size-x-1);
				minDiff = Math.min(minDiff, v);
				maxDiff = Math.max(maxDiff, v);
				v *= HEIGHT_DIFF_FACTOR;
				v = Math.min(0.5f, Math.max(-0.5f, v));
				byte val = (byte) (255*(v+0.5f));
				data.put(val).put(val).put(val).put((byte) 255);
			}
		}
		LOG.log(Level.INFO, "minimal height difference: {0} maximal height difference: {1}", new Object[]{minDiff, maxDiff});
		data.rewind();
		image.setFormat(Image.Format.RGBA8);
		image.setWidth(size);
		image.setHeight(size);
		image.setData(0, data);
		heightDiffTexture.setMagFilter(Texture.MagFilter.Bilinear);
		heightDiffMaterial.setTexture("ColorMap", heightDiffTexture);
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
		riverSourceNode.detachAllChildren();
		riverSources.clear();
	}
	/**
	 * Sets the display mode: 0=none, 1=show+edit temperature, 2=show+edit moisture
	 * @param mode 
	 */
	void guiDisplayMode(int mode) {
		LOG.log(Level.INFO, "switch to display mode {0}", mode);
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
		riverEditMode = mode;
		LOG.log(Level.INFO, "switching to river mode {0}", mode);
	}
	void guiDeleteRiverSource() {
		if (selectedRiver>=0) {
			riverSourceNode.detachChild(riverSources.get(selectedRiver).geom);
			riverSources.remove(selectedRiver);
			selectedRiver=-1;
		}
	}
	void guiRiverSourceRadiusChanged(float radius) {
		
	}
	void guiRiverSourceIntensityChanged(float intensity) {
		
	}
	
	void guiRun() {
		screenController.setSolving(true);
		if (solver == null) {
			int size = map.getSize();
			solver = new ErosionSolver(temperature, moisture, map);
			solverAlphaTexture = new Texture2D(size, size, Image.Format.ABGR8);
			solverHeightTexture = new Texture2D(size, size, Image.Format.Depth16);
			iteration = 0;
		} //else: resume after guiStop()
		solver.setRiverSources(riverSources);
		updateSolverTexture();
		updateSolverHeightmap();
		app.forceTerrainMaterial(solverMaterial);
		solving = true;
		LOG.info("start solving");
	}
	private void solverUpdate() {
		solver.oneIteration(screenController.isRaining(), screenController.isRiverActive());
		updateSolverTexture();
		updateSolverHeightmap();
		newMap = solver.getTerrainHeight();
		iteration++;
		screenController.setIteration(iteration);
	}
	void guiStop() {
		screenController.setSolving(false);
		solving = false;
		LOG.info("solving stopped");
	}
	void guiReset() {
		solver = null;
		app.forceTerrainMaterial(null);
		screenController.setIteration(0);
		solving = false;
		LOG.info("solver resetted");
	}
	void guiShowHeightDifference(boolean enabled) {
		if (enabled) {
			updateHeightDiffTexture();
			app.forceTerrainMaterial(heightDiffMaterial);
		} else {
			if (solver!=null) {
				app.forceTerrainMaterial(solverMaterial);
			} else {
				app.forceTerrainMaterial(null);
			}
		}
	}
	void guiDeleteWater() {
		if (solver != null) {
			solver.deleteWater();
		}
	}
	void guiNextStep() {
		Map<Object, Object> props = new HashMap<>(properties);
		props.put(KEY_HEIGHTMAP, newMap==null ? map : newMap);
		props.put(KEY_MOISTURE, moisture);
		props.put(KEY_TEMPERATURE, temperature);
		props.put(KEY_WATER, solver==null ? new Heightmap(map.getSize()) : solver.getWaterHeight());
		props.put(KEY_RIVER_SOURCES, riverSources);
		props.put(KEY_TERRAIN_SCALE, 1f/scaleFactor);
		super.nextStep(NEXT_STEP, props);
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
			app.getInputManager().addMapping("ErosionCameraLock", new KeyTrigger(KeyInput.KEY_L));
			app.getInputManager().addMapping("ErosionRecording", new KeyTrigger(KeyInput.KEY_R));
		}
		app.getInputManager().addListener(listener, "ErosionMouseX+", "ErosionMouseX-", "ErosionMouseY+", "ErosionMouseY-", 
				"ErosionMouseLeft", "ErosionMouseRight", "ErosionCameraLock", "ErosionRecording");
	}
	private void unregisterListener() {
		app.getInputManager().removeListener(listener);
	}
	private class ListenerImpl implements ActionListener, AnalogListener {
		private float mouseX, mouseY;
		private boolean left, right;
		
		private void update(float tpf, boolean moved) {
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
			mouseMoved(mouseX, mouseY, mouse, tpf, moved);
		}

		@Override
		public void onAction(String name, boolean isPressed, float tpf) {
			if ("ErosionMouseLeft".equals(name)) {
				left = isPressed;
				update(tpf, false);
			} else if ("ErosionMouseRight".equals(name)) {
				right = isPressed;
				update(tpf, false);
			} else if ("ErosionCameraLock".equals(name) && isPressed) {
				cameraLocked = !cameraLocked;
				app.setCameraEnabled(!cameraLocked);
				screenController.setMessageLabelText(cameraLocked ? "Camera locked" : "");
			} else if ("ErosionRecording".equals(name) && isPressed) {
				recording = !recording;
				if (recording) {
					cameraLocked = true;
					screenController.setMessageLabelText("Camera locked - Recording");
				} else {
					screenController.setMessageLabelText(cameraLocked ? "Camera locked" : "");
				}
			}
		}

		@Override
		public void onAnalog(String name, float value, float tpf) {
			mouseX = app.getInputManager().getCursorPosition().x;
			mouseY = app.getInputManager().getCursorPosition().y;
			update(tpf, true);
		}
		
	}
		
	public static class ErosionSolver {
		//Settings
		private static final float RAINDROPS_PER_ITERATION = 0.001f;
		private static final float RAINDROP_WATER = 0.25f;
		private static final float DELTA_T = 0.015f;
		private static final float A = 1; //tube area
		private static final float L = 1; //cell distances
		private static final float G = 20; //graviation
		private static final float MIN_SLOPE = 0.005f; //lower threshold for sediment erosion
		private static final float MAX_SLOPE = 0.1f; //upper bound for the slope
		private static final float MAX_EROSION = 0.05f; //limits the maximal height that can be eroded
		private static final float MAX_EVAPORATION = 0.05f; //limits the maximal height that can be evaporated
		private static final float Kc = 0.5f; //sediment capacity constant
		private static final float KcOcean = 0.1f; //sediment capacity constant
		private static final float Ks = 0.1f; //sediment dissolving constant
		private static final float Kd = 0.1f; //sediment deposition constant
		private static final float EROSION_FACTOR = 200;
		private static final float Ke = 0.005f; //evaporation constant
		private static final float RIVER_FACTOR = 0.2f;
		//Input
		private final int size;
		private final int raindropsPerIteration;
		private final Heightmap originalHeight;
		private final Heightmap temperature;
		private final Heightmap moisture;
		private final Random rand = new Random();
		private int iteration = 0;
		private List<? extends RiverSource> riverSources;
		//maps
		private Heightmap terrainHeight;
		private Heightmap waterHeight;
		private Heightmap sediment;
		private Heightmap tmpSediment;
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
			
			this.raindropsPerIteration = (int) (RAINDROPS_PER_ITERATION * size * size);
			this.waterHeight = new Heightmap(size);
			this.sediment = new Heightmap(size);
			this.tmpSediment = new Heightmap(size);
			this.outflowFlux = new Vectorfield(size, 4);
			this.velocity = new Vectorfield(size, 2);
		}
		
		public void setRiverSources(List<? extends RiverSource> sources) {
			riverSources = sources;
		}
		
		public void oneIteration(boolean raining, boolean riverActive) {
			iteration++;
			if (raining) {
				addRainWater(); //periodic rain fall
			}
			if (riverActive) {
				addRiverWater();
			}
			computeFlow();
			computeWaterVolumeAndVelocity();
			computeErosion();
			computeEvaporation();
			computeOceanBoundaryConditions();
		}
		
		/**
		 * For debugging: deletes all water and adds all sediment to the terrain
		 */
		public void deleteWater() {
//			for (int x=0; x<size; ++x) {
//				for (int y=0; y<size; ++y) {
//					waterHeight.setHeightAt(x, y, 0);
//					terrainHeight.adjustHeightAt(x, y, sediment.getHeightAt(x, y));
//					sediment.setHeightAt(x, y, 0);
//				}
//			}
			waterHeight.fillHeight(0);
			sediment.fillHeight(0);
		}
		
		private void addRainWater() {
			//create raindrops
			int n = 0;
			while (n<raindropsPerIteration) {
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
		private void addRiverWater() {
			if (riverSources!=null) {
				for (RiverSource s : riverSources) {
					for (int x = Math.max(0, (int) (s.x - s.radius)); x<Math.min(size, (int) (s.x + s.radius + 1)); ++x) {
						for (int y = Math.max(0, (int) (s.y - s.radius)); y<Math.min(size, (int) (s.y + s.radius + 1)); ++y) {
							double dist = Math.sqrt((x-s.x)*(x-s.x) + (y-s.y)*(y-s.y));
							if (dist<s.radius) {
								dist /= s.radius;
								dist = Math.cos(dist*dist*Math.PI/2);
								waterHeight.adjustHeightAt(x, y, (float) (dist * DELTA_T * s.intensity * RIVER_FACTOR));
							}
						}
					}
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
		}
		private void computeWaterVolumeAndVelocity() {
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
					if (averageD==0) {
						velocity.setVectorAt(x, y, NULL_VELOCITY);
					} else {
						float deltaWx = (outflowFlux.getScalarAtClamping(x-1, y, 1) - outflowFlux.getScalarAt(x, y, 0)
								+ outflowFlux.getScalarAt(x, y, 1) - outflowFlux.getScalarAtClamping(x+1, y, 0));
						velocity.setScalarAt(x, y, 0, deltaWx / L / averageD);
						float deltaWy = (outflowFlux.getScalarAtClamping(x, y-1, 3) - outflowFlux.getScalarAt(x, y, 2)
								+ outflowFlux.getScalarAt(x, y, 3) - outflowFlux.getScalarAtClamping(x, y+1, 2));
						velocity.setScalarAt(x, y, 1, deltaWy / (L * averageD));
					}
				}
			}
		}
		private void computeErosion() {
			//sediment erosion and deposition
			float erodedSum = 0;
			float deposSum = 0;
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					float slope = Math.max(MIN_SLOPE, Math.min(MAX_SLOPE, terrainHeight.getSlopeAt(x, y)));
					float[] v = velocity.getVectorAt(x, y);
					float c = (terrainHeight.getHeightAt(x, y)<=0 ? KcOcean : Kc) * slope 
							* FastMath.sqrt(v[0]*v[0] + v[1]*v[1]) * waterHeight.getHeightAt(x, y);
					float st = sediment.getHeightAt(x, y);
					float delta = originalHeight.getHeightAt(x, y)-terrainHeight.getHeightAt(x, y);
					if (c>st) {
						//erosion
						float ks = Ks * FastMath.exp(-delta*EROSION_FACTOR);
						float eroded = ks * (c-st);
						if (delta>MAX_EROSION) {
//							System.err.println("maximal erosion reached!");
							continue;
						}
						terrainHeight.adjustHeightAt(x, y, -eroded);
						if (terrainHeight.getHeightAt(x, y)<-5) {
							System.err.println("error");
						}
						sediment.adjustHeightAt(x, y, eroded);
						erodedSum+=eroded;
					} else if (c<st) {
						//deposition
						float kd = Kd * FastMath.exp(delta*EROSION_FACTOR);
						float depos = Kd * (st-c);
						if (-delta>MAX_EVAPORATION) {
							continue;
						}
						terrainHeight.adjustHeightAt(x, y, depos);
						sediment.adjustHeightAt(x, y, -depos);
						deposSum +=depos;
					}
				}
			}
			System.out.println("Total sediment eroded="+erodedSum+", deposited="+deposSum);
			//sediment transportation
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					float[] v = velocity.getVectorAt(x, y);
					tmpSediment.setHeightAt(x, y, sediment.getHeightInterpolating(x-v[0]*DELTA_T, y-v[1]*DELTA_T));
				}
			}
			Heightmap tmp = sediment;
			sediment = tmpSediment;
			tmpSediment = tmp;
		}
		private void computeEvaporation() {	
			//evaporation
			float evaporationFactor = (1-Ke*DELTA_T);
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					waterHeight.setHeightAt(x, y, waterHeight.getHeightAt(x, y)*evaporationFactor);
				}
			}
		}
		private void computeOceanBoundaryConditions() {	
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
