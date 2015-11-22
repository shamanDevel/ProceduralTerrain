/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.math.Vector3f;
import de.lessvoid.nifty.Nifty;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.erosion.RiverSource;
import org.shaman.terrain.erosion.WaterErosionScreenController;
import org.shaman.terrain.polygonal.Biome;

/**
 *
 * @author Sebastian Weiss
 */
public class VegetationGenerator extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(VegetationGenerator.class.getName());
	
	//GUI and settings
	private VegetationScreenController screenController;
	private boolean editDensity = false;
	private Biome selectedBiome;
	private float brushSize;
	private float plantSize;
	private long seed;
	
	//Input
	private Vector3f originalMapScale;
	private float scaleFactor;
	private Heightmap map;
	private Heightmap temperature;
	private Heightmap moisture;
	private Heightmap water;
	private List<? extends RiverSource> riverSources;

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
			scaleFactor = 0.5f; //test
		}
		
		Nifty nifty = app.getNifty();
		screenController = new VegetationScreenController(this);
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/vegetation/VegetationScreen.xml");
		nifty.gotoScreen("Vegetation");
		
		app.setTerrain(map);
		originalMapScale = app.getHeightmapSpatial().getLocalScale().clone();
		app.getHeightmapSpatial().setLocalScale(originalMapScale.x * scaleFactor, originalMapScale.y, originalMapScale.z * scaleFactor);
	}

	@Override
	protected void disable() {
		app.getHeightmapSpatial().setLocalScale(originalMapScale);
	}

	@Override
	public void update(float tpf) {
		
	}
	
	void guiBiomeSelected(@Nullable Biome biome) {
		LOG.info("biome selected: "+biome);
		selectedBiome = biome;
	}
	void guiBrushSizeChanged(float size) {
		LOG.info("brush size changed: "+size);
		brushSize = size;
	}
	void guiSeedChanged(long seed) {
		LOG.info("seed changed: "+seed);
		this.seed = seed;
	}
	void guiEditDensity(boolean editing) {
		LOG.info("density editing "+(editing ? "enabled" : "disabled"));
		editDensity = editing;
	}
	void guiPlantSizeChanged(float scale) {
		LOG.info("plant size changed: "+scale);
		plantSize = scale;
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
}
