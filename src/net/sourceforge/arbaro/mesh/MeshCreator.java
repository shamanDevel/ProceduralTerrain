//  #**************************************************************************
//  #
//  #    Copyright (C) 2003-2006  Wolfram Diestel
//  #
//  #    This program is free software; you can redistribute it and/or modify
//  #    it under the terms of the GNU General Public License as published by
//  #    the Free Software Foundation; either version 2 of the License, or
//  #    (at your option) any later version.
//  #
//  #    This program is distributed in the hope that it will be useful,
//  #    but WITHOUT ANY WARRANTY; without even the implied warranty of
//  #    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  #    GNU General Public License for more details.
//  #
//  #    You should have received a copy of the GNU General Public License
//  #    along with this program; if not, write to the Free Software
//  #    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  #
//  #    Send comments and bug fixes to diestel@steloj.de
//  #
//  #**************************************************************************/

package net.sourceforge.arbaro.mesh;

import java.util.Enumeration;
import net.sourceforge.arbaro.export.Progress;
import net.sourceforge.arbaro.export.Console;
import net.sourceforge.arbaro.transformation.Vector;
import net.sourceforge.arbaro.tree.*;

/**
 * Create a mesh from the tree's stems using then TreeTraversal interface
 * 
 * @author wolfram
 *
 */

class MeshCreator implements TreeTraversal {
	Mesh mesh;
	Tree tree;
	Progress progress;
	int level; // only stems of this level should be created
	boolean useQuads;
//	Params params;
	
	//private Stack meshparts;

	public MeshCreator(/*Params params,*/ Mesh mesh, int level, boolean useQuads, 
			Progress progress) {
		super();
//		this.params = params;
		this.mesh=mesh;
		this.level=level;
		this.useQuads = useQuads;
		this.progress = progress;
	}
	
	public boolean enterStem(Stem stem) {
		// TODO instead of addToMesh, the traversal should
		// proceed into segments and subsegments itself
		// removing all mesh creation code from Stem, Segment, 
		// Subsegment
//		System.out.println("enter stem "+stem.getTreePosition()+" at level "+stem.getLevel());

		if (level >= 0 && stem.getLevel() < level) {
			return true; // look further for stems
			
		} else if (level >= 0 && stem.getLevel() > level) {
			return false; // go back to higher level
			
		} else { 
//			try {
				// FIXME: for better performance create only
				// one MeshPartCreator and change stem for every stem
				MeshPartCreator partCreator = new MeshPartCreator(stem, /*params,*/ useQuads);
				MeshPart meshpart = partCreator.createMeshPart(progress);
				if (meshpart != null) {
					rewriteUVCoordinates(meshpart);
					mesh.addMeshpart(meshpart);
				}
				
				// show progress
				if (stem.getLevel()<=1 && ! stem.isClone()) 
					Console.progressChar();
				progress.incProgress(1);
				return true; // proceed
				
//			} catch(Exception e) {
//				e.printStackTrace(System.err);
//				throw new TraversalException(e.toString());
//			}
		}	
	}
	
	private static void rewriteUVCoordinates(MeshPart meshpart) {
		Stem stem = meshpart.getStem();
//		System.out.println("min="+stem.getMinPoint()+" max="+stem.getMaxPoint()+" length="+stem.getLength()+" vertices="+meshpart.vertexCount());
//		assert (meshpart.uvCount() == meshpart.vertexCount());
		Vector N = stem.getMaxPoint().sub(stem.getMinPoint()).normalize();
		Vector W = stem.getMinPoint().add(new Vector(1, 0, 0));
		W = W.sub(N.mul(N.getX()*W.getX() + N.getY()*W.getY() + N.getZ()*W.getZ()));
		Vector U = new Vector(N.getY()*W.getZ() - N.getZ()*W.getY(), N.getZ()*W.getX() - N.getX()*W.getZ(), N.getX()*W.getY() - N.getY()*W.getX());
//		Enumeration<UVVector> e1 = meshpart.allVertices(true);
		Enumeration<Vertex> e2 = meshpart.allVertices(false);
		for (; e2.hasMoreElements(); ) {
//			UVVector tex = e1.nextElement();
			Vertex vertex = e2.nextElement();
			Vector vec = vertex.point.sub(stem.getMinPoint());
			//project vec on N to get v
			double texV = N.getX()*vec.getX() + N.getY()*vec.getY() + N.getZ()*vec.getZ();
			//project on the plane to find the angle
			Vector w = vec.sub(N.mul(texV));
			double dx = w.getX()*W.getX() + w.getY()*W.getY() + w.getZ()*W.getZ();
			double dy = w.getX()*U.getX() + w.getY()*U.getY() + w.getZ()*U.getZ();
			double texU = (Math.atan2(dy, dx)+Math.PI)/(4*Math.PI);
			//set tex coordinates
			vec.u = texU;
			vec.v = texV * stem.getLength();
		}
	}
	
	public boolean enterTree(Tree tree) {
		this.tree = tree; 
		return true;
	}

	public boolean leaveStem(Stem stem) {
		return true;
	}

	public boolean leaveTree(Tree tree) {
		return true; // Mesh created successfully
	}

	public boolean visitLeaf(Leaf leaf) {
		// TODO Auto-generated method stub
		return false;
	}
}
