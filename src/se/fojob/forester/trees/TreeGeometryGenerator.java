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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.IntMap.Entry;
import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * Class used to generate tree batches. This class borrows from
 * BatchNode.java.
 * 
 * @author Andreas
 */
public class TreeGeometryGenerator {
    
    //Used when creating buffers
    private Vector3f posTemp;
    private Vector3f normTemp;
    private Vector3f tanTemp;
    private Quaternion rot;
    
    //Temporary arrays to store vertex values
    private float[] baseP = null; //Positions
    private float[] baseN = null; //Normals
    private float[] baseT = null; //Tangents
    private float[] baseX = null; //TexCoords
    
    private float[] tempP = null;
    private float[] tempN = null;
    private float[] tempT = null;
    
    //Booleans
    private boolean usesNormals = false;
    private boolean usesTangents = false;
    //Used to label and keep track of the buffers used by the base mesh.    
    private int[] compsForBuf = null;
    VertexBuffer.Format[] formatForBuf = null;
    //Keep track of how many components there are in tangents.
    private int tanComps = 4;
    
    public TreeGeometryGenerator(){
    }
    
    public Geometry generateStaticGeometry( Geometry baseGeom, 
                                            TreeDataList treeList,
                                            boolean createTanBin
                                          ) 
    {
        Mesh baseMesh = baseGeom.getMesh();
        //DEBUG
        if(treeList.isEmpty()){
            return null;
        }
        //The batch mesh.
        Mesh batchMesh = new Mesh();
        //Only triangle meshes allowed. Simplex component count is always 3.
        batchMesh.setMode(Mesh.Mode.Triangles);
        batchMesh.setDynamic();
        
        //Arrays to keep track of component count and formats for each buffer.
        compsForBuf = new int[VertexBuffer.Type.values().length];
        formatForBuf = new VertexBuffer.Format[compsForBuf.length];
        
        posTemp = new Vector3f();
        normTemp = new Vector3f();
        tanTemp = new Vector3f();
        
        int vertCount = baseMesh.getVertexCount();
        int triCount = baseMesh.getTriangleCount();
        
        int totalVerts = vertCount*treeList.size();
        int totalTris = triCount*treeList.size();  
        
        //Set the arrays with the component number and format for each buffer.
        for (Entry<VertexBuffer> entry : baseMesh.getBuffers()) {
            compsForBuf[entry.getKey()] = entry.getValue().getNumComponents();
            formatForBuf[entry.getKey()] = entry.getValue().getFormat();
        }
        //The number is 2^16 - 1, which is the largest unsigned short.
        if (totalVerts > 65535) {
            // make sure we create an UnsignedInt buffer so
            // we can fit all of the meshes
            formatForBuf[VertexBuffer.Type.Index.ordinal()] = VertexBuffer.Format.UnsignedInt;
        } else {
            formatForBuf[VertexBuffer.Type.Index.ordinal()] = VertexBuffer.Format.UnsignedShort;
        }

        //Generate buffers for the batch mesh.
        for (int i = 0; i < compsForBuf.length; i++) {
            if (compsForBuf[i] == 0) {
                continue;
            }

            Buffer data;
            if (i == VertexBuffer.Type.Index.ordinal()) {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalTris);                
            } else {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalVerts);                
            }

            VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.values()[i]);
            
            vb.setupData(VertexBuffer.Usage.Dynamic, compsForBuf[i], formatForBuf[i], data);
            batchMesh.setBuffer(vb);
            
            //Set up the arrays that will be used.
            if(vb.getBufferType() == VertexBuffer.Type.Position){
                baseP = new float[vertCount*3];
                tempP = new float[vertCount*3];
                FloatBuffer basePos = baseMesh.getFloatBuffer(VertexBuffer.Type.Position);
                basePos.position(0);
                basePos.get(baseP);
            }else if(vb.getBufferType() == VertexBuffer.Type.Normal){
                assert(vb.getNumElements() == vertCount);
                baseN = new float[vertCount*3];
                tempN = new float[vertCount*3];
                FloatBuffer baseNorm = baseMesh.getFloatBuffer(VertexBuffer.Type.Normal);
                baseNorm.position(0);
                baseNorm.get(baseN);
                usesNormals = true;
            }else if(vb.getBufferType() == VertexBuffer.Type.Tangent){
                assert(vb.getNumElements() == vertCount);
                //This is because of tangent parities; sometimes meshes
                //uses them, sometimes they don't.
                tanComps = vb.getNumComponents();
                baseT = new float[vertCount*tanComps];
                tempT = new float[vertCount*tanComps];
                FloatBuffer baseTan= baseMesh.getFloatBuffer(VertexBuffer.Type.Tangent);
                baseTan.position(0);
                baseTan.get(baseT);
                usesTangents = true;
            }else if(vb.getBufferType() == VertexBuffer.Type.TexCoord){
                //The texcoords are not to be manipulated, so no temp
                //array is needed.
                baseX = new float[vertCount*2];
                FloatBuffer baseTex= baseMesh.getFloatBuffer(VertexBuffer.Type.TexCoord);
                baseTex.position(0);
                baseTex.get(baseX);
            }
            
        }//for-loop

        //Keep track of where in the base mesh buffers we currently are.
        int globalVertIndex = 0;
        int globalTriIndex = 0;
        
        //Bounding box initial values.
        //float xMin = 0, xMax = 0, zMin = 0, zMax = 0, yMin = 0, yMax = 0;
        
        for (int i = 0; i < treeList.size(); i++) {
            
            TreeData data = treeList.get(i);
            
            //Create new buffers for each tree data. These buffers end up in
            //the temporary arrays
            generateBuffers(data, vertCount);
            
            for (int bufType = 0; bufType < compsForBuf.length; bufType++) {
                
                VertexBuffer outBuf = batchMesh.getBuffer(VertexBuffer.Type.values()[bufType]);

                if (outBuf == null) {
                    continue;
                }

                if (VertexBuffer.Type.Index.ordinal() == bufType) {
                    int components = compsForBuf[bufType];

                    IndexBuffer inIdx = baseMesh.getIndicesAsList();
                    IndexBuffer outIdx = batchMesh.getIndexBuffer();

                    for (int tri = 0; tri < triCount; tri++) {
                        for (int comp = 0; comp < components; comp++) {
                            int idx = inIdx.get(tri * components + comp) + globalVertIndex;
                            outIdx.put((globalTriIndex + tri) * components + comp, idx);
                        }
                    }
                } else if (VertexBuffer.Type.Position.ordinal() == bufType) {
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    outPos.put(tempP);
                } else if (VertexBuffer.Type.Normal.ordinal() == bufType) {
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    outPos.put(tempN);
                } else if (VertexBuffer.Type.Tangent.ordinal() == bufType){
                    FloatBuffer outTan = (FloatBuffer) outBuf.getData();
                    outTan.put(tempT);
                } else if (VertexBuffer.Type.TexCoord.ordinal() == bufType){
                    FloatBuffer outTex = (FloatBuffer) outBuf.getData();
                    //Just keep feeding it the base mesh texcoords each iteration.
                    outTex.put(baseX);
                }
            }//innerFor
            
            //Increase the counters.
            globalVertIndex += vertCount;
            globalTriIndex += triCount;
            
            //Boundingbox stuff.
            
//            if(data.x > xMax){
//                xMax = data.x;
//            } else if (data.x < xMin){
//                xMin = data.x;
//            }
//            if(data.y > yMax){
//                yMax = data.y;
//            } else if (data.y < yMin){
//                yMin = data.y;
//            }
//            if(data.z > zMax){
//                zMax = data.z;
//            } else if (data.z < zMin){
//                zMin = data.z;
//            }
            
        }//outerFor
        
        //Reset temporary variables
        usesNormals = false;
        usesTangents = false;
        compsForBuf = null;
        formatForBuf = null;
        posTemp = null;
        normTemp = null;
        tanTemp = null;
        baseP = null;
        baseN = null;
        baseT = null;
        baseX = null;
        tempP = null;
        tempN = null;
        tempT = null;
        rot = null;
        
        //Create bounding box.
//        float rad = ((BoundingSphere)baseGeom.getModelBound()).getRadius();
        
        BoundingBox box = new BoundingBox();
//        Vector3f boxCenter = new Vector3f();
//        boxCenter.x = (xMin + xMax)*0.5f;
//        boxCenter.y = (yMin + yMax)*0.5f;
//        boxCenter.z = (zMin + zMax)*0.5f;
//        box.setCenter(boxCenter);
//        box.setXExtent((xMax - xMin)*0.5f + rad);
//        box.setYExtent((yMax - yMin)*0.5f + rad);
//        box.setZExtent((zMax - zMin)*0.5f + rad);
        batchMesh.setBound(box);
//        batchMesh.setBound(new BoundingBox());
        batchMesh.updateBound();
        
        batchMesh.updateCounts();
        batchMesh.setStatic();
        //Create the static geometry and set the material
        Geometry geom = new Geometry(baseGeom.getMaterial().toString(),batchMesh);
        geom.setMaterial(baseGeom.getMaterial().clone());
        geom.setQueueBucket(baseGeom.getQueueBucket());
        
        return geom;
        
    }//generateStaticGeometry
    
    private void generateBuffers(TreeData data, int vertCount) {
        Vector3f pos = posTemp;
        Vector3f norm = normTemp;
        Vector3f tan = tanTemp;
                
        int pIt = 0, nIt = 0, tIt = 0;
        
        rot = new Quaternion().fromAngleNormalAxis(data.rot, Vector3f.UNIT_Y);
        
        for(int i = 0; i < vertCount; i++){
            // Positions
            pos.x = baseP[pIt++];
            pos.y = baseP[pIt++];
            pos.z = baseP[pIt];
            
            //scale
            pos.multLocal(data.scale);
            //rotate
            rot.mult(pos,pos);
            //translate
            pos.addLocal(data.x,data.y,data.z);
            
            pIt -= 2;
            
            tempP[pIt++] = pos.x;
            tempP[pIt++] = pos.y;
            tempP[pIt++] = pos.z;
            
            if(usesNormals){
                norm.x = baseN[nIt++];
                norm.y = baseN[nIt++];
                norm.z = baseN[nIt];
                //rotate
                rot.mult(norm,norm);
            
                nIt -= 2;
                tempN[nIt++] = norm.x;
                tempN[nIt++] = norm.y;
                tempN[nIt++] = norm.z;
            }
            
            if(usesTangents){
                tan.x = baseT[tIt++];
                tan.y = baseT[tIt++];
                tan.z = baseT[tIt];
                //rotate
                rot.mult(tan,tan);
            
                tIt -= 2;
                tempT[tIt++] = tan.x;
                tempT[tIt++] = tan.y;
                tempT[tIt++] = tan.z;
                
                if(tanComps == 4){
                    tIt++;
                }
            }//outerIf
            
        }//for-loop
    }//generateBuffers
    
}//TreeGeometryGenerator
