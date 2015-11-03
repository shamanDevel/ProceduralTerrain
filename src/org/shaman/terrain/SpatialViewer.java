/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.ChaseCamera;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Box;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static de.lessvoid.nifty.layout.align.HorizontalAlign.center;

/**
 *
 * @author Sebastian Weiss
 */
public class SpatialViewer extends SimpleApplication {
	private static final Logger LOG = Logger.getLogger(SpatialViewer.class.getName());
	
	private ChaseCamera chaseCam;
    private Spatial center;
	private PointLight light;
    private boolean useLight = true;
	
	private Spatial spatial;
	private Geometry bounds;

	@Override
	public void simpleInitApp() {
		center = createBox(0, 0, 0, ColorRGBA.Blue);
        attachCoordinateAxes(Vector3f.ZERO);

		flyCam.setDragToRotate(true);
//        flyCam.setEnabled(false);
//        chaseCam = new ChaseCamera(cam, center, inputManager);
		
		light = new PointLight();
		light.setColor(ColorRGBA.White);
		light.setPosition(new Vector3f(0,6,-6));
		rootNode.addLight(light);
		AmbientLight am = new AmbientLight();
		am.setColor(ColorRGBA.White.mult(0.5f));
		rootNode.addLight(am);
		
		bounds = new Geometry("bounds", new WireBox(0, 0, 0));
		Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Gray);
        bounds.setMaterial(mat);
		rootNode.attachChild(bounds);
	}
	
	public void setSpatial(final Spatial geom) {
		LOG.info("show spatial "+geom);
		LOG.info("vertex count: "+geom.getVertexCount());
		LOG.info("triangle count: "+geom.getTriangleCount());
		LOG.info("bounding volume: "+geom.getWorldBound());
		enqueue(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				if (spatial != null) {
					rootNode.detachChild(spatial);
				}
				geom.rotate(-FastMath.HALF_PI, 0, 0);
				BoundingBox b = (BoundingBox) geom.getWorldBound();
				bounds.setMesh(new WireBox(b.getXExtent(), b.getYExtent(), b.getZExtent()));
				bounds.setLocalTranslation(b.getCenter());
				spatial = geom;
				rootNode.attachChild(spatial);
				LOG.info("attached");
				return null;
			}
		});
	}
	
	@Override
	public void update() {
		super.update();
		if (light != null) {
			light.setPosition(cam.getLocation());
		}
	}
	
	private Geometry createBox(float x, float y, float z, ColorRGBA color) {
        Box b = new Box(Vector3f.ZERO, 0.2f, 0.2f, 0.2f); // create cube shape at the origin
        Geometry g = new Geometry("Box", b);  // create cube geometry from the shape
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        mat.setColor("Color", color);   // set color of material to blue
        g.setMaterial(mat);                   // set the cube's material
        g.setLocalTranslation(x, y, z);
        rootNode.attachChild(g);              // make the cube appear in the scene
        return g;
    }

    private void attachCoordinateAxes(Vector3f pos) {
        Arrow arrow = new Arrow(Vector3f.UNIT_X.mult(3));
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Red).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Y.mult(3));
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Green).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Z.mult(3));
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Blue).setLocalTranslation(pos);
    }

    private Geometry putShape(Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        rootNode.attachChild(g);
        return g;
    }
}
