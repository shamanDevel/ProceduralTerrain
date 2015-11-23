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
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import se.fojob.forester.image.ImageReader;
import se.fojob.forester.image.normdisp.filters.CentralDifference;
import se.fojob.forester.image.normdisp.filters.Prewitt3x3;
import se.fojob.forester.image.normdisp.filters.Sobel3x3;
import se.fojob.forester.image.normdisp.filters.Sobel5x5;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;

/**
 * This Class generates normal maps and displacement maps from
 * jME textures.
 * 
 * @author Andreas
 */
public class NormDispGenerator extends ImageReader{
    
    public enum Filtering { Sobel3x3 , Sobel5x5, Prewitt3x3, CentralDifference}
    
    protected Filter filter;
    protected float normalStrength = 1.0f;
    protected boolean invertedDisp = false;
    protected String assetDir = "Assets/";
    
    /**
     * Default constructor
     */
    public NormDispGenerator(){
    }
    
    /**
     * 
     * @param filter The filter to use when calculating normals.
     * @param useInvertedDisp Use inverted displacement.
     */
    public NormDispGenerator(Filter filter, float normalStrength, boolean useInvertedDisp){
        this.filter = filter;
        this.normalStrength = normalStrength;
        this.invertedDisp = useInvertedDisp;
    }
    
    /**
     * 
     * @param filter The filter to use when calculating normals.
     */
    public void setFilter(Filter filter){
        this.filter = filter;
    }
    
    /**
     * 
     * @param filtering The type of filter to use when calculating normals.
     */
    public void setFilter(Filtering filtering){
        if(filtering == Filtering.Sobel3x3){
            filter = new Sobel3x3();
        } else if(filtering == Filtering.Sobel5x5){
            filter = new Sobel5x5();
        } else if(filtering == Filtering.Prewitt3x3){
            filter = new Prewitt3x3();
        } else if(filtering == Filtering.CentralDifference){
            filter = new CentralDifference();
        }
    }
    
    /**
     * Generates a normal map with displacement data in the alpha channel.
     * 
     * @param tex The texture used as base.
     * @param flipX Used to flip the texture.
     * @param flipY Used to flip the texture.
     * @param normalStrength Edges becomes more accented with a higher normalStrength.
     */
    public void generateNormDispMap(Texture tex, boolean flipX, boolean flipY, float normalStrength){
        this.normalStrength = normalStrength;
        float[] disp = generateDispMap(tex,flipX,flipY);
        byte[] normals = generateNormMap(disp);
        byte[] normDisp = new byte[4*imageWidth*imageHeight];
        
        for(int i = 0; i < imageWidth*imageHeight; i++){
            normDisp[4*i]       = normals[3*i];
            normDisp[4*i + 1]   = normals[3*i + 1];
            normDisp[4*i + 2]   = normals[3*i + 2];
            normDisp[4*i + 3]   = (byte) (disp[i]*255);
        }
        printImage(normDisp,4,tex);
    }
    
    /**
     * Generates a normal map with displacement data in the alpha channel.
     * 
     * @param tex The texture used as base.
     * @param flipX Used to flip the texture.
     * @param flipY Used to flip the texture.
     */
    public void generateNormDispMap(Texture tex, boolean flipX, boolean flipY){
        generateNormDispMap(tex,flipX,flipY,1.0f);
    }
    
    /**
     * Generates a normal map with displacement data in the alpha channel.
     * 
     * @param tex The texture used as base.
     */
    public void generateNormalDispMap(Texture tex){
        generateNormDispMap(tex,false,false,1.0f);
    }
    
    /**
     * Generates a displacement map from a texture.
     * 
     * @param tex The texture used as base.
     * @param flipX Used to flip the texture.
     * @param flipY Used to flip the texture.
     */
    public void generateDisplacementMap(Texture tex, boolean flipX, boolean flipY){
        float[] disp = generateDispMap(tex,flipX,flipY);
        byte[] dispByte = new byte[disp.length];
        for(int i = 0; i < disp.length; i++){
            dispByte[i] = (byte) (disp[i]*255);
        }
        printImage(dispByte,1,tex);
    }
    
    /**
     * Generates a displacement map from a texture.
     * 
     * @param tex The texture used as base.
     */
    public void generateDisplacementMap(Texture tex){
        generateDisplacementMap(tex,false,false);
    }
    
    protected float[] generateDispMap(Texture tex, boolean flipX, boolean flipY){
        setupImage(tex.getImage());
        float[] disp = new float[imageWidth*imageHeight];
        
        int index = 0;
        if (flipY) {
            for (int h = 0; h < imageHeight; ++h) {
                if (flipX) {
                    for (int w = imageWidth - 1; w >= 0; --w) {
                        if(invertedDisp){
                            disp[index++] = (1.0f - getLuminance(w,h));
                        } else
                            disp[index++] = getLuminance(w,h);
                    }
                } else {
                    for (int w = 0; w < imageWidth; ++w) {
                        if(invertedDisp){
                            disp[index++] = (1.0f - getLuminance(w,h));
                        } else
                            disp[index++] = getLuminance(w,h);
                    }
                }
            }
        } else {
            for (int h = imageHeight - 1; h >= 0; --h) {
                if (flipX) {
                    for (int w = imageWidth - 1; w >= 0; --w) {
                        if(invertedDisp){
                            disp[index++] = (1.0f - getLuminance(w,h));
                        } else
                            disp[index++] = getLuminance(w,h);
                    }
                } else {
                    for (int w = 0; w < imageWidth; ++w) {
                        if(invertedDisp){
                            disp[index++] = (1.0f - getLuminance(w,h));
                        } else
                            disp[index++] = getLuminance(w,h);
                    }
                }
            }
        }
        return disp;
    }
    
    /**
     * Generates a normal map from a base texture. The normal map is based
     * on a displacement map that is automatically generated beforehand. Both
     * maps are printed as images.
     * 
     * @param tex The texture used as base.
     * @param flipX Used to flip the texture.
     * @param flipY Used to flip the texture.
     * @param normalStrength Edges becomes more accented with a higher normalStrength.
     */
    public void generateNormalMap(Texture tex, boolean flipX, boolean flipY, float normalStrength){
        this.normalStrength = normalStrength;
        float[] disp = generateDispMap(tex,flipX,flipY);
        byte[] normals = generateNormMap(disp);
        byte[] dispByte = new byte[disp.length];
        for(int i = 0; i < disp.length; i++){
            dispByte[i] = (byte) (disp[i]*255);
        }
        //Don't print another displacement map if image uses a grayscale format.
        if(tex.getImage().getFormat() != Format.Luminance8 && 
                tex.getImage().getFormat() != Format.Luminance16F){
            printImage(dispByte,1,tex);
        }
        printImage(normals,3,tex);
    }
    
    /**
     * Generates a normal map from a base texture. The normal map is based
     * on a displacement map that is automatically generated beforehand. Both
     * maps are printed as images.
     * 
     * @param tex The texture used as base.
     * @param flipX Used to flip the texture.
     * @param flipY Used to flip the texture.
     */
    public void generateNormalMap(Texture tex, boolean flipX, boolean flipY){
        generateNormalMap(tex,flipX,flipY,1.0f);
    }
    
    /**
     * Generates a normal map from a base texture. The normal map is based
     * on a displacement map that is automatically generated beforehand. Both
     * maps are printed as images.
     * 
     * @param tex The texture used as base.
     */
    public void generateNormalMap(Texture tex){
        generateNormalMap(tex,false,false,1.0f);
    }
    
    protected byte[] generateNormMap(float[] disp){
        if(filter == null){
            filter = new CentralDifference();
        }
        filter.init(imageWidth,imageHeight,disp);
        byte[] norm = new byte[3*imageWidth*imageHeight];
        
        Vector3f vec = null;
        float invNormStr = 1/normalStrength;
        for(int j = 0; j < imageHeight; j++){
            for(int i = 0; i < imageWidth; i++){
                vec = filter.filter(i, j);
                vec.z =invNormStr;
                //Transform the color-values into the range [0,1].
                packNormal(vec);
                
                //Fill up the normal map array
                //Switching x and z to get the right order.
                norm[3*i +     j*3*imageWidth] = (byte)(vec.x*255); // b
                norm[3*i + 1 + j*3*imageWidth] = (byte)(vec.y*255); // g
                norm[3*i + 2 + j*3*imageWidth] = (byte)(vec.z*255); // r
            }
        }
        return norm;
    }
    
    protected void packNormal(Vector3f normVec){
        normVec.normalizeLocal().divideLocal(2f).addLocal(new Vector3f(0.5f,0.5f,0.5f));
    }
    
    /**
     * Set this to true if you want the displacement maps to be inverted.
     * This will also generate the normal map based on the inverted disp map.
     * 
     * @param useInvertedDisp 
     */
    public void setUseInvertedDisplacement(boolean useInvertedDisp){
        this.invertedDisp = useInvertedDisp;
    }
    
    /**
     * Set the assetdir. Use a '/' at the end. Default is "Assets/".
     * 
     * @param assetDir The asset dir.
     */
    public void setAssetDir(String assetDir){
        this.assetDir = assetDir;
    }
    
    protected void printImage(byte[] byteArray, int channels, Texture tex){
        
        BufferedImage img = null;
        String str = null;
        if(channels == 4){
            img = new BufferedImage(imageWidth,imageHeight,BufferedImage.TYPE_4BYTE_ABGR);
            str = "_NORMDISP";
        } else if (channels == 3){
            img = new BufferedImage(imageWidth,imageHeight,BufferedImage.TYPE_3BYTE_BGR);
            str = "_NORM";
        } else if (channels == 1){
            img = new BufferedImage(imageWidth,imageHeight,BufferedImage.TYPE_BYTE_GRAY);
            str = "_DISP";
        }
        WritableRaster wr = img.getRaster();
        wr.setDataElements(0, 0, imageWidth, imageHeight, byteArray);
        
        StringTokenizer tokenizer = new StringTokenizer(tex.getName(),"/");
        
        while(tokenizer.countTokens() > 1){
            tokenizer.nextToken();
        }
        String fileName = tokenizer.nextToken();
        tokenizer = new StringTokenizer(fileName,".");
        String name = tokenizer.nextToken(".");
        String postFix = tokenizer.nextToken();
        
        StringBuilder strBuf = new StringBuilder();
        strBuf.append(assetDir);
        strBuf.append(tex.getKey().getFolder());
        strBuf.append(name);
        strBuf.append(str);
        strBuf.append(".");
        strBuf.append(postFix);
        
        
        try {
            ImageIO.write(img, postFix, new File(strBuf.toString()));
        } catch (IOException ex){
        }
    }
}//NormDispGenerator
