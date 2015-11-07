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
import java.util.logging.Logger;
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
	
	private final TerrainHeighmapCreator app;
	private final Heightmap map;
	
	private Node guiNode;
	private Node sceneNode;
	private float planeDistance = INITIAL_PLANE_DISTANCE;
	private Spatial sketchPlane;

	private final ArrayList<Vector3f[]> featureCurves;
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
		addFeatureCurve(new Vector3f[]{new Vector3f(-50, 0, -10), new Vector3f(-10,20,20), new Vector3f(20,40,-5), new Vector3f(50,0,10)});
		
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
	
	private void addFeatureCurve(Vector3f[] controlPoints) {
		Node node = new Node("feature"+(featureCurveNodes.size()+1));
		
		Material sphereMat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		sphereMat.setBoolean("UseMaterialColors", true);
		sphereMat.setColor("Diffuse", ColorRGBA.Gray);
		sphereMat.setColor("Ambient", ColorRGBA.White);
		for (int i=0; i<controlPoints.length; ++i) {
			Sphere s = new Sphere(CURVE_RESOLUTION, CURVE_RESOLUTION, CURVE_SIZE);
			Geometry g = new Geometry("ControlPoint", s);
			g.setMaterial(sphereMat);
			g.setLocalTranslation(controlPoints[i]);
			node.attachChild(g);
		}
		
		Material tubeMat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		tubeMat.setBoolean("UseMaterialColors", true);
		tubeMat.setColor("Diffuse", ColorRGBA.Blue);
		tubeMat.setColor("Ambient", ColorRGBA.White);
		for (int i=1; i<controlPoints.length; ++i) {
			Vector3f P1 = controlPoints[i-1];
			Vector3f P2 = controlPoints[i];
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
	
	
}
