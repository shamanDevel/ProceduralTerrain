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
package se.fojob.forester.image.formatreader;

import com.jme3.math.ColorRGBA;
import se.fojob.forester.image.AbstractFormatReader;
import java.nio.ByteBuffer;

/**
 * An RGB8 format reader.
 * 
 * @author Andreas
 */
public class RDR_RGBA8 extends AbstractFormatReader{

    @Override
    public ColorRGBA getColor(int position, ByteBuffer buf, ColorRGBA store) {
        buf.position( position * 4 );
        store.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()));
        return store.clone();
    }

    @Override
    public float getColor(int position, Channel channel, ByteBuffer buf) {
        float f = 0;
        switch(channel){
            case Red:
                buf.position(position*4);
                f = byte2float(buf.get());
                break;
            case Green:
                buf.position(position*4 + 1);
                f = byte2float(buf.get());
                break;
            case Blue:
                buf.position(position*4 + 2);
                f = byte2float(buf.get());
                break;
            case Alpha:
                buf.position(position*4 + 3);
                f = byte2float(buf.get());
                break;
            default:
                throw new UnsupportedOperationException("Image does not contain channel.");
        }
        return f;
    }
    
    @Override
    public float getLuminance(int position, ByteBuffer buf, ColorRGBA store) {
        buf.position( position * 4 );
        store.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()));
        return this.calculateLuminance(store.r, store.g, store.b, store.a);
    }
    
}
