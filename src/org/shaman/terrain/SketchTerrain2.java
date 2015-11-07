/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
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
		ControlPoint p1 = new ControlPoint(40, 40, 0, 0, 0, 0, 0, 0, 0, 0);
		ControlPoint p2 = new ControlPoint(80, 70, 0.2f, 10, 20*FastMath.DEG_TO_RAD, 30, 20*FastMath.DEG_TO_RAD, 30, 0, 0);
		ControlPoint p3 = new ControlPoint(120, 130, 0.3f, 10, 20*FastMath.DEG_TO_RAD, 40, 20*FastMath.DEG_TO_RAD, 40, 0, 0);
		ControlPoint p4 = new ControlPoint(150, 160, 0.15f, 10, 20*FastMath.DEG_TO_RAD, 30, 20*FastMath.DEG_TO_RAD, 30, 0, 0);
		ControlPoint p5 = new ControlPoint(160, 200, 0, 0, 0, 0, 0, 0, 0, 0);
		ControlCurve c = new ControlCurve(new ControlPoint[]{p1, p2, p3, p4, p5});
		addFeatureCurve(c);
		
		//init sketch plane
		initSketchPlane();
		
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
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		
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
			System.out.println("interpolate time "+time+": p1="+(i-1)+", p2="+i+", t="+t);
			
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
}
