/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import Jama.Matrix;
import com.jme3.app.Application;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.ArrayUtils;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.heightmap.Noise;
import org.shaman.terrain.sketch.SketchTerrain;

/**
 * Converts the graph to a heightmap.<br>
 * Input: a {@link Graph} with all the informations of elevation, temperature,
 * moisture, biomes.<br>
 * Output: a {@link Map} object with the heightmap, temperature and moisture
 * stored in {@link Heightmap} instances, using the keys from
 * {@link AbstractTerrainStep}.
 *
 * @author Sebastian Weiss
 */
public class GraphToHeightmap {
	private static final Logger LOG = Logger.getLogger(GraphToHeightmap.class.getName());
	/**
	 * Maps the biomes to noise amplitude (0), noise roughness (1),
	 * voronoi amplitude (2) and the factor of the height on the voronoi amplitude(3)
	 */
	private static final Map<Biome, double[]> BIOME_PROPERTIES = new HashMap<>();
	static {
		BIOME_PROPERTIES.put(Biome.SNOW, new double[]{0.5, 0.7, 0.7, 0.5});
		BIOME_PROPERTIES.put(Biome.TUNDRA, new double[]{0.5, 0.5, 0.5, 0.5});
		BIOME_PROPERTIES.put(Biome.BARE, new double[]{0.4, 0.4, 0.5, 0.5});
		BIOME_PROPERTIES.put(Biome.SCORCHED, new double[]{0.7, 0.3, 0, 0});
		BIOME_PROPERTIES.put(Biome.TAIGA, new double[]{0.4, 0.3, 0, 0});
		BIOME_PROPERTIES.put(Biome.SHRUBLAND, new double[]{0.5, 0.2, 0, 0});
		BIOME_PROPERTIES.put(Biome.TEMPERATE_DESERT, new double[]{0.1, 0.1, 0.3, 0});
		BIOME_PROPERTIES.put(Biome.TEMPERATE_RAIN_FOREST, new double[]{0.3, 0.2, 0, 0});
		BIOME_PROPERTIES.put(Biome.TEMPERATE_DECIDUOUS_FOREST, new double[]{0.3, 0.4, 0, 0});
		BIOME_PROPERTIES.put(Biome.GRASSLAND, new double[]{0.4, 0.5, 0, 0});
		BIOME_PROPERTIES.put(Biome.TROPICAL_RAIN_FOREST, new double[]{0.3, 0.2, 0.05, 0});
		BIOME_PROPERTIES.put(Biome.TROPICAL_SEASONAL_FOREST, new double[]{0.3, 0.2, 0, 0});
		BIOME_PROPERTIES.put(Biome.SUBTROPICAL_DESERT, new double[]{0.3, 0.6, 0.3, 0});
		BIOME_PROPERTIES.put(Biome.BEACH, new double[]{0.3, 0.5, 0.1, 0});
		BIOME_PROPERTIES.put(Biome.LAKE, new double[]{0.2, 0.2, 0, 0});
		BIOME_PROPERTIES.put(Biome.OCEAN, new double[]{0.2, 0.1, 0, 0});
	}
	private static final int NOISE_OCTAVES = 6;
	private static final float NOISE_OCTAVE_FACTOR = 2;
	private static final float BASE_FREQUENCY = 1/16f;
	private static final float PERLIN_NOISE_SCALE = 0.2f;
	private static final float DISTORTION_FREQUENCY = 32;
	private static final float DISTORTION_AMPLITUDE = 0.01f;
	private static final float SMOOTHING_STEPS = 10;
	private static final int[][] NEIGHBORS = new int[][]{
		{1, -1}, {1, 0}, {1, 1},
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, 1}, {0, -1}
	};
	private static final int VORONOI_CELL_COUNT = 32;
	private static final int VORONOI_POINTS_PER_CELL = 3;
	private static final float VORONOI_DISTORTION_FREQUENCY = 32;
	private static final float VORONOI_DISTORTION_AMPLITUDE = 0.01f;
	private static final float VORONOI_SCALE = 1f;
	
	//Input
	private final Graph graph;
	private final int size;
	private final Application app;
	private final Random rand;
	//Output
	private final Heightmap heightmap;
	private final Heightmap temperature;
	private final Heightmap moisture;
	private final Map<Object, Object> properties;
	//temporal values
	private float[][][] noise;
	
	public GraphToHeightmap(Graph graph, int size, Application app, long seed) {
		this.graph = graph;
		this.size = size;
		this.app = app;
		this.rand = new Random(seed);

		heightmap = new Heightmap(size);
		temperature = new Heightmap(size);
		moisture = new Heightmap(size);
		properties = new HashMap<>();
		properties.put(AbstractTerrainStep.KEY_HEIGHTMAP, heightmap);
		properties.put(AbstractTerrainStep.KEY_TEMPERATURE, temperature);
		properties.put(AbstractTerrainStep.KEY_MOISTURE, moisture);
		properties.put("PolygonalGraph", graph); //for backup

		calculate();
	}

	public Map<Object, Object> getResult() {
		return properties;
	}

	private void calculate() {
		calculateTemperatureAndMoisture();
		calculateElevation();
		
		saveMaps();
	}
	
	private void calculateElevation() {
		calculateBaseElevation();
		
		//get noise parameters
		Geometry geom = createNoiseGeometry();
		noise = new float[size][size][3];
		renderColor(noise, geom, ColorRGBA.Black, 0, 1);
		LOG.info("noise properties calculated");
		
		addPerlinNoise();
		
		addVoronoiNoise();
	}

	private void addPerlinNoise() {
		float[] perlinFactors = new float[NOISE_OCTAVES];
		Noise[] noiseGenerators = new Noise[NOISE_OCTAVES];
		for (int i=0; i<NOISE_OCTAVES; ++i) {
			noiseGenerators[i] = new Noise(rand.nextLong());
			perlinFactors[i] = (float) (BASE_FREQUENCY * Math.pow(NOISE_OCTAVE_FACTOR, i));
		}
		Heightmap values = new Heightmap(size);
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float roughness = noise[x][y][1];
				//multi-fractal perlin noise
				double perlin = 0;
				for (int i=0; i<NOISE_OCTAVES; ++i) {
					perlin += noiseGenerators[i].noise(perlinFactors[i]*x, perlinFactors[i]*y)
							/ Math.pow(NOISE_OCTAVE_FACTOR, i*(1-roughness));
				}
				values.setHeightAt(x, y, (float) perlin);
				min = (float) Math.min(perlin, min);
				max = (float) Math.max(perlin, max);
			}
		}
		float factor = 1f / (max-min);
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float amplitude = noise[x][y][0];
				float perlin = (values.getHeightAt(x, y) - min) * factor;
				perlin *= amplitude * PERLIN_NOISE_SCALE;
				heightmap.adjustHeightAt(x, y, perlin);
			}
		}
		LOG.info("perlin noise added");
	}
	
	private void addVoronoiNoise() {
		float cellSize = (float) size / (float) VORONOI_CELL_COUNT;
		List<Vector3f> pointList = new ArrayList<>();
		for (int x=0; x<VORONOI_CELL_COUNT; ++x) {
			for (int y=0; y<VORONOI_CELL_COUNT; ++y) {
				float nx = x*cellSize;
				float ny = y*cellSize;
				for (int i=0; i<VORONOI_POINTS_PER_CELL; ++i) {
					pointList.add(new Vector3f(
							nx + rand.nextFloat()*cellSize,
							ny + rand.nextFloat()*cellSize,
							rand.nextFloat()));
				}
			}
		}
		Vector3f[] points = pointList.toArray(new Vector3f[pointList.size()]);
		LOG.info("voronoi point created");
		//now cycle through cells and find influencing hills
		Heightmap tmp = new Heightmap(size);
		Vector3f first = new Vector3f();
		Vector3f second = new Vector3f();
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				//sort points by distance to (x,y)
				final float px = x;
				final float py = y;
				findClosestTwoPoints(points, px, py, first, second);
				assert (dist(first, px, py) <= dist(second, px, py));
				//calc height
				float v = -1*dist(first, px, py) 
						+ 1*dist(second, px, py);
				tmp.setHeightAt(x, y, v);
				min = Math.min(v, min);
				max = Math.max(v, max);
			}
		}
		//normalize
		float factor = 1f / (max-min);
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float h = tmp.getHeightAt(x, y);
				tmp.setHeightAt(x, y, (h-min) * factor);
			}
		}
		LOG.info("voronoi cells calculated");
		//distort
		Noise distortionNoise = new Noise(rand.nextLong());
		Heightmap tmp2 = new Heightmap(size);
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float s = x/(float)size;
				float t = y/(float)size;
				float ss = (float) (s + VORONOI_DISTORTION_AMPLITUDE 
						* 2*distortionNoise.noise(s*VORONOI_DISTORTION_FREQUENCY, 
								t*VORONOI_DISTORTION_FREQUENCY, 0));
				float tt = (float) (t + VORONOI_DISTORTION_AMPLITUDE 
						* 2*distortionNoise.noise(s*VORONOI_DISTORTION_FREQUENCY, 
								t*VORONOI_DISTORTION_FREQUENCY, 3.4));
				float v = tmp.getHeightInterpolating(ss*size, tt*size);
				tmp2.setHeightAt(x, y, v);
			}
		}
		LOG.info("voronoi cells distorted");
		//apply
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float amplitude = noise[x][y][2];
				heightmap.adjustHeightAt(x, y, amplitude * tmp.getHeightAt(x, y) * VORONOI_SCALE);
			}
		}
		
		LOG.info("voronoi noise added");
	}
	private static void findClosestTwoPoints(Vector3f[] points, float px, float py, Vector3f first, Vector3f second) {
		float dist1 = Float.POSITIVE_INFINITY;
		float dist2 = Float.POSITIVE_INFINITY;
		for (Vector3f p : points) {
			float d = dist(p, px, py);
			if (d<dist1) {
				dist2 = dist1;
				second.set(first);
				dist1 = d;
				first.set(p);
			} else if (d<dist2) {
				dist2 = d;
				second.set(p);
			}
		}
	}
	private static float dist(Vector3f hillCenter, float px, float py) {
		//return FastMath.sqrt((hillCenter.x-px)*(hillCenter.x-px) + (hillCenter.y-py)*(hillCenter.y-py))*hillCenter.z;
		return ((hillCenter.x-px)*(hillCenter.x-px) + (hillCenter.y-py)*(hillCenter.y-py))*hillCenter.z;
	}

	private void calculateBaseElevation() {
		//assign elevation to oceans
		for (Graph.Corner c : graph.corners) {
			if (c.ocean) {
				c.elevation = -1;
			}
		}
		Queue<Graph.Corner> q = new ArrayDeque<>();
		for (Graph.Corner c : graph.corners) {
			if (c.coast) {
				q.add(c);
			}
		}
		while (!q.isEmpty()) {
			Graph.Corner c = q.poll();
			for (Graph.Corner r : c.adjacent) {
				float h = Math.max(-1, c.elevation - 0.2f);
				if (r.ocean && r.elevation<h) {
					r.elevation = h;
					q.add(r);
				}
			}
		}
		assignCenterElevations();
		//render
		Geometry geom = createElevationGeometry();
		Heightmap tmp = new Heightmap(size);
		render(tmp.getRawData(), geom, ColorRGBA.Black, -1, 1);
		//scale
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float h = tmp.getHeightAt(x, y);
				h = Math.signum(h) * h * h;
				tmp.setHeightAt(x, y, h);
			}
		}
		//distort
		Noise distortionNoise = new Noise(rand.nextLong());
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				float s = x/(float)size;
				float t = y/(float)size;
				float ss = (float) (s + DISTORTION_AMPLITUDE * 2*distortionNoise.noise(s*DISTORTION_FREQUENCY, t*DISTORTION_FREQUENCY, 0));
				float tt = (float) (t + DISTORTION_AMPLITUDE * 2*distortionNoise.noise(s*DISTORTION_FREQUENCY, t*DISTORTION_FREQUENCY, 3.4));
				float v = tmp.getHeightInterpolating(ss*size, tt*size);
				heightmap.setHeightAt(x, y, v);
			}
		}
		//smooth
		for (int i=0; i<SMOOTHING_STEPS; ++i) {
			smooth(heightmap);
		}
		//reset height
		for (Graph.Corner c : graph.corners) {
			if (c.ocean) {
				c.elevation = 0;
			}
		}
		assignCenterElevations();
		LOG.info("base elevation assigned");
	}
	private void smooth_old(Heightmap map) {
		Heightmap m = map.clone();
		float t = 2 / map.getSize();
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float h = map.getHeightAt(x, y);
				float[] hi = new float[NEIGHBORS.length];
				float[] di = new float[NEIGHBORS.length];
				float dmax = 0;
				float dtotal = 0;
				//compute slopes
				for (int i=0; i<NEIGHBORS.length; ++i) {
					hi[i] = m.getHeightAtClamping(x + NEIGHBORS[i][0], y + NEIGHBORS[i][1]);
					di[i] = h-hi[i];
					dmax = Math.max(dmax, di[i]);
					if (di[i] > t) {
						dtotal+=di[i];
					}
				}
				//move terrain
				float vsum = 0;
				for (int i=0; i<NEIGHBORS.length; ++i) {
					if (di[i] > t) {
						float v = 0.25f*(dmax-t)*di[i]/dtotal;
						map.adjustHeightAt(x + NEIGHBORS[i][0], y + NEIGHBORS[i][1], v);
						vsum+=v;
					}
				}
				map.adjustHeightAt(x, y, -vsum);
			}
		}
	}
	private void smooth(Heightmap map) {
		Heightmap m = map.clone();
		float t = 2 / map.getSize();
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				float h = map.getHeightAt(x, y);
				float nh = 0;
				//compute average
				for (int i=0; i<NEIGHBORS.length; ++i) {
					nh += m.getHeightAtClamping(x + NEIGHBORS[i][0], y + NEIGHBORS[i][1]);
				}
				nh /= NEIGHBORS.length;
				float diff = nh - h;
				diff *= 0.2f;
				map.adjustHeightAt(x, y, diff);
			}
		}
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
	
	private void calculateTemperatureAndMoisture() {
		Geometry geom = createTemperatureGeometry();
		render(temperature.getRawData(), geom, ColorRGBA.White, 0, 1);
		LOG.info("temperature map created");
		
		geom = createMoistureGeometry();
		render(moisture.getRawData(), geom, ColorRGBA.Black, 0, 1);
		LOG.info("moisture map created");
	}
	
	private Geometry createElevationGeometry() {
		ArrayList<Vector3f> posList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>();
		Map<Graph.Corner, Integer> cornerIndices = new HashMap<>();
		for (Graph.Center c : graph.centers) {
			int i = posList.size();
			posList.add(new Vector3f(c.location.x, c.location.y, 1));
			colorList.add(new ColorRGBA(c.elevation/2 + 0.5f, c.elevation/2 + 0.5f, c.elevation/2 + 0.5f, 1));
			cornerIndices.clear();
			for (Graph.Corner q : c.corners) {
				cornerIndices.put(q, posList.size());
				posList.add(new Vector3f(q.point.x, q.point.y, 1));
				colorList.add(new ColorRGBA(q.elevation/2 + 0.5f, q.elevation/2 + 0.5f, q.elevation/2 + 0.5f, 1));
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
		Geometry geom = new Geometry("elevationGeom", mesh);
		geom.setMaterial(mat);
		geom.setLocalScale(size);
		geom.setQueueBucket(RenderQueue.Bucket.Gui);
		geom.setCullHint(Spatial.CullHint.Never);
		return geom;
	}
	private Geometry createNoiseGeometry() {
		ArrayList<Vector3f> posList = new ArrayList<>(graph.centers.size());
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>(graph.centers.size());
		for (Graph.Center c : graph.centers) {
			double[] settings = BIOME_PROPERTIES.get(c.biome);
			float r = (float) settings[0];
			float g = (float) settings[1];
			float b = (float) (settings[2] + settings[3]*Math.max(0, c.elevation)*Math.max(0, c.elevation));
			//float b = Math.max(0, c.elevation); b*=b;
			ColorRGBA col = new ColorRGBA(r, g, b, 1);
			colorList.add(col);
			posList.add(new Vector3f(c.location.x, c.location.y, 0));
		}
		for (Graph.Corner c : graph.corners) {
			if (c.touches.size() != 3) {
				//LOG.log(Level.INFO, "corner {0} does not touch 3 centers but {1}", new Object[]{c, c.touches.size()});
				continue; //border
			} else {
				for (Graph.Center c2 : c.touches) {
					indexList.add(c2.index);
				}
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
		Geometry geom = new Geometry("elevationGeom", mesh);
		geom.setMaterial(mat);
		geom.setLocalScale(size);
		geom.setQueueBucket(RenderQueue.Bucket.Gui);
		geom.setCullHint(Spatial.CullHint.Never);
		return geom;
	}
	private Geometry createTemperatureGeometry() {
		ArrayList<Vector3f> posList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>();
		Map<Graph.Corner, Integer> cornerIndices = new HashMap<>();
		for (Graph.Center c : graph.centers) {
			int i = posList.size();
			posList.add(new Vector3f(c.location.x, c.location.y, 1));
			colorList.add(new ColorRGBA(c.temperature, c.temperature, c.temperature, 1));
			cornerIndices.clear();
			for (Graph.Corner q : c.corners) {
				cornerIndices.put(q, posList.size());
				posList.add(new Vector3f(q.point.x, q.point.y, 1));
				colorList.add(new ColorRGBA(q.temperature, q.temperature, q.temperature, 1));
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
		geom.setLocalScale(size);
		geom.setQueueBucket(RenderQueue.Bucket.Gui);
		geom.setCullHint(Spatial.CullHint.Never);
		return geom;
	}
	private Geometry createMoistureGeometry() {
		//moisture
		ArrayList<Vector3f> posList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>();
		ArrayList<ColorRGBA> colorList = new ArrayList<>();
		Map<Graph.Corner, Integer> cornerIndices = new HashMap<>();
		ColorRGBA waterMoisture = new ColorRGBA(1, 1, 1, 1);
		ColorRGBA oceanMoisture = new ColorRGBA(0, 0, 0, 1);
		for (Graph.Center c : graph.centers) {
			int i = posList.size();
			posList.add(new Vector3f(c.location.x, c.location.y, 1));
			if (c.ocean) {
				colorList.add(oceanMoisture);
			} else if (c.water) {
				colorList.add(waterMoisture);
			} else {
				colorList.add(new ColorRGBA(1-c.moisture, 1-c.moisture, 1-c.moisture, 1));
			}
			cornerIndices.clear();
			for (Graph.Corner q : c.corners) {
				cornerIndices.put(q, posList.size());
				posList.add(new Vector3f(q.point.x, q.point.y, 1));
				if (c.ocean) {
					colorList.add(oceanMoisture);
				} else if (c.water) {
					colorList.add(waterMoisture);
				} else {
					colorList.add(new ColorRGBA(1-q.moisture, 1-q.moisture, 1-q.moisture, 1));
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
		geom.setLocalScale(size);
		geom.setQueueBucket(RenderQueue.Bucket.Gui);
		geom.setCullHint(Spatial.CullHint.Never);
		return geom;
	}

	/**
	 * Renders the given scene in a top-down manner in the given matrix
	 *
	 * @param matrix
	 * @param scene
	 */
	private void render(float[][] matrix, final Spatial scene, final ColorRGBA background, float min, float max) {
		final ByteBuffer data = BufferUtils.createByteBuffer(size * size * 4 * 4);
		try {
			app.enqueue(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					//init
					Camera cam = new Camera(size, size);
					cam.setParallelProjection(true);
					final ViewPort view = new ViewPort("Off", cam);
					view.setBackgroundColor(background);
					view.setClearFlags(true, true, true);
					final FrameBuffer buffer = new FrameBuffer(size, size, 1);
					buffer.setDepthBuffer(Image.Format.Depth);
					buffer.setColorBuffer(Image.Format.RGBA32F);
					view.setOutputFrameBuffer(buffer);
					view.attachScene(scene);
					//render
					scene.setCullHint(Spatial.CullHint.Never);
					scene.updateGeometricState();
					view.setEnabled(true);
					app.getRenderManager().renderViewPort(view, 0);
					app.getRenderer().readFrameBufferWithFormat(buffer, data, Image.Format.RGBA32F);
					return new Object();
				}
			}).get();
		} catch (InterruptedException | ExecutionException ex) {
			Logger.getLogger(GraphToHeightmap.class.getName()).log(Level.SEVERE, "unable to render", ex);
			return;
		}
		data.rewind();
		for (int y = 0; y < size; ++y) {
			for (int x = 0; x < size; ++x) {
				float v = data.getFloat();
				v *= (max-min);
				v += min;
				matrix[x][y] = v;
				data.getFloat();
				data.getFloat();
				data.getFloat();
			}
		}
	}
	/**
	 * Renders the given scene in a top-down manner in the given matrix
	 *
	 * @param matrix
	 * @param scene
	 */
	private void renderColor(float[][][] matrix, final Spatial scene, final ColorRGBA background, float min, float max) {
		final ByteBuffer data = BufferUtils.createByteBuffer(size * size * 4 * 4);
		try {
			app.enqueue(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					//init
					Camera cam = new Camera(size, size);
					cam.setParallelProjection(true);
					final ViewPort view = new ViewPort("Off", cam);
					view.setBackgroundColor(background);
					view.setClearFlags(true, true, true);
					final FrameBuffer buffer = new FrameBuffer(size, size, 1);
					buffer.setDepthBuffer(Image.Format.Depth);
					buffer.setColorBuffer(Image.Format.RGBA32F);
					view.setOutputFrameBuffer(buffer);
					view.attachScene(scene);
					//render
					scene.setCullHint(Spatial.CullHint.Never);
					scene.updateGeometricState();
					view.setEnabled(true);
					app.getRenderManager().renderViewPort(view, 0);
					app.getRenderer().readFrameBufferWithFormat(buffer, data, Image.Format.RGBA32F);
					return new Object();
				}
			}).get();
		} catch (InterruptedException | ExecutionException ex) {
			Logger.getLogger(GraphToHeightmap.class.getName()).log(Level.SEVERE, "unable to render", ex);
			return;
		}
		data.rewind();
		for (int y = 0; y < size; ++y) {
			for (int x = 0; x < size; ++x) {
				float v;
				v = data.getFloat();
				v *= (max-min);
				v += min;
				matrix[x][y][0] = v;
				
				v = data.getFloat();
				v *= (max-min);
				v += min;
				matrix[x][y][1] = v;
				
				v = data.getFloat();
				v *= (max-min);
				v += min;
				matrix[x][y][2] = v;
				
				data.getFloat();
			}
		}
	}
	
	private void saveMaps() {
		saveMatrix(temperature.getRawData(), "temperature.png", 0, 1);
		saveMatrix(moisture.getRawData(), "moisture.png", 0, 1);
		saveMatrix(heightmap.getRawData(), "elevation.png", -1.5f, 1.5f);
		saveColorMatrix(noise, "noise.png", 0, 1);
	}
	
	private void saveMatrix(float[][] matrix, String filename, float min, float max) {
		byte[] buffer = new byte[size*size];
		int i=0;
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				buffer[i] = (byte) ((matrix[x][y]-min) * 255 / (max-min));
				i++;
			}
		}
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		int[] nBits = { 8 };
		ColorModel cm = new ComponentColorModel(cs, nBits, false, true,
				Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		SampleModel sm = cm.createCompatibleSampleModel(size, size);
		DataBufferByte db = new DataBufferByte(buffer, size * size);
		WritableRaster raster = Raster.createWritableRaster(sm, db, null);
		BufferedImage result = new BufferedImage(cm, raster, false, null);
		try {
			ImageIO.write(result, "png", new File(filename));
		} catch (IOException ex) {
			Logger.getLogger(SketchTerrain.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	private void saveColorMatrix(float[][][] matrix, String filename, float min, float max) {
		byte[] buffer = new byte[size*size*3];
		int i=0;
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				buffer[i] = (byte) ((matrix[x][y][0]-min) * 255 / (max-min));
				i++;
				buffer[i] = (byte) ((matrix[x][y][1]-min) * 255 / (max-min));
				i++;
				buffer[i] = (byte) ((matrix[x][y][2]-min) * 255 / (max-min));
				i++;
			}
		}
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
		int[] nBits = { 8, 8, 8 };
		ColorModel cm = new ComponentColorModel(cs, nBits, false, true,
				Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		SampleModel sm = cm.createCompatibleSampleModel(size, size);
		DataBufferByte db = new DataBufferByte(buffer, size * size * 3);
		WritableRaster raster = Raster.createWritableRaster(sm, db, null);
		BufferedImage result = new BufferedImage(cm, raster, false, null);
		try {
			ImageIO.write(result, "png", new File(filename));
		} catch (IOException ex) {
			Logger.getLogger(SketchTerrain.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
