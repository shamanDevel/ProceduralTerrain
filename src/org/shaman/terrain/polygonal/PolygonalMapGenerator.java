/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.scene.Node;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Logger;
import org.shaman.terrain.TerrainHeighmapCreator;

/**
 *
 * @author Sebastian Weiss
 */
public class PolygonalMapGenerator {
	private static final Logger LOG = Logger.getLogger(PolygonalMapGenerator.class.getName());
	private final TerrainHeighmapCreator app;
	
	private Node guiNode;
	private Node sceneNode;
	private PolygonalScreenController screenController;

	public PolygonalMapGenerator(TerrainHeighmapCreator app) {
		this.app = app;
		init();
	}
	
	private void init() {
		guiNode = new Node("polygonalGui");
		app.getGuiNode().attachChild(guiNode);
		sceneNode = new Node("polygonalScene");
		app.getRootNode().attachChild(sceneNode);
		
		Nifty nifty = app.getNifty();
		screenController = new PolygonalScreenController(this);
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/polygonal/PolygonalScreen.xml");
		nifty.gotoScreen("Polygonal");
	}
}
