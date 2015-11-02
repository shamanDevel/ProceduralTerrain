/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.zero_separation.plugins.imagepainter.ImagePainter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.shaman.terrain.heightmap.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public class SketchTerrain implements AnalogListener, ActionListener {
	private static final Logger LOG = Logger.getLogger(SketchTerrain.class.getName());
	private static final int MAX_STROKES = 10;
	private static final int STROKE_WIDTH = 5;
	private static final int STROKE_T_JUNCTION_TRHESHOLD = 20;
	private static final ColorRGBA NEW_STROKE_COLOR = ColorRGBA.Black;
	private static final ColorRGBA OLD_STROKE_COLOR = ColorRGBA.White;
	private static final ColorRGBA[] STROKE_COLORS = new ColorRGBA[] { //has to be the same length as MAX_STROKES
		new ColorRGBA(0.5f, 0, 0, 1),
		new ColorRGBA(1, 0, 0, 1),
		new ColorRGBA(1, 0.5f, 0.5f, 1),
		new ColorRGBA(0, 0.5f, 0, 1),
		new ColorRGBA(0, 1f, 0, 1),
		new ColorRGBA(0.5f, 1, 0.5f, 1),
		new ColorRGBA(0, 0, 0.5f, 1),
		new ColorRGBA(0, 0, 1f, 1),
		new ColorRGBA(0.5f, 0.5f, 1f, 1),
		new ColorRGBA(1, 0, 1, 1)
	};
	
	private final TerrainHeighmapCreator app;
	private final Heightmap map;

	private Node guiNode;
	private int width, height;
	private ImagePainter tmpImage;
	private ImagePainter image;
	private Texture2D tmpTexture;
	private Material tmpQuadMaterial;
	private Texture2D texture;
	private Material quadMaterial;

	private boolean sketchStarted = false;
	private float lastX, lastY;
	private List<Vector2f> currentPoints = new ArrayList<>();
	/**
	 * this list contains all sketches. The sketches are point list from left to right
	 */
	private List<List<Vector2f>> sketches = new ArrayList<>();
	private Integer[] strokeOrder;
	/**
	 * Contains the features. Each feature is a list of adjacent heighmap coordinates (integers)
	 */
	private List<List<Vector2f>> features = new ArrayList<>();
	private Node featureNode = new Node("features");
	private DepthGrabber depthGrabber;

	public SketchTerrain(TerrainHeighmapCreator app, Heightmap map) {
		this.app = app;
		this.map = map;
		init();
	}

	private void init() {
		guiNode = new Node("sketch gui");
		app.getGuiNode().attachChild(guiNode);
		
		width = app.getCamera().getWidth();
		height = app.getCamera().getHeight();
		tmpImage = new ImagePainter(Image.Format.ABGR8, width, height);
		tmpImage.wipe(new ColorRGBA(0, 0, 0, 0f));
		image = new ImagePainter(Image.Format.ABGR8, width, height);
		image.wipe(new ColorRGBA(0, 0, 0, 0f));
		texture = new Texture2D(image.getImage());
		quadMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		quadMaterial.setTexture("ColorMap", texture);
		quadMaterial.setTransparent(true);
		quadMaterial.getAdditionalRenderState().setAlphaTest(true);
		quadMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		Quad quad = new Quad(width, height);
		Geometry quadGeom = new Geometry("SketchQuad", quad);
		quadGeom.setMaterial(quadMaterial);
		guiNode.attachChild(quadGeom);
		tmpTexture = new Texture2D(tmpImage.getImage());
		tmpQuadMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		tmpQuadMaterial.setTexture("ColorMap", tmpTexture);
		tmpQuadMaterial.setTransparent(true);
		tmpQuadMaterial.getAdditionalRenderState().setAlphaTest(true);
		tmpQuadMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		quad = new Quad(width, height);
		quadGeom = new Geometry("TmpSketchQuad", quad);
		quadGeom.setMaterial(tmpQuadMaterial);
		guiNode.attachChild(quadGeom);

		app.getInputManager().addMapping("SketchStartEnd", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		app.getInputManager().addMapping("SketchEditL", new MouseAxisTrigger(MouseInput.AXIS_X, true));
		app.getInputManager().addMapping("SketchEditR", new MouseAxisTrigger(MouseInput.AXIS_X, false));
		app.getInputManager().addMapping("SketchEditU", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
		app.getInputManager().addMapping("SketchEditD", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
		app.getInputManager().addListener(this, "SketchStartEnd", "SketchEditL", "SketchEditR", "SketchEditU", "SketchEditD");
		app.getInputManager().addMapping("SweepStrokes", new KeyTrigger(KeyInput.KEY_RETURN));
		app.getInputManager().addMapping("DeleteStrokes", new KeyTrigger(KeyInput.KEY_BACK));
		app.getInputManager().addListener(this, "SweepStrokes", "DeleteStrokes");
		
		BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Console.fnt");
		BitmapText orderText = new BitmapText(font);
		orderText.setText("Stroke order colors:");
		orderText.setLocalTranslation(0, height, 0);
		guiNode.attachChild(orderText);
		for (int i=0; i<STROKE_COLORS.length; ++i) {
			Quad q = new Quad(50, STROKE_WIDTH);
			Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			m.setColor("Color", STROKE_COLORS[i]);
			Geometry g = new Geometry("StrokeColor"+i, q);
			g.setMaterial(m);
			g.setLocalTranslation(5, height - orderText.getHeight() - (i*3+3)*STROKE_WIDTH, 0);
			guiNode.attachChild(g);
		}
		
		app.getRootNode().attachChild(featureNode);
		depthGrabber = new DepthGrabber(app);
	}

	public void onUpdate(float tpf) {
		Vector2f cursor = app.getInputManager().getCursorPosition();
		if (cursor.x == lastX && cursor.y == lastY) {
			return;
		}
		if (sketchStarted) {
			currentPoints.add(cursor.clone());
			int x1 = (int) currentPoints.get(currentPoints.size() - 2).x;
			int y1 = (int) currentPoints.get(currentPoints.size() - 2).y;
			int x2 = (int) currentPoints.get(currentPoints.size() - 1).x;
			int y2 = (int) currentPoints.get(currentPoints.size() - 1).y;
			drawLineBresenham(x1, y1, x2, y2, STROKE_WIDTH, NEW_STROKE_COLOR, tmpImage);
			tmpQuadMaterial.setTexture("ColorMap", tmpTexture);
		}
		lastX = cursor.x;
		lastY = cursor.y;
	}
	
	/**
	 * Adds the current sketch to the list of strokes
	 */
	private void addCurrentSketch() {
		for (int i=1; i<currentPoints.size(); ++i) {
			Vector2f p1 = currentPoints.get(i-1);
			Vector2f p2 = currentPoints.get(i);
			drawLineBresenham((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, STROKE_WIDTH, OLD_STROKE_COLOR, image);
			quadMaterial.setTexture("ColorMap", texture);
		}
		sketches.add(new ArrayList<>(currentPoints));
	}

	@Override
	public void onAnalog(String name, float value, float tpf) {
		if (!name.startsWith("SketchEdit")) {
			return;
		}
		Vector2f cursor = app.getInputManager().getCursorPosition();
		if (!sketchStarted) {
			lastX = cursor.x;
			lastY = cursor.y;
		}
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if ("SketchStartEnd".equals(name)) {
			if (isPressed) {
				if (sketches.size() == MAX_STROKES) {
					LOG.warning("maximal number of strokes reached, cannot add more");
					return;
				}
				LOG.info("start sketch");
				currentPoints.clear();
				currentPoints.add(new Vector2f(lastX, lastY));
				sketchStarted = true;
			} else {
				sketchStarted = false;
				LOG.log(Level.INFO, "sketch finished: {0}", currentPoints);
				if (isSketchValid(currentPoints)) {
					addCurrentSketch();
				} else {
					LOG.warning("sketch is invalid");
				}
				tmpImage.wipe(new ColorRGBA(0, 0, 0, 0f));
			}
		} else if ("SweepStrokes".equals(name) && isPressed) {
			sweepStrokes();
			findFeatures();
		} else if ("DeleteStrokes".equals(name) && isPressed) {
			deleteStrokes();
		}
	}
	
	/**
	 * Checks if the given sketch is a valid stroke
	 * @param points
	 * @return {@code true} if it is valid
	 */
	private boolean isSketchValid(List<Vector2f> points) {
		if (points.size()<2) {
			return false;
		}
		int dir = 0;
		for (int i=1; i<points.size(); ++i) {
			Vector2f p1 = points.get(i-1);
			Vector2f p2 = points.get(i);
			float dx = p2.x - p1.x;
			int d = (int) Math.signum(dx);
			if (d*dir == -1) {
				return false; //changed direction
			}
			dir = d!=0 ? d : dir;
		}
		if (dir < 0) {
			Collections.reverse(points);
		}
		return true;
	}
	
	/**
	 * Deletes all strokes
	 */
	private void deleteStrokes() {
		sketches.clear();
		image.wipe(ColorRGBA.BlackNoAlpha);
		quadMaterial.setTexture("ColorMap", texture);
		featureNode.detachAllChildren();
	}
	
	/**
	 * fills the gap in x-coordinates between the stroke's control points
	 * @param stroke the stroke
	 * @return an array with all control points
	 */
	private Vector2f[] fillStroke(List<Vector2f> stroke) {
		List<Vector2f> l = new ArrayList<>(stroke.size()*2);
		l.add(stroke.get(0));
		for (int i=1; i<stroke.size(); ++i) {
			Vector2f p0 = stroke.get(i-1);
			Vector2f p1 = stroke.get(i);
			for (int x=(int) p0.x+1; x<(int) p1.x; ++x) {
				float y = p0.y + (p1.y-p0.y)*(x-p0.x)/(p1.x-p0.x);
				l.add(new Vector2f(x, y));
			}
			l.add(p1);
		}
		return l.toArray(new Vector2f[l.size()]);
	}
	/**
	 * Sweeps the strokes to determine the order of them.
	 * Input: {@link #sketches}
	 * Output: {@link #strokeOrder}
	 */
	private void sweepStrokes() {
		LOG.info("sweep strokes");
		//for simplicity, copy into arrays
		int n = sketches.size();
		Vector2f[][] strokes = new Vector2f[n][];
		for (int i=0; i<n; ++i) {
			strokes[i] = fillStroke(sketches.get(i));
		}
		int[] index = new int[n];
		boolean[] started = new boolean[n];
		boolean[] ended = new boolean[n];
		boolean[] junctionsFound = new boolean[n];
		//list of 'first in-front-of second'-relations
		final Set<Pair<Integer, Integer>> relations = new HashSet<>();
		//sweep
		for (int sweep=0; sweep<width; ++sweep) {
			//check for starts of sketches
			for (int i=0; i<n; ++i) {
				if (!started[i] && strokes[i][0].x==sweep) {
					started[i]=true;
					index[i]=0;
					//check if it is a T-junction
					for (int j=0; j<n; ++j) {
						if (started[j] && !ended[j] && j!=i) {
							int intersection = distance(strokes[i][0], strokes[j]);
							if (strokes[i][0].distanceSquared(strokes[j][intersection]) < STROKE_T_JUNCTION_TRHESHOLD) {
								//t-junction, compare tangents
								float dy1 = strokes[i][1].y - strokes[i][0].y;
								float dy2 = strokes[j][intersection+1].y - strokes[j][intersection].y;
								if (dy1 > dy2) {
									//new stroke is behind
									relations.add(new ImmutablePair<>(j, i));
								} else {
									//new stroke is in front
									relations.add(new ImmutablePair<>(i, j));
								}
								junctionsFound[i] = true;
								junctionsFound[j] = true;
							}
						}
					}
				}
			}
			//increment index of active sketches
			for (int i=0; i<n; ++i) {
				if (started[i] && !ended[i]) {
					while(strokes[i][index[i]].x<=sweep) {
						index[i]++;
						if (index[i]>=strokes[i].length) {
							ended[i] = true;
							//end of stroke, check for t-junctions
							for (int j=0; j<n; ++j) {
								if (started[j] && !ended[j] && j!=i) {
									int intersection = distance(strokes[i][strokes[i].length-1], strokes[j]);
									if (strokes[i][strokes[i].length-1].distanceSquared(strokes[j][intersection]) < STROKE_T_JUNCTION_TRHESHOLD) {
										//t-junction, compare tangents
										float dy1 = strokes[i][strokes[i].length-1].y - strokes[i][strokes[i].length-2].y;
										float dy2 = strokes[j][intersection+1].y - strokes[j][intersection].y;
										if (dy1<dy2) {
											//ended stroke is behind
											relations.add(new ImmutablePair<>(j, i));
										} else {
											//ended stroke is in front
											relations.add(new ImmutablePair<>(i, j));
										}
										junctionsFound[i] = true;
										junctionsFound[j] = true;
									}
								}
							}
							break;
						}
					}
					index[i]--;
				}
			}
		}
		LOG.info("relations from t-junctions: "+relations);
		//check for strokes that have no junctions
		for (int i=0; i<n; ++i) {
			if (junctionsFound[i]) continue;
			for (int j=0; j<n; ++j) {
				if (j==i) continue;
				int startA, startB, endA, endB;
				if (strokes[i][0].x <= strokes[j][0].x) {
					if (strokes[i][strokes[i].length-1].x < strokes[j][0].x) {
						continue; //completely left
					} else {
						//search for start and end
						for (startA=0; strokes[i][startA].x<strokes[j][0].x; ++startA);
						endA = strokes[i].length-1;
						startB = 0;
						for (endB=strokes[j].length-1; strokes[i][strokes[i].length-1].x<strokes[j][endB].x; --endB);
					}
				} else {
					if (strokes[i][0].x > strokes[j][strokes[j].length-1].x) {
						continue; //completely right
					} else {
						//search for start and end
						startA = 0;
						for (endA=strokes[i].length-1; strokes[i][endA].x>strokes[j][strokes[j].length-1].x; --endA);
						for (startB=0; strokes[i][0].x>strokes[j][startB].x; ++startB);
						endB = strokes[j].length-1;
					}
				}
				//check intervall if it is completely below or above
				boolean below = false;
				boolean above = false;
				for (int a=startA; a<=endA; ++a) {
					//slow search, but simple
					for (int b=startB; b<=endB; ++b) {
						if (strokes[i][a].x==strokes[j][b].x) {
							if (strokes[i][a].y > strokes[j][b].y) {
								above = true;
							} else if (strokes[i][a].y < strokes[j][b].y) {
								below = true;
							}
						}
					}
				}
				if (above && !below) {
					relations.add(new ImmutablePair<>(j, i));
				} else if (!above && below) {
					relations.add(new ImmutablePair<>(i, j));
				}
			}
		}
		LOG.info("relations after area check: "+relations);
		//make the relation transient
		boolean added = true;
		trans:
		while (added) {
			added = false;
			for (Pair<Integer,Integer> p1 : relations) {
				for (Pair<Integer, Integer> p2 : relations) {
					if (p1.getRight().equals(p2.getLeft())) {
						int i1 = p1.getLeft();
						int i2 = p2.getRight();
						if (i1==i2 || relations.contains(new ImmutablePair<>(i2, i1))) {
							LOG.warning("cycle created with ("+i1+","+i2+") in "+relations);
						}
						if (!relations.contains(new ImmutablePair<>(i1, i2))) {
							relations.add(new ImmutablePair<>(i1, i2));
							added = true;
							continue trans;
						}
					}
				}
			}
		}
		//end of sweeping, now sort
		LOG.info("final relations: "+relations);
		strokeOrder = new Integer[n];
		for (int i=0; i<n; ++i) {strokeOrder[i]=i;}
		Arrays.sort(strokeOrder, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				if (relations.contains(new ImmutablePair<>(o1, o2))) {
					return 1;
				} else if (relations.contains(new ImmutablePair<>(o2, o1))) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		LOG.info("order: "+Arrays.toString(strokeOrder));
		drawStrokesInOrder();
	}
	private int distance(Vector2f p, Vector2f[] curve) {
		float dist = Float.MAX_VALUE;
		int index = 0;
		for (int i=0; i<curve.length; ++i) {
			Vector2f c = curve[i];
			float d = p.distanceSquared(c);
			if (d<dist) {
				dist = d;
				index = i;
			}
		}
		return index;
	}
	
	private void drawStrokesInOrder() {
		image.wipe(ColorRGBA.BlackNoAlpha);
		for (int i=0; i<strokeOrder.length; ++i) {
			List<Vector2f> points = sketches.get(strokeOrder[i]);
			for (int j=1; j<points.size(); ++j) {
				Vector2f p1 = points.get(j-1);
				Vector2f p2 = points.get(j);
				drawLineBresenham((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, STROKE_WIDTH, STROKE_COLORS[i], image);
			}
		}
		quadMaterial.setTexture("ColorMap", texture);
	}

	/**
	 * Finds the features (silhouettes and ringelines) of the terrain.
	 * Input: {@link #map}
	 * Output: {@link #features}
	 */
	private void findFeatures() {
		featureNode.detachAllChildren();
		Camera cam = app.getCamera();
		
		//1. find silhouettes: edges were the front face is visible, but not the back face
		//collect distances and visibility
		LOG.info("compute visibility");
		boolean[][] visible = new boolean[map.getSize()][map.getSize()];
		float[][] distance = new float[map.getSize()][map.getSize()];
		Ray ray = new Ray();
		CollisionResults results = new CollisionResults();
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				Vector3f p3 = app.getHeightmapPoint(x, y);
				Vector3f p2 = cam.getScreenCoordinates(p3);
				distance[x][y] = p2.z;
				if (p2.x<0 || p2.x>=width || p2.y<0 || p2.y>=height) {
					visible[x][y] = false; //outside of the screen
				} else {
					//shoot a ray to detect visibility
					Vector3f dir = cam.getLocation().subtract(p3).normalizeLocal();
					ray.setDirection(dir);
					ray.setOrigin(p3.add(dir.mult(1f)));
					results.clear();
					app.getHeightmapSpatial().collideWith(ray, results);
					visible[x][y] = results.size()==0;
				}
			}
		}
		//Test
		LOG.info("show visibility");
		List<Vector3f> visiblePoints = new ArrayList<>();
		List<Vector3f> hiddenPoints = new ArrayList<>();
		Material vmat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		vmat.setColor("Color", ColorRGBA.Red);
		Material hmat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		hmat.setColor("Color", ColorRGBA.Gray);
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				Vector3f p = app.getHeightmapPoint(x, y);
				p.addLocal(0, 0.5f, 0);
				if (visible[x][y]) {
					visiblePoints.add(p);
				} else {
					hiddenPoints.add(p);
				}
			}
		}
		Mesh vm = new Mesh();
		vm.setMode(Mesh.Mode.Points);
		vm.setPointSize(10);
		vm.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(visiblePoints.toArray(new Vector3f[visiblePoints.size()])));
		vm.updateCounts();
		vm.updateBound();
		Geometry vg = new Geometry("visible", vm);
		vg.setMaterial(vmat);
		featureNode.attachChild(vg);
		Mesh hm = new Mesh();
		hm.setMode(Mesh.Mode.Points);
		hm.setPointSize(10);
		hm.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(hiddenPoints.toArray(new Vector3f[hiddenPoints.size()])));
		hm.updateCounts();
		hm.updateBound();
		Geometry hg = new Geometry("hidden", hm);
		hg.setMaterial(hmat);
		featureNode.attachChild(hg);
		
		//find silhouettes in x-direction
		LOG.info("find silhouettes");
		for (int x=1; x<map.getSize(); ++x) {
			for (int y=1; y<map.getSize()-1; ++y) {
				if (visible[x][y] && visible[x-1][y] && (!visible[x][y-1] || !visible[x][y+1])) {
					features.add(Arrays.asList(new Vector2f(x-1, y), new Vector2f(x, y)));
				}
			}
		}
		for (int y=1; y<map.getSize(); ++y) {
			for (int x=1; x<map.getSize()-1; ++x) {
				if (visible[x][y] && visible[x][y-1] && (!visible[x-1][y] || !visible[x+1][y])) {
					features.add(Arrays.asList(new Vector2f(x, y-1), new Vector2f(x, y)));
				}
			}
		}
		
		LOG.info("features: "+features);
		displayFeatures();
	}
	
	private void displayFeatures() {
		LOG.info("display features");
//		featureNode.detachAllChildren();
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.White);
		for (List<Vector2f> feature : features) {
			for (int i=1; i<feature.size(); ++i) {
				Vector2f a = feature.get(i-1);
				Vector2f b = feature.get(i);
				com.jme3.scene.shape.Line l = new com.jme3.scene.shape.Line(
						app.getHeightmapPoint((int) a.x, (int) a.y),
						app.getHeightmapPoint((int) b.x, (int) b.y));
				l.setLineWidth(5);
				Geometry g = new Geometry("feature", l);
				g.setMaterial(mat);
				featureNode.attachChild(g);
			}
		}
		
		//Test
		Vector3f p1 = app.getHeightmapPoint(50, 50);
		Vector3f p2 = app.getHeightmapPoint(200, 50);
		Sphere s1 = new Sphere(16, 16, 1);
		Sphere s2 = new Sphere(16, 16, 1);
		Geometry g1 = new Geometry("s1", s1);
		Geometry g2 = new Geometry("s2", s2);
		g1.setLocalTranslation(p1);
		g2.setLocalTranslation(p2);
		g1.setMaterial(mat);
		g2.setMaterial(mat);
		featureNode.attachChild(g1);
		featureNode.attachChild(g2);
	}
	
	/*
	 * Implementation of Bresenham's fast line drawing algorithm. Based on the
	 * wikipedia article:
	 * http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
	 */
	protected void drawLineBresenham(int x0, int y0, int x1, int y1, int thickness, ColorRGBA color, ImagePainter img) {
		final double theta = Math.atan2(y1 - y0, x1 - x0);
		thickness = (int) Math.round(thickness * Math.max(Math.abs(Math.cos(theta)), Math.abs(Math.sin(theta))));

		final int offset = thickness / 2;
		final int extra = thickness % 2;

		// implementation of Bresenham's algorithm from Wikipedia.
		int Dx = x1 - x0;
		int Dy = y1 - y0;
		final boolean steep = (Math.abs(Dy) >= Math.abs(Dx));
		if (steep) {
			int tmp;
			// SWAP(x0, y0);
			tmp = x0;
			x0 = y0;
			y0 = tmp;
			// SWAP(x1, y1);
			tmp = x1;
			x1 = y1;
			y1 = tmp;

			// recompute Dx, Dy after swap
			Dx = x1 - x0;
			Dy = y1 - y0;
		}
		int xstep = 1;
		if (Dx < 0) {
			xstep = -1;
			Dx = -Dx;
		}
		int ystep = 1;
		if (Dy < 0) {
			ystep = -1;
			Dy = -Dy;
		}
		final int TwoDy = 2 * Dy;
		final int TwoDyTwoDx = TwoDy - 2 * Dx; // 2*Dy - 2*Dx
		int E = TwoDy - Dx; // 2*Dy - Dx
		int y = y0;
		int xDraw, yDraw;
		for (int x = x0; x != x1; x += xstep) {
			if (steep) {
				xDraw = y;
				yDraw = x;
			} else {
				xDraw = x;
				yDraw = y;
			}
			// plot
			if (xDraw >= 0 && xDraw < width && yDraw >= 0 && yDraw < height) {
				if (thickness == 1) {
					img.paintPixel(xDraw, yDraw, color, ImagePainter.BlendMode.SET);
				} else if (thickness > 1) {
					for (int yy = yDraw - offset; yy < yDraw + offset + extra; yy++) {
						for (int xx = xDraw - offset; xx < xDraw + offset + extra; xx++) {
							if (xx >= 0 && yy >= 0 && xx < width && yy < height) {
								img.paintPixel(xx, yy, color, ImagePainter.BlendMode.SET);
							}
						}
					}
				}
			}

			// next
			if (E > 0) {
				E += TwoDyTwoDx; // E += 2*Dy - 2*Dx;
				y += ystep;
			} else {
				E += TwoDy; // E += 2*Dy;
			}
		}
	}
}
