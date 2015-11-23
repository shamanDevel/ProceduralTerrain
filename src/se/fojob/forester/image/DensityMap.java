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
package se.fojob.forester.image;

import com.jme3.math.FastMath;
import com.jme3.texture.Texture;
import se.fojob.forester.image.FormatReader.Channel;
import se.fojob.paging.GeometryPage;

/**
 * This class contains density map data. It borrows from the terrain height
 * map system.
 * 
 * @author Andreas
 */
public class DensityMap extends ImageReader {

    protected int size;
    protected float scale;
    /**
     * Create a density map based on a texture. The luminance values of the
     * pixels will be used as densities [0,1].
     * 
     * @param tex The texture.
     * @param tileSize the tile size used by the grassloader.
     */
    public DensityMap(Texture tex, int tileSize) {
        this.setupImage(tex.getImage());
        this.scale = tileSize/(float)imageWidth;
    }

    /**
     * A method to get density values.
     * 
     * @param page The page object.
     * @param channel The channel to read.
     * @return The density value.
     */
    public float[] getDensityUnfiltered(GeometryPage page, Channel channel) {
        
        int width = (int) page.getBounds().getWidth();
        
        int offsetX = page.getX() * width;
        //Not flipped
        int offsetZ = (int)(imageHeight*scale) - 1 - page.getZ() * width;
        //Get a set of density values from the densityMap;
        float[] dens = new float[width * width];
        
        //TODO A better check.
        if (image.getFormat().getBitsPerPixel() <= 24 && channel == Channel.Alpha) {
            throw new RuntimeException("Image" + image.toString() + "does not contain an alpha channel");
        }
        
        if (channel != Channel.Luminance) {
            for (int j = 0; j < width; j++) {
                for (int i = 0; i < width; i++) {
                    dens[i + width * j] = getColor((int)(scale*(i + offsetX)), (int)(scale*(offsetZ - j)), channel);
                }
            }
        } else if (channel == Channel.Luminance) {
            for (int j = 0; j < width; j++) {
                for (int i = 0; i < width; i++) {
                    dens[i + width * j] = getLuminance((int)(scale*(i + offsetX)), (int)(scale*(offsetZ - j)));
                }
            }
        }
        this.buf.clear();
        return dens;
    }
    
}//DensityMap
