/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.scene.Node;
import de.lessvoid.nifty.Nifty;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.heightmap.Heightmap;
import org.shaman.terrain.sketch.SketchTerrain;

/**
 *
 * @author Sebastian Weiss
 */
public class PolygonalMapGenerator extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(PolygonalMapGenerator.class.getName());
	private static final Class<? extends AbstractTerrainStep> NEXT_STEP = SketchTerrain.class;

	private PolygonalScreenController screenController;

	public PolygonalMapGenerator() {
	}
	
	private void init() {
		guiNode.detachAllChildren();
		sceneNode.detachAllChildren();
		
		Nifty nifty = app.getNifty();
		screenController = new PolygonalScreenController(this);
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/polygonal/PolygonalScreen.xml");
		nifty.gotoScreen("Polygonal");
	}

	@Override
	protected void enable() {
		init();
		app.setTerrain(null);
		app.setSkyEnabled(false);
		app.setCameraEnabled(false);
	}

	@Override
	protected void disable() {
		app.setSkyEnabled(true);
		app.setCameraEnabled(true);
	}

	@Override
	public void update(float tpf) {
		app.setCameraEnabled(false);
	}
	
	void guiNextStep() {
		//generate map
		//TODO
		Heightmap map = new Heightmap(256);
		
		//generate properties
		Map<Object, Object> prop = new HashMap<>(properties);
		prop.put(KEY_HEIGHTMAP, map);
		
		//activate next step
		nextStep(NEXT_STEP, prop);
	}
}
