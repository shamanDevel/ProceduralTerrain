/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import Jama.Matrix;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
import org.apache.commons.lang3.StringUtils;
import org.shaman.terrain.heightmap.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public class SketchTerrain2 implements ActionListener, AnalogListener {
	private static final Logger LOG = Logger.getLogger(SketchTerrain2.class.getName());
	private static final float PLANE_QUAD_SIZE = 200;
	private static final float INITIAL_PLANE_DISTANCE = 150f;
	private static final float PLANE_MOVE_SPEED = 0.02f;
	private static final float CURVE_SIZE = 0.5f;
	private static final int CURVE_RESOLUTION = 8;
	private static final int CURVE_SAMPLES = 128;
	private static final boolean DEBUG_DIFFUSION_SOLVER = false;
	private static final int DIFFUSION_SOLVER_ITERATIONS = 500;
	
	private final TerrainHeighmapCreator app;
	private final Heightmap map;
	
	private Node guiNode;
	private Node sceneNode;
	private float planeDistance = INITIAL_PLANE_DISTANCE;
	private Spatial sketchPlane;

	private final ArrayList<ControlCurve> featureCurves;
	private final ArrayList<Node> featureCurveNodes;
	
	public SketchTerrain2(TerrainHeighmapCreator app, Heightmap map) {
		this.app = app;
		this.map = map;
		this.featureCurves = new ArrayList<>();
		this.featureCurveNodes = new ArrayList<>();
		init();
	}
	
	private void init() {
		//init nodes
		guiNode = new Node("sketchGUI");
		app.getGuiNode().attachChild(guiNode);
		sceneNode = new Node("sketch3D");
		app.getRootNode().attachChild(sceneNode);
		
		//add test feature curve
		ControlPoint p1 = new ControlPoint(40, 40, 0.05f, 0, 0, 0, 0, 0, 0, 0);
		ControlPoint p2 = new ControlPoint(80, 70, 0.2f, 10, 20f*FastMath.DEG_TO_RAD, 20, 0*FastMath.DEG_TO_RAD, 0, 0, 0);
		ControlPoint p3 = new ControlPoint(120, 130, 0.3f, 10, 20f*FastMath.DEG_TO_RAD, 35, 0*FastMath.DEG_TO_RAD, 0, 0, 0);
		ControlPoint p4 = new ControlPoint(150, 160, 0.15f, 10, 20f*FastMath.DEG_TO_RAD, 20, 0*FastMath.DEG_TO_RAD, 0, 0, 0);
		ControlPoint p5 = new ControlPoint(160, 200, 0.05f, 0, 0, 0, 0, 0, 0, 0);
		ControlCurve c = new ControlCurve(new ControlPoint[]{p1, p2, p3, p4, p5});
		addFeatureCurve(c);
		
		//init sketch plane
		initSketchPlane();
		
		//init actions
		app.getInputManager().addMapping("SolveDiffusion", new KeyTrigger(KeyInput.KEY_RETURN));
		app.getInputManager().addListener(this, "SolveDiffusion");
		
		//init light for shadow
		DirectionalLight light = new DirectionalLight(new Vector3f(0, -1, 0));
		light.setColor(ColorRGBA.White);
		sceneNode.addLight(light);
		DirectionalLightShadowRenderer shadowRenderer = new DirectionalLightShadowRenderer(app.getAssetManager(), 512, 1);
		shadowRenderer.setLight(light);
		app.getHeightmapSpatial().setShadowMode(RenderQueue.ShadowMode.Receive);
		app.getViewPort().addProcessor(shadowRenderer);
	}
	private void initSketchPlane() {
		Quad quad = new Quad(PLANE_QUAD_SIZE*2, PLANE_QUAD_SIZE*2);
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(0, 0, 1, 0.5f));
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
		Node node = new Node("feature"+(featureCurveNodes.size()+1));
		featureCurves.add(curve);
		
		Material sphereMat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		sphereMat.setBoolean("UseMaterialColors", true);
		sphereMat.setColor("Diffuse", ColorRGBA.Gray);
		sphereMat.setColor("Ambient", ColorRGBA.White);
		for (int i=0; i<curve.points.length; ++i) {
			Sphere s = new Sphere(CURVE_RESOLUTION, CURVE_RESOLUTION, CURVE_SIZE*1.5f);
			Geometry g = new Geometry("ControlPoint", s);
			g.setMaterial(sphereMat);
			g.setLocalTranslation(app.mapHeightmapToWorld(curve.points[i].x, curve.points[i].y, curve.points[i].height));
			node.attachChild(g);
		}
		
		Material tubeMat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		tubeMat.setBoolean("UseMaterialColors", true);
		tubeMat.setColor("Diffuse", ColorRGBA.Blue);
		tubeMat.setColor("Ambient", ColorRGBA.White);
		for (int i=1; i<=CURVE_SAMPLES; ++i) {
			float t1 = (i-1) / (float) CURVE_SAMPLES;
			float t2 = i / (float) CURVE_SAMPLES;
			ControlPoint p1 = curve.interpolate(t1);
			ControlPoint p2 = curve.interpolate(t2);
			Vector3f P1 = app.mapHeightmapToWorld(p1.x, p1.y, p1.height);
			Vector3f P2 = app.mapHeightmapToWorld(p2.x, p2.y, p2.height);
			Cylinder c = new Cylinder(CURVE_RESOLUTION, CURVE_RESOLUTION, CURVE_SIZE, P1.distance(P2), false);
			Geometry g = new Geometry("Curve", c);
			g.setMaterial(tubeMat);
			Quaternion q = g.getLocalRotation();
			q.lookAt(P2.subtract(P1), Vector3f.UNIT_Y);
			g.setLocalRotation(q);
			g.setLocalTranslation(P1.interpolateLocal(P2, 0.5f));
			node.attachChild(g);
		}
		
		node.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
		sceneNode.attachChild(node);
	}

	public void onUpdate(float tpf) {
		
	}
	
	private void solveDiffusion() {
		LOG.info("Solve diffusion");
		//create solver
		DiffusionSolver solver = new DiffusionSolver(map.getSize(), featureCurves.toArray(new ControlCurve[featureCurves.size()]));
		Matrix mat = new Matrix(map.getSize(), map.getSize());
		if (DEBUG_DIFFUSION_SOLVER) {
			solver.saveMatrix(mat, "diffusion/Iter0.png");
		}
		//run solver
		for (int i=1; i<=DIFFUSION_SOLVER_ITERATIONS; ++i) {
			System.out.println("iteration "+i);
			mat = solver.oneIteration(mat, i);
			if (DEBUG_DIFFUSION_SOLVER) {
			//if (i%10 == 0) {
				solver.saveFloatMatrix(mat, "diffusion/Iter"+i+".png");
			}
		}
		LOG.info("solved");
		//fill heighmap
		for (int x=0; x<map.getSize(); ++x) {
			for (int y=0; y<map.getSize(); ++y) {
				map.setHeightAt(x, y, (float) mat.get(x, y));
			}
		}
		app.updateAlphaMap();
		app.updateTerrain();
		LOG.info("terrain updated");
	}
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if ("SolveDiffusion".equals(name) && isPressed) {
			solveDiffusion();
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
	
	private static class ControlPoint {
		/**
		 * Coordinates on the heightmap
		 */
		private float x, y;
		/**
		 * the target height at that control point.
		 * h_i in the paper.
		 */
		private float height;
		/**
		 * The radius of the plateau on both sides of the feature curve at this point.
		 * r_i in the paper.
		 */
		private float plateau;
		/**
		 * The angle and extend of the slope on the left side.
		 * a_i, theta_i in the paper.
		 */
		private float angle1, extend1;
		/**
		 * The angle and extend of the slope on the right side.
		 * b_i, phi_i in the paper.
		 */
		private float angle2, extend2;
		/**
		 * Noise parameters: amplitude and roughness.
		 */
		private float noiseAmplitude, noiseRoughness;

		private ControlPoint() {
		}

		private ControlPoint(float x, float y, float height, float plateau, float angle1, float extend1, 
				float angle2, float extend2, float noiseAmplitude, float noiseRoughness) {
			this.x = x;
			this.y = y;
			this.height = height;
			this.plateau = plateau;
			this.angle1 = angle1;
			this.extend1 = extend1;
			this.angle2 = angle2;
			this.extend2 = extend2;
			this.noiseAmplitude = noiseAmplitude;
			this.noiseRoughness = noiseRoughness;
		}

		@Override
		public String toString() {
			return "ControlPoint{" + "x=" + x + ", y=" + y + ", height=" + height + ", plateau=" + plateau + ", angle1=" + angle1 + ", extend1=" + extend1 + ", angle2=" + angle2 + ", extend2=" + extend2 + ", noiseAmplitude=" + noiseAmplitude + ", noiseRoughness=" + noiseRoughness + '}';
		}
		
	}
	
	private static class ControlCurve {
		private ControlPoint[] points;
		private float[] times;

		public ControlCurve(ControlPoint[] points) {
			if (points.length<2) {
				throw new IllegalArgumentException("At least two control points have to be specified");
			}
			this.points = points;
			//approximate geodesic sampling by the euclidian distance
			float[] distances = new float[points.length];
			distances[0] = 0;
			for (int i=1; i<points.length; ++i) {
				distances[i] = new Vector3f(points[i].x - points[i-1].x, points[i].y - points[i-1].y, points[i].height - points[i-1].height).length();
				distances[i] += distances[i-1];
			}
			times = new float[points.length];
			for (int i=0; i<points.length; ++i) {
				times[i] = distances[i] / distances[points.length-1];
			}
			
			LOG.info("Control points:\n"+StringUtils.join(points, '\n'));
			LOG.info("Distances: "+Arrays.toString(distances));
			LOG.info("Times: "+Arrays.toString(times));
		}
		
		private ControlPoint interpolate(float time) {
			int i;
			for (i=1; i<points.length; ++i) {
				if (times[i-1]<=time && times[i]>=time) {
					break;
				}
			}
			if (i==points.length) {
				return null; //outside of the bounds
			}
			ControlPoint p1 = points[i-1];
			ControlPoint p2 = points[i];
			float t = (time-times[i-1]) / (times[i]-times[i-1]);
			
			//interpolate
			ControlPoint p = new ControlPoint();
			//position
			if (points.length==2) {
				//linear interpolation of the position
				p.x = (1-t)*p1.x + t*p2.x;
				p.y = (1-t)*p1.y + t*p2.y;
				p.height = (1-t)*p1.height + t*p2.height;
			} else if (i==1) {
				//quadratic hermite
				Vector3f P0 = new Vector3f(p1.x, p1.y, p1.height);
				Vector3f P1 = new Vector3f(p2.x, p2.y, p2.height);
				Vector3f T1 = new Vector3f(points[i+1].x-p1.x, points[i+1].y-p1.y, points[i+1].height-p1.height);
				T1.multLocal(0.5f);
				Vector3f P = quadraticHermite(P0, T1, P1, t);
				p.x = P.x; p.y = P.y; p.height = P.z;
			} else if (i==points.length-1) {
				//quadratic hermite
				Vector3f P0 = new Vector3f(p1.x, p1.y, p1.height);
				Vector3f P1 = new Vector3f(p2.x, p2.y, p2.height);
				Vector3f T0 = new Vector3f(p2.x-points[i-2].x, p2.y-points[i-2].y, p2.height-points[i-2].height);
				T0.multLocal(-0.5f);
				Vector3f P = quadraticHermite(P1, T0, P0, 1-t);
				p.x = P.x; p.y = P.y; p.height = P.z;
			} else {
				Vector3f P0 = new Vector3f(p1.x, p1.y, p1.height);
				Vector3f P1 = new Vector3f(p2.x, p2.y, p2.height);
				Vector3f T0 = new Vector3f(p2.x-points[i-2].x, p2.y-points[i-2].y, p2.height-points[i-2].height);
				Vector3f T1 = new Vector3f(points[i+1].x-p1.x, points[i+1].y-p1.y, points[i+1].height-p1.height);
				T0.multLocal(0.5f);
				T1.multLocal(0.5f);
				Vector3f P = cubicHermite(P0, T0, P1, T1, t);
				p.x = P.x; p.y = P.y; p.height = P.z;
			}
			//all other properties are linearly interpolated
			p.plateau = (1-t)*p1.plateau + t*p2.plateau;
			p.angle1 = (1-t)*p1.angle1 + t*p2.angle1;
			p.extend1 = (1-t)*p1.extend1 + t*p2.extend1;
			p.angle2 = (1-t)*p1.angle2 + t*p2.angle2;
			p.extend2 = (1-t)*p1.extend2 + t*p2.extend2;
			p.noiseAmplitude = (1-t)*p1.noiseAmplitude + t*p2.noiseAmplitude;
			p.noiseRoughness = (1-t)*p1.noiseRoughness + t*p2.noiseRoughness;
			
			return p;
		}
		
		/**
		 * Hermite interpolation of the points P0 to P1 at time t=0 to t=1 with
		 * the specified velocities T0 and T1.
		 *
		 * @param P0
		 * @param T0
		 * @param P1
		 * @param T1
		 * @param t
		 * @return
		 */
		private static Vector3f cubicHermite(Vector3f P0, Vector3f T0, Vector3f P1, Vector3f T1, float t) {
			float t2 = t * t;
			float t3 = t2 * t;
			Vector3f P = new Vector3f();
			P.scaleAdd(2 * t3 - 3 * t2 + 1, P0, P);
			P.scaleAdd(t3 - 2 * t2 + t, T0, P);
			P.scaleAdd(-2 * t3 + 3 * t2, P1, P);
			P.scaleAdd(t3 - t2, T1, P);
			return P;
		}

		/**
		 * A variation of Hermite where a velocity is given only for the first
		 * point. It interpolates P0 at t=0 and P1 at t=1. P(t) = (t^2 - 2*t +
		 * 1) * P0 + (-t^2 + 2*t) * P1 + (t^2 - t) * T0
		 *
		 * @param P0
		 * @param T0
		 * @param P1
		 * @param t
		 * @return
		 */
		private static Vector3f quadraticHermite(Vector3f P0, Vector3f T0, Vector3f P1, float t) {
			float t2 = t * t;
			Vector3f P = new Vector3f();
			P.scaleAdd(t2 - 2 * t + 1, P0, P);
			P.scaleAdd(-t2 + 2 * t, P1, P);
			P.scaleAdd(t2 - t, T0, P);
			return P;
		}
	}
	
	private class DiffusionSolver {
		//settings
		private final double BETA_SCALE = 0.9;
		private final double ALPHA_SCALE = 0.5;
		private final double GRADIENT_SCALE = 0.01;
		private final float SLOPE_ALPHA_FACTOR = 0f;
		
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
			
			for (ControlCurve curve : curves) {
				//sample curve
				int samples = 32;
				ControlPoint[] points = new ControlPoint[samples+1];
				for (int i=0; i<=samples; ++i) {
					points[i] = curve.interpolate(i / (float) samples);
				}
				
				//render meshes
				Material vertexColorMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				vertexColorMat.setBoolean("VertexColor", true);
				vertexColorMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
				
				Geometry lineGeom = new Geometry("line", createLineMesh(points));
				lineGeom.setMaterial(vertexColorMat);
				lineGeom.setQueueBucket(RenderQueue.Bucket.Gui);
				Geometry plateauGeom = new Geometry("plateau", createPlateauMesh(points));
				plateauGeom.setMaterial(vertexColorMat);
				plateauGeom.setQueueBucket(RenderQueue.Bucket.Gui);
				Node node = new Node();
				node.attachChild(lineGeom);
				node.attachChild(plateauGeom);
				node.updateModelBound();
				node.setCullHint(Spatial.CullHint.Never);
				fillMatrix(elevation, node);
				
				vertexColorMat.setBoolean("VertexColor", false);
				vertexColorMat.setColor("Color", ColorRGBA.White);
				fillMatrix(beta, node);
				
				vertexColorMat.setBoolean("VertexColor", true);
				vertexColorMat.clearParam("Color");
				Geometry slopeGeom = new Geometry("slope", createSlopeMesh(points));
				slopeGeom.setMaterial(vertexColorMat);
				slopeGeom.setQueueBucket(RenderQueue.Bucket.Gui);
				fillSlopeMatrix(slopeGeom);
				slopeGeom.setMesh(createSlopeAlphaMesh(points));
				fillMatrix(alpha, slopeGeom);
				
				alpha.timesEquals(ALPHA_SCALE);
				beta.timesEquals(BETA_SCALE);
				gradH.timesEquals(GRADIENT_SCALE);
				
				//save for debugging
				if (DEBUG_DIFFUSION_SOLVER) {
					saveMatrix(elevation, "diffusion/Elevation.png");
					saveMatrix(beta, "diffusion/Beta.png");
					saveMatrix(alpha, "diffusion/Alpha.png");
					saveFloatMatrix(gradX, "diffusion/GradX.png");
					saveFloatMatrix(gradY, "diffusion/GradY.png");
					saveFloatMatrix(gradH, "diffusion/GradH.png");
				}
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
						v += gx*gx*last.get(clamp(x-(int) Math.signum(gx)), y);
						v += gy*gy*last.get(x, clamp(y-(int) Math.signum(gy)));
					}
					gradient.set(x, y, v);
				}
			}
			Matrix oldGradient;
			if (DEBUG_DIFFUSION_SOLVER) {
				oldGradient = gradient.copy(); //Test
			}
			if (DEBUG_DIFFUSION_SOLVER) {
				saveFloatMatrix(gradient, "diffusion/Gradient"+iteration+".png");
			}
			Matrix gradChange = gradH.plus(gradient);
			gradChange.arrayTimesEquals(alpha);
			if (DEBUG_DIFFUSION_SOLVER) {
				saveFloatMatrix(gradChange, "diffusion/GradChange"+iteration+".png");
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
				saveFloatMatrix(diff, "diffusion/Diff"+iteration+".png");
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
				col[i] = new ColorRGBA(points[i].height, points[i].height, points[i].height, 1);
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
				pos[3*i] = new Vector3f(points[i].x, points[i].y, 1-points[i].height);
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
				pos[3*i + 1] = pos[3*i].add(points[i].plateau * -dy, points[i].plateau * dx, 0);
				pos[3*i + 2] = pos[3*i].add(points[i].plateau * dy, points[i].plateau * -dx, 0);
				col[3*i] = new ColorRGBA(points[i].height, points[i].height, points[i].height, 1);
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
				pos[4*i + 1] = p.add((points[i].plateau + points[i].extend1) * -dy, (points[i].plateau + points[i].extend1) * dx, 0);
				pos[4*i + 2] = p.add(points[i].plateau * dy, points[i].plateau * -dx, 0);
				pos[4*i + 3] = p.add((points[i].plateau + points[i].extend2) * dy, (points[i].plateau + points[i].extend2) * -dx, 0);
				ColorRGBA c1, c2, c3, c4;
				c1 = new ColorRGBA(-dy/2 + 0.5f, dx/2 + 0.5f, -FastMath.sin(points[i].angle1) + 0.5f, 1);
				c2 = c1;
				c3 = new ColorRGBA(dy/2 + 0.5f, -dx/2 + 0.5f, -FastMath.sin(points[i].angle2) + 0.5f, 1);
				c4 = c3;
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
				pos[6*i + 1] = p.add((points[i].plateau + points[i].extend1*factor) * -dy, (points[i].plateau + points[i].extend1*factor) * dx, 0);
				pos[6*i + 2] = p.add((points[i].plateau + points[i].extend1) * -dy, (points[i].plateau + points[i].extend1) * dx, 0);
				pos[6*i + 3] = p.add(points[i].plateau * dy, points[i].plateau * -dx, 0);
				pos[6*i + 4] = p.add((points[i].plateau + points[i].extend2*factor) * dy, (points[i].plateau + points[i].extend2*factor) * -dx, 0);
				pos[6*i + 5] = p.add((points[i].plateau + points[i].extend2) * dy, (points[i].plateau + points[i].extend2) * -dx, 0);
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
		private void fillMatrix(Matrix matrix, Spatial scene) {
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
					float v = data.getFloat();
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
			view.setBackgroundColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
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
					gradX.set(x, y, gx / s);
					gradY.set(x, y, gy / s);
//					double v = (((data.get() & 0xff) / 255.0) - 0.5);
					double v = (data.getFloat() - 0.5);
					if (Math.abs(v)<0.002) {
						v=0;
					}
					gradH.set(x, y, v);
//					data.get();
					data.getFloat();
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
				Logger.getLogger(SketchTerrain2.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		private void saveFloatMatrix(Matrix matrix, String filename) {
			byte[] buffer = new byte[size*size];
			int i=0;
			for (int x=0; x<size; ++x) {
				for (int y=0; y<size; ++y) {
					buffer[i] = (byte) ((matrix.get(x, y)/2 + 0.5) * 255);
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
				Logger.getLogger(SketchTerrain2.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
