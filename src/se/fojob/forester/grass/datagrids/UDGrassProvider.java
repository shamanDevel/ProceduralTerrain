/*
 * Copyright (c) 2012, Andreas Olofsson
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
package se.fojob.forester.grass.datagrids;

import se.fojob.forester.MapBlock;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import se.fojob.forester.grass.GrassLoader;
import se.fojob.forester.grass.GrassTile;
import se.fojob.forester.image.DensityMap;
import java.nio.ByteBuffer;

/**
 *
 * @author Andreas
 */
public class UDGrassProvider implements MapProvider {
    protected GrassLoader grassLoader;
    protected DensityMap map;
    protected int xMin,xMax,zMin,zMax;
    protected boolean useBounds;

    public UDGrassProvider(GrassLoader grassLoader){
        this.grassLoader = grassLoader;
        int size = grassLoader.getTileSize();
        byte[] array = new byte[size*size];
        for(int i = 0; i < array.length; i++){
            array[i] = (byte)127;
        }
        ByteBuffer buf = BufferUtils.createByteBuffer(array.length);
        Image image = new Image(Format.Luminance8,size,size,buf);
        Texture tex = new Texture2D();
        tex.setImage(image);
        tex.setName("UDGrassTex");
        map = new DensityMap(tex,size);
    }
    
    @Override
    public MapBlock getMaps(GrassTile tile) {
        int x = tile.getX();
        int z = tile.getZ();
        if(x < xMin || x > xMax || z < zMin || z > zMax){
            return null;
        }
        MapBlock block = new MapBlock();
        //Just feed it the uniform map.
        for(int i = 0; i < grassLoader.getLayers().size(); i++){
            block.getDensityMaps().put(i, map);
        }
        return block;
    }
    
    public void setBounds(int xMin, int xMax, int zMin, int zMax){
        this.xMin = xMin;
        this.xMax = xMax;
        this.zMin = zMin;
        this.zMax = zMax;
        useBounds = true;
    }
    
    public void setUseBounds(boolean useBounds){
        this.useBounds = useBounds;
    }
    
}
