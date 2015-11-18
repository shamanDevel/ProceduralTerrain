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
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.TerrainHeighmapCreator;
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
		moistureMaterial.setTexture("DiffuseMap", moistureTexture);
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
		
	}
	void guiStop() {
		
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
}
