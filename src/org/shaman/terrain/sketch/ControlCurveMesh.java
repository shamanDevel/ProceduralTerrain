/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.sketch.SketchTerrain.ControlCurve;
import org.shaman.terrain.sketch.SketchTerrain.ControlPoint;

/**
 *
 * @author Sebastian Weiss
 */
public class ControlCurveMesh {
	private static final int TUBE_SAMPLES = 64;
	private static final int TUBE_RESOLUTION = 4;
	private static final int SLOPE_SAMPLES = 32;
	private static final float CURVE_SIZE = 0.5f;
	
	private final TerrainHeighmapCreator app;
	private final ControlCurve curve;
	private final Mesh tubeMesh;
	private final Mesh slopeMesh;
	private final Geometry tubeGeom;
	private final Geometry slopeGeom;

	public ControlCurveMesh(ControlCurve curve, String tubeName, TerrainHeighmapCreator app) {
		this.curve = curve;
		this.app = app;
		
		tubeMesh = new Mesh();
		slopeMesh = new Mesh();
		slopeMesh.setMode(Mesh.Mode.Lines);
		slopeMesh.setLineWidth(5);
		updateMesh();
		tubeGeom = new Geometry(tubeName, tubeMesh);
		slopeGeom = new Geometry("slope", slopeMesh);
		Material tubeMat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		tubeMat.setBoolean("UseMaterialColors", true);
		tubeMat.setColor("Diffuse", ColorRGBA.Blue);
		tubeMat.setColor("Ambient", ColorRGBA.White);
		tubeGeom.setMaterial(tubeMat);
		tubeGeom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
		tubeGeom.setCullHint(Spatial.CullHint.Never);
		Material slopeMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		slopeMat.setBoolean("VertexColor", true);
		slopeMat.getAdditionalRenderState().setAlphaTest(true);
		slopeGeom.setMaterial(slopeMat);
		slopeGeom.setCullHint(Spatial.CullHint.Never);
	}
	
	public Geometry getTubeGeometry() {
		return tubeGeom;
	}
	
	public Geometry getSlopeGeometry() {
		return slopeGeom;
	}
	
	public final void updateMesh() {
		updateTube();
		updateSlope();
	}
	
	private void updateTube() {
		//buffers
		ArrayList<Vector3f> positions = new ArrayList<>();
		ArrayList<Vector3f> normals = new ArrayList<>();
		
		//create samples
		Vector3f[] A = new Vector3f[TUBE_SAMPLES+1];
		for (int i=0; i<=TUBE_SAMPLES; ++i) {
			ControlPoint p = curve.interpolate(i / (float) TUBE_SAMPLES);
			A[i] = app.mapHeightmapToWorld(p.x, p.y, p.height);
		}
		
		Vector3f[] CAs = new Vector3f[A.length]; //rotation axis
		Vector3f[] Ns = new Vector3f[A.length]; //rotation normals
		float[] angles = new float[A.length]; //start angles
		CAs[0] = Vector3f.ZERO;
		Ns[0] = Vector3f.ZERO;
		angles[0] = 0;
		for (int i = 1; i < A.length - 1; i++) {
			Vector3f N = A[i+1].subtract(A[i-1]).normalizeLocal();
			Vector3f CA = new Vector3f(0, 1, 0);
			CA.subtractLocal(N.mult(CA.dot(N)));
			CA.normalizeLocal();
			float angle = 0;
			//set arrays
			CAs[i] = CA;
			Ns[i] = N;
			angles[i] = angle;
		}
		CAs[A.length-1] = Vector3f.ZERO;
		Ns[A.length-1] = Vector3f.ZERO;
		angles[A.length-1] = 0;
		//draw the tube
		float step = (float) (2*Math.PI / TUBE_RESOLUTION); //if the medial axis was traced correctly, the angle should always be 180°
		for (int i = 0; i < A.length - 1; i++) {
			for (int j = 0; j < TUBE_RESOLUTION; j++) {
				Vector3f N1 = rotate(j*step + angles[i], CAs[i].mult(CURVE_SIZE), Ns[i]);
				Vector3f N2 = rotate(j*step + angles[i+1], CAs[i+1].mult(CURVE_SIZE), Ns[i+1]);
				Vector3f N3 = rotate((j+1)*step + angles[i+1], CAs[i+1].mult(CURVE_SIZE), Ns[i+1]);
				Vector3f N4 = rotate((j+1)*step + angles[i], CAs[i].mult(CURVE_SIZE), Ns[i]);
				
				Vector3f P1 = N1.add(A[i]);
				Vector3f P2 = N2.add(A[i+1]);
				Vector3f P3 = N3.add(A[i+1]);
				Vector3f P4 = N4.add(A[i]);

				positions.add(P1);
				positions.add(P2);
				positions.add(P3);
				positions.add(P1);
				positions.add(P3);
				positions.add(P4);

				N1.normalizeLocal();
				N2.normalizeLocal();
				N3.normalizeLocal();
				N4.normalizeLocal();
				
				normals.add(N1);
				normals.add(N2);
				normals.add(N3);
				normals.add(N1);
				normals.add(N3);
				normals.add(N4);
			}
		}
		
		tubeMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions.toArray(new Vector3f[positions.size()])));
		tubeMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(new Vector3f[normals.size()])));
		tubeMesh.setMode(Mesh.Mode.Triangles);
		tubeMesh.updateCounts();
	}
	private static Vector3f rotate(float angle, Vector3f X, Vector3f N) {
		Vector3f W = N.mult(N.dot(X));
		Vector3f U = X.subtract(W);
		return W.add(U.mult((float) Math.cos(angle)))
				.subtract(N.cross(U).mult((float) Math.sin(angle)));
	}
	
	private void updateSlope() {
		//buffers
		ArrayList<Vector3f> positions = new ArrayList<>(7*(SLOPE_SAMPLES+1));
		ArrayList<ColorRGBA> colors = new ArrayList<>(13*(SLOPE_SAMPLES+1));
		ArrayList<Integer> indices = new ArrayList<>();
		
		//create samples
		ControlPoint[] points = new ControlPoint[SLOPE_SAMPLES+1];
		for (int i=0; i<=SLOPE_SAMPLES; ++i) {
			points[i] = curve.interpolate(i / (float) SLOPE_SAMPLES);
		}
		
		ColorRGBA c1 = ColorRGBA.White;
		ColorRGBA c2 = ColorRGBA.Gray;
		
		//add vertices
		for (int i=0; i<points.length; ++i) {
			ControlPoint p = points[i];
			Vector3f c = app.mapHeightmapToWorld(p.x, p.y, p.height);
			float dx,dy;
			if (i==0) {
				dx = points[i+1].x - points[i].x;
				dy = points[i+1].y - points[i].y;
			} else if (i==points.length-1) {
				dx = points[i].x - points[i-1].x;
				dy = points[i].y - points[i-1].y;
			} else {
				dx = (points[i+1].x - points[i-1].x) / 2f;
				dy = (points[i+1].y - points[i-1].y) / 2f;
			}
			float sum = (float) Math.sqrt(dx*dx + dy*dy);
			dx /= sum;
			dy /= sum;
			Vector3f l1 = app.mapHeightmapToWorld(p.x - p.plateau*dy, p.y + p.plateau*dx, p.height);
			Vector3f r1 = app.mapHeightmapToWorld(p.x + p.plateau*dy, p.y - p.plateau*dx, p.height);
			Vector3f l2 = app.mapHeightmapToWorld(
					p.x - (p.plateau + FastMath.cos(p.angle1 * FastMath.DEG_TO_RAD)*p.extend1)*dy, 
					p.y + (p.plateau + FastMath.cos(p.angle1 * FastMath.DEG_TO_RAD)*p.extend1)*dx, 
					p.height - FastMath.sin(p.angle1 * FastMath.DEG_TO_RAD)*p.extend1);
			Vector3f r2 = app.mapHeightmapToWorld(
					p.x + (p.plateau + FastMath.cos(p.angle2 * FastMath.DEG_TO_RAD)*p.extend2)*dy, 
					p.y - (p.plateau + FastMath.cos(p.angle2 * FastMath.DEG_TO_RAD)*p.extend2)*dx, 
					p.height - FastMath.sin(p.angle2 * FastMath.DEG_TO_RAD)*p.extend2);
			
			positions.add(c);
			positions.add(l1);
			positions.add(l1);
			positions.add(l2);
			positions.add(r1);
			positions.add(r1);
			positions.add(r2);
			colors.add(c1);
			colors.add(c1);
			colors.add(c2);
			colors.add(c2);
			colors.add(c1);
			colors.add(c2);
			colors.add(c2);
		}
		
		//add indices
		for (int i=0; i<points.length; ++i) {
			indices.add(7*i + 0);
			indices.add(7*i + 1);
			indices.add(7*i + 2);
			indices.add(7*i + 3);
			indices.add(7*i + 0);
			indices.add(7*i + 4);
			indices.add(7*i + 5);
			indices.add(7*i + 6);
			
			if (i<points.length-1) {
				indices.add(7*i + 1);
				indices.add(7*i + 1 + 7);
				indices.add(7*i + 3);
				indices.add(7*i + 3 + 7);
				indices.add(7*i + 4);
				indices.add(7*i + 4 + 7);
				indices.add(7*i + 6);
				indices.add(7*i + 6 + 7);
			}
		}
		
		//set buffers
		slopeMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions.toArray(new Vector3f[positions.size()])));
		slopeMesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors.toArray(new ColorRGBA[colors.size()])));
		slopeMesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(ArrayUtils.toPrimitive(indices.toArray(new Integer[indices.size()]))));
		slopeMesh.setMode(Mesh.Mode.Lines);
		slopeMesh.updateCounts();
	}
}
