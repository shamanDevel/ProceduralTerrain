/*
 * Copyright (c) 2011, Andreas Olofsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package se.fojob.forester.trees;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;

/**
 * Not finished.
 * 
 * @author Andreas
 */
public class TreeImpostorGenerator {
        
    public TreeImpostorGenerator(){
        
    }
    
    public Geometry buildImpostorGeometry(TreeDataList list, Node tree, Material mat)
    {
        Mesh mesh = new Mesh();
        
        mesh.setMode(Mesh.Mode.Triangles);
        mesh.setDynamic();
        
        //Position float array.
        float[] positions = new float[12*list.size()];
        float[] texCoords = new float[8*list.size()];
        float[] texCoords2 = new float[positions.length];
        int[] indices = new int[6*list.size()];
        
        BoundingSphere treeSphere = (BoundingSphere) tree.getWorldBound();
        Vector3f center = treeSphere.getCenter();
        
        float rad = treeSphere.getRadius();
        
        //Iterators
        int pIt = 0;
        int tIt = 0;
        int iIt = 0;
        
        for(int i = 0; i < list.size(); i++){
            //Position values
            TreeData data = list.get(i);
            
            //Left to right
            float x0 = -rad;
            float x1 = rad;
            //Top to bottom
            float y0 = rad; 
            float y1 = -rad;
            
            // ******************** Adding vertices ********************** 
            
            //All four verts in a quad is at the same position.
            for(int j = 0; j < 4; j++){
                positions[pIt++] = data.x + center.x;
                positions[pIt++] = data.y + center.y;
                positions[pIt++] = data.z + center.z;
            }
            
            //Texcoords for each vert + the position relative to the quad center
            //stored in texCoord2.
            texCoords[tIt] = 0.f;
            texCoords2[tIt++] = x0;
            texCoords[tIt]=0.f;
            texCoords2[tIt++] = y0;
            texCoords2[tIt++] = data.rot;
            
            texCoords[tIt] = 1.f;
            texCoords2[tIt++] = x1;
            texCoords[tIt]=0.f;
            texCoords2[tIt++] = y0;
            texCoords2[tIt++] = data.rot;
            
            texCoords[tIt] = 0.f;  
            texCoords2[tIt++] = x0;
            texCoords[tIt]=1.f;
            texCoords2[tIt++] = y1;
            texCoords2[tIt++] = data.rot;
            
            texCoords[tIt] = 1.f;  
            texCoords2[tIt++] = x1;
            texCoords[tIt]=1.f;
            texCoords2[tIt++] = y1;
            texCoords2[tIt++] = data.rot;

            //Indices
            int offset = i*4;
            //First triangle
            indices[iIt++] = 0 + offset;
            indices[iIt++] = 2 + offset;
            indices[iIt++] = 1 + offset;
            //Second triangle
            indices[iIt++] = 1 + offset; 
            indices[iIt++] = 2 + offset;
            indices[iIt++] = 3 + offset;
        }
        
        mesh.setBuffer(VertexBuffer.Type.Position, 3, positions);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texCoords);
        mesh.setBuffer(VertexBuffer.Type.TexCoord2, 3, texCoords2);
        mesh.setBuffer(VertexBuffer.Type.Index, 1, indices);
        
        //The mesh is now finished.
        mesh.setBound(new BoundingBox());
        mesh.updateBound();
        mesh.updateCounts();
        mesh.setStatic();
        
        Geometry geom = new Geometry("ImpostorGeom",mesh);
        geom.setMaterial(mat);
        //This geometry always use an alpha texture.
        geom.setQueueBucket(Bucket.Transparent);
        return geom;
    }
}//TreeImpostorGenerator
