/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Vector2d;
import org.apache.commons.lang3.ArrayUtils;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.heightmap.Heightmap;
import org.shaman.terrain.heightmap.Noise;
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
	public static enum Coastline {
		PERLIN, RADIAL
	}
	private Coastline coastline;
	private boolean initialized = false;
	
	//computation
	private Voronoi voronoi;
	private float voronoiScale;
	private List<Vector2d> voronoiSites;
	private List<Edge> voronoiEdges;
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
		computeVoronoi();
		//display graph
		updateGraphNode();
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
	private void computeVoronoi() {
		if (!initialized || pointCount==0) {
			return;
		}
		LOG.info("compute cells");
		
		//Generate voronoiSites
		voronoiRandom = new Random(seed);
		voronoiScale = 100 * pointCount;
		voronoiSites = new ArrayList<>(pointCount);
		while (true) {
			voronoiSites.clear();
			for (int i=0; i<pointCount; ++i) {
				float x = voronoiRandom.nextFloat() * voronoiScale;
				float y = voronoiRandom.nextFloat() * voronoiScale;
				voronoiSites.add(new Vector2d(x, y));
			}

			//compute voronoi diagram
			voronoiEdges = voronoi.getEdges(voronoiSites, voronoiScale, voronoiScale);
			voronoiEdges = Voronoi.closeEdges(voronoiEdges, voronoiScale, voronoiScale);

			//relax voronoiSites
			for (int i=0; i<relaxationIterations; ++i) {
				voronoiSites = VoronoiUtils.generateRelaxedSites(voronoiSites, voronoiEdges);
				voronoiEdges = voronoi.getEdges(voronoiSites, voronoiScale, voronoiScale);
				voronoiEdges = Voronoi.closeEdges(voronoiEdges, voronoiScale, voronoiScale);
			}
			
			//check if valid
			//if (VoronoiUtils.isValid(voronoiEdges, voronoiScale)) {
				break;
			//}
			//LOG.warning("voronoi diagram is illegal, try again");
		}
		LOG.info("point and edges generated");
		//next step
		generateGraph();
	}
	
	/**
	 * Second step, generate the graph
	 */
	private void generateGraph() {
		//generate graph
		graph = new Graph();
		//add centers
		Map<Vector2d, Graph.Center> centerMap = new HashMap<>();
		for (Vector2d p : voronoiSites) {
			Graph.Center c = new Graph.Center();
			c.location = new Vector2f((float) p.x / voronoiScale, (float) p.y / voronoiScale);
			c.index = graph.centers.size();
			graph.centers.add(c);
			centerMap.put(p, c);
		}
		//add corners
		Map<Vector2d, Graph.Corner> cornerMap = new HashMap<>();
		float epsilon = 0.001f;
		for (Edge edge : voronoiEdges) {
			Vector2d start = edge.getStart();
			if (!cornerMap.containsKey(start)) {
				Graph.Corner c = new Graph.Corner();
				c.point = new Vector2f((float) start.x / voronoiScale, (float) start.y / voronoiScale);
				c.index = graph.corners.size();
				if (c.point.x < epsilon || c.point.y < epsilon
						|| c.point.x > 1-epsilon || c.point.y > 1-epsilon) {
					c.border = true;
				}
				graph.corners.add(c);
				cornerMap.put(start, c);
			}
			Vector2d end = edge.getEnd();
			if (!cornerMap.containsKey(end)) {
				Graph.Corner c = new Graph.Corner();
				c.point = new Vector2f((float) end.x / voronoiScale, (float) end.y / voronoiScale);
				c.index = graph.corners.size();
				if (c.point.x < epsilon || c.point.y < epsilon
						|| c.point.x > 1-epsilon || c.point.y > 1-epsilon) {
					c.border = true;
				}
				graph.corners.add(c);
				cornerMap.put(end, c);
			}
		}
		//add edges and connect
		for (Edge edge : voronoiEdges) {
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
			e.d0.neighbors.add(e.d1);
			e.d1.neighbors.add(e.d0);
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
		int borderCorners = 0;
		for (Graph.Corner c : graph.corners) {
			if (c.border) borderCorners++;
		}
		LOG.info("border corners: "+borderCorners);
		LOG.info("graph created");
		//next step
		assignCoastline();
	}
	
	/**
	 * Third step, assign coastline
	 */
	private void assignCoastline() {
		if (graph==null || coastline==null) {
			return;
		}
		Random rand = new Random(seed);
		//reset
		for (Graph.Center c : graph.centers) {
			c.water = false;
			c.border = false;
			c.ocean = false;
		}
		for (Graph.Corner c : graph.corners) {
			c.water = false;
			c.ocean = false;
		}
		//set water parameter of corners
		int waterCorners = 0;
		switch (coastline) {
			case PERLIN:
				//Fractal perlin noise
				Noise[] noise = new Noise[5];
				for (int i=0; i<noise.length; ++i) {
					noise[i] = new Noise(rand.nextLong());
				}
				for (Graph.Corner c : graph.corners) {
					float val = 0;
					float octave = 6; //to be tuned
					float amplitude = 0.5f; //to be tuned
					for (int i=0; i<noise.length; ++i) {
						val += noise[i].noise(c.point.x * octave, c.point.y * octave) * amplitude;
						octave *= 2;
						amplitude /= 2.5;
					}
					float dist = c.point.distanceSquared(0.5f, 0.5f);
					float distInfluence = 1.5f; //to be tuned
					float perlinOffset = -0.2f; //to be tuned
					if (val > perlinOffset + distInfluence*dist && !c.border) {
						c.water = false;
					} else {
						c.water = true;
						waterCorners++;
					}
				}
				break;
				
			case RADIAL:
				//radial sine waves
				double islandFactor = 1.07;
				int bumps = rand.nextInt(6)+1;
				double startAngle = rand.nextDouble() * 2 * Math.PI;
				double dipAngle = rand.nextDouble() * 2 * Math.PI;
				double dipWidth = rand.nextDouble() * 0.5 + 0.2;
				for (Graph.Corner c : graph.corners) {
					double x = (c.point.x - 0.5) * 2;
					double y = (c.point.y - 0.5) * 2;
					double angle = Math.atan2(y, x);
					double length = 0.5 * (Math.max(Math.abs(x), Math.abs(y))
							+ new Vector2d(x, y).length());
					double r1 = 0.5 * 0.4*Math.sin(startAngle + bumps*angle
							+ Math.cos((bumps+3)*angle));
					double r2 = 0.7 - 0.2*Math.sin(startAngle + bumps*angle
							- Math.sin((bumps+2)*angle));
					if (Math.abs(angle - dipAngle) < dipWidth
							|| Math.abs(angle - dipAngle + 2*Math.PI) < dipWidth
							|| Math.abs(angle - dipAngle - 2*Math.PI) < dipWidth) {
						r1 = r2 = 0.2;
					}
					if ((length<r1 || (length>r1*islandFactor && length<r2)) && !c.border) {
						c.water = false;
					} else {
						c.water = true;
						waterCorners++;
					}
				}
				break;
		}
		LOG.log(Level.INFO, "corners with water: {0}, without water: {1}", 
				new Object[]{waterCorners, graph.corners.size()-waterCorners});
		
		//set water parameter of centers
		float LAKE_THRESHOLD = 0.3f;
		Queue<Graph.Center> queue = new ArrayDeque<>();
		for (Graph.Center p : graph.centers) {
			int numWater = 0;
			for (Graph.Corner c : p.corners) {
				if (c.border || c.ocean) {
					p.border = true;
					p.water = true;
					p.ocean = true;
					queue.add(p);
					break;
				}
				if (c.water) {
					numWater++;
				}
			}
			p.water = (p.ocean || numWater >= p.corners.size() * LAKE_THRESHOLD);
		}
		LOG.info("border cells: "+queue.size());
		//float fill borders to distinguish between ocean and likes
		while (!queue.isEmpty()) {
			Graph.Center c = queue.poll();
			for (Graph.Center r : c.neighbors) {
				if (r.water && !r.ocean) {
					r.ocean = true;
					queue.add(r);
				}
			}
		}
		//assign basic biomes
		int oceanCount = 0;
		int lakeCount = 0;
		int landCount = 0;
		for (Graph.Center c : graph.centers) {
			if (c.ocean) {
				c.biome = Biome.OCEAN;
				oceanCount++;
			} else if (c.water) {
				c.biome = Biome.LAKE;
				lakeCount++;
			} else {
				c.biome = Biome.BEACH;
				lakeCount++;
			}
		}
		LOG.log(Level.INFO, "ocean cells: {0}, lake cells: {1}, land cells: {2}", 
				new Object[]{oceanCount, lakeCount, landCount});
	}
	
	//display graph
	private void updateGraphNode() {
		if (graphNode==null) {
			return;
		}
		graphNode.detachAllChildren();
		Material mat;
		Geometry geom;
		
		//biomes
		ArrayList<Vector3f> posList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>();
		Map<Graph.Corner, Integer> cornerIndices = new HashMap<>();
		for (Graph.Center c : graph.centers) {
			int i = posList.size();
			posList.add(new Vector3f(c.location.x, c.location.y, 1));
			colorList.add(c.biome.color);
			cornerIndices.clear();
			for (Graph.Corner corner : c.corners) {
				cornerIndices.put(corner, posList.size());
				posList.add(new Vector3f(corner.point.x, corner.point.y, 1));
				colorList.add(c.biome.color);
			}
			for (Graph.Edge edge : c.borders) {
				indexList.add(i);
				indexList.add(cornerIndices.get(edge.v0));
				indexList.add(cornerIndices.get(edge.v1));
			}
		}
		Mesh biomesMesh = new Mesh();
		biomesMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posList.toArray(new Vector3f[posList.size()])));
		biomesMesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colorList.toArray(new ColorRGBA[colorList.size()])));
		biomesMesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(ArrayUtils.toPrimitive(indexList.toArray(new Integer[indexList.size()]))));
		biomesMesh.setMode(Mesh.Mode.Triangles);
		biomesMesh.updateCounts();
		mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setBoolean("VertexColor", true);
		mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		geom = new Geometry("biomes", biomesMesh);
		geom.setMaterial(mat);
		graphNode.attachChild(geom);
		
		//edges
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
		mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Gray);
		geom = new Geometry("edges", edgeMesh);
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
	void guiInitialValues(int seed, int pointCount, int relaxationIterations, Coastline coastline) {
		this.seed = seed;
		this.pointCount = pointCount;
		this.relaxationIterations = relaxationIterations;
		this.coastline = coastline;
		computeVoronoi();
		//display graph
		updateGraphNode();
	}
	void guiSeedChanged(int seed) {
		this.seed = seed;
		computeVoronoi();
		//display graph
		updateGraphNode();
	}
	void guiPointCountChanged(int count) {
		this.pointCount = count;
		computeVoronoi();
		//display graph
		updateGraphNode();
	}
	void guiRelaxationChanged(int iterations) {
		this.relaxationIterations = iterations;
		computeVoronoi();
		//display graph
		updateGraphNode();
	}
	void guiCoastlineChanged(Coastline coastline) {
		this.coastline = coastline;
		assignCoastline();
		//display graph
		updateGraphNode();
	}
	
}
