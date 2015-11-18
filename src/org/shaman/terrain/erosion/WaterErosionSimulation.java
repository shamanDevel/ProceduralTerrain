/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.erosion;

import com.jme3.math.Vector3f;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Logger;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public class WaterErosionSimulation extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(WaterErosionSimulation.class.getName());
	
	//GUI and settings
	private WaterErosionScreenController screenController;

	//maps
	private Heightmap map;
	private Heightmap scaledMap;
	private Heightmap originalMap;
	private Vector3f originalMapScale;
	private Heightmap moisture;
	private Heightmap originalMoisture;
	private Heightmap temperature;
	private Heightmap originalTemperature;

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
		
	}

	@Override
	protected void disable() {
		app.getHeightmapSpatial().setLocalScale(originalMapScale);
	}

	@Override
	public void update(float tpf) {
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
	}
	/**
	 * Sets the display mode: 0=none, 1=show+edit temperature, 2=show+edit moisture
	 * @param mode 
	 */
	void guiDisplayMode(int mode) {
		LOG.info("switch to display mode "+mode);
	}
	void guiBrushSizeChanged(int brushSize) {
		
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
}
