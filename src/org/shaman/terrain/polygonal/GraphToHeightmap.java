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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.ArrayUtils;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;
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

	//Input
	private final Graph graph;
	private final int size;
	private final Application app;
	//Output
	private final Heightmap heightmap;
	private final Heightmap temperature;
	private final Heightmap moisture;
	private final Map<Object, Object> properties;

	public GraphToHeightmap(Graph graph, int size, Application app) {
		this.graph = graph;
		this.size = size;
		this.app = app;

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
		render(heightmap.getRawData(), geom, ColorRGBA.Black, -1, 1);
		
		//reset height
		for (Graph.Corner c : graph.corners) {
			if (c.ocean) {
				c.elevation = 0;
			}
		}
		assignCenterElevations();
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
	private void render(float[][] matrix, Spatial scene, ColorRGBA background, float min, float max) {
		//init
		Camera cam = new Camera(size, size);
		cam.setParallelProjection(true);
		ViewPort view = new ViewPort("Off", cam);
		view.setBackgroundColor(background);
		view.setClearFlags(true, true, true);
		FrameBuffer buffer = new FrameBuffer(size, size, 1);
		buffer.setDepthBuffer(Image.Format.Depth);
		buffer.setColorBuffer(Image.Format.RGBA32F);
		view.setOutputFrameBuffer(buffer);
		view.attachScene(scene);
		//render
		scene.setCullHint(Spatial.CullHint.Never);
		scene.updateGeometricState();
		view.setEnabled(true);
		app.getRenderManager().renderViewPort(view, 0);
		//retrive data
		ByteBuffer data = BufferUtils.createByteBuffer(size * size * 4 * 4);
		app.getRenderer().readFrameBufferWithFormat(buffer, data, Image.Format.RGBA32F);
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
	
	private void saveMaps() {
		saveMatrix(temperature.getRawData(), "temperature.png", 0, 1);
		saveMatrix(moisture.getRawData(), "moisture.png", 0, 1);
		saveMatrix(heightmap.getRawData(), "elevation.png", -1, 1);
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
}
