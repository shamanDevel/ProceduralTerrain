/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.scene.Node;
import de.lessvoid.nifty.Nifty;
import java.util.*;
import java.util.logging.Logger;
import javax.vecmath.Vector2d;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.heightmap.Heightmap;
import org.shaman.terrain.sketch.SketchTerrain;
import org.shaman.terrain.voronoi.Edge;
import org.shaman.terrain.voronoi.Voronoi;

/**
 *
 * @author Sebastian Weiss
 */
public class PolygonalMapGenerator extends AbstractTerrainStep {
	private static final Logger LOG = Logger.getLogger(PolygonalMapGenerator.class.getName());
	private static final Class<? extends AbstractTerrainStep> NEXT_STEP = SketchTerrain.class;

	//GUI and settings
	private PolygonalScreenController screenController;
	private int seed;
	private int pointCount;
	private int relaxationIterations;
	
	//computation
	private Voronoi voronoi;
	private List<Vector2d> points;
	private List<Edge> edges;
	private Random voronoiRandom;

	public PolygonalMapGenerator() {
	}
	
	private void init() {
		guiNode.detachAllChildren();
		sceneNode.detachAllChildren();
		
		voronoi = new Voronoi();
		
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
	
	/**
	 * The first step, compute the cells
	 */
	private void computeCells() {
		LOG.info("compute cells");
		
		//Generate points
		voronoiRandom = new Random(seed);
		points = new ArrayList<>(pointCount);
		float scale = 100 * pointCount;
		for (int i=0; i<pointCount; ++i) {
			float x = voronoiRandom.nextFloat() * scale;
			float y = voronoiRandom.nextFloat() * scale;
			points.add(new Vector2d(x, y));
		}
		
		//compute voronoi diagram
		edges = voronoi.getEdges(points, scale, scale);
		edges = Voronoi.closeEdges(edges, scale, scale);
		
		//relax points
		for (int i=0; i<relaxationIterations; ++i) {
			points = VoronoiUtils.generateRelaxedSites(points, edges);
			edges = voronoi.getEdges(points, scale, scale);
			edges = Voronoi.closeEdges(edges, scale, scale);
		}
		
		LOG.info("done");
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
	void guiInitialValues(int seed, int pointCount, int relaxationIterations) {
		this.seed = seed;
		this.pointCount = pointCount;
		this.relaxationIterations = relaxationIterations;
		computeCells();
	}
	void guiSeedChanged(int seed) {
		this.seed = seed;
		computeCells();
	}
	void guiPointCountChanged(int count) {
		this.pointCount = count;
		computeCells();
	}
	void guiRelaxationChanged(int iterations) {
		this.relaxationIterations = iterations;
		computeCells();
	}
	
}
