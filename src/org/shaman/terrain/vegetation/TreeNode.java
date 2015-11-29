/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Weiss
 */
public class TreeNode extends Node {
	private static final Logger LOG = Logger.getLogger(TreeNode.class.getName());
	private final TreeInfo tree;
	private final AssetManager assetManager;
	private final Camera camera;
	
	private Geometry impositor;
	private Geometry highResStem;
	private Geometry highResLeaves;
	private float fadeFar;
	private boolean useHighRes;

	public TreeNode(TreeInfo tree, AssetManager assetManager, Camera camera) {
		super(tree.name);
		this.tree = tree;
		this.assetManager = assetManager;
		this.camera = camera;
		
		fadeFar = Math.max(tree.highResLeavesFadeFar, tree.highResStemFadeFar)*1.1f;
		fadeFar *= fadeFar;
	}

	public boolean isUseHighRes() {
		return useHighRes;
	}

	public void setUseHighRes(boolean useHighRes) {
		this.useHighRes = useHighRes;
	}

	@Override
	public void updateLogicalState(float tpf) {
		if (impositor==null) {
			loadImpostor();
//			LOG.info("impostor added");
		}
		float dist = camera.getLocation().distanceSquared(this.getWorldTranslation());
		if (dist <= fadeFar && highResStem==null && useHighRes) {
			loadHighResTree();
			LOG.log(Level.INFO, "dist={0} -> load high resultion mesh", dist);
		} else if (dist > fadeFar && highResStem!=null) {
			detachChild(highResStem);
			if (highResLeaves != null) {
				detachChild(highResLeaves);
			}
			highResStem = null;
			highResLeaves = null;
			LOG.log(Level.INFO, "dist={0} -> discard high resultion mesh", dist);
		}
		super.updateLogicalState(tpf);
	}
	
	private void loadHighResTree() {
		Node treeNode = (Node) assetManager.loadModel(tree.name + "/Tree.j3o");
		//Note: all tree nodes that share the same model file use the same
		//material -> the same settings for fade distances
		highResStem = (Geometry) treeNode.getChild(0);
		highResStem.getMaterial().setFloat("FadeNear", tree.highResStemFadeNear);
		highResStem.getMaterial().setFloat("FadeFar", tree.highResStemFadeFar);
		highResStem.getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		highResStem.getMaterial().setTransparent(true);
		highResStem.setQueueBucket(RenderQueue.Bucket.Transparent);
		super.attachChild(highResStem);
		if (treeNode.getChildren().size()>=1) {
			highResLeaves = (Geometry) treeNode.getChild(0);
			highResLeaves.getMaterial().setFloat("FadeNear", tree.highResLeavesFadeNear);
			highResLeaves.getMaterial().setFloat("FadeFar", tree.highResLeavesFadeFar);
			highResLeaves.setQueueBucket(RenderQueue.Bucket.Transparent);
			super.attachChild(highResLeaves);
		}
		
	}
	
	private void loadImpostor() {
		if (tree.impostorMaterial == null) {
			List<Image> images = new ArrayList<>(tree.impostorCount);
			for (int i=0; i<ImpositorCreator.IMPOSITOR_COUNT; ++i) {
				Texture t = assetManager.loadTexture(name + "/" + i + ".png");
				images.add(t.getImage());
			}
			TextureArray tex = new TextureArray(images);
			tex.setMinFilter(Texture.MinFilter.Trilinear);
			tex.setMagFilter(Texture.MagFilter.Bilinear);
			Material mat = new Material(assetManager, "org/shaman/terrain/shader/Impositor.j3md");
			mat.setTexture("ColorMap", tex);
			mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
			mat.setFloat("AlphaDiscardThreshold", 0.5f);
			if (useHighRes) {
			mat.setFloat("FadeNear", tree.impostorFadeNear);
			mat.setFloat("FadeFar", tree.impostorFadeFar);
			} else {
				mat.setFloat("FadeNear", 0);
				mat.setFloat("FadeFar", 1);
			}
			tree.impostorMaterial = mat;
		}
		Mesh mesh = getImpostorMesh();
		impositor = new Geometry("impostor", mesh);
		impositor.setMaterial(tree.impostorMaterial);
		impositor.scale(tree.treeSize);
		impositor.setQueueBucket(RenderQueue.Bucket.Transparent);
		super.attachChild(impositor);
		super.updateModelBound();
	}
	
	private static final HashMap<Integer, Mesh> IMPOSTORS = new HashMap<>();
	private Mesh getImpostorMesh() {
		Mesh mesh = IMPOSTORS.get(tree.impostorCount);
		if (mesh != null) {
			return mesh;
		}
		float[] pos = new float[3 * 4 * tree.impostorCount];
		float[] tex = new float[3 * 4 * tree.impostorCount];
		int[] index = new int[6 * tree.impostorCount];
		for (int i=0; i<tree.impostorCount; ++i) {
			float angle = i * FastMath.TWO_PI / tree.impostorCount + FastMath.PI;
			float x1 = (float) (Math.cos(angle) * -0.5);
			float y1 = (float) (Math.sin(angle) * -0.5);
			float x2 = (float) (Math.cos(angle) * 0.5);
			float y2 = (float) (Math.sin(angle) * 0.5);
			pos[12*i+0] = x2; pos[12*i+1] = y2; pos[12*i+2] = 0;
			pos[12*i+3] = x1; pos[12*i+4] = y1; pos[12*i+5] = 0;
			pos[12*i+6] = x1; pos[12*i+7] = y1; pos[12*i+8] = 1;
			pos[12*i+9] = x2; pos[12*i+10] = y2; pos[12*i+11] = 1;
			tex[12*i+0] = 0; tex[12*i+1] = 0; tex[12*i+2] = i;
			tex[12*i+3] = 1; tex[12*i+4] = 0; tex[12*i+5] = i;
			tex[12*i+6] = 1; tex[12*i+7] = 1; tex[12*i+8] = i;
			tex[12*i+9] = 0; tex[12*i+10] = 1; tex[12*i+11] = i;
			index[6*i+0] = 4*i+0; index[6*i+1] = 4*i+1; index[6*i+2] = 4*i+2;
			index[6*i+3] = 4*i+0; index[6*i+4] = 4*i+2; index[6*i+5] = 4*i+3;
		}
		mesh = new Mesh();
		mesh.setBuffer(VertexBuffer.Type.Position, 3, pos);
		mesh.setBuffer(VertexBuffer.Type.TexCoord, 3, tex);
		mesh.setBuffer(VertexBuffer.Type.Index, 3, index);
		mesh.updateCounts();
		IMPOSTORS.put(tree.impostorCount, mesh);
		return mesh;
	}
}
