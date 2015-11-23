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
package se.fojob.forester.grass;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.terrain.Terrain;
import se.fojob.forester.grass.GrassLayer.MeshType;
import se.fojob.forester.image.ColorMap;
import se.fojob.forester.image.DensityMap;
import se.fojob.forester.RectBounds;
import java.nio.Buffer;

/**
 * This class contains a few methods for generating grass meshes.
 * 
 * @author Andreas
 */
public class GrassGeometryGenerator {
    
    protected Terrain terrain;
    
    public GrassGeometryGenerator(Terrain terrain){
        this.terrain = terrain;
    }
    
    /**
     * This method creates a grass geometry. This is this method you call
     * from the grassloader.
     * 
     * @param layer The grasslayer.
     * @param page The grass page.
     * @param densityMap The densitymap (or null).
     * @param colorMap The colormap (or null).
     * @return A batched grass geometry.
     */
    public Geometry createGrassGeometry(GrassLayer layer, 
                                        GrassPage page,
                                        DensityMap densityMap,
                                        ColorMap colorMap
                                        )
    {
        RectBounds bounds = page.getBounds();
        //Calculate the area of the page
        float area = bounds.getWidth()*bounds.getWidth();
            
        //This is the grasscount variable. The initial value is the maximum
        //possible count. It may be reduced by densitymaps, height restrictions
        //and other stuff.
        int grassCount = (int) (area * layer.getDensityMultiplier());
        
        //Each "grass data point" consists of coords (x,z), scale and rotation-angle.
        //That makes 4 data points per patch of grass.
        float[] grassData = new float[grassCount*4];
        
        //The planting algorithm returns the final amount of grass.
        grassCount = layer.getPlantingAlgorithm().generateGrassData(page, layer, densityMap, grassData, grassCount);
        
        Mesh grassMesh = new Mesh();
        
        MeshType meshType = layer.getMeshType();
        
        //No need running this if there's no grass data.
        if(grassCount != 0)
        {
            if(meshType == MeshType.QUADS){
                grassMesh = generateGrass_QUADS(layer,page,grassData,grassCount,colorMap);
            } else if(meshType == MeshType.CROSSQUADS){
                grassMesh = generateGrass_CROSSQUADS(layer,page,grassData,grassCount,colorMap);
            } else if(meshType == MeshType.BILLBOARDS){
                grassMesh = generateGrass_BILLBOARDS(layer,page,grassData,grassCount,colorMap);
            }
        }
        
        grassMesh.setStatic();
        grassMesh.updateCounts();
        Geometry geom = new Geometry();
        geom.setMesh(grassMesh);
        geom.setMaterial(layer.getMaterial().clone());
        geom.setQueueBucket(Bucket.Transparent);
        
        return geom;
    }
    
    /**
     * Method for creating a static quad mesh.
     *
     * @param layer The grass-layer.
     * @param page The page.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry method.
     * @param colorMap The colormap to use (or null).
     * @return A static quad mesh.
     */
    protected Mesh generateGrass_QUADS( GrassLayer layer,
                                        GrassPage page,
                                        float[] grassData, 
                                        int grassCount,
                                        ColorMap colorMap
                                      )
    {
        //The grass mesh
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Triangles);
        
        // ***************** Setting up the mesh buffers. *****************
        
        //Each grass has four positions, each vertice is 3 floats
        float[] positions = new float[grassCount*12];
        //This is the xz normals of the quad.
        float[] normals = new float[grassCount*8];
        //Each grass has got 4 texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount*8];
        
        //Colormap stuff
        float[] colors = null; //for color buffer
        ColorRGBA cols[] = null; //color map values
        ColorRGBA color = null; //placeholder for color values.
        
        boolean useColorMap = false;
        
        if(colorMap != null){
            useColorMap = true;
            colors = new float[grassCount*16];
            cols = colorMap.getColorsUnfiltered(page);
        }
        
        //Slim the mesh down a little.
        Format form = Format.UnsignedShort;
        if (grassCount*4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount*4 > 255){ 
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }
        
        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount*6);           
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);
        
        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();
        
        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();
        
        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position,texcoord, angle and color iterators
        int pIt = 0;
        int tIt = 0;
        int nIt = 0;
        int cIt = 0;
        
        RectBounds bounds = page.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;
        
        int pw = (int)bounds.getWidth();
        
        float xOffset = -page.getCenterPoint().x + pw*0.5f;
        float zOffset = -page.getCenterPoint().z + pw*0.5f;
        
        //Bounding box stuff.
//        float yMin = 0,yMax = 0;
        
        
        //Generating quads
        for(int i = 0; i < grassCount; i++)
        {
            //Position values
            float x = grassData[gIt++];
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            float angle = grassData[gIt++];
            
            float halfScaleX = (minWidth + size*(maxWidth - minWidth))*0.5f;
            float scaleY = minHeight + size*(maxHeight - minHeight);
            
            float xAng = (float)(Math.cos(angle));
            float zAng = (float)(Math.sin(angle));
            
            float xTrans = xAng * halfScaleX;
            float zTrans = zAng * halfScaleX;
            
            float x1 = x - xTrans, z1 = z - zTrans;
            float x2 = x + xTrans, z2 = z + zTrans;
            
            float y1 = terrain.getHeight(new Vector2f(x1,z1));
            float y2 = terrain.getHeight(new Vector2f(x2,z2));
            
            float y1h = y1 + scaleY;
            float y2h = y2 + scaleY;
            
            //Bounding box stuff.
//            float ym = (y1 <= y2) ? y1 : y2;
//            if(ym < yMin ){
//                yMin = ym;
//            }
//            float yM = (y1h >= y2h) ? y1h : y2h;
//            if(yM > yMax){
//                yMax = yM;
//            }
            
            // ******************** Adding vertices ********************** 
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1h;
            positions[pIt++] = z1 - cZ;
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 0.f;    texCoords[tIt++]=1.f;    //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2h;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 1.f;   texCoords[tIt++]=1.f;     //uv
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1;
            positions[pIt++] = z1 - cZ;
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 0.f;  texCoords[tIt++]=0.f;      //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 1.f;  texCoords[tIt++]=0.f;      //uv

            if(useColorMap){
                //Get the map coordinates for x and z.
                int xIdx = (int) (x + xOffset);
                int zIdx = (int) (z + zOffset);
                
                color = cols[xIdx + pw*zIdx];
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
            } 
        }
        
        //************************ Indices **************************
        
        int iIt = 0;
        
        int offset = 0;
        IndexBuffer iB = mesh.getIndexBuffer();
        for(int i = 0; i < grassCount; i++){
            offset = i*4;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);
                
            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);
        }
        
        
        // ******************** Finalizing the mesh ***********************
                
        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.TexCoord2,2, normals);
        if(useColorMap){
            mesh.setBuffer(Type.Color,4,colors);
        }
        
        BoundingBox box = new BoundingBox();
        
//        Vector3f boxCenter = new Vector3f();
//        boxCenter.y = (yMax + yMin)*0.5f;
//        box.setCenter(boxCenter);
//        float extent = (bounds.getWidth() + maxWidth)*0.5f;
//        box.setXExtent(extent);
//        box.setYExtent((yMax - yMin)*0.5f);
//        box.setZExtent(extent);
        
        mesh.setBound(box);
        mesh.updateBound();
        return mesh;
    }
    
    /**
     * Method for creating a static cross-quad mesh.
     * 
     * @param layer The grass-layer.
     * @param page The page.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry method.
     * @param colorMap The colormap to use (or null).
     * @return A static cross-quad mesh.
     */
    protected Mesh generateGrass_CROSSQUADS(GrassLayer layer,
                                            GrassPage page,
                                            float[] grassData, 
                                            int grassCount,
                                            ColorMap colorMap
                                            )
    {
        //The grass mesh
        Mesh mesh = new Mesh();
        
        mesh.setMode(Mesh.Mode.Triangles);        
        
        // ***************** Setting up the mesh buffers. *****************
        
        //Each grass has eight positions, each position is 3 floats.
        float[] positions = new float[grassCount*24];
        //Each grass has got eight texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount*16];
        //This is the angle of the quad.
        float[] normals = new float[grassCount*16];
        
        //Colormap stuff
        float[] colors = null;
        ColorRGBA cols[] = null;
        ColorRGBA color = null;
        
        boolean useColorMap = false;
        if(colorMap != null){
            useColorMap = true;
            //Each grass has got four vertices, each vertice has one color, each
            //color is 4 floats.
            colors = new float[grassCount*32];
            cols = colorMap.getColorsUnfiltered(page);
        }
        
        //Slim the mesh down a little.
        Format form = Format.UnsignedShort;
        if (grassCount*4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount*4 > 255){ 
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }
        
        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount*12);           
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);
        
        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();
        
        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();
        
        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position, texcoord, angle and color iterators
        int pIt = 0;
        int tIt = 0;
        int nIt = 0;
        int cIt = 0;
        
        RectBounds bounds = page.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;
        
        int pw = (int) bounds.getWidth();
        
        float xOffset = -page.getCenterPoint().x + pw*(page.getX() + 0.5f);
        float zOffset = -page.getCenterPoint().z + pw*(page.getZ() + 0.5f);
        
        //Bounding box stuff.
//        float yMin = 0, yMax = 0;
        
        //Generating quads
        for(int i = 0; i < grassCount; i++)
        {
            //Position values
            float x = grassData[gIt++];
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            float angle = grassData[gIt++];
            
            float halfScaleX = (minWidth + size*(maxWidth - minWidth))*0.5f;
            float scaleY = minHeight + size*(maxHeight - minHeight);
            
            float xAng = (float)(Math.cos(angle));
            float zAng = (float)(Math.sin(angle));
            
            float xTrans = xAng * halfScaleX;
            float zTrans = zAng * halfScaleX;
            
            float x1 = x - xTrans, z1 = z - zTrans;
            float x2 = x + xTrans, z2 = z + zTrans;
            float x3 = x + zTrans, z3 = z - xTrans;
            float x4 = x - zTrans, z4 = z + xTrans;
            
            float y1 = terrain.getHeight(new Vector2f(x1,z1)); 
            float y2 = terrain.getHeight(new Vector2f(x2,z2));
            float y3 = terrain.getHeight(new Vector2f(x3,z3));
            float y4 = terrain.getHeight(new Vector2f(x4,z4));
            
            float y1h = y1 + scaleY;
            float y2h = y2 + scaleY;
            float y3h = y3 + scaleY;
            float y4h = y4 + scaleY;
            
            //Bounding box stuff.
//            float ym1 = (y1 <= y2) ? y1 : y2;
//            float ym2 = (y3 <= y4) ? y3 : y4;
//            float ym = (ym1 <= ym2) ? ym1 : ym2;
//            if(ym < yMin ){
//                yMin = ym;
//            }
//            
//            float yM1 = (y1h >= y2h) ? y1h : y2h;
//            float yM2 = (y3h >= y4h) ? y3h : y4h;
//            float yM = (yM1 >= yM2) ? yM1 : yM2;
//            if(yM > yMax){
//                yMax = yM;
//            }
            
            //************Generate the first quad**************
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1h;
            positions[pIt++] = z1 - cZ;
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 0.f;    texCoords[tIt++]=1.f;    //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2h;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 1.f;   texCoords[tIt++]=1.f;     //uv
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1; 
            positions[pIt++] = z1 - cZ; 
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 0.f;  texCoords[tIt++]=0.f;      //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;    normals[nIt++] = -xAng;   //xz normal
            texCoords[tIt++] = 1.f;  texCoords[tIt++]=0.f;      //uv
            
            //************Generate the second quad**************
            
            positions[pIt++] = x3 - cX;                         //pos
            positions[pIt++] = y3h;
            positions[pIt++] = z3 - cZ;
            
            normals[nIt++] = xAng;    normals[nIt++] = zAng;    //xz normal
            texCoords[tIt++] = 0.f;    texCoords[tIt++]=1.f;    //uv
            
            positions[pIt++] = x4 - cX;                         //pos
            positions[pIt++] = y4h;
            positions[pIt++] = z4 - cZ;
            
            normals[nIt++] = xAng;    normals[nIt++] = zAng;    //xz normal
            texCoords[tIt++] = 1.f;   texCoords[tIt++]=1.f;     //uv
            
            positions[pIt++] = x3 - cX;                         //pos
            positions[pIt++] = y3; 
            positions[pIt++] = z3 - cZ;
            
            normals[nIt++] = xAng;    normals[nIt++] = zAng;    //xz normal
            texCoords[tIt++] = 0.f;  texCoords[tIt++]=0.f;      //uv
            
            positions[pIt++] = x4 - cX;                         //pos
            positions[pIt++] = y4;
            positions[pIt++] = z4 - cZ;
            
            normals[nIt++] = xAng;    normals[nIt++] = zAng;    //xz normal
            texCoords[tIt++] = 1.f;  texCoords[tIt++]=0.f;      //uv

            if(useColorMap){
                //Get the map coordinates for x and z.
                int xIdx = (int) (x + xOffset);
                int zIdx = (int) (z + zOffset);
                
                color = cols[xIdx + pw*zIdx];
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
            }
        }
        
        //Indices
        int iIt = 0;
        
        int offset = 0;
        IndexBuffer iB = mesh.getIndexBuffer();
        for(int i = 0; i < grassCount; i++){
            offset = i*8;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);
                
            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);
            
            iB.put(iIt++, 4 + offset);
            iB.put(iIt++, 6 + offset);
            iB.put(iIt++, 5 + offset);
                
            iB.put(iIt++, 5 + offset);
            iB.put(iIt++, 6 + offset);
            iB.put(iIt++, 7 + offset);
        }
        
        //********************* Finalizing the mesh ***********************
        
        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.TexCoord2,2, normals);
        
        if(useColorMap){
            mesh.setBuffer(Type.Color,4,colors);
        }
        
        BoundingBox box = new BoundingBox();
        
//        Vector3f boxCenter = Vector3f.ZERO;
//        boxCenter.y = (yMax + yMin)*0.5f;
//        box.setCenter(boxCenter);
//        float extent = (bounds.getWidth() + maxWidth)*0.5f;
//        box.setXExtent(extent);
//        box.setYExtent((yMax - yMin)*0.5f);
//        box.setZExtent(extent);
        
        mesh.setBound(box);
        mesh.updateBound();
        return mesh;
    }
    
    /**
     * Method for creating a billboarded quad mesh. Billboarded quad meshes
     * requires a certain type of shader to work.
     * 
     * @param layer The grass-layer.
     * @param page The page.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry method.
     * @param colorMap The colormap to use (or null).
     * @return A billboarded quad mesh.
     */
    protected Mesh generateGrass_BILLBOARDS(    GrassLayer layer,
                                                GrassPage page,
                                                float[] grassData, 
                                                int grassCount,
                                                ColorMap colorMap
                                           )
    {
        Mesh mesh = new Mesh();
        
        mesh.setMode(Mesh.Mode.Triangles);        
        
        // ***************** Setting up the mesh buffers. *****************
        
        //Each grass has four positions, each vertice is 3 floats
        float[] positions = new float[grassCount*12];
        //Each grass has got 4 texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount*8];
        //Each vertex need a texCoord for displacement data.
        float[] texCoords2 = new float[grassCount*8];
        
        float[] colors = null;
        
        //Colormap stuff
        ColorRGBA cols[] = null;
        ColorRGBA color = null;
        
        boolean useColorMap = false;
        if(colorMap != null){
            useColorMap = true;
            colors = new float[grassCount*16];
            cols = colorMap.getColorsUnfiltered(page);
        }
        
        //Slim the mesh down a little.
        Format form = Format.UnsignedShort;
        if (grassCount*4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount*4 > 255){ 
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }
        
        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount*6);           
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);
        
        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();
        
        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();
        
        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position, texcoord and color iterators
        int pIt = 0;
        int tIt = 0;
        int t2It = 0;
        int cIt = 0;
        
        
        RectBounds bounds = page.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;
        
        int pw = (int) bounds.getWidth();
        
        float xOffset = -page.getCenterPoint().x + pw*(page.getX() + 0.5f);
        float zOffset = -page.getCenterPoint().z + pw*(page.getZ() + 0.5f);
        
//        float yMin = 0, yMax = 0;
    
        //Generating quads
        for(int i = 0; i < grassCount; i++)
        {
            //Position values
            float x = grassData[gIt++];
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            //Not using angle.
            gIt++;
            
            float halfScaleX = (minWidth + size*(maxWidth - minWidth))*0.5f;
            float scaleY = minHeight + size*(maxHeight - minHeight);
            
            float y = terrain.getHeight(new Vector2f(x,z));
            
            //Bounding box stuff.
//            if(y < yMin){
//                yMin = y;
//            }
//            float yh = y + scaleY;
//            if(yh > yMax){
//                yMax = yh; 
//            }
            
            float xx = x - cX;
            float zz = z - cZ;
            // ******************** Adding vertices ********************** 
            
            positions[pIt++] = xx;                          //pos
            positions[pIt++] = y;
            positions[pIt++] = zz;
            
            texCoords[tIt++] = 0.f;    texCoords[tIt++]=1.f;    //uv
            texCoords2[t2It++] = -halfScaleX;   texCoords2[t2It++] = scaleY; //disp
            
            positions[pIt++] = xx;                          //pos
            positions[pIt++] = y;
            positions[pIt++] = zz;
            
            texCoords[tIt++] = 1.f;   texCoords[tIt++]=1.f;     //uv
            texCoords2[t2It++] = halfScaleX;   texCoords2[t2It++] = scaleY; //disp
            
            positions[pIt++] = xx;                          //pos
            positions[pIt++] = y; 
            positions[pIt++] = zz; 
            
            
            texCoords[tIt++] = 0.f;  texCoords[tIt++]=0.f;      //uv
            texCoords2[t2It++] = -halfScaleX;   texCoords2[t2It++] = 0.f; //disp
            
            positions[pIt++] = xx;                          //pos
            positions[pIt++] = y;
            positions[pIt++] = zz;
            
            
            texCoords[tIt++] = 1.f;  texCoords[tIt++]=0.f;      //uv
            texCoords2[t2It++] = halfScaleX;   texCoords2[t2It++] = 0.f; //disp

            if(useColorMap){
                //Get the map coordinates for x and z.
                int xIdx = (int) (x + xOffset);
                int zIdx = (int) (z + zOffset);
                
                color = cols[xIdx + pw*zIdx];
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
                
                colors[cIt++] = color.r;
                colors[cIt++] = color.g;
                colors[cIt++] = color.b;
                colors[cIt++] = 1.f;
            }
        }
        
        //Indices
        int iIt = 0;
        
        int offset = 0;
        IndexBuffer iB = mesh.getIndexBuffer();
        for(int i = 0; i < grassCount; i++){
            offset = i*4;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);
                
            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);
        }
        
        // ******************** Finalizing the mesh ***********************
                
        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.TexCoord2,2, texCoords2);
        if(useColorMap){
            mesh.setBuffer(Type.Color,4,colors);
        }
        BoundingBox box = new BoundingBox();
        
//        Vector3f boxCenter = Vector3f.ZERO;
//        boxCenter.y = (yMax + yMin)*0.5f;
//        box.setCenter(boxCenter);
//        float extent = (bounds.getWidth() + maxWidth)*0.5f;
//        box.setXExtent(extent);
//        box.setYExtent((yMax - yMin)*0.5f);
//        box.setZExtent(extent);
        
        mesh.setBound(box);
        mesh.updateBound();
        
        return mesh;
    }
    
}//GrassGeometryGenerator
