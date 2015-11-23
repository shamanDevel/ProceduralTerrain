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
import com.jme3.texture.Image;
import se.fojob.forester.image.FormatReader.Channel;
import se.fojob.forester.image.formatreader.RDR_ABGR8;
import se.fojob.forester.image.formatreader.RDR_BGR8;
import se.fojob.forester.image.formatreader.RDR_L16;
import se.fojob.forester.image.formatreader.RDR_L8;
import se.fojob.forester.image.formatreader.RDR_RGB8;
import se.fojob.forester.image.formatreader.RDR_RGBA8;
import java.nio.ByteBuffer;

/**
 * Class used for reading jME images. Borrows heavily from the terrain
 * classes (the way they set up their image reading system).
 * 
 * @author Andreas
 */
public class ImageReader {
    
    protected Image image;
    protected int imageWidth;
    protected int imageHeight;
    protected ByteBuffer buf;
    protected ColorRGBA store = new ColorRGBA();
    protected FormatReader fReader;
    
    public ImageReader(){}
    
    public void setupImage(Image image){
        this.image = image;
        this.imageWidth = image.getWidth();
        this.imageHeight = image.getHeight();
        this.buf = image.getData(0);
        switch(image.getFormat()){
            case ABGR8:
                fReader = new RDR_ABGR8();
                break;
            case RGBA8:
                fReader = new RDR_RGBA8();
                break;
            case BGR8:
                fReader = new RDR_BGR8();
                break;
            case RGB8:
                fReader = new RDR_RGB8();
                break;
            case Luminance8:
                fReader = new RDR_L8();
                break;
            case Luminance16F:
                fReader = new RDR_L16();
                break;
            default:
                throw new UnsupportedOperationException("Image format not supported: " + image.getFormat());
        }
    }
    
    /**
     * Get color at position x,y.
     * 
     * @param x Position x
     * @param y Position y
     * @return The colorvalue.
     */
    public ColorRGBA getColor(int x,int y){
        int position = x + imageWidth*y;
        return fReader.getColor(position, buf, store);
    }
    
    /**
     * Get value of channel "channel" at position x,y.
     * 
     * @param x Position x
     * @param y Position y
     * @param channel The channel
     * @return The color value as a float.
     */
    public float getColor(int x, int y, Channel channel){
        int position = x + imageWidth*y;
        return fReader.getColor(position, channel, buf);
    }
    
    /**
     * Get luminance value at x,y.
     * 
     * @param x Position x
     * @param y Position y
     * @return The luminance value as a float.
     */
    public float getLuminance(int x, int y){
        int position = x + imageWidth*y;
        return fReader.getLuminance(position, buf, store);
    }
}
