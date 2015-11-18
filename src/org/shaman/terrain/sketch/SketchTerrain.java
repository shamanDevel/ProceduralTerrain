/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

import Jama.Matrix;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public class SketchTerrain extends AbstractTerrainStep implements ActionListener, AnalogListener {
	private static final Logger LOG = Logger.getLogger(SketchTerrain.class.getName());
	private static final float PLANE_QUAD_SIZE = 200 * TerrainHeighmapCreator.TERRAIN_SCALE;
	private static final float INITIAL_PLANE_DISTANCE = 150f * TerrainHeighmapCreator.TERRAIN_SCALE;
	private static final float PLANE_MOVE_SPEED = 0.002f * TerrainHeighmapCreator.TERRAIN_SCALE;
	private static final float CURVE_SIZE = 0.5f * TerrainHeighmapCreator.TERRAIN_SCALE;
	private static final int CURVE_RESOLUTION = 8;
	private static final int CURVE_SAMPLES = 128;
	private static final boolean DEBUG_DIFFUSION_SOLVER = false;
	private static final int DIFFUSION_SOLVER_ITERATIONS = 100;
	
	private Heightmap map;
	private Heightmap originalMap;
	private Spatial waterPlane;
	
	private float planeDistance = INITIAL_PLANE_DISTANCE;
	private Spatial sketchPlane;
	private SketchTerrainScreenController screenController;

	private Node curveNode;
	private final ArrayList<ControlCurve> featureCurves;
	private final ArrayList<Node> featureCurveNodes;
	private final ArrayList<ControlCurveMesh> featureCurveMesh;
	private boolean addNewCurves = true;
	
	private int selectedCurveIndex;
	private int selectedPointIndex;
	
	private ControlCurve newCurve;
	private Node newCurveNode;
	private ControlCurveMesh newCurveMesh;
	private long lastTime;
	
	private final CurvePreset[] presets;
	private int selectedPreset;
	
	private DiffusionSolver solver;
	private Matrix solverMatrix;
	private int step;
	private long lastUpdateTime;
	
	public SketchTerrain() {
		this.featureCurves = new ArrayList<>();
		this.featureCurveNodes = new ArrayList<>();
		this.featureCurveMesh = new ArrayList<>();
		this.presets = DefaultCurvePresets.DEFAULT_PRESETS;
	}

	@Override
	protected void enable() {
		this.map = (Heightmap) properties.get(AbstractTerrainStep.KEY_HEIGHTMAP);
		if (this.map == null) {
			throw new IllegalStateException("SketchTerrain requires a heightmap");
		}
		this.originalMap = this.map.clone();
		selectedPreset = 0;
		init();
	}

	@Override
	protected void disable() {
		app.getInputManager().removeListener(this);
	}
	
	private void init() {
		//init nodes
		guiNode.detachAllChildren();
		sceneNode.detachAllChildren();
		curveNode = new Node("curves");
		sceneNode.attachChild(curveNode);
		
		//initial terrain 
		app.setTerrain(originalMap);
		
		//init water plane
		initWaterPlane();
		
		//init sketch plane
		initSketchPlane();
		
		//init actions
//		app.getInputManager().addMapping("SolveDiffusion", new KeyTrigger(KeyInput.KEY_RETURN));
		app.getInputManager().addMapping("MouseClicked", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		app.getInputManager().addListener(this, "SolveDiffusion", "MouseClicked");
		
		//init light for shadow
		DirectionalLight light = new DirectionalLight(new Vector3f(0, -1, 0));
		light.setColor(ColorRGBA.White);
		sceneNode.addLight(light);
		DirectionalLightShadowRenderer shadowRenderer = new DirectionalLightShadowRenderer(app.getAssetManager(), 512, 1);
		shadowRenderer.setLight(light);
		app.getHeightmapSpatial().setShadowMode(RenderQueue.ShadowMode.Receive);
		app.getViewPort().addProcessor(shadowRenderer);
		
		//TODO: add pseudo water at height 0
		
		initNifty();
	}
	private void initNifty() {
		Nifty nifty = app.getNifty();
		screenController = new SketchTerrainScreenController(this);
		nifty.registerScreenController(screenController);
		nifty.addXml("org/shaman/terrain/sketch/SketchTerrainScreen.xml");
		nifty.gotoScreen("SketchTerrain");
		sendAvailablePresets();
//		nifty.setDebugOptionPanelColors(true);
	}
	private void initWaterPlane() {
		float size = map.getSize() * TerrainHeighmapCreator.TERRAIN_SCALE;
		Quad quad = new Quad(size, size);
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(0, 0, 0.5f, 0.5f));
		mat.setTransparent(true);
		mat.getAdditionalRenderState().setAlphaTest(true);
		mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		mat.getAdditionalRenderState().setDepthWrite(false);
		Geometry geom = new Geometry("water", quad);
		geom.setMaterial(mat);
		geom.setQueueBucket(RenderQueue.Bucket.Transparent);
		geom.rotate(FastMath.HALF_PI, 0, 0);
		geom.move(-size/2, 0, -size/2);
		waterPlane = geom;
		sceneNode.attachChild(geom);
	}
	private void initSketchPlane() {
		Quad quad = new Quad(PLANE_QUAD_SIZE*2, PLANE_QUAD_SIZE*2);
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(0.5f, 0, 0, 0.5f));
		mat.setTransparent(true);
		mat.getAdditionalRenderState().setAlphaTest(true);
		mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		Geometry geom = new Geometry("SketchPlaneQuad", quad);
		geom.setMaterial(mat);
		geom.setQueueBucket(RenderQueue.Bucket.Translucent);
		geom.setLocalTranslation(-PLANE_QUAD_SIZE, -PLANE_QUAD_SIZE, 0);
		sketchPlane = new Node("SketchPlane");
		((Node) sketchPlane).attachChild(geom);
		sceneNode.attachChild(sketchPlane);
		sketchPlane.addControl(new AbstractControl() {

			@Override
			protected void controlUpdate(float tpf) {
				//place the plane at the given distance from the camera
				Vector3f pos = app.getCamera().getLocation().clone();
				pos.addLocal(app.getCamera().getDirection().mult(planeDistance));
				sketchPlane.setLocalTranslation(pos);
				//face the camera
				Quaternion q = sketchPlane.getLocalRotation();
				q.lookAt(app.getCamera().getDirection().negate(), Vector3f.UNIT_Y);
				sketchPlane.setLocalRotation(q);
			}

			@Override
			protected void controlRender(RenderManager rm, ViewPort vp) {}
		});
		app.getInputManager().addMapping("SketchPlaneDist-", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
		app.getInputManager().addMapping("SketchPlaneDist+", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
		app.getInputManager().addListener(this, "SketchPlaneDist-", "SketchPlaneDist+");
	}
	
	private void addFeatureCurve(ControlCurve curve) {
		int index = featureCurveNodes.size();
		Node node = new Node("feature"+index);
		featureCurves.add(curve);
		
		addControlPointsToNode(curve.getPoints(), node, index);
		
		ControlCurveMesh mesh = new ControlCurveMesh(curve, "Curve"+index, app);
		node.attachChild(mesh.getTubeGeometry());
		node.attachChild(mesh.getSlopeGeometry());
		
		node.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
		mesh.getSlopeGeometry().setShadowMode(RenderQueue.ShadowMode.Off);
		featureCurveNodes.add(node);
		featureCurveMesh.add(mesh);
		curveNode.attachChild(node);
	}
	private void addControlPointsToNode(ControlPoint[] points, Node node, int index) {
		Material sphereMat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		sphereMat.setBoolean("UseMaterialColors", true);
		sphereMat.setColor("Diffuse", ColorRGBA.Gray);
		sphereMat.setColor("Ambient", ColorRGBA.White);
		for (int i=0; i<points.length; ++i) {
			Sphere s = new Sphere(CURVE_RESOLUTION, CURVE_RESOLUTION, CURVE_SIZE*3f);
			Geometry g = new Geometry(index<0 ? "dummy" : ("ControlPoint"+index+":"+i), s);
			g.setMaterial(sphereMat);
			g.setLocalTranslation(app.mapHeightmapToWorld(points[i].x, points[i].y, points[i].height));
			node.attachChild(g);
		}
	}
	
	private void startSolving() {
		LOG.info("Solve diffusion");
		//create solver
		solver = new DiffusionSolver(map.getSize(), featureCurves.toArray(new ControlCurve[featureCurves.size()]));
		solverMatrix = new Matrix(map.getSize(), map.getSize());
		//Save
		if (DEBUG_DIFFUSION_SOLVER) {
			solver.saveFloatMatrix(solverMatrix, "diffusion/Iter0.png",1);
		}
		step = 1;
		screenController.startSolving();
		lastUpdateTime = System.currentTimeMillis();
	}
	private void runSolving() {
		long maxTime = 50;
		int minIterations = 5;
		//run iterations
		long time = System.currentTimeMillis() + maxTime;
		for (int i=0; (System.currentTimeMillis()<time) || (i<minIterations); ++i) {
			solverMatrix = solver.oneIteration(solverMatrix, step);
			step++;
		}
		screenController.setSolvingIteration(step);
		//update terrain occasionally
		time = System.currentTimeMillis();
		if (time > lastUpdateTime + 500) {
			lastUpdateTime = time;
			for (int x=0; x<map.getSize(); ++x) {
				for (int y=0; y<map.getSize(); ++y) {
					map.setHeightAt(x, y, (float) solverMatrix.get(x, y) + originalMap.getHeightAt(x, y));
				}
			}
			app.setTerrain(map);
		}
	}
	private void solvingFinished() {
		LOG.info("solved");
		//fill heighmap
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				map.setHeightAt(x, y, (float) solverMatrix.get(x, y) + originalMap.getHeightAt(x, y));
			}
		}
		app.setTerrain(map);
		LOG.info("terrain updated");
		solver = null;
		screenController.stopSolving();
	}
	@Override
	public void update(float tpf) {
		if (solver != null) {
			runSolving();
		}
	}
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if ("SolveDiffusion".equals(name) && isPressed) {
			startSolving();
		} else if ("MouseClicked".equals(name) && isPressed) {
			Vector3f dir = app.getCamera().getWorldCoordinates(app.getInputManager().getCursorPosition(), 1);
			dir.subtractLocal(app.getCamera().getLocation());
			dir.normalizeLocal();
			Ray ray = new Ray(app.getCamera().getLocation(), dir);
			if (addNewCurves) {
				addNewPoint(ray);
			} else {
				pickCurve(ray);
			}
		}
	}

	@Override
	public void onAnalog(String name, float value, float tpf) {
		switch (name) {
			case "SketchPlaneDist-":
				planeDistance *= 1+PLANE_MOVE_SPEED;
				break;
			case "SketchPlaneDist+":
				planeDistance /= 1+PLANE_MOVE_SPEED;
				break;
		}
	}
	
	//Edit
	private void addNewPoint(Ray ray) {
		long time = System.currentTimeMillis();
		if (time < lastTime+500) {
			//finish current curve
			if (newCurve==null) {
				return;
			}
			newCurveMesh = null;
			curveNode.detachChild(newCurveNode);
			newCurveNode = null;
			if (newCurve.getPoints().length>=2) {
				addFeatureCurve(newCurve);
			}
			newCurve = null;
			LOG.info("new feature added");
			screenController.setMessage("");
			return;
		}
		lastTime = time;
		//create new point
		CollisionResults results = new CollisionResults();
		sketchPlane.collideWith(ray, results);
		if (results.size()==0) {
			return;
		}
		Vector3f point = results.getClosestCollision().getContactPoint();
		point = app.mapWorldToHeightmap(point);
		ControlPoint p = createNewControlPoint(point.x, point.y, point.z);
		//add to curve
		if (newCurve==null) {
			LOG.info("start a new feature");
			newCurve = new ControlCurve(new ControlPoint[]{p});
			newCurveNode = new Node();
			addControlPointsToNode(newCurve.getPoints(), newCurveNode, -1);
			newCurveMesh = new ControlCurveMesh(newCurve, "dummy", app);
			newCurveNode.attachChild(newCurveMesh.getSlopeGeometry());
			newCurveNode.attachChild(newCurveMesh.getTubeGeometry());
			curveNode.attachChild(newCurveNode);
			screenController.setMessage("ADDING A NEW FEATURE");
		} else {
			newCurve.setPoints(ArrayUtils.add(newCurve.getPoints(), p));
			newCurveNode.detachAllChildren();
			addControlPointsToNode(newCurve.getPoints(), newCurveNode, -1);
			newCurveMesh.updateMesh();
			newCurveNode.attachChild(newCurveMesh.getSlopeGeometry());
			newCurveNode.attachChild(newCurveMesh.getTubeGeometry());
		}
		newCurveNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
		newCurveMesh.getSlopeGeometry().setShadowMode(RenderQueue.ShadowMode.Off);
	}
	
	private ControlPoint createNewControlPoint(float x, float y, float h) {
		ControlPoint[] points = newCurve==null ? new ControlPoint[0] : newCurve.getPoints();
		return presets[selectedPreset].createControlPoint(x, y, h, points, map);
	}
	
	private void pickCurve(Ray ray) {
		CollisionResults results = new CollisionResults();
		curveNode.collideWith(ray, results);
		if (results.size()==0) {
			selectedCurveIndex = -1;
			selectedPointIndex = -1;
			selectCurve(-1, null);
		} else {
			CollisionResult result = results.getClosestCollision();
			Geometry geom = result.getGeometry();
			if (geom.getName().startsWith("Curve")) {
				int index = Integer.parseInt(geom.getName().substring("Curve".length()));
				selectedCurveIndex = index;
				selectedPointIndex = -1;
				selectCurve(index, null);
			} else if (geom.getName().startsWith("ControlPoint")) {
				String n = geom.getName().substring("ControlPoint".length());
				String[] parts = n.split(":");
				selectedCurveIndex = Integer.parseInt(parts[0]);
				selectedPointIndex = Integer.parseInt(parts[1]);
				selectCurve(selectedCurveIndex, featureCurves.get(selectedCurveIndex).getPoints()[selectedPointIndex]);
			} else {
				selectedCurveIndex = -1;
				selectedPointIndex = -1;
				selectCurve(-1, null);
			}
		}
	}
	
	//GUI-Interface
	public void guiAddCurves() {
		addNewCurves = true;
	}
	public void guiEditCurves() {
		addNewCurves = false;
		if (newCurve != null) {
			newCurveMesh = null;
			curveNode.detachChild(newCurveNode);
			newCurveNode = null;
			newCurve = null;
			screenController.setMessage("");
		}
	}
	public void guiShowCurves(boolean show) {
		curveNode.setCullHint(show ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
	}
	private void selectCurve(int curveIndex, ControlPoint point) {
		screenController.selectCurve(curveIndex, point);
	}
	public void guiDeleteCurve() {
		if (selectedCurveIndex==-1) {
			return;
		}
		Node n = featureCurveNodes.remove(selectedCurveIndex);
		featureCurves.remove(selectedCurveIndex);
		curveNode.detachChild(n);
		selectCurve(-1, null);
	}
	public void guiDeleteControlPoint() {
		if (selectedCurveIndex==-1 || selectedPointIndex==-1) {
			return;
		}
		ControlCurve c = featureCurves.get(selectedCurveIndex);
		if (c.getPoints().length<=2) {
			LOG.warning("Cannot delete control point, at least 2 points are required");
			return;
		}
		featureCurves.remove(selectedCurveIndex);
		Node n = featureCurveNodes.remove(selectedCurveIndex);
		curveNode.detachChild(n);
		c = new ControlCurve(ArrayUtils.remove(c.getPoints(), selectedPointIndex));
		addFeatureCurve(c);
	}
	public void guiControlPointChanged() {
		if (selectedCurveIndex==-1 || selectedPointIndex==-1) {
			return;
		}
		System.out.println("control point changed: "+featureCurves.get(selectedCurveIndex).getPoints()[selectedPointIndex]);
		ControlCurveMesh mesh = featureCurveMesh.get(selectedCurveIndex);
		mesh.updateMesh();
	}
	private void sendAvailablePresets() {
		String[] names = new String[presets.length];
		for (int i=0; i<presets.length; ++i) {
			names[i] = presets[i].getName();
		}
		screenController.setAvailablePresets(names);
	}
	public void guiPresetChanged(int index) {
		selectedPreset = index;
	}
	public void guiSolve() {
		startSolving();
	}
	public void guiStopSolve() {
		solvingFinished();
	}
	
	private class DiffusionSolver {
		//settings
		private final double BETA_SCALE = 0.9;
		private final double ALPHA_SCALE = 0.5;
		private final double GRADIENT_SCALE = 0.01;
		private final float SLOPE_ALPHA_FACTOR = 0f;
		private final boolean EVALUATE_SLOPE_RELATIVE_TO_ORIGINAL = true;
		
		//input
		private final int size;
		private final ControlCurve[] curves;
		
		//matrices
		private Matrix elevation; //target elevation
		private Matrix alpha, beta; //factors specifying the influence of the gradient, smootheness and elevation
		private Matrix gradX, gradY; //the normalized direction of the gradient at this point
		private Matrix gradH; //the target gradient / height difference from the reference point specified by gradX, gradY

		private DiffusionSolver(int size, ControlCurve[] curves) {
			this.size = size;
			this.curves = curves;
			rasterize();
		}
		
		/**
		 * Rasterizes the control curves in the matrices
		 */
		private void rasterize() {
			elevation = new Matrix(size, size);
			alpha = new Matrix(size, size);
			beta = new Matrix(size, size);
			gradX = new Matrix(size, size);
			gradY = new Matrix(size, size);
			gradH = new Matrix(size, size);
			
			//render meshes
			Material vertexColorMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			vertexColorMat.setBoolean("VertexColor", true);
			vertexColorMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
			vertexColorMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
			vertexColorMat.getAdditionalRenderState().setDepthTest(true);
			vertexColorMat.getAdditionalRenderState().setDepthWrite(true);
			Node elevationNode = new Node();
			Node gradientNode = new Node();
			
			for (ControlCurve curve : curves) {
				//sample curve
				int samples = 32;
				ControlPoint[] points = new ControlPoint[samples+1];
				for (int i=0; i<=samples; ++i) {
					points[i] = curve.interpolate(i / (float) samples);
				}
				
				Geometry lineGeom = new Geometry("line", createLineMesh(points));
				lineGeom.setMaterial(vertexColorMat);
				lineGeom.setQueueBucket(RenderQueue.Bucket.Gui);
				Geometry plateauGeom = new Geometry("plateau", createPlateauMesh(points));
				plateauGeom.setMaterial(vertexColorMat);
				plateauGeom.setQueueBucket(RenderQueue.Bucket.Gui);
				elevationNode.attachChild(lineGeom);
				elevationNode.attachChild(plateauGeom);
				
				Geometry slopeGeom = new Geometry("slope", createSlopeMesh(points));
				slopeGeom.setMaterial(vertexColorMat);
				slopeGeom.setQueueBucket(RenderQueue.Bucket.Gui);
				gradientNode.attachChild(slopeGeom);
			}
			
			elevationNode.setCullHint(Spatial.CullHint.Never);
			gradientNode.setCullHint(Spatial.CullHint.Never);
			
			for (Spatial s : elevationNode.getChildren()) {
				fillMatrix(elevation, s, true);
			}
			
			fillSlopeMatrix(gradientNode);
			
			vertexColorMat.setBoolean("VertexColor", false);
			vertexColorMat.setColor("Color", ColorRGBA.White);
			fillMatrix(beta, elevationNode, false);
			
			//copy existing heightmap data
			for (int x=0; x<map.getSize(); ++x) {
				for (int y=0; y<map.getSize(); ++y) {
					elevation.set(x, y, elevation.get(x, y) - originalMap.getHeightAt(x, y) - 0.5f);
				}
			}
			
			alpha.timesEquals(ALPHA_SCALE);
			beta.timesEquals(BETA_SCALE);
			gradH.timesEquals(GRADIENT_SCALE);

			//save for debugging
			if (DEBUG_DIFFUSION_SOLVER) {
				saveMatrix(elevation, "diffusion/Elevation.png");
				saveMatrix(beta, "diffusion/Beta.png");
				saveMatrix(alpha, "diffusion/Alpha.png");
				saveFloatMatrix(gradX, "diffusion/GradX.png",1);
				saveFloatMatrix(gradY, "diffusion/GradY.png",1);
				saveFloatMatrix(gradH, "diffusion/GradH.png",50);
			}
			
			LOG.info("curves rasterized");
		}
		
		public Matrix oneIteration(Matrix last, int iteration) {
			Matrix mat = new Matrix(size, size);
			//add elevation constraint
			mat.plusEquals(elevation.arrayTimes(beta));
			//add laplace constraint
			Matrix laplace = new Matrix(size, size);
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					double v = 0;
					v += last.get(Math.max(0, x-1), y);
					v += last.get(Math.min(size-1, x+1), y);
					v += last.get(x, Math.max(0, y-1));
					v += last.get(x, Math.min(size-1, y+1));
					v /= 4.0;
					v *= 1 - alpha.get(x, y) - beta.get(x, y);
					laplace.set(x, y, v);
				}
			}
			mat.plusEquals(laplace);
			//add gradient constraint
			Matrix gradient = new Matrix(size, size);
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					double v = 0;
					//v = gradH.get(x, y);
					double gx = gradX.get(x, y);
					double gy = gradY.get(x, y);
					if (gx==0 && gy==0) {
						v = 0; //no gradient
					} else {
						double h1, h2;
						if (EVALUATE_SLOPE_RELATIVE_TO_ORIGINAL) {
							h1 = last.get(clamp(x-(int) Math.signum(gx)), y)
									+ originalMap.getHeightAt(clamp(x-(int) Math.signum(gx)), y)
									- originalMap.getHeightAt(x, y);
							h2 = last.get(x, clamp(y-(int) Math.signum(gy)))
									+ originalMap.getHeightAt(x, clamp(y-(int) Math.signum(gy)))
									- originalMap.getHeightAt(x, y);
						} else {
							h1 = last.get(clamp(x-(int) Math.signum(gx)), y);
							h2 = last.get(x, clamp(y-(int) Math.signum(gy)));
						}
						v += gx*gx*h1;
						v += gy*gy*h2;
					}
					gradient.set(x, y, v);
				}
			}
			Matrix oldGradient;
			if (DEBUG_DIFFUSION_SOLVER) {
				oldGradient = gradient.copy(); //Test
			}
			if (DEBUG_DIFFUSION_SOLVER) {
				saveFloatMatrix(gradient, "diffusion/Gradient"+iteration+".png",1);
			}
			Matrix gradChange = gradH.plus(gradient);
			gradChange.arrayTimesEquals(alpha);
			if (DEBUG_DIFFUSION_SOLVER) {
				saveFloatMatrix(gradChange, "diffusion/GradChange"+iteration+".png",1);
			}
			mat.plusEquals(gradChange);
			//Test
			if (DEBUG_DIFFUSION_SOLVER) {
				Matrix newGradient = new Matrix(size, size);
				for (int x=0; x<size; ++x) {
					for (int y=0; y<size; ++y) {
						double v = 0;
						v += gradX.get(x, y)*gradX.get(x, y)
								*mat.get(clamp(x-(int) Math.signum(gradX.get(x, y))), y);
						v += gradY.get(x, y)*gradY.get(x, y)
								*mat.get(x, clamp(y-(int) Math.signum(gradY.get(x, y))));
						v -= mat.get(x, y);
						newGradient.set(x, y, -v);
					}
				}
				Matrix diff = oldGradient.minus(newGradient);
				saveFloatMatrix(diff, "diffusion/Diff"+iteration+".png",1);
			}
			
			return mat;
		}
		private int clamp(int i) {
			return Math.max(0, Math.min(size-1, i));
		}
		
		private Mesh createLineMesh(ControlPoint[] points) {
			Vector3f[] pos = new Vector3f[points.length];
			ColorRGBA[] col = new ColorRGBA[points.length];
			for (int i=0; i<points.length; ++i) {
				pos[i] = new Vector3f(points[i].x, points[i].y, 1-points[i].height);
				float height = points[i].hasElevation ? points[i].height : 0;
				col[i] = new ColorRGBA(height, height, height, 1);
			}
			Mesh mesh = new Mesh();
			mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(pos));
			mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(col));
			mesh.setMode(Mesh.Mode.LineStrip);
			mesh.setLineWidth(1);
			return mesh;
		}
		private Mesh createPlateauMesh(ControlPoint[] points) {
			Vector3f[] pos = new Vector3f[points.length*3];
			ColorRGBA[] col = new ColorRGBA[points.length*3];
			for (int i=0; i<points.length; ++i) {
				pos[3*i] = new Vector3f(points[i].x, points[i].y, points[i].height*100-100);
				float dx,dy;
				if (i==0) {
					dx = points[i+1].x - points[i].x;
					dy = points[i+1].y - points[i].y;
				} else if (i==points.length-1) {
					dx = points[i].x - points[i-1].x;
					dy = points[i].y - points[i-1].y;
				} else {
					dx = (points[i+1].x - points[i-1].x) / 2f;
					dy = (points[i+1].y - points[i-1].y) / 2f;
				}
				float sum = (float) Math.sqrt(dx*dx + dy*dy);
				dx /= sum;
				dy /= sum;
				pos[3*i + 1] = pos[3*i].add(points[i].plateau * -dy, points[i].plateau * dx, points[i].height*100-100);
				pos[3*i + 2] = pos[3*i].add(points[i].plateau * dy, points[i].plateau * -dx, points[i].height*100-100);
				float height = points[i].hasElevation ? points[i].height : 0;
				col[3*i] = new ColorRGBA(height, height, height, 1);
				col[3*i+1] = col[3*i];
				col[3*i+2] = col[3*i];
			}
			int[] index = new int[(points.length-1) * 12];
			for (int i=0; i<points.length-1; ++i) {
				index[12*i] = 3*i;
				index[12*i + 1] = 3*i + 3;
				index[12*i + 2] = 3*i + 1;
				index[12*i + 3] = 3*i + 3;
				index[12*i + 4] = 3*i + 4;
				index[12*i + 5] = 3*i + 1;
				
				index[12*i + 6] = 3*i;
				index[12*i + 7] = 3*i + 2;
				index[12*i + 8] = 3*i + 3;
				index[12*i + 9] = 3*i + 3;
				index[12*i + 10] = 3*i + 2;
				index[12*i + 11] = 3*i + 5;
			}
			Mesh m = new Mesh();
			m.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(pos));
			m.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(col));
			m.setBuffer(VertexBuffer.Type.Index, 1, index);
			m.setMode(Mesh.Mode.Triangles);
			return m;
		}		
		private Mesh createSlopeMesh(ControlPoint[] points) {
			Vector3f[] pos = new Vector3f[points.length*4];
			ColorRGBA[] col = new ColorRGBA[points.length*4];
			for (int i=0; i<points.length; ++i) {
				Vector3f p = new Vector3f(points[i].x, points[i].y, 1-points[i].height);
				float dx,dy;
				if (i==0) {
					dx = points[i+1].x - points[i].x;
					dy = points[i+1].y - points[i].y;
				} else if (i==points.length-1) {
					dx = points[i].x - points[i-1].x;
					dy = points[i].y - points[i-1].y;
				} else {
					dx = (points[i+1].x - points[i-1].x) / 2f;
					dy = (points[i+1].y - points[i-1].y) / 2f;
				}
				float sum = (float) Math.sqrt(dx*dx + dy*dy);
				dx /= sum;
				dy /= sum;
				pos[4*i + 0] = p.add(points[i].plateau * -dy, points[i].plateau * dx, 0);
				pos[4*i + 1] = p.add(
						(points[i].plateau + FastMath.cos(points[i].angle1)*points[i].extend1) * -dy, 
						(points[i].plateau + FastMath.cos(points[i].angle1)*points[i].extend1) * dx, 0);
				pos[4*i + 2] = p.add(points[i].plateau * dy, points[i].plateau * -dx, 0);
				pos[4*i + 3] = p.add(
						(points[i].plateau + FastMath.cos(points[i].angle2)*points[i].extend2) * dy, 
						(points[i].plateau + FastMath.cos(points[i].angle2)*points[i].extend2) * -dx, 0);
				ColorRGBA c1, c2, c3, c4;
				c1 = new ColorRGBA(-dy/2 + 0.5f, dx/2 + 0.5f, -FastMath.sin(points[i].angle1) + 0.5f, 1);
				c2 = new ColorRGBA(c1.r, c1.g, c1.b, 0);
				c3 = new ColorRGBA(dy/2 + 0.5f, -dx/2 + 0.5f, -FastMath.sin(points[i].angle2) + 0.5f, 1);
				c4 = new ColorRGBA(c3.r, c3.g, c3.b, 0);
				col[4*i + 0] = c1;
				col[4*i + 1] = c2;
				col[4*i + 2] = c3;
				col[4*i + 3] = c4;
			}
			int[] index = new int[(points.length-1) * 12];
			for (int i=0; i<points.length-1; ++i) {
				index[12*i] = 4*i;
				index[12*i + 1] = 4*i + 4;
				index[12*i + 2] = 4*i + 1;
				index[12*i + 3] = 4*i + 4;
				index[12*i + 4] = 4*i + 5;
				index[12*i + 5] = 4*i + 1;
				
				index[12*i + 6] = 4*i + 2;
				index[12*i + 7] = 4*i + 3;
				index[12*i + 8] = 4*i + 6;
				index[12*i + 9] = 4*i + 6;
				index[12*i + 10] = 4*i + 3;
				index[12*i + 11] = 4*i + 7;
			}
			Mesh m = new Mesh();
			m.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(pos));
			m.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(col));
			m.setBuffer(VertexBuffer.Type.Index, 1, index);
			m.setMode(Mesh.Mode.Triangles);
			return m;
		}
		@Deprecated
		private Mesh createSlopeAlphaMesh(ControlPoint[] points) {
			Vector3f[] pos = new Vector3f[points.length*6];
			ColorRGBA[] col = new ColorRGBA[points.length*6];
			for (int i=0; i<points.length; ++i) {
				Vector3f p = new Vector3f(points[i].x, points[i].y, 1-points[i].height);
				float dx,dy;
				if (i==0) {
					dx = points[i+1].x - points[i].x;
					dy = points[i+1].y - points[i].y;
				} else if (i==points.length-1) {
					dx = points[i].x - points[i-1].x;
					dy = points[i].y - points[i-1].y;
				} else {
					dx = (points[i+1].x - points[i-1].x) / 2f;
					dy = (points[i+1].y - points[i-1].y) / 2f;
				}
				float sum = (float) Math.sqrt(dx*dx + dy*dy);
				dx /= sum;
				dy /= sum;
				float factor = SLOPE_ALPHA_FACTOR;
				pos[6*i + 0] = p.add(points[i].plateau * -dy, points[i].plateau * dx, 0);
				pos[6*i + 1] = p.add(
						(points[i].plateau + FastMath.cos(points[i].angle1)*points[i].extend1*factor) * -dy, 
						(points[i].plateau + FastMath.cos(points[i].angle1)*points[i].extend1*factor) * dx, 0);
				pos[6*i + 2] = p.add(
						(points[i].plateau + FastMath.cos(points[i].angle1)*points[i].extend1) * -dy, 
						(points[i].plateau + FastMath.cos(points[i].angle1)*points[i].extend1) * dx, 0);
				pos[6*i + 3] = p.add(points[i].plateau * dy, points[i].plateau * -dx, 0);
				pos[6*i + 4] = p.add(
						(points[i].plateau + FastMath.cos(points[i].angle2)*points[i].extend2*factor) * dy, 
						(points[i].plateau + FastMath.cos(points[i].angle2)*points[i].extend2*factor) * -dx, 0);
				pos[6*i + 5] = p.add(
						(points[i].plateau + FastMath.cos(points[i].angle2)*points[i].extend2) * dy, 
						(points[i].plateau + FastMath.cos(points[i].angle2)*points[i].extend2) * -dx, 0);
				ColorRGBA c1 = new ColorRGBA(0, 0, 0, 1);
				ColorRGBA c2 = new ColorRGBA(1, 1, 1, 1);
				col[6*i + 0] = c1;
				col[6*i + 1] = c2;
				col[6*i + 2] = c1;
				col[6*i + 3] = c1;
				col[6*i + 4] = c2;
				col[6*i + 5] = c1;
			}
			int[] index = new int[(points.length-1) * 24];
			for (int i=0; i<points.length-1; ++i) {
				index[24*i + 0] = 6*i;
				index[24*i + 1] = 6*i + 6;
				index[24*i + 2] = 6*i + 1;
				index[24*i + 3] = 6*i + 6;
				index[24*i + 4] = 6*i + 7;
				index[24*i + 5] = 6*i + 1;
				
				index[24*i + 6] = 6*i + 1;
				index[24*i + 7] = 6*i + 7;
				index[24*i + 8] = 6*i + 2;
				index[24*i + 9] = 6*i + 7;
				index[24*i + 10] = 6*i + 8;
				index[24*i + 11] = 6*i + 2;
				
				index[24*i + 12] = 6*i + 3;
				index[24*i + 13] = 6*i + 9;
				index[24*i + 14] = 6*i + 4;
				index[24*i + 15] = 6*i + 9;
				index[24*i + 16] = 6*i + 10;
				index[24*i + 17] = 6*i + 4;
				
				index[24*i + 18] = 6*i + 4;
				index[24*i + 19] = 6*i + 10;
				index[24*i + 20] = 6*i + 5;
				index[24*i + 21] = 6*i + 10;
				index[24*i + 22] = 6*i + 11;
				index[24*i + 23] = 6*i + 5;
			}
			Mesh m = new Mesh();
			m.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(pos));
			m.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(col));
			m.setBuffer(VertexBuffer.Type.Index, 1, index);
			m.setMode(Mesh.Mode.Triangles);
			return m;
		}
		
		/**
		 * Renders the given scene in a top-down manner in the given matrix
		 * @param matrix
		 * @param scene 
		 */
		private void fillMatrix(Matrix matrix, Spatial scene, boolean max) {
			//init
			Camera cam = new Camera(size, size);
			cam.setParallelProjection(true);
			ViewPort view = new ViewPort("Off", cam);
			view.setClearFlags(true, true, true);
			FrameBuffer buffer = new FrameBuffer(size, size, 1);
			buffer.setDepthBuffer(Image.Format.Depth);
			buffer.setColorBuffer(Image.Format.RGBA32F);
			view.setOutputFrameBuffer(buffer);
			view.attachScene(scene);
			//render
			scene.updateGeometricState();
			view.setEnabled(true);
			app.getRenderManager().renderViewPort(view, 0);
			//retrive data
			ByteBuffer data = BufferUtils.createByteBuffer(size*size*4*4);
			app.getRenderer().readFrameBufferWithFormat(buffer, data, Image.Format.RGBA32F);
			data.rewind();
			for (int y=0; y<size; ++y) {
				for (int x=0; x<size; ++x) {
//					byte d = data.get();
//					matrix.set(x, y, (d & 0xff) / 255.0);
//					data.get(); data.get(); data.get();
					double v = data.getFloat();
					double old = matrix.get(x, y);
					if (max) {
						v = Math.max(v, old);
					} else {
						v += old;
					}
					matrix.set(x, y, v);
					data.getFloat(); data.getFloat(); data.getFloat();
				}
			}
		}
		
		/**
		 * Renders the given scene in a top-down manner in the given matrix
		 * @param matrix
		 * @param scene 
		 */
		private void fillSlopeMatrix(Spatial scene) {
			//init
			Camera cam = new Camera(size, size);
			cam.setParallelProjection(true);
			ViewPort view = new ViewPort("Off", cam);
			view.setClearFlags(true, true, true);
			view.setBackgroundColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 0f));
			FrameBuffer buffer = new FrameBuffer(size, size, 1);
			buffer.setDepthBuffer(Image.Format.Depth);
			buffer.setColorBuffer(Image.Format.RGBA32F);
			view.setOutputFrameBuffer(buffer);
			view.attachScene(scene);
			//render
			scene.updateGeometricState();
			view.setEnabled(true);
			app.getRenderManager().renderViewPort(view, 0);
			//retrive data
			ByteBuffer data = BufferUtils.createByteBuffer(size*size*4*4);
			app.getRenderer().readFrameBufferWithFormat(buffer, data, Image.Format.RGBA32F);
			data.rewind();
			for (int y=0; y<size; ++y) {
				for (int x=0; x<size; ++x) {
//					double gx = (((data.get() & 0xff) / 256.0) - 0.5) * 2;
//					double gy = (((data.get() & 0xff) / 256.0) - 0.5) * 2;
					double gx = (data.getFloat() - 0.5) * 2;
					double gy = (data.getFloat() - 0.5) * 2;
					double s = Math.sqrt(gx*gx + gy*gy);
					if (s==0) {
						gx=0; gy=0; s=1;
					}
					gradX.set(x, y, (gx / s) + gradX.get(x, y));
					gradY.set(x, y, (gy / s) + gradY.get(x, y));
//					double v = (((data.get() & 0xff) / 255.0) - 0.5);
					double v = (data.getFloat() - 0.5);
					if (Math.abs(v)<0.002) {
						v=0;
					}
					gradH.set(x, y, v + gradH.get(x, y));
//					data.get();
					double a = data.getFloat();
					alpha.set(x, y, a);
				}
			}
		}
		
		private void saveMatrix(Matrix matrix, String filename) {
			byte[] buffer = new byte[size*size];
			int i=0;
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					buffer[i] = (byte) (matrix.get(x, y) * 255);
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
		
		private void saveFloatMatrix(Matrix matrix, String filename, double scale) {
			byte[] buffer = new byte[size*size];
			int i=0;
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					buffer[i] = (byte) ((matrix.get(x, y)*scale/2 + 0.5) * 255);
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
}
