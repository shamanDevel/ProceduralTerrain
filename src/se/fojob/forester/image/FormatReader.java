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

import com.jme3.math.ColorRGBA;
import java.nio.ByteBuffer;

/**
 * The format-reader interface.
 * 
 * @author Andreas
 */
public interface FormatReader {
    
    //Enum of various different channels.
    public enum Channel {Red,Green,Blue,Alpha,Luminance}
    
    /**
     * Get an RGBA color value from an image.
     * 
     * @param position The buffer position.
     * @param buf The bytebuffer of the image.
     * @param store A ColorRGBA object for storing values. This is not the
     * same object that is returned by the method.
     * @return A colorRGBA vector.
     */
    public ColorRGBA getColor(int position, ByteBuffer buf, ColorRGBA store);
    
    /**
     * Get the color value from a specific band.
     * 
     * @param position The buffer position.
     * @param channel The color channel.
     * @param buf The bytebuffer of the image.
     * @return The color value as a float. [0,1]
     */
    public float getColor(int position, Channel channel, ByteBuffer buf);
    
    /**
     * Get the luminance value.
     * 
     * @param position The buffer position
     * @param buf The bytebuffer of the image.
     * @param store see getColor
     * @return The luminance as a float. [0,1]
     */
    public float getLuminance(int position, ByteBuffer buf, ColorRGBA store);
}
