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
package se.fojob.forester.grass.algorithms;

import se.fojob.forester.grass.GrassPage;
import se.fojob.forester.image.DensityMap;
import se.fojob.forester.util.FastRandom;
import se.fojob.forester.RectBounds;
import se.fojob.forester.grass.GrassLayer;

/**
 * The default planting algorithm.
 * 
 * @author Andreas
 */
public class GPAUniform implements GrassPlantingAlgorithm {

    public enum Scaling {Linear, Quadratic}
    protected Scaling scaling = Scaling.Linear;
    protected float threshold;
    
    public GPAUniform(){}
    
    public GPAUniform(float threshold){
        this.threshold = threshold;
    }
    
    @Override
    public int generateGrassData(   GrassPage page,
                                    GrassLayer layer,
                                    DensityMap densityMap,
                                    float[] grassData, 
                                    int grassCount
                                ) 
    {
        RectBounds bounds = page.getBounds();
        //Populating the array of locations (and also getting the total amount
        //of quads).
        FastRandom rand = new FastRandom();
        float width = bounds.getWidth();
        //Dens is size width * width.
        float[] dens = densityMap.getDensityUnfiltered(page, layer.getDmChannel());
        //Iterator
        int iIt = 0;

        for (int i = 0; i < grassCount; i++) {
            float x = rand.unitRandom() * (bounds.getWidth() - 0.01f);
            float z = rand.unitRandom() * (bounds.getWidth() - 0.01f);
            
            float d = dens[(int)x + (int)width * (int)z];
            
            if(scaling == Scaling.Quadratic){
                d *= d;
            }
            
            if (rand.unitRandom() + threshold < d ) {
                grassData[iIt++] = x + bounds.getxMin();
                grassData[iIt++] = z + bounds.getzMin();
                grassData[iIt++] = rand.unitRandom();
                //-pi/2 -> pi/2
                grassData[iIt++] = (-0.5f + rand.unitRandom())*3.141593f;
            }
        }
        //The iterator divided by four is the grass-count.
        return iIt/4;
    }

    public Scaling getScaling() {
        return scaling;
    }

    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
}//GPAUniform
