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
import com.jme3.scene.shape.Quad;
import java.util.logging.Logger;
import org.shaman.terrain.heightmap.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public class SketchTerrain2 implements ActionListener, AnalogListener {
	private static final Logger LOG = Logger.getLogger(SketchTerrain2.class.getName());
	private static final float PLANE_QUAD_SIZE = 200;
	private static final float INITIAL_PLANE_DISTANCE = 100f;
	private static final float PLANE_MOVE_SPEED = 0.02f;
	
	private final TerrainHeighmapCreator app;
	private final Heightmap map;
	
	private Node guiNode;
	private Node sceneNode;
	private float planeDistance = INITIAL_PLANE_DISTANCE;
	private Spatial sketchPlane;

	public SketchTerrain2(TerrainHeighmapCreator app, Heightmap map) {
		this.app = app;
		this.map = map;
		init();
	}
	
	private void init() {
		//init nodes
		guiNode = new Node("sketchGUI");
		app.getGuiNode().attachChild(guiNode);
		sceneNode = new Node("sketch3D");
		app.getRootNode().attachChild(sceneNode);
		
		//init sketch plane
		initSketchPlane();
		
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
		geom.setQueueBucket(RenderQueue.Bucket.Transparent);
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
