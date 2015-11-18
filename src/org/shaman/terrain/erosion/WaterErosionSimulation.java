/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.erosion;

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

	@Override
	protected void enable() {
		app.enableWater(0);
		app.setSkyEnabled(true);
		app.setCameraEnabled(true);
		
		map = (Heightmap) properties.get(KEY_HEIGHTMAP);
		originalMap = map.clone();
		scaledMap = originalMap;
		app.setTerrain(map);
		
		Nifty nifty = app.getNifty();
		screenController = new WaterErosionScreenController(this, originalMap.getSize());
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/erosion/WaterErosionScreen.xml");
		nifty.gotoScreen("WaterErosion");
		
	}

	@Override
	protected void disable() {
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
		float invFactor = 1f / factor;
		for (int x=0; x<scaledMap.getSize(); ++x) {
			for (int y=0; y<scaledMap.getSize(); ++y) {
				scaledMap.setHeightAt(x, y, originalMap.getHeightInterpolating(x*invFactor, y*invFactor));
			}
		}
		map = scaledMap.clone();
		app.setTerrain(map);
	}
	/**
	 * Sets the display mode: 0=none, 1=show+edit temperature, 2=show+edit moisture
	 * @param mode 
	 */
	void guiDisplayMode(int mode) {
		
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
