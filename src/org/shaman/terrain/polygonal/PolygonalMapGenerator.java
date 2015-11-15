/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
	private int pointCount = 0;
	private int relaxationIterations;
	private boolean initialized = false;
	
	//computation
	private Voronoi voronoi;
	private Graph graph;
	private Random voronoiRandom;
	private Node graphNode;

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
		graphNode = new Node("graph");
		guiNode.attachChild(graphNode);
		//place graph node
		float x = 250+10;
		float y = 10;
		float w = app.getCamera().getWidth() - x - 10;
		float h = app.getCamera().getHeight() - 20;
		if (w<h) {
			h = w;
			y = (app.getCamera().getHeight()-h) / 2;
		} else {
			w = h;
			x = (app.getCamera().getWidth()-250-w)/2 + 250;
		}
		graphNode.setLocalTranslation(x, y, 0);
		graphNode.setLocalScale(w, h, 1);
		
		initialized = true;
		computeCells();
	}

	@Override
	protected void disable() {
		app.setSkyEnabled(true);
		app.setCameraEnabled(true);
		
		initialized = false;
	}

	@Override
	public void update(float tpf) {
		app.setCameraEnabled(false);
	}
	
	/**
	 * The first step, compute the cells
	 */
	private void computeCells() {
		if (!initialized || pointCount==0) {
			return;
		}
		LOG.info("compute cells");
		
		//Generate points
		voronoiRandom = new Random(seed);
		float scale = 100 * pointCount;
		List<Vector2d> points = new ArrayList<>(pointCount);
		List<Edge> edges;
		while (true) {
			points.clear();
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
			
			//check if valid
			if (VoronoiUtils.isValid(edges, scale)) {
				break;
			}
			LOG.warning("voronoi diagram is illegal, try again");
		}
		
		//generate graph
		graph = new Graph();
		List<VoronoiUtils.Site> sites = VoronoiUtils.findSites(points, edges);
		//add centers
		Map<Vector2d, Graph.Center> centerMap = new HashMap<>();
		for (Vector2d p : points) {
			Graph.Center c = new Graph.Center();
			c.location = new Vector2f((float) p.x / scale, (float) p.y / scale);
			c.index = graph.centers.size();
			graph.centers.add(c);
			centerMap.put(p, c);
		}
		//add corners
		Map<Vector2d, Graph.Corner> cornerMap = new HashMap<>();
		for (Edge edge : edges) {
			Vector2d start = edge.getStart();
			if (!cornerMap.containsKey(start)) {
				Graph.Corner c = new Graph.Corner();
				c.point = new Vector2f((float) start.x / scale, (float) start.y / scale);
				c.index = graph.corners.size();
				graph.corners.add(c);
				cornerMap.put(start, c);
			}
			Vector2d end = edge.getEnd();
			if (!cornerMap.containsKey(end)) {
				Graph.Corner c = new Graph.Corner();
				c.point = new Vector2f((float) end.x / scale, (float) end.y / scale);
				c.index = graph.corners.size();
				graph.corners.add(c);
				cornerMap.put(end, c);
			}
		}
		//add edges and connect
		for (Edge edge : edges) {
			if (edge.getLeft()==null || edge.getRight()==null) {
				cornerMap.get(edge.getStart()).border = true;
				cornerMap.get(edge.getEnd()).border = true;
				continue;
			}
			Graph.Edge e = new Graph.Edge();
			e.index = graph.edges.size();
			graph.edges.add(e);
			e.d0 = centerMap.get(edge.getLeft());
			e.d1 = centerMap.get(edge.getRight());
			e.v0 = cornerMap.get(edge.getStart());
			e.v1 = cornerMap.get(edge.getEnd());
			e.midpoint = new Vector2f().interpolateLocal(e.v0.point, e.v1.point, 0.5f);
			e.d0.borders.add(e);
			e.d1.borders.add(e);
			e.d0.corners.add(e.v0);
			e.d0.corners.add(e.v1);
			e.d1.corners.add(e.v0);
			e.d1.corners.add(e.v1);
			e.v0.protrudes.add(e);
			e.v1.protrudes.add(e);
			e.v0.touches.add(e.d0);
			e.v1.touches.add(e.d0);
			e.v0.touches.add(e.d1);
			e.v1.touches.add(e.d1);
			e.v0.adjacent.add(e.v1);
			e.v1.adjacent.add(e.v0);
		}
		//remove duplicate corner references
		for (Graph.Center c : graph.centers) {
			HashSet<Graph.Corner> cx = new HashSet<>(c.corners);
			c.corners.clear();
			c.corners.addAll(cx);
		}
		
		//display graph
		updateGraphNode();
		
		LOG.info("done");
	}
	
	//display graph
	private void updateGraphNode() {
		graphNode.detachAllChildren();
		
		Mesh edgeMesh = new Mesh();
		FloatBuffer pos = BufferUtils.createVector3Buffer(graph.corners.size());
		IntBuffer index = BufferUtils.createIntBuffer(graph.edges.size()*2);
		pos.rewind();
		for (Graph.Corner c : graph.corners) {
			pos.put(c.point.x).put(c.point.y).put(0);
		}
		pos.rewind();
		index.rewind();
		for (Graph.Edge e : graph.edges) {
			index.put(e.v0.index).put(e.v1.index);
		}
		index.rewind();
		edgeMesh.setBuffer(VertexBuffer.Type.Position, 3, pos);
		edgeMesh.setBuffer(VertexBuffer.Type.Index, 1, index);
		edgeMesh.setMode(Mesh.Mode.Lines);
		edgeMesh.setLineWidth(1);
		edgeMesh.updateCounts();
		edgeMesh.updateBound();
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Gray);
		Geometry geom = new Geometry("edges", edgeMesh);
		geom.setMaterial(mat);
		graphNode.attachChild(geom);
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
