/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.app.SimpleApplication;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.zero_separation.plugins.imagepainter.ImagePainter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.shaman.terrain.heightmap.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public class SketchTerrain implements AnalogListener, ActionListener {
	private static final Logger LOG = Logger.getLogger(SketchTerrain.class.getName());
	private final TerrainHeighmapCreator app;
	private final Heightmap map;
	
	private ImagePainter image;
	private Texture2D texture;
	private Material quadMaterial;
	
	private boolean sketchStarted = false;
	private float lastX, lastY;
	private List<Vector2f> points = new ArrayList<>();

	public SketchTerrain(TerrainHeighmapCreator app, Heightmap map) {
		this.app = app;
		this.map = map;
		init();
	}
	
	private void init() {
		int w = app.getCamera().getWidth();
		int h = app.getCamera().getHeight();
		image = new ImagePainter(Image.Format.ABGR8, w, h);
		image.wipe(new ColorRGBA(0, 0, 0, 0f));
		image.paintLine(100, 100, 200, 200, 5, ColorRGBA.White, ImagePainter.BlendMode.SET);
		texture = new Texture2D(image.getImage());
		quadMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		quadMaterial.setTexture("ColorMap", texture);
		quadMaterial.setTransparent(true);
		quadMaterial.getAdditionalRenderState().setAlphaTest(true);
		quadMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		Quad quad = new Quad(w, h);
		Geometry quadGeom = new Geometry("ScetchQuad", quad);
		quadGeom.setMaterial(quadMaterial);
//		quadGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
		app.getGuiNode().attachChild(quadGeom);
		
		app.getInputManager().addMapping("SketchStartEnd", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		app.getInputManager().addMapping("SketchEditL", new MouseAxisTrigger(MouseInput.AXIS_X, true));
		app.getInputManager().addMapping("SketchEditR", new MouseAxisTrigger(MouseInput.AXIS_X, false));
		app.getInputManager().addMapping("SketchEditU", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
		app.getInputManager().addMapping("SketchEditD", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
		app.getInputManager().addListener(this, "SketchStartEnd", "SketchEditL", "SketchEditR", "SketchEditU", "SketchEditD");
	}
	
	public void onUpdate(float tpf) {
		Vector2f cursor = app.getInputManager().getCursorPosition();
		if (cursor.x==lastX && cursor.y==lastY) {
			return;
		}
		if (sketchStarted) {
			points.add(cursor.clone());
			int x1 = (int) points.get(points.size()-2).x;
			int y1 = (int) points.get(points.size()-2).y;
			int x2 = (int) points.get(points.size()-1).x;
			int y2 = (int) points.get(points.size()-1).y;
			if (y1<y2) {
				image.paintLine(x1, y1, x2, y2, 5, ColorRGBA.Blue, ImagePainter.BlendMode.SET);
			} else {
				image.paintLine(x2, y2, x1, y1, 5, ColorRGBA.Blue, ImagePainter.BlendMode.SET);
			}
			quadMaterial.setTexture("ColorMap", texture);
		}
		lastX = cursor.x;
		lastY = cursor.y;
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
//		if (sketchStarted) {
//			switch (name) {
//				case "SketchEditL": lastX -= value*app.getCamera().getWidth(); break;
//				case "SketchEditR": lastX += value*app.getCamera().getWidth(); break;
//				case "SketchEditU": lastY -= value*app.getCamera().getHeight(); break;
//				case "SketchEditD": lastY += value*app.getCamera().getHeight(); break;
//			}
//			points.add(new Vector2f(lastX, lastY));
//			image.paintLine((int) points.get(points.size()-2).x, (int) points.get(points.size()-2).y,
//					(int) points.get(points.size()-1).x, (int) points.get(points.size()-1).y,
//					5, ColorRGBA.Blue, ImagePainter.BlendMode.SET);
//			quadMaterial.setTexture("ColorMap", texture);
//		}
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if ("SketchStartEnd".equals(name)) {
			if (isPressed) {
				LOG.info("start sketch");
				points.clear();
				points.add(new Vector2f(lastX, lastY));
				sketchStarted = true;
			} else {
				sketchStarted = false;
				LOG.log(Level.INFO, "sketch finished: {0}", points);
			}
		}
	}
}
