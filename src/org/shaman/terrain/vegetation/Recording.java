/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.shaman.terrain.TerrainHeighmapCreator;

/**
 *
 * @author Sebastian Weiss
 */
public class Recording implements AnalogListener, ActionListener {
	private static final Logger LOG = Logger.getLogger(Recording.class.getName());
	private static final float SPEED = 10;
	
	private final TerrainHeighmapCreator app;
	private final Camera cam;
	private final Node sceneNode;
	
	private static class Point {
		private Vector3f position;
		private Vector3f direction;
		private Quaternion rotation;
		private float speed = SPEED;
		
		private Geometry sphere;
		private Geometry arrow;
	}
	private float currentSpeed = SPEED;
	private ArrayList<Point> points;
	private Mesh sphereMesh;
	private Material sphereMat;
	private Material arrowMat;
	private boolean playing = false;
	private boolean recording = false;
	private float time = 0;
	private int currentStep;
	private float stepLength;
	private String oldScreen;
	
	public Recording(TerrainHeighmapCreator app, Camera cam, Node sceneNode) {
		this.app = app;
		this.cam = cam;
		this.sceneNode = new Node("record");
		sceneNode.attachChild(this.sceneNode);
		this.points = new ArrayList<>();
		
		sphereMesh = new Sphere(8, 8, 0.25f*TerrainHeighmapCreator.TERRAIN_SCALE);
		sphereMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		sphereMat.setColor("Color", ColorRGBA.Blue);
		arrowMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		arrowMat.getAdditionalRenderState().setWireframe(true);
		arrowMat.setColor("Color", ColorRGBA.Red);
		
		app.getNifty().addXml("org/shaman/terrain/vegetation/DummyScreen.xml");
		app.getInputManager().addMapping("RecordingAdd", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
		app.getInputManager().addMapping("RecordingSpeed-", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
		app.getInputManager().addMapping("RecordingSpeed+", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
		app.getInputManager().addListener(this, "RecordingAdd", "RecordingSpeed-", "RecordingSpeed+");
	}
	
	public void addPoint() {
		Point p = new Point();
		p.position = cam.getLocation().clone();
		p.direction = cam.getDirection().clone();
		p.rotation = cam.getRotation().clone();
		p.speed = currentSpeed;
		LOG.info("point added: "+p.position+"  "+p.rotation);
		
		p.sphere = new Geometry("point", sphereMesh);
		p.sphere.setMaterial(sphereMat);
		p.sphere.setLocalTranslation(p.position);
		sceneNode.attachChild(p.sphere);
		p.arrow = new Geometry("arrow", new Arrow(p.direction.mult(TerrainHeighmapCreator.TERRAIN_SCALE)));
		p.arrow.getMesh().setLineWidth(4);
		p.arrow.setMaterial(arrowMat);
		p.arrow.setLocalTranslation(p.position);
		sceneNode.attachChild(p.arrow);
		
		points.add(p);
	}
	
	public void deleteAll() {
		for (Point p : points) {
			p.arrow.removeFromParent();
			p.sphere.removeFromParent();
		}
		points.clear();
		LOG.info("all points deleted");
	}
	
	public void play() {
		if (points.size()<3) {
			LOG.warning("can only play when there are at least 3 points");
			return;
		}
		LOG.info("play");
		app.setCameraEnabled(false);
		playing = true;
		time = 0;
		stepLength = -1;
		currentStep = -1;
		app.getGuiNode().setCullHint(Spatial.CullHint.Always);
		oldScreen = app.getNifty().getCurrentScreen().getScreenId();
		app.getNifty().gotoScreen("Dummy");
	}
	
	public void record() {
		this.sceneNode.setCullHint(Spatial.CullHint.Always);
		recording = true;
		app.getStateManager().attach(new VideoRecorderAppState(1f));
		LOG.info("recording");
		play();
	}
	
	private void stop() {
		LOG.info("stop");
		playing = false;
		time = 0;
		app.setCameraEnabled(true);
		app.getGuiNode().setCullHint(Spatial.CullHint.Inherit);
		app.getNifty().gotoScreen(oldScreen);
		this.sceneNode.setCullHint(Spatial.CullHint.Inherit);
		if (recording) {
			recording = false;
			app.getStateManager().detach(app.getStateManager().getState(VideoRecorderAppState.class));
		}
	}
	
	public void update(float tpf) {
		if (!playing) {
			return;
		}
		if (currentStep+2 >= points.size()) {
			stop();
			return;
		}
		
		if (time>stepLength) {
			currentStep++;
			computeLength();
			time = 0;
		}
		
		Point p = interpolate(time / stepLength);
		app.getCamera().setLocation(p.position);
		app.getCamera().setRotation(p.rotation);
		time += tpf * p.speed * TerrainHeighmapCreator.TERRAIN_SCALE;
	}
	
	private void computeLength() {
		int M = 16;
		Vector3f old = null;
		float length = 0;
		for (int i=0; i<=M; ++i) {
			Vector3f v = interpolate(i / (float) M).position;
			if (old != null) {
				length += old.distance(v);
			}
			old = v;
		}
		stepLength = length;
	}
	
	private Point interpolate(float time) {
		Vector3f pos;
		float speed;
		if (currentStep==0) {
			Vector3f P0 = points.get(0).position;
			Vector3f P1 = points.get(1).position;
			Vector3f T0 = points.get(2).position.subtract(points.get(0).position).multLocal(0.5f);
			pos = quadraticHermite(P0, T0, P1, time);
			speed = quadraticHermite(points.get(0).speed, (points.get(2).speed-points.get(0).speed)/2, points.get(1).speed, time);
		} else if (currentStep==points.size()-2) {
			//last part
			int n = points.size();
			Vector3f P0 = points.get(n-1).position;
			Vector3f P1 = points.get(n-2).position;
			Vector3f T0 = points.get(n-3).position.subtract(points.get(n-1).position).multLocal(0.5f);
			pos = quadraticHermite(P0, T0, P1, 1-time);
			speed = quadraticHermite(points.get(n-1).speed, (points.get(n-3).speed-points.get(n-1).speed)/2, points.get(n-2).speed, 1-time);
		} else {
			//middle
			Vector3f P0 = points.get(currentStep).position;
			Vector3f P1 = points.get(currentStep+1).position;
			Vector3f T0 = points.get(currentStep+1).position.subtract(points.get(currentStep-1).position).multLocal(0.5f);
			Vector3f T1 = points.get(currentStep+2).position.subtract(points.get(currentStep).position).multLocal(0.5f);
			pos = cubicHermite(P0, T0, P1, T1, time);
			speed = cubicHermite(points.get(currentStep).speed, (points.get(currentStep+1).speed - points.get(currentStep-1).speed)/2, 
					points.get(currentStep+1).speed, (points.get(currentStep+2).speed - points.get(currentStep).speed)/2, time);
		}
		Quaternion rot = new Quaternion();
		rot.slerp(points.get(currentStep).rotation, points.get(currentStep+1).rotation, time);
		
		Point p = new Point();
		p.position = pos;
		p.rotation = rot;
		p.speed = speed;
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
	private static float cubicHermite(float P0, float T0, float P1, float T1, float t) {
		float t2 = t * t;
		float t3 = t2 * t;
		float v = 0;
		v += (2 * t3 - 3 * t2 + 1) * P0;
		v += (t3 - 2 * t2 + t) * T0;
		v += (-2 * t3 + 3 * t2) * P1;
		v += (t3 - t2) * T1;
		return v;
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
	private static float quadraticHermite(float P0, float T0, float P1, float t) {
		float t2 = t * t;
		float v = 0;
		v += (t2 - 2 * t + 1) * P0;
		v += (-t2 + 2 * t) * P1;
		v += (t2 - t) * T0;
		return v;
	}
	
	@Override
	public void onAnalog(String name, float value, float tpf) {
		if ("RecordingSpeed-".equals(name)) {
			currentSpeed /= 1+(value / 16);
			LOG.info("set speed to "+currentSpeed);
		} else if ("RecordingSpeed+".equals(name)) {
			currentSpeed *= 1+(value / 16);
			LOG.info("set speed to "+currentSpeed);
		}
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if ("RecordingAdd".equals(name) && isPressed) {
			addPoint();
		}
	}
}
