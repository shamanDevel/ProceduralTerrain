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
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 *
 * @author Sebastian Weiss
 */
public class ImpositorViewer extends SimpleApplication {
	private static final String TREE = "Tree1";
	float minDist = 30;
	float maxDist = 90;
	
	private Spatial highResTree;
	private TreeNode impostor;
	private boolean useImpositor = false;
	private List<MutablePair<Float, Geometry>> impositors = new ArrayList<>();
	private Comparator<MutablePair<Float, Geometry>> impositorComparator = new Comparator<MutablePair<Float, Geometry>>() {
		@Override
		public int compare(MutablePair<Float, Geometry> o1, MutablePair<Float, Geometry> o2) {
			return -Float.compare(o1.left, o2.left);
		}
	};
	private ChaseCamera chaseCam;
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new ImpositorViewer().start();
	}

	@Override
	public void simpleInitApp() {
		flyCam.setEnabled(false);
		chaseCam = new ChaseCamera(cam, rootNode, inputManager);
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
		highResTree = assetManager.loadModel(TREE+"/Tree.j3o");
		float size = highResTree.getWorldBound().getCenter().z * 2;
		
		//load impositor
		impostor = new TreeNode(TREE, ImpositorCreator.IMPOSITOR_COUNT, assetManager, cam);
		impostor.setImpostorFadeNear(30);
		impostor.setImpostorFadeFar(50);
		impostor.setTreeSize(size);
		impostor.setHighResStemFadeNear(30);
		impostor.setHighResStemFadeFar(50);
		impostor.setHighResLeavesFadeNear(35);
		impostor.setHighResLeavesFadeFar(55);
		impostor.move(0, 0, -size/2);
		rootNode.attachChild(impostor);
		
		//input
		inputManager.addMapping("Impositor", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addListener(new ActionListener() {
			@Override
			public void onAction(String name, boolean isPressed, float tpf) {
				if ("Impositor".equals(name) && isPressed) {
					useImpositor = !useImpositor;
					highResTree.setCullHint(useImpositor ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
					impostor.setCullHint(useImpositor ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
				}
			}
		}, "Impositor");
	}

}
