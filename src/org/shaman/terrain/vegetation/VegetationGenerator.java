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
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.erosion.RiverSource;
import org.shaman.terrain.erosion.WaterErosionScreenController;

/**
 *
 * @author Sebastian Weiss
 */
public class VegetationGenerator extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(VegetationGenerator.class.getName());
	
	//GUI and settings
	private VegetationScreenController screenController;
	
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
	
}
