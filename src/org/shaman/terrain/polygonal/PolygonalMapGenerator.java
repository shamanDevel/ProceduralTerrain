/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Vector2d;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.Heightmap;
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
	private static final float BRUSH_RADIUS = 0.03f;
	private static final boolean SHOW_RIVERS = false;

	//GUI and settings
	private PolygonalScreenController screenController;
	private InputListenerImpl listener;
	private int brush = 0; //0: disabled, 1: elevation, 2: temperature, 3: moisture
	private int seed;
	private int pointCount = 0;
	private int relaxationIterations;
	public static enum Coastline {
		PERLIN, RADIAL
	}
	private Coastline coastline;
	private int mapSize;
	private int mapSeed;
	private boolean initialized = false;
	
	//computation
	private Voronoi voronoi;
	private float voronoiScale;
	private List<Vector2d> voronoiSites;
	private List<Edge> voronoiEdges;
	private Graph graph;
	private Random voronoiRandom;
	
	private Node graphNode;
	private Node edgeNode;
	private Node biomesNode;
	private Node elevationNode;
	private Node temperatureNode;
	private Node moistureNode;
	private Geometry brushGeom;

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
		graphNode.setCullHint(Spatial.CullHint.Never);
		edgeNode = new Node("edges");
		biomesNode = new Node("biomes");
		elevationNode = new Node("elevation");
		temperatureNode = new Node("temperature");
		moistureNode = new Node("moisture");
		edgeNode.setCullHint(Spatial.CullHint.Never);
		biomesNode.setCullHint(Spatial.CullHint.Never);
		elevationNode.setCullHint(Spatial.CullHint.Always);
		temperatureNode.setCullHint(Spatial.CullHint.Always);
		moistureNode.setCullHint(Spatial.CullHint.Always);
		graphNode.attachChild(elevationNode);
		graphNode.attachChild(biomesNode);
		graphNode.attachChild(temperatureNode);
		graphNode.attachChild(moistureNode);
		graphNode.attachChild(edgeNode);
		guiNode.attachChild(graphNode);
		
		registerInput();
		
		initialized = true;
		computeVoronoi();
		//display graph
		updateGraphNode();
		updateBiomesGeometry();
	}

	@Override
	protected void disable() {
		app.setSkyEnabled(true);
		app.setCameraEnabled(true);
		
		unregisterInput();
		
		initialized = false;
	}

	@Override
	public void update(float tpf) {
//		app.setCameraEnabled(false);
	}
	
//<editor-fold defaultstate="collapsed" desc=" Graph creation ">
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
			if (VoronoiUtils.isValid(voronoiEdges, voronoiScale)) {
				break;
			}
			LOG.warning("voronoi diagram is illegal, try again");
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
		for (Graph.Corner c : graph.corners) {
			HashSet<Graph.Center> cx = new HashSet<>(c.touches);
			c.touches.clear();
			c.touches.addAll(cx);
		}
		int borderCorners = 0;
		for (Graph.Corner c : graph.corners) {
			if (c.border) borderCorners++;
		}
		LOG.info("border corners: "+borderCorners);
		LOG.info("graph created");
		
		//next step
		updateGraphNode();
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
					float distInfluence = 2.2f; //to be tuned
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
					double x = (c.point.x - 0.5) * 2.2;
					double y = (c.point.y - 0.5) * 2.2;
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
		
		findOceans();
		
		updateBiomesGeometry();
	}
	private void findOceans() {
		for (Graph.Center c : graph.centers) {
			c.ocean = false;
			c.water = false;
		}
		for (Graph.Corner c : graph.corners) {
			c.ocean = false;
		}
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
		//assign coast tag
		for (Graph.Corner q : graph.corners) {
			q.coast = false;
		}
		for (Graph.Center c : graph.centers) {
			if (c.ocean) {
				for (Graph.Corner q : c.corners) {
					if (!q.water) {
						q.coast = true;
					} else {
						q.ocean = true;
					}
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
	
	/**
	 * Step 4: assign elevation
	 */
	private void assignElevation() {
		if (graph==null) {
			return;
		}
		Random rand = new Random(seed * 2);
		//initialize border corners with zero elevation
		Deque<Graph.Corner> q = new ArrayDeque<>();
		for (Graph.Corner c : graph.corners) {
			if (c.border) {
				c.elevation = 0;
				q.add(c);
			} else {
				c.elevation = Float.POSITIVE_INFINITY;
			}
		}
		// Traverse the graph and assign elevations to each point. As we
		// move away from the map border, increase the elevations. This
		// guarantees that rivers always have a way down to the coast by
		// going downhill (no local minima).
		while (!q.isEmpty()) {
			Graph.Corner c = q.poll();
			for (Graph.Corner a : c.adjacent) {
				if (c.ocean && a.ocean && a.elevation>0) {
					a.elevation = 0;
					q.addFirst(a);
					continue;
				}
				float elevation = c.elevation + (a.ocean ? 0 : 0.01f);
				if (!c.water && !a.water) {
					elevation += 1;
				}
				//add some more randomness
				//elevation += rand.nextDouble()/4;
				if (elevation < a.elevation) {
					a.elevation = elevation;
					q.add(a);
				}
			}
		}
		
		//redistribute elevation
		float SCALE_FACTOR = 1.1f;
		ArrayList<Graph.Corner> corners = new ArrayList<>();
		for (Graph.Corner c : graph.corners) {
			if (!c.ocean) {
				corners.add(c);
			}
		}
		Collections.sort(corners, new Comparator<Graph.Corner>() {
			@Override
			public int compare(Graph.Corner o1, Graph.Corner o2) {
				return Float.compare(o1.elevation, o2.elevation);
			}
		});
		for (int i=0; i < corners.size(); i++) {
			// Let y(x) be the total area that we want at elevation <= x.
			// We want the higher elevations to occur less than lower
			// ones, and set the area to be y(x) = 1 - (1-x)^2.
			float y = (float) i/ (float) (corners.size()-1);
			float x = (float) (Math.sqrt(SCALE_FACTOR) - Math.sqrt(SCALE_FACTOR*(1-y)));
			if (x > 1.0) x = 1;  // TODO: does this break downslopes?
			corners.get(i).elevation = x;
		}
		
		assignCenterElevations();
		
		//update mesh
		updateElevationGeometry();
	}
	private void assignCenterElevations() {
		//assign elevation to centers
		for (Graph.Center c : graph.centers) {
			float elevation = 0;
			for (Graph.Corner corner : c.corners) {
				elevation += corner.elevation;
			}
			elevation /= c.corners.size();
			c.elevation = elevation;
		}
	}
	
	private void createBiomes() {
		if (graph==null) {
			return;
		}
		
		//assign temperatures
		for (Graph.Corner c : graph.corners) {
			c.temperature = c.elevation;
			c.temperature *= c.temperature;
			c.temperature = 1-c.temperature;
		}
		assignCenterTemperature();
		
		//create random rivers
		Random rand = new Random(seed * 3);
		for (Graph.Corner c : graph.corners) {
			c.river = 0;
		}
		float riverProb = 0.2f;
		float riverStartHeight = 0.7f;
		int riverCounter = 0;
		corner:
		for (Graph.Corner c : graph.corners) {
			if (c.water || c.elevation<riverStartHeight) {
				continue;
			}
			if (rand.nextFloat() > riverProb) {
				continue;
			}
			if (c.river>0) continue;
			for (Graph.Corner c2 : c.adjacent) {
				if (c2.river>0) {
					continue corner;
				}
				for (Graph.Corner c3 : c2.adjacent) {
					if (c3.river>0) {
						continue corner;
					}
				}
			}
			//start new river from here
			Graph.Corner current = c;
			current.river = Math.max(current.river, 1);
			while (!current.ocean && !current.coast) {
				float minH = current.elevation;
				Graph.Corner minC = null;
				for (Graph.Corner c2 : current.adjacent) {
					if (c2.river>0 && c2.elevation<current.elevation) {
						minC = c2; //force closing of rivers
						break;
					}
					if (c2.elevation < minH) {
						minC = c2;
						minH = c2.elevation;
					}
				}
				if (minC == null) {
					LOG.warning("river stuck in a local minima without reaching the ocean");
					break;
				}
				minC.river = Math.max(minC.river, current.river + 1);
				current = minC;
			}
			riverCounter++;
		}
		LOG.info("count of created rivers: "+riverCounter);
		
		//assign moisture
		Queue<Graph.Corner> queue = new ArrayDeque<>();
		for (Graph.Corner q : graph.corners) {
			if ((q.water || q.river > 0) && !q.ocean) {
				q.moisture = q.river > 0 ? Math.min(3.0f, (0.4f * q.river)) : 1;
				queue.add(q);
			} else {
				q.moisture = 0;
			}
		}
		while (!queue.isEmpty()) {
			Graph.Corner q = queue.poll();
			for (Graph.Corner r : q.adjacent) {
				float newMoisture = q.moisture * 0.8f;
				if (newMoisture > r.moisture) {
					r.moisture = newMoisture;
					queue.add(r);
				}
			}
		}
		for (Graph.Corner q : graph.corners) {
			if (q.ocean || q.coast) {
				q.moisture = 1;
			}
		}
		
		//redistribute moisture
		ArrayList<Graph.Corner> corners = new ArrayList<>();
		for (Graph.Corner q : graph.corners) {
			if (!q.ocean && !q.coast) {
				corners.add(q);
			}
		}
		Collections.sort(corners, new Comparator<Graph.Corner>() {
			@Override
			public int compare(Graph.Corner o1, Graph.Corner o2) {
				return Float.compare(o1.moisture, o2.moisture);
			}
		});
		for (int i = 0; i < corners.size(); i++) {
			corners.get(i).moisture = i/(float) (corners.size()-1);
		}
		assignCenterMoisture();
		
		assignBiomes();
		
		//update mesh
		updateTemperatureGeometry();
		updateMoistureGeometry();
		updateBiomesGeometry();
	}
	private void assignCenterTemperature() {
		for (Graph.Center c : graph.centers) {
			float t = 0;
			for (Graph.Corner q : c.corners) {
				t += q.temperature;
			}
			t /= c.corners.size();
			c.temperature = t;
		}
	}
	private void assignCenterMoisture() {
		for (Graph.Center c : graph.centers) {
			float m = 0;
			for (Graph.Corner q : c.corners) {
				m += q.moisture;
			}
			c.moisture = m / c.corners.size();
		}
	}
	private void assignBiomes() {
		//create biomes
		biomes:
		for (Graph.Center c : graph.centers) {
			if (!c.water && !c.ocean) {
				for (Graph.Corner o : c.corners) {
					if ((o.ocean || o.coast) && c.moisture<0.7 && c.temperature>0.5) {
						c.biome = Biome.BEACH;
						continue biomes;
					}
				}
				c.biome = Biome.getBiome(c.temperature, 1-c.moisture);
			}
		}
	}
	private float smootherstep(float edge0, float edge1, float x) {
		// Scale, and clamp x to 0..1 range
		x = Math.max(0, Math.min(1, (x - edge0)/(edge1 - edge0)));
		// Evaluate polynomial
		return x*x*x*(x*(x*6 - 15) + 10);
	}
//</editor-fold>
	
//<editor-fold defaultstate="collapsed" desc=" Mesh creation ">
	//display graph
	private void updateGraphNode() {
		if (graphNode==null) {
			return;
		}
		
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
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Gray);
		Geometry edgeGeom = new Geometry("edgeGeom", edgeMesh);
		edgeGeom.setMaterial(mat);
		edgeGeom.setCullHint(Spatial.CullHint.Never);
		edgeNode.detachAllChildren();
		edgeNode.attachChild(edgeGeom);
		LOG.info("edge geometry updated");
	}
	private void updateTemperatureGeometry() {
		ArrayList<Vector3f> posList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>();
		Map<Graph.Corner, Integer> cornerIndices = new HashMap<>();
		for (Graph.Center c : graph.centers) {
			int i = posList.size();
			posList.add(new Vector3f(c.location.x, c.location.y, 1));
			if (c.water) {
				colorList.add(c.biome.color);
			} else {
				colorList.add(new ColorRGBA(Math.max(0, c.temperature), 0, Math.min(1, 1-c.temperature), 1));
			}
			cornerIndices.clear();
			for (Graph.Corner q : c.corners) {
				cornerIndices.put(q, posList.size());
				posList.add(new Vector3f(q.point.x, q.point.y, 1));
				if (c.water) {
					colorList.add(c.biome.color);
				} else {
					colorList.add(new ColorRGBA(Math.max(0, q.temperature), 0, Math.min(1, 1-q.temperature), 1));
				}
			}
			for (Graph.Edge edge : c.borders) {
				indexList.add(i);
				indexList.add(cornerIndices.get(edge.v0));
				indexList.add(cornerIndices.get(edge.v1));
			}
		}
		Mesh mesh = new Mesh();
		mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posList.toArray(new Vector3f[posList.size()])));
		mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colorList.toArray(new ColorRGBA[colorList.size()])));
		mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(ArrayUtils.toPrimitive(indexList.toArray(new Integer[indexList.size()]))));
		mesh.setMode(Mesh.Mode.Triangles);
		mesh.updateCounts();
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setBoolean("VertexColor", true);
		mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		Geometry geom = new Geometry("biomesGeom", mesh);
		geom.setMaterial(mat);
		temperatureNode.detachAllChildren();
		temperatureNode.attachChild(geom);
		LOG.info("biomes geometry updated");
	}
	private void updateMoistureGeometry() {
		moistureNode.detachAllChildren();
		//moisture
		ArrayList<Vector3f> posList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>();
		Map<Graph.Corner, Integer> cornerIndices = new HashMap<>();
		for (Graph.Center c : graph.centers) {
			int i = posList.size();
			posList.add(new Vector3f(c.location.x, c.location.y, 1));
			if (c.water) {
				colorList.add(c.biome.color);
			} else {
				colorList.add(new ColorRGBA(Math.max(0, 1-c.moisture), Math.max(0, 0.5f-c.moisture/2), Math.min(1, c.moisture), 1));
			}
			cornerIndices.clear();
			for (Graph.Corner q : c.corners) {
				cornerIndices.put(q, posList.size());
				posList.add(new Vector3f(q.point.x, q.point.y, 1));
				if (c.water) {
					colorList.add(c.biome.color);
				} else {
					colorList.add(new ColorRGBA(Math.max(0, 1-q.moisture), Math.max(0, 0.5f-q.moisture/2), Math.min(1, q.moisture), 1));
				}
			}
			for (Graph.Edge edge : c.borders) {
				indexList.add(i);
				indexList.add(cornerIndices.get(edge.v0));
				indexList.add(cornerIndices.get(edge.v1));
			}
		}
		Mesh mesh = new Mesh();
		mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posList.toArray(new Vector3f[posList.size()])));
		mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colorList.toArray(new ColorRGBA[colorList.size()])));
		mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(ArrayUtils.toPrimitive(indexList.toArray(new Integer[indexList.size()]))));
		mesh.setMode(Mesh.Mode.Triangles);
		mesh.updateCounts();
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setBoolean("VertexColor", true);
		mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		Geometry geom1 = new Geometry("biomesGeom", mesh);
		geom1.setMaterial(mat);
		moistureNode.attachChild(geom1);
		//river
		if (SHOW_RIVERS) {
			posList.clear();
			colorList.clear();
			for (Graph.Edge e : graph.edges) {
				if (e.v0.river>0 && e.v1.river>0) {
					posList.add(new Vector3f(e.v0.point.x, e.v0.point.y, 0.5f));
					colorList.add(new ColorRGBA(1-e.v0.river/20f, 1-e.v0.river/20f, 1, 1));
					posList.add(new Vector3f(e.v1.point.x, e.v1.point.y, 0.5f));
					colorList.add(new ColorRGBA(1-e.v1.river/20f, 1-e.v1.river/20f, 1, 1));
				}
			}
			mesh = new Mesh();
			mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posList.toArray(new Vector3f[posList.size()])));
			mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colorList.toArray(new ColorRGBA[colorList.size()])));
			mesh.setMode(Mesh.Mode.Lines);
			mesh.setLineWidth(5);
	//		mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
	//		mat.setColor("Color", ColorRGBA.Blue);
			Geometry geom2 = new Geometry("riverGeom", mesh);
			geom2.setMaterial(mat);
			moistureNode.attachChild(geom2);
		}
		
		LOG.info("biomes geometry updated");
	}
	private void updateBiomesGeometry() {
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
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setBoolean("VertexColor", true);
		mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		Geometry biomesGeom = new Geometry("biomesGeom", biomesMesh);
		biomesGeom.setMaterial(mat);
		biomesNode.detachAllChildren();
		biomesNode.attachChild(biomesGeom);
		LOG.info("biomes geometry updated");
	}
	private void updateElevationGeometry() {
		//biomes
		ArrayList<Vector3f> posList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>();
		Map<Graph.Corner, Integer> cornerIndices = new HashMap<>();
		for (Graph.Center c : graph.centers) {
			int i = posList.size();
			posList.add(new Vector3f(c.location.x, c.location.y, 1));
			if (c.biome == Biome.LAKE || c.biome == Biome.OCEAN) {
				colorList.add(c.biome.color);
			} else {
				colorList.add(new ColorRGBA(Math.max(0, c.elevation*2-1), Math.min(1, c.elevation*2), Math.max(0, c.elevation*2-1), 1));
			}
			cornerIndices.clear();
			for (Graph.Corner corner : c.corners) {
				cornerIndices.put(corner, posList.size());
				posList.add(new Vector3f(corner.point.x, corner.point.y, 1));
				if (c.biome == Biome.LAKE || c.biome == Biome.OCEAN) {
					colorList.add(c.biome.color);
				} else {
					colorList.add(new ColorRGBA(Math.max(0, corner.elevation*2-1), Math.min(1, corner.elevation*2), Math.max(0, corner.elevation*2-1), 1));
				}
			}
			for (Graph.Edge edge : c.borders) {
				indexList.add(i);
				indexList.add(cornerIndices.get(edge.v0));
				indexList.add(cornerIndices.get(edge.v1));
			}
		}
		Mesh elevation = new Mesh();
		elevation.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posList.toArray(new Vector3f[posList.size()])));
		elevation.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colorList.toArray(new ColorRGBA[colorList.size()])));
		elevation.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(ArrayUtils.toPrimitive(indexList.toArray(new Integer[indexList.size()]))));
		elevation.setMode(Mesh.Mode.Triangles);
		elevation.updateCounts();
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setBoolean("VertexColor", true);
		mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		Geometry elevationGeom = new Geometry("elevationGeom", elevation);
		elevationGeom.setMaterial(mat);
		elevationNode.detachAllChildren();
		elevationNode.attachChild(elevationGeom);
		LOG.info("elevation geometry updated");
	}
//</editor-fold>
	
//<editor-fold defaultstate="collapsed" desc=" GUI communication ">
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
	void guiInitialValues(int seed, int pointCount, int relaxationIterations, Coastline coastline,
			int mapSize, int mapSeed) {
		this.seed = seed;
		this.pointCount = pointCount;
		this.relaxationIterations = relaxationIterations;
		this.coastline = coastline;
		this.mapSeed = mapSeed;
		this.mapSize = mapSize;
		computeVoronoi();
	}
	void guiSeedChanged(int seed) {
		this.seed = seed;
		computeVoronoi();
	}
	void guiPointCountChanged(int count) {
		this.pointCount = count;
		computeVoronoi();
	}
	void guiRelaxationChanged(int iterations) {
		this.relaxationIterations = iterations;
		computeVoronoi();
	}
	void guiCoastlineChanged(Coastline coastline) {
		this.coastline = coastline;
		assignCoastline();
	}
	void guiGenerateElevation() {
		assignElevation();
	}
	void guiShowDrawElevation(boolean enabled) {
		if (elevationNode == null) {
			return ;
		}
		elevationNode.setCullHint(enabled ? Spatial.CullHint.Never : Spatial.CullHint.Always);
		if (enabled) {
			brush = 1;
		} else if (brush==1) {
			brush = 0;
		}
	}
	void guiShowBiomes(boolean enabled) {
		if (biomesNode == null) {
			return ;
		}
		biomesNode.setCullHint(enabled ? Spatial.CullHint.Never : Spatial.CullHint.Always);
	}
	void guiShowDrawTemperature(boolean enabled) {
		if (temperatureNode != null) {
			temperatureNode.setCullHint(enabled ? Spatial.CullHint.Never : Spatial.CullHint.Always);
		}
		if (enabled) {
			brush = 2;
		} else if (brush==2) {
			brush = 0;
		}
	}
	void guiShowDrawMoisture(boolean enabled) {
		if (moistureNode != null) {
			moistureNode.setCullHint(enabled ? Spatial.CullHint.Never : Spatial.CullHint.Always);
		}
		if (enabled) {
			brush = 3;
		} else if (brush==3) {
			brush = 0;
		}
	}
	void guiGenerateBiomes() {
		createBiomes();
	}
	void guiMapSizeChanged(int mapSize) {
		this.mapSize = mapSize;
	}
	void guiMapSeedChanged(int seed) {
		this.mapSeed = seed;
	}
	void guiGenerateMap() {
		LOG.info("generate map with size "+mapSize+" and seed "+Integer.toHexString(mapSeed));
		GraphToHeightmap converter = new GraphToHeightmap(graph, mapSize, app, mapSeed);
		Map<Object, Object> props = converter.getResult();
		final Heightmap map = (Heightmap) props.get(KEY_HEIGHTMAP);
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				app.setTerrain(map);
				app.setSkyEnabled(true);
				app.setCameraEnabled(true);
				app.enableWater(0);
		//		unregisterInput();
				graphNode.setCullHint(Spatial.CullHint.Always);
				return null;
			}
		});
		//TODO
	}
//</editor-fold>
	
	/**
	 * Mouse is dragged over the graph.
	 * @param x x-coordinate on the graph, from 0 to 1
	 * @param y y-coordinate on the graph, from 0 to 1
	 * @param left {@code true} on left click, {@code false} on right click
	 */
	private void mouseDragged(float x, float y, boolean left, float tpf) {
		if (graph==null || brush==0) {
			return;
		}
		Vector2f p = new Vector2f(x, y);
		ArrayList<Pair<Graph.Corner, Float>> influenced = new ArrayList<>();
		for (Graph.Corner q : graph.corners) {
			float dist = q.point.distance(p);
			if (dist < BRUSH_RADIUS) {
				dist /= BRUSH_RADIUS;
				float influence = FastMath.cos(dist*dist*FastMath.HALF_PI);
				influenced.add(new ImmutablePair<>(q, influence));
			}
		}
		float amount = tpf * 0.5f * (left ? 1 : -1);
		switch (brush) {
			case 1: //elevation
				for (Pair<Graph.Corner, Float> c : influenced) {
					c.getLeft().elevation = Math.max(0, Math.min(1, c.getLeft().elevation + amount*c.getRight()));
					if (c.getLeft().elevation==0) {
						c.getLeft().water = true;
					} else {
						c.getLeft().water = false;
					}
				}
				findOceans();
				assignCenterElevations();
				updateElevationGeometry();
				break;
			case 2: //temperature
				for (Pair<Graph.Corner, Float> c : influenced) {
					c.getLeft().temperature = Math.max(0, Math.min(1, c.getLeft().temperature + amount*c.getRight()));
				}
				assignCenterTemperature();
				assignBiomes();
				updateTemperatureGeometry();
				updateBiomesGeometry();
				break;
			case 3: //moisture
				for (Pair<Graph.Corner, Float> c : influenced) {
					c.getLeft().moisture = Math.max(0, Math.min(1, c.getLeft().moisture + amount*c.getRight()));
				}
				assignCenterMoisture();
				assignBiomes();
				updateMoistureGeometry();
				updateBiomesGeometry();
				break;
		}
	}
	
	private void registerInput() {
		if (brushGeom == null) {
			brushGeom = new Geometry("brush", new Quad(BRUSH_RADIUS*2, BRUSH_RADIUS*2));
			Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			mat.setTexture("ColorMap", app.getAssetManager().loadTexture("org/shaman/terrain/polygonal/Brush.png"));
			mat.getAdditionalRenderState().setDepthTest(false);
			mat.getAdditionalRenderState().setDepthWrite(false);
			mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
			brushGeom.setMaterial(mat);
			graphNode.attachChild(brushGeom);
			brushGeom.setCullHint(Spatial.CullHint.Always);
			
			listener = new InputListenerImpl();
			app.getInputManager().addMapping("PolygonalMouseX+", new MouseAxisTrigger(MouseInput.AXIS_X, false));
			app.getInputManager().addMapping("PolygonalMouseX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));
			app.getInputManager().addMapping("PolygonalMouseY+", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
			app.getInputManager().addMapping("PolygonalMouseY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
			app.getInputManager().addMapping("PolygonalMouseLeft", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
			app.getInputManager().addMapping("PolygonalMouseRight", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
		}
		app.getInputManager().addListener(listener, "PolygonalMouseX+", "PolygonalMouseX-", "PolygonalMouseY+", 
				"PolygonalMouseY-", "PolygonalMouseLeft", "PolygonalMouseRight");
	}
	private void unregisterInput() {
		app.getInputManager().removeListener(listener);
	}
	
	private class InputListenerImpl implements ActionListener, AnalogListener {
		private float mouseX, mouseY;
		private boolean left, right;

		private void update(float tpf) {
			float x = (mouseX - graphNode.getLocalTranslation().x) / graphNode.getLocalScale().x;
			float y = (mouseY - graphNode.getLocalTranslation().y) / graphNode.getLocalScale().y;
			if (x<0 || y<0 || x>1 || y>1) {
				brushGeom.setCullHint(Spatial.CullHint.Always);
			} else {
				brushGeom.setLocalTranslation(x - BRUSH_RADIUS, y - BRUSH_RADIUS, 0);
				brushGeom.setCullHint(Spatial.CullHint.Never);
				if ((!left && !right) || (left && right)) {
					return;
				}
				boolean up = left;
				mouseDragged(x, y, up, tpf);
			}
		}
		
		@Override
		public void onAction(String name, boolean isPressed, float tpf) {
			if ("PolygonalMouseLeft".equals(name)) {
				left = isPressed;
				update(tpf);
			} else if ("PolygonalMouseRight".equals(name)) {
				right = isPressed;
				update(tpf);
			}
		}

		@Override
		public void onAnalog(String name, float value, float tpf) {
			mouseX = app.getInputManager().getCursorPosition().x;
			mouseY = app.getInputManager().getCursorPosition().y;
			update(tpf);
		}
		
	}
}
