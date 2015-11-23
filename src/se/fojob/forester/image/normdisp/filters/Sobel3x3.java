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
 * This filter is based on the Sobel 3x3 operators.
 * 
 * @author Andreas
 */
public class Sobel3x3 extends AbstractFilter {

    public Sobel3x3(){
        super(KernelSize.K3x3);
    }
    
    /**
     * 
     *       1 0 -1
     * Sx =  2 0 -2
     *       1 0 -1
     *    
     *      -1 -2 -1
     * Sy =  0  0  0
     *       1  2  1
     * 
     * @return Vector with dX,dY and 1
     */
    @Override
    protected Vector3f filter() {
        
        float dX = (samples[0] + 2*samples[3] + samples[6]) - 
                        (samples[2] + 2*samples[5] + samples[8]);
        float dY = (samples[6] + 2*samples[7] + samples[8]) - 
                        (samples[0] + 2*samples[1] + samples[2]);
        
        return new Vector3f(dX,dY,1f);
    }

}
