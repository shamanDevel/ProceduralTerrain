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

import se.fojob.forester.grass.GrassLayer;
import se.fojob.forester.grass.GrassPage;
import se.fojob.forester.image.DensityMap;

/**
 * Algorithm interface. Subject for change.
 * 
 * @author Andreas
 */
public interface GrassPlantingAlgorithm {
    /**
     * This should be an algorithm for generating grass. It should always
     * return the number of grass patches that was generated.
     * 
     * @param page The grass page.
     * @param layer The grasslayer.
     * @param densityMap A density map (or null).
     * @param grassData The array for storing grass data.
     * @param grassCount The initial number of grass patches.
     * @return The number of grass patches after running the algorithm.
     */
    public int generateGrassData(   GrassPage page,
                                    GrassLayer layer,
                                    DensityMap densityMap,
                                    float[] grassData, 
                                    int grassCount
                                );
}
