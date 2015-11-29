/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.shaman.terrain.Biome;
import org.shaman.terrain.Heightmap;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.Vectorfield;

/**
 *
 * @author Sebastian Weiss
 */
public class TreePlanter {
	private static final Logger LOG = Logger.getLogger(TreePlanter.class.getName());
	private static EnumMap<Biome, Pair<Float, TreeInfo>[]> TREES;
	
	private final TerrainHeighmapCreator app;
	private final Heightmap map;
	private final Vectorfield biomes;
	private final Node sceneNode;
	private final float scaleFactor;
	private final float size;

	public TreePlanter(TerrainHeighmapCreator app, Heightmap map, Vectorfield biomes, 
			Node sceneNode, float scaleFactor, float treeSize) {
		this.app = app;
		this.map = map;
		this.biomes = biomes;
		this.sceneNode = new Node("trees");
		sceneNode.attachChild(this.sceneNode);
		this.scaleFactor = scaleFactor;
		this.size = treeSize;
		INIT(app.getAssetManager());
	}
	
	@SuppressWarnings("unchecked")
	private static synchronized void INIT(AssetManager assetManager) {
		if (TREES != null) {
			return;
		}
		assetManager.registerLocator(ImpositorCreator.OUTPUT_FOLDER, FileLocator.class);
		TREES = new EnumMap<>(Biome.class);
		//load tree data
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(ImpositorCreator.TREE_DATA_FILE)))) {
			List<TreeInfo> list = (List<TreeInfo>) in.readObject();
			for (Biome b : Biome.values()) {
				List<Pair<Float, TreeInfo>> trees = new ArrayList<>();
				float p = 0;
				for (TreeInfo t : list) {
					if (t.biome == b) {
						p += t.probability;
						trees.add(new ImmutablePair<>(p, t));
					}
				}
				TREES.put(b, trees.toArray(new Pair[trees.size()]));
			}
			LOG.info("tree information loaded");
		} catch (IOException | ClassNotFoundException ex) {
			Logger.getLogger(TreePlanter.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void update(float tpf) {
		
	}
	
	public void showTrees(boolean show) {
		sceneNode.detachAllChildren();
		if (show) {
			plantTrees();
		}
	}
	
	private void plantTrees() {
//		Box testMesh = new Box(1, 1, 1);
//		Material testMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//		testMat.setColor("Color", ColorRGBA.Red);
		
		//bucketing for speed improvement
		MultiMap<TreeInfo, TreeNode> nodes = new MultiValueMap<>();
		
		//first, simple planting algorithm
		float density = 0.2f / size;
		Random rand = new Random();
		Biome[] allBiomes = Biome.values();
		for (int x=0; x<biomes.getSize(); ++x) {
			for (int y=0; y<biomes.getSize(); ++y) {
				if (map.getHeightAt(x, y)<=0) {
					continue;
				}
				//check if we can plant here
				if (rand.nextFloat() > density) {
					continue;
				}
				//find highest scoring biome
				float[] v = biomes.getVectorAt(x, y);
				float max = 0;
				Biome biome = null;
				for (int i=0; i<v.length; ++i) {
					if (v[i]>max) {
						max = v[i];
						biome = allBiomes[i];
					}
				}
				if (biome == null) {
					LOG.log(Level.WARNING, "no biome found at ({0},{1})", new Object[]{x, y});
					continue;
				}
				//get tree sample
				Pair<Float, TreeInfo>[] trees = TREES.get(biome);
				float f = rand.nextFloat();
				TreeInfo tree = null;
				for (int i=0; i<trees.length; ++i) {
					if (trees[i].getLeft() > f) {
						tree = trees[i].getRight();
						break;
					}
				}
				if (tree == null) {
					continue;
				}
				//create tree node
				TreeNode treeNode = new TreeNode(tree, app.getAssetManager(), app.getCamera());
				treeNode.setUseHighRes(false);
//				Geometry treeNode = new Geometry("tree", testMesh);
//				treeNode.setMaterial(testMat);
				treeNode.setLocalScale(size);
				treeNode.rotate(-FastMath.HALF_PI, 0, 0);
				Vector3f pos = app.getHeightmapPoint(x + rand.nextFloat() - 0.5f, y + rand.nextFloat() - 0.5f);
				pos.x *= scaleFactor;
				pos.z *= scaleFactor;
				treeNode.setLocalTranslation(pos);
				nodes.put(tree, treeNode);
			}
		}
		
		for (Map.Entry<TreeInfo, Object> entries : nodes.entrySet()) {
			Collection<TreeNode> col = (Collection<TreeNode>) entries.getValue();
			for (TreeNode n : col) {
				sceneNode.attachChild(n);
			}
		}
		
		LOG.info(sceneNode.getChildren().size()+" trees planted");
	}
}
