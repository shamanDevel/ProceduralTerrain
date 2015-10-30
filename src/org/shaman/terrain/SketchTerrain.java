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
import java.util.Collections;
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

	private int width, height;
	private ImagePainter tmpImage;
	private ImagePainter image;
	private Texture2D tmpTexture;
	private Material tmpQuadMaterial;
	private Texture2D texture;
	private Material quadMaterial;

	private boolean sketchStarted = false;
	private float lastX, lastY;
	private ArrayList<Vector2f> currentPoints = new ArrayList<>();
	/**
	 * this list contains all sketches. The sketches are point list from left to right
	 */
	private ArrayList<ArrayList<Vector2f>> sketches = new ArrayList<>();

	public SketchTerrain(TerrainHeighmapCreator app, Heightmap map) {
		this.app = app;
		this.map = map;
		init();
	}

	private void init() {
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
		app.getGuiNode().attachChild(quadGeom);
		tmpTexture = new Texture2D(tmpImage.getImage());
		tmpQuadMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		tmpQuadMaterial.setTexture("ColorMap", tmpTexture);
		tmpQuadMaterial.setTransparent(true);
		tmpQuadMaterial.getAdditionalRenderState().setAlphaTest(true);
		tmpQuadMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		quad = new Quad(width, height);
		quadGeom = new Geometry("TmpSketchQuad", quad);
		quadGeom.setMaterial(tmpQuadMaterial);
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
		if (cursor.x == lastX && cursor.y == lastY) {
			return;
		}
		if (sketchStarted) {
			currentPoints.add(cursor.clone());
			int x1 = (int) currentPoints.get(currentPoints.size() - 2).x;
			int y1 = (int) currentPoints.get(currentPoints.size() - 2).y;
			int x2 = (int) currentPoints.get(currentPoints.size() - 1).x;
			int y2 = (int) currentPoints.get(currentPoints.size() - 1).y;
			drawLineBresenham(x1, y1, x2, y2, 5, ColorRGBA.Blue, tmpImage);
			tmpQuadMaterial.setTexture("ColorMap", tmpTexture);
		}
		lastX = cursor.x;
		lastY = cursor.y;
	}
	
	private void addCurrentSketch() {
		for (int i=1; i<currentPoints.size(); ++i) {
			Vector2f p1 = currentPoints.get(i-1);
			Vector2f p2 = currentPoints.get(i);
			drawLineBresenham((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, 5, ColorRGBA.Black, image);
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
		}
	}
	
	private boolean isSketchValid(ArrayList<Vector2f> points) {
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
