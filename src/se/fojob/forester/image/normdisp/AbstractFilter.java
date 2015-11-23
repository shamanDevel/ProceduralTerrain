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
package se.fojob.forester.image.normdisp;

import com.jme3.math.Vector3f;

/**
 * Extend this to create new filters.
 * 
 * @author Andreas
 */
public abstract class AbstractFilter implements Filter{
    
    protected KernelSize size;
    protected float[] disp;
    protected int imageWidth;
    protected int imageHeight;
    protected float[] samples = null;
    
    public AbstractFilter(KernelSize size){
        this.size = size;
    }
    
    @Override
    public Vector3f filter(int n, int m){
        sample(n,m);
        return filter();
    }
    
    @Override
    public void init(int width, int height, float[] disp){
        this.imageWidth = width;
        this.imageHeight = height;
        this.disp = disp;
        if(size == KernelSize.CD){
            samples = new float[4];
        } else if(size == KernelSize.K3x3){
            samples = new float[9];
        } else if(size == KernelSize.K5x5){
            samples = new float[25];
        }
    }
    
    protected abstract Vector3f filter();
    
    /**
     * Sample at position n,m.
     * 
     * @param n
     * @param m 
     */
    public void sample(int n, int m){
        if(size == KernelSize.CD){
            samples[0]  = disp[getPos(n - 1, m    )];
            samples[1]  = disp[getPos(n + 1, m    )];
            
            samples[2]  = disp[getPos(n    , m - 1)];
            samples[3]  = disp[getPos(n    , m + 1)];
        }else if (size == KernelSize.K3x3) {
            samples[0]  = disp[getPos(n - 1, m - 1)];
            samples[1]  = disp[getPos(n    , m - 1)];
            samples[2]  = disp[getPos(n + 1, m - 1)];
            
            samples[3] = disp[getPos(n - 1, m    )];
            samples[4] = disp[getPos(n    , m    )];
            samples[5] = disp[getPos(n + 1, m    )];
            
            samples[6] = disp[getPos(n - 1, m + 1)];
            samples[7] = disp[getPos(n    , m + 1)];
            samples[8] = disp[getPos(n + 1, m + 1)];
        } else {
            samples[0]  = disp[getPos(n - 2, m - 2)];
            samples[1]  = disp[getPos(n - 1, m - 2)];
            samples[2]  = disp[getPos(n    , m - 2)];
            samples[3]  = disp[getPos(n + 1, m - 2 )];
            samples[4]  = disp[getPos(n + 2, m - 2 )];
            
            samples[5]  = disp[getPos(n - 2, m - 1)];
            samples[6]  = disp[getPos(n - 1, m - 1)];
            samples[7]  = disp[getPos(n    , m - 1)];
            samples[8]  = disp[getPos(n + 1, m - 1)];
            samples[9]  = disp[getPos(n + 2, m - 1)];
            
            samples[10] = disp[getPos(n - 2, m    )];
            samples[11] = disp[getPos(n - 1, m    )];
            samples[12] = disp[getPos(n    , m    )];
            samples[13] = disp[getPos(n + 1, m    )];
            samples[14] = disp[getPos(n + 2, m    )];
            
            samples[15] = disp[getPos(n - 2, m + 1)];
            samples[16] = disp[getPos(n - 1, m + 1)];
            samples[17] = disp[getPos(n    , m + 1)];
            samples[18] = disp[getPos(n + 1, m + 1)];
            samples[19] = disp[getPos(n + 2, m + 1)];
            
            samples[20] = disp[getPos(n - 2, m + 2)];
            samples[21] = disp[getPos(n - 1, m + 2)];
            samples[22] = disp[getPos(n    , m + 2)];
            samples[23] = disp[getPos(n + 1, m + 2)];
            samples[24] = disp[getPos(n + 2, m + 2)];
        }
    }
    
    //Clamp and pack.
    protected int getPos(int n, int m){
        n = (n < 0) ? 0 : (n > imageWidth - 1) ? imageWidth - 1: n;
        m = (m < 0) ? 0 : (m > imageHeight - 1) ? imageHeight - 1: m;
        return n + imageWidth*m; 
    }
    
}
