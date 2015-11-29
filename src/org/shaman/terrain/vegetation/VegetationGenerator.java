/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
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
import de.lessvoid.nifty.Nifty;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.Vectorfield;
import org.shaman.terrain.erosion.RiverSource;
import org.shaman.terrain.Biome;

/**
 *
 * @author Sebastian Weiss
 */
public class VegetationGenerator extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(VegetationGenerator.class.getName());
	private static final double BRUSH_STRENGTH = 0.3;
	
	//GUI and settings
	private VegetationScreenController screenController;
	private boolean editDensity = false;
	private Biome selectedBiome;
	private float brushSize;
	private boolean textured = false;
	private float plantSize;
	private boolean showGrass;
	private boolean showTrees;
	private long seed;
	
	//Input
	private Vector3f originalMapScale;
	private float scaleFactor;
	private Heightmap map;
	private Heightmap temperature;
	private Heightmap moisture;
	private Heightmap water;
	private List<? extends RiverSource> riverSources;
	
	//brush
	private Geometry brushSphere;
	private ListenerImpl listener;
	
	//biomes
	private BiomesMaterialCreator materialCreator;
	private GrassPlanter grass;
	private TreePlanter trees;

	@Override
	@SuppressWarnings("unchecked")
	protected void enable() {
		app.enableWater(0);
		app.setSkyEnabled(true);
		app.setCameraEnabled(true);
		
		map = (Heightmap) properties.get(KEY_HEIGHTMAP);
		temperature = (Heightmap) properties.get(KEY_TEMPERATURE);
		moisture = (Heightmap) properties.get(KEY_MOISTURE);
		water = (Heightmap) properties.get(KEY_WATER);
		riverSources = (List<? extends RiverSource>) properties.get(KEY_RIVER_SOURCES);
		if (properties.containsKey(KEY_TERRAIN_SCALE)) {
			scaleFactor = (float) properties.get(KEY_TERRAIN_SCALE);
		} else {
			LOG.warning("no terrain scale factor defined, use default one");
			scaleFactor = 0.5f; //test
		}
		LOG.info("terrain scale factor: "+scaleFactor);
		
		Nifty nifty = app.getNifty();
		screenController = new VegetationScreenController(this);
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/vegetation/VegetationScreen.xml");
		nifty.gotoScreen("Vegetation");
		
		app.setTerrain(map);
		originalMapScale = app.getHeightmapSpatial().getLocalScale().clone();
		app.getHeightmapSpatial().setLocalScale(originalMapScale.x * scaleFactor, originalMapScale.y, originalMapScale.z * scaleFactor);
		
		brushSphere = new Geometry("brush", new Sphere(32, 32, 1));
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(1, 1, 1, 0.5f));
		mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		brushSphere.setMaterial(mat);
		brushSphere.setQueueBucket(RenderQueue.Bucket.Transparent);
		sceneNode.attachChild(brushSphere);
		brushSphere.setCullHint(Spatial.CullHint.Always);
		guiBrushSizeChanged(brushSize);
		
		registerListener();
		
		//initial biomes
		Vectorfield biomes = new Vectorfield(map.getSize(), Biome.values().length);
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float m = clamp(moisture.getHeightAt(x, y));
				float t = clamp(temperature.getHeightAt(x, y));
				float h = map.getHeightAt(x, y);
				if (h<=0) {
					biomes.setScalarAt(x, y, Biome.OCEAN.ordinal(), 1);
				} else {
					Biome b = Biome.getBiome(t, 1-m);
					biomes.setScalarAt(x, y, b.ordinal(), 1);
				}
			}
		}
		materialCreator = new BiomesMaterialCreator(app.getAssetManager(), biomes);
		app.forceTerrainMaterial(materialCreator.getMaterial(textured));
	}

	@Override
	protected void disable() {
		app.getHeightmapSpatial().setLocalScale(originalMapScale);
		unregisterListener();
	}

	@Override
	public void update(float tpf) {
		if (grass != null) {
			grass.update(tpf);
		}
		if (trees != null) {
			trees.update(tpf);
		}
	}
	
	private float clamp(float v) {
		return Math.max(0, Math.min(1, v));
	}
	
	private void mouseMoved(float mouseX, float mouseY, int clicked, float tpf, boolean mouseMoved) {
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
			brushSphere.setCullHint(selectedBiome==null ? Spatial.CullHint.Always : Spatial.CullHint.Never);
			Vector3f point = results.getClosestCollision().getContactPoint();
			brushSphere.setLocalTranslation(point);
			point.x /= scaleFactor;
			point.z /= scaleFactor;
			Vector3f mapPoint = app.mapWorldToHeightmap(point);
			//apply brush
			if (clicked!=1 || selectedBiome==null) {return;}
			float radius = brushSize/scaleFactor;
			float cx = mapPoint.x;
			float cy = mapPoint.y;
			for (int x = Math.max(0, (int) (cx - radius)); x<Math.min(map.getSize(), (int) (cx + radius + 1)); ++x) {
				for (int y = Math.max(0, (int) (cy - radius)); y<Math.min(map.getSize(), (int) (cy + radius + 1)); ++y) {
					double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
					if (dist<radius) {
						dist /= radius;
						dist = Math.cos(dist*dist*Math.PI/2) * BRUSH_STRENGTH;
						materialCreator.updateBiomes(x, y, selectedBiome, (float) dist);
					}
				}
			}
			materialCreator.updateMaterial(textured);
		}
	}
	
	private void updateGrass() {
		if (!showGrass) {
			if (grass != null) {
				grass.showGrass(false);
				grass = null;
			}
			return;
		}
		if (grass==null) {
			grass = new GrassPlanter(app, map, materialCreator.getBiomes(), sceneNode, scaleFactor, plantSize);
			grass.showGrass(true);
		}
		LOG.info("grass added");
	}
	
	private void updateTrees() {
		if (!showTrees) {
			if (trees != null) {
				trees.showTrees(false);
				trees = null;
			}
		} else if (trees == null) {
			trees = new TreePlanter(app, map, materialCreator.getBiomes(), sceneNode, scaleFactor, plantSize);
			trees.showTrees(true);
			LOG.info("trees added");
		}
	}
	
	void guiBiomeSelected(@Nullable Biome biome) {
		LOG.info("biome selected: "+biome);
		selectedBiome = biome;
	}
	void guiBrushSizeChanged(float size) {
		LOG.info("brush size changed: "+size);
		brushSize = size;
		if (brushSphere != null) {
			brushSphere.setLocalScale(brushSize*TerrainHeighmapCreator.TERRAIN_SCALE);
		}
	}
	void guiShowTextured(boolean textured) {
		this.textured = textured;
		materialCreator.updateMaterial(textured);
		app.forceTerrainMaterial(materialCreator.getMaterial(textured));
	}
	void guiSeedChanged(long seed) {
		LOG.info("seed changed: "+seed);
		this.seed = seed;
	}
	@Deprecated
	void guiEditDensity(boolean editing) {
		LOG.info("density editing "+(editing ? "enabled" : "disabled"));
		editDensity = editing;
	}
	void guiPlantSizeChanged(float scale) {
		LOG.info("plant size changed: "+scale);
		plantSize = scale;
	}
	void guiShowGrass(boolean show) {
		LOG.info("show grass: "+show);
		showGrass = show;
		updateGrass();
	}
	void guiShowTrees(boolean show) {
		LOG.info("show trees: "+show);
		showTrees = show;
		updateTrees();
	}
	void guiGeneratePlants() {
		LOG.info("generate plants");
		//Test
		screenController.setGenerating(true);
		screenController.setProgress(0.5f);
	}
	void guiCancelGeneration() {
		LOG.info("cancel generating");
		//Test
		screenController.setGenerating(false);
		screenController.setProgress(1);
	}
	
	private void registerListener() {
		if (listener == null) {
			listener = new ListenerImpl();
			app.getInputManager().addMapping("VegetationMouseX+", new MouseAxisTrigger(MouseInput.AXIS_X, false));
			app.getInputManager().addMapping("VegetationMouseX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));
			app.getInputManager().addMapping("VegetationMouseY+", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
			app.getInputManager().addMapping("VegetationMouseY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
			app.getInputManager().addMapping("VegetationMouseLeft", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
			app.getInputManager().addMapping("VegetationMouseRight", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
		}
		app.getInputManager().addListener(listener, "VegetationMouseX+", "VegetationMouseX-", "VegetationMouseY+", "VegetationMouseY-", 
				"VegetationMouseLeft", "VegetationMouseRight");
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
			if ("VegetationMouseLeft".equals(name)) {
				left = isPressed;
				update(tpf, false);
			} else if ("VegetationMouseRight".equals(name)) {
				right = isPressed;
				update(tpf, false);
			}
		}

		@Override
		public void onAnalog(String name, float value, float tpf) {
			mouseX = app.getInputManager().getCursorPosition().x;
			mouseY = app.getInputManager().getCursorPosition().y;
			update(tpf, true);
		}
		
	}
}
