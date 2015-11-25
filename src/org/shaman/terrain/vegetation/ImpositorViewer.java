/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

/**
 *
 * @author Sebastian Weiss
 */
public class ImpositorViewer extends SimpleApplication {
	private static final String TREE = "Tree1";
	
	private Spatial highResTree;
	private Node impositor;
	private boolean useImpositor = false;
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new ImpositorViewer().start();
	}

	@Override
	public void simpleInitApp() {
		flyCam.setEnabled(false);
		ChaseCamera chaseCam = new ChaseCamera(cam, rootNode, inputManager);
		rootNode.rotate(-FastMath.HALF_PI, 0, 0);
		chaseCam.setMinVerticalRotation(-FastMath.HALF_PI+0.01f);
		chaseCam.setMaxDistance(200);
		
		DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.1f, -0.1f, -0.1f)).normalize());
        rootNode.addLight(light);
		AmbientLight ambientLight = new AmbientLight(new ColorRGBA(0.6f, 0.6f, 0.6f, 1));
		rootNode.addLight(ambientLight);
		
		//load high-resolution model
		assetManager.registerLocator("./treemesh/", FileLocator.class);
		highResTree = assetManager.loadModel(TREE+".j3o");
		float size = highResTree.getWorldBound().getCenter().z * 2;
		highResTree.move(0, 0, -size/2);
		rootNode.attachChild(highResTree);
		
		//load impositor
		impositor = new Node();
		for (int i=0; i<ImpositorCreator.IMPOSITOR_COUNT; ++i) {
			QuadXZ quad = new QuadXZ(size, size);
			Geometry geom = new Geometry("impositor"+i, quad);
			Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			mat.setTexture("ColorMap", assetManager.loadTexture(TREE+"/"+i+".png"));
			mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
			mat.setFloat("AlphaDiscardThreshold", 0.5f);
			geom.setMaterial(mat);
			geom.setQueueBucket(RenderQueue.Bucket.Transparent);
			geom.rotate(0, 0, i * FastMath.TWO_PI / ImpositorCreator.IMPOSITOR_COUNT);
			impositor.attachChild(geom);
		}
		impositor.setLocalTranslation(highResTree.getLocalTranslation());
		rootNode.attachChild(impositor);
		impositor.setCullHint(Spatial.CullHint.Always);
		
		//input
		inputManager.addMapping("Impositor", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addListener(new ActionListener() {
			@Override
			public void onAction(String name, boolean isPressed, float tpf) {
				if ("Impositor".equals(name) && isPressed) {
					useImpositor = !useImpositor;
					highResTree.setCullHint(useImpositor ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
					impositor.setCullHint(useImpositor ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
				}
			}
		}, "Impositor");
	}
	
}
