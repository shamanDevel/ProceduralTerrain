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
package se.fojob.forester.util;

import com.jme3.math.Vector3f;

/**
 * This class is used to produce random number seeds. It generates
 * the numbers each time so it isn't a table per se, but acts like one. 
 * 
 * @author Andreas
 */
public class RandomTable {
    
    protected static final FastRandom random = new FastRandom();
    
    /**
     * Generates a long-value.
     *
     * @param loc The xz location as a Vector3f object.
     * @return A long.
     */
    synchronized
    public static long lookup(Vector3f loc, short ID){
        short v1 = (short) loc.x;
        short v2 = (short) loc.z;
        long seed = (long)v1 + (long)(v2 << 16) + ((long)ID << 32);
        random.reSeed(seed);
        return random.next();
    }
    
    /**
     * Offset the table. This value will re-seed the random generator
     * with the given value. This method should not be called after
     * the lookup method has been called, as it will change all values
     * generated by that method.
     * 
     * @param offset A long value used to re-seed the generator.
     */
    synchronized
    public static void offsetTable(long offset){
        random.reSeed(offset);
    }
    
    synchronized
    public static void offsetTable(){
        random.reSeed();
    }
}
