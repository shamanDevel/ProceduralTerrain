/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 *
 * @author Sebastian Weiss
 */
public class ImpositorViewer extends SimpleApplication {
//	private static final String TREE = "black_tupelo_2_v0";
//	private static final String TREE = "ca_black_oak_1_v1";
	private static final String TREE = "oak_1_v0";
	float minDist = 30;
	float maxDist = 90;
	
	private Spatial highResTree;
	private TreeNode impostor;
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
		stateManager.attach(new ScreenshotAppState());
//		viewPort.setBackgroundColor(ColorRGBA.White);
		
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
		highResTree = null;
		assetManager.clearCache();
		
		//load impositor
		TreeInfo treeInfo = new TreeInfo();
		treeInfo.name = TREE;
		treeInfo.impostorCount = ImpositorCreator.IMPOSITOR_COUNT;
		treeInfo.treeSize = size;
		treeInfo.impostorFadeNear = 30;
		treeInfo.impostorFadeFar = 50;
		treeInfo.highResStemFadeNear = 30;
		treeInfo.highResStemFadeFar = 50;
		treeInfo.highResLeavesFadeNear = 35;
		treeInfo.highResLeavesFadeFar = 55;
		impostor = new TreeNode(treeInfo, assetManager, cam);
		impostor.setUseHighRes(true);
		impostor.move(0, 0, -size/2);
		rootNode.attachChild(impostor);
	}

}
