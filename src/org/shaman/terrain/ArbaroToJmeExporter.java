/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.arbaro.export.*;
import net.sourceforge.arbaro.mesh.*;
import net.sourceforge.arbaro.params.FloatFormat;
import net.sourceforge.arbaro.transformation.Vector;
import net.sourceforge.arbaro.tree.DefaultTreeTraversal;
import net.sourceforge.arbaro.tree.Leaf;
import net.sourceforge.arbaro.tree.Tree;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author Sebastian Weiss
 */
public class ArbaroToJmeExporter extends MeshExporter{
	private static final Logger LOG = Logger.getLogger(ArbaroToJmeExporter.class.getName());
	private static final float BARK_UV_SCALING = 1;
	
	long vertexProgressCount=0;
	long faceProgressCount=0;
	Mesh mesh;
	LeafMesh leafMesh;
	Tree tree;
	long smoothingGroup;
	int vertexOffset;
	int uvVertexOffset;
	
	private final AssetManager assetManager;
	private ArrayList<Vector3f> stemVertices;
	private ArrayList<Vector3f> stemNormals;
	private ArrayList<Vector2f> stemUVs;
	private ArrayList<Integer> stemIndices;
	private ArrayList<Vector3f> leafVertices;
	private ArrayList<Vector3f> leafNormals;
	private ArrayList<Vector2f> leafUVs;
	private ArrayList<Vector2f> leafTmpUVs;
	private ArrayList<Integer> leafIndices;
	private com.jme3.scene.Node spatial;
	private String barkTex;
	private String leafTex;
	private ColorRGBA barkCol;
	private boolean writeLeaves = true;
	private float leafRotation = 0;
	
	public ArbaroToJmeExporter(AssetManager assetManager, Tree tree, MeshGenerator meshGenerator) {
		super(meshGenerator);
		this.assetManager = assetManager;
		this.tree = tree;
		LOG.info("exporter created");
		progress = new Progress();
	}
	
	public void setBarkTexture(String tex) {
		LOG.log(Level.INFO, "use bark texture {0}", tex);
		barkTex = tex;
	}
	public void setBarkColor(ColorRGBA col) {
		barkCol = col;
	}
	public void setLeafTexture(String tex) {
		LOG.log(Level.INFO, "use leaf texture {0}", tex);
		leafTex = tex;
	}
	public void writeLeaves(boolean leaves) {
		LOG.log(Level.INFO, "write leaves: {0}", leaves);
		writeLeaves = leaves;
	}
	public void setLeafRotation(float rotDegree) {
		LOG.log(Level.INFO, "use leaf rotation of {0}", rotDegree);
		leafRotation = FastMath.DEG_TO_RAD * rotDegree;
	}
	
	public com.jme3.scene.Spatial getSpatial() {
		return spatial;
	}
	
	@Override
	public void doWrite() {
		LOG.info("doWrite");
		
		//create buffers
		stemVertices = new ArrayList<>();
		stemNormals = new ArrayList<>();
		stemUVs = new ArrayList<>();
		stemIndices = new ArrayList<>();
		leafVertices = new ArrayList<>();
		leafNormals = new ArrayList<>();
		leafUVs = new ArrayList<>();
		leafTmpUVs = new ArrayList<>();
		leafIndices = new ArrayList<>();
		
		smoothingGroup=1;

		mesh = meshGenerator.createStemMeshByLevel(tree,progress);
		leafMesh = meshGenerator.createLeafMesh(tree,meshGenerator.getUseQuads());

		//stem
		writeStemVertices();
		writeStemFaces();
		
		//leaves
		if (writeLeaves) {
			writeLeafVertices();
			writeLeafFaces();
			duplicateLeaves();
		}
		
		//create normals
		createNormals(stemVertices, stemNormals, stemIndices);
		createNormals(leafVertices, leafNormals, leafIndices);
		
		//create spatial
		spatial = new com.jme3.scene.Node("tree");
		
		com.jme3.scene.Mesh stemMesh = new com.jme3.scene.Mesh();
		stemMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(stemVertices.toArray(new Vector3f[stemVertices.size()])));
		stemMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(stemNormals.toArray(new Vector3f[stemNormals.size()])));
		stemMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(stemUVs.toArray(new Vector2f[stemUVs.size()])));
		stemMesh.setBuffer(VertexBuffer.Type.Index, 3, ArrayUtils.toPrimitive(stemIndices.toArray(new Integer[stemIndices.size()])));
		stemMesh.updateCounts();
		stemMesh.updateBound();
		Geometry geom = new Geometry("Stem", stemMesh);
//		Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		Material mat = new Material(assetManager, "org/shaman/terrain/shader/Tree.j3md");
		mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		geom.setQueueBucket(RenderQueue.Bucket.Transparent);
		if (barkTex!=null) {
			Texture tex = assetManager.loadTexture(barkTex);
			tex.setWrap(Texture.WrapMode.Repeat);
			mat.setTexture("DiffuseMap", tex);
		} else {
			mat.setColor("Diffuse", barkCol != null ? barkCol : ColorRGBA.Brown);
			mat.setBoolean("UseMaterialColors", true);
		}
		geom.setMaterial(mat);
		spatial.attachChild(geom);
		
		if (writeLeaves) {
			com.jme3.scene.Mesh leafMesh = new com.jme3.scene.Mesh();
			leafMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(leafVertices.toArray(new Vector3f[leafVertices.size()])));
			leafMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(leafNormals.toArray(new Vector3f[leafNormals.size()])));
			leafMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(leafUVs.toArray(new Vector2f[leafUVs.size()])));
			leafMesh.setBuffer(VertexBuffer.Type.Index, 3, ArrayUtils.toPrimitive(leafIndices.toArray(new Integer[leafIndices.size()])));
			leafMesh.updateCounts();
			leafMesh.updateBound();
			geom = new Geometry("Leaves", leafMesh);
//			mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
			mat = new Material(assetManager, "org/shaman/terrain/shader/Tree.j3md");
			if (leafTex!=null) {
				mat.setTexture("DiffuseMap", assetManager.loadTexture(leafTex));
				mat.getAdditionalRenderState().setAlphaTest(true);
				mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
				mat.setFloat("AlphaDiscardThreshold", 0.5f);
				geom.setQueueBucket(RenderQueue.Bucket.Transparent);
			} else {
				mat.setColor("Diffuse", ColorRGBA.Green);
				mat.setBoolean("UseMaterialColors", true);
			}
			geom.setMaterial(mat);
			spatial.attachChild(geom);
		}
		
	}
	
	private void createNormals(List<Vector3f> positions, List<Vector3f> normals, List<Integer> indices) {
		normals.clear();
		for (int i=0; i<positions.size(); ++i) {
			normals.add(new Vector3f());
		}
		for (int i=0; i<indices.size()/3; ++i) {
			int a = indices.get(3*i);
			int b = indices.get(3*i + 1);
			int c = indices.get(3*i + 2);
			Vector3f A = positions.get(a);
			Vector3f B = positions.get(b);
			Vector3f C = positions.get(c);
			Vector3f N = (B.subtract(A)).cross((C.subtract(A)));
			//N.normalizeLocal();
			normals.get(a).addLocal(N);
			normals.get(b).addLocal(N);
			normals.get(c).addLocal(N);
		}
		for (Vector3f n : normals) {
			n.normalizeLocal();
		}
	}
	
	private void writeStemVertices() {
		
		for (Enumeration vertices = mesh.allVertices(false);
			vertices.hasMoreElements();) {
				Vertex vertex = (Vertex)vertices.nextElement();
				stemVertices.add(new Vector3f((float) vertex.point.getX(), (float) vertex.point.getY(), (float) vertex.point.getZ()));
				stemUVs.add(new Vector2f((float) vertex.point.u * BARK_UV_SCALING, (float) vertex.point.v * BARK_UV_SCALING));
		}
//		for (Enumeration vertices = mesh.allVertices(true);
//			vertices.hasMoreElements();) {
//				UVVector vertex = (UVVector)vertices.nextElement();
//				stemUVs.add(new Vector2f((float) vertex.u * BARK_UV_SCALING, (float) vertex.v * 256));
//		}
	}
	
	private void writeStemFaces() {
		// output mesh triangles
		vertexOffset = 0;
		//boolean separate_trunk = false;
		for (int stemLevel = 0; stemLevel<tree.getLevels(); stemLevel++) {
			
			for (Enumeration parts=mesh.allParts(stemLevel);
				parts.hasMoreElements();) { 

				MeshPart mp = (MeshPart)parts.nextElement();
				uvVertexOffset = 1 + mesh.firstUVIndex(mp.getStem().getLevel());
				
				Enumeration faces=mp.allFaces(mesh,vertexOffset,false);
				Enumeration uvFaces=mp.allFaces(mesh,uvVertexOffset,true);
				
				while (faces.hasMoreElements()) {
					Face face = (Face)faces.nextElement();
					Face uvFace = (Face)uvFaces.nextElement();
					//face can be a triangle or a quad
					for (int i=2; i<face.points.length; ++i) {
						stemIndices.add((int) face.points[0]);
						stemIndices.add((int) face.points[i]);
						stemIndices.add((int) face.points[i-1]);
					}
				}
				
				vertexOffset += mp.vertexCount();
				
				uvVertexOffset += mp.uvCount();
				
				incProgressCount(AbstractExporter.MESH_PROGRESS_STEP);
			}
		}
	}
	
	private void writeLeafVertices() {
		//Write positions
		LeafVertexWriter vertexExporter = 
				new LeafVertexWriter(tree, this, leafMesh, 
					vertexOffset, "v");
			tree.traverseTree(vertexExporter);
			vertexOffset = vertexExporter.leafVertexOffset;
		//write uvs
		if (leafMesh.isFlat()) {
			for (int i=0; i<leafMesh.getShapeVertexCount(); i++) {
				UVVector v = leafMesh.shapeUVAt(i);
				float x = (float) v.u;
				float y = (float) v.v;
//				leafUVs.add(new Vector2f((float) v.u, (float) v.v));
				if (leafRotation != 0) {
					//transform it
					x -= 0.5f; y -= 0.5f;
					float x2 = FastMath.cos(leafRotation)*x - FastMath.sin(leafRotation)*y;
					float y2 = FastMath.sin(leafRotation)*x + FastMath.cos(leafRotation)*y;
					x = x2+0.5f;
					y = y2+0.5f;
				}
				leafTmpUVs.add(new Vector2f(x, y));
			}	
		}
		
		LOG.info("leave counts: "+leafVertices.size() + "," +leafUVs.size());
	}
	
	private void writeLeafFaces() {
		LeafFaceWriter faceExporter = 
				new LeafFaceWriter(tree,this,leafMesh,
					vertexOffset, uvVertexOffset,smoothingGroup);
		tree.traverseTree(faceExporter);
		LOG.info("leave triangle count: "+leafIndices.size()/3);
	}
	
	private void duplicateLeaves() {
		ArrayList<Vector3f> tmp = leafVertices;
		int count = tmp.size();
		leafVertices = new ArrayList<>(leafVertices.size()*2);
		for (Vector3f v : tmp) leafVertices.add(v.clone());
		for (Vector3f v : tmp) leafVertices.add(v.clone());
		
		tmp = leafNormals;
		leafNormals = new ArrayList<>(leafNormals.size()*2);
		for (Vector3f v : tmp) leafNormals.add(v.clone());
		for (Vector3f v : tmp) leafNormals.add(v.clone());
		
		ArrayList<Vector2f> tmp2 = leafUVs;
		leafUVs = new ArrayList<>(leafUVs.size()*2);
		for (Vector2f v : tmp2) leafUVs.add(v.clone());
		for (Vector2f v : tmp2) leafUVs.add(v.clone());
		
		ArrayList<Integer> index = leafIndices;
		leafIndices = new ArrayList<>(index.size()*2);
		for (int i=0; i<index.size(); i+=3) {
			int a = index.get(i);
			int b = index.get(i+1);
			int c = index.get(i+2);
			leafIndices.add(a);
			leafIndices.add(b);
			leafIndices.add(c);
			leafIndices.add(a + count);
			leafIndices.add(c + count);
			leafIndices.add(b + count);
		}
	}
	
	private class LeafWriterBase extends DefaultTreeTraversal {
		Progress progress;
		LeafMesh leafMesh;
		AbstractExporter exporter;
		public int leafVertexOffset;
		PrintWriter w;
		long leavesProgressCount=0;
		Tree tree;

		final NumberFormat fmt = FloatFormat.getInstance();

		/**
		 * 
		 */
		public LeafWriterBase(Tree tree,
				LeafMesh leafMesh,
				AbstractExporter exporter,
				int leafVertexOffset) {
			super();
			this.exporter = exporter;
			this.w = exporter.getWriter();
			this.leafMesh = leafMesh;
			this.leafVertexOffset = leafVertexOffset;
		}

		public boolean enterTree(Tree tree) {
			this.tree = tree;
			return true;
		}
	}
	
	private class LeafVertexWriter extends LeafWriterBase {
		String type;
		long vertexProgressCount=0;

		/**
		 * @param pw
		 * @param leafMesh
		 * @param leafVertexOffset
		 */
		public LeafVertexWriter(Tree tree, AbstractExporter exporter, LeafMesh leafMesh,
				int leafVertexOffset, String type) {
			super(tree, leafMesh, exporter, leafVertexOffset);
			this.type=type;
		}

		public boolean visitLeaf(Leaf l) {
			for (int i=0; i<leafMesh.getShapeVertexCount(); i++) {

				if ("v".equals(type)) {
					Vector v = l.getTransformation().apply(leafMesh.shapeVertexAt(i).point);
					leafVertices.add(new Vector3f((float) v.getX(), (float) v.getY(), (float) v.getZ()));
					leafUVs.add(new Vector2f());
				} else {
					Vector v = l.getTransformation().apply(leafMesh.shapeVertexAt(i).normal);
					leafNormals.add(new Vector3f((float) v.getX(), (float) v.getY(), (float) v.getZ()));
				}
			}


			return true;
		}

//		private void writeVertex(Vector v, String type) {
//			w.println(type+" "
//					+fmt.format(v.getX())+" "
//					+fmt.format(v.getZ())+" "
//					+fmt.format(v.getY()));
//		}

	}
	
	private class LeafFaceWriter extends LeafWriterBase {
		long faceProgressCount=0;

		long smoothingGroup;
		boolean outputLeafUVs=true;
		int offset = 0;

		/**
		 * @param pw
		 * @param leafMesh
		 * @param leafVertexOffset
		 */
		public LeafFaceWriter(Tree tree, AbstractExporter exporter,
				LeafMesh leafMesh,
				int leafVertexOffset, int uvVertexOffset,
				long smoothingGroup) {
			super(tree, leafMesh, exporter, leafVertexOffset);

			this.outputLeafUVs = true;
		}


		public boolean visitLeaf(Leaf l) {

			for (int i=0; i<leafMesh.getShapeFaceCount(); i++) {
				Face face = leafMesh.shapeFaceAt(i);
				writeFace(face);
			}
			offset += leafMesh.getShapeVertexCount();

			return true;
		}


		private void writeFace(Face f) {
			for (int i=2; i<f.points.length; ++i) {
				leafIndices.add((int) f.points[0]+offset);
				leafIndices.add((int) f.points[i]+offset);
				leafIndices.add((int) f.points[i-1]+offset);
			}
			if (!leafTmpUVs.isEmpty()) {
				for (int i=0; i<f.points.length; ++i) {
					leafUVs.get((int) f.points[i]+offset).set(leafTmpUVs.get((int) f.points[i]));
				}
			}
		}
	}
}
