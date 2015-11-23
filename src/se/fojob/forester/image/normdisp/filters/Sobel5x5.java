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
package se.fojob.forester.image.normdisp.filters;

import se.fojob.forester.image.normdisp.AbstractFilter;
import com.jme3.math.Vector3f;

/**
 * This is a filter based on the Sobel 5x5 operators.
 * 
 * @author Andreas
 */
public class Sobel5x5 extends AbstractFilter{

    public Sobel5x5(){
        super(KernelSize.K5x5);
    }
    
    /**
     * 
     *          1   2   0   -2  -1
     *          4   8   0   -8  -4
     * Sx =     6   12  0   -12 -6
     *          4   8   0   -8  -4
     *          1   2   0   -2  -1
     * 
     *         -1  -4  -6  -4  -1
     *         -2  -8  -12 -8  -2
     * Sy =     0   0   0   0   0
     *          2   8   12  8   2
     *          1   4   6   4   1
     * 
     * @return Vector with dX,dY and 1
     */
    @Override
    protected Vector3f filter() {
        float dX = (samples[0] + 4f*samples[5] + 6f*samples[10] + 4f*samples[15] + samples[20]) +
                   (2f*samples[1] + 8f*samples[6] + 12f*samples[11] + 8f*samples[16] + 2f*samples[21]) -
                   (2f*samples[3] + 8f*samples[8] + 12f*samples[13] + 8f*samples[18] + 2f*samples[23]) -
                   (samples[4] + 4f*samples[9] + 6f*samples[14] + 4f*samples[19] + samples[24]);
        
        float dY = (samples[20] + 4f*samples[21] + 6f*samples[22] + 4f*samples[23] + samples[24]) +
                   (2f*samples[15] + 8f*samples[16] + 12f*samples[17] + 8f*samples[18] + 2f*samples[19]) -
                   (2f*samples[5] + 8f*samples[6] + 12f*samples[7] + 8f*samples[8] + 2f*samples[9]) -
                   (samples[0] + 4f*samples[1] + 6f*samples[2] + 4f*samples[3] + samples[4]);
        
        return new Vector3f(dX,dY,1f);
    }
    
}
