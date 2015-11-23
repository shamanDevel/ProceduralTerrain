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
package se.fojob.forester.grass;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import se.fojob.forester.SPLightMaterial;
import se.fojob.forester.Forester;
import se.fojob.forester.grass.algorithms.GPAUniform;
import se.fojob.forester.grass.algorithms.GrassPlantingAlgorithm;
import se.fojob.forester.image.FormatReader.Channel;

/**
 * The GrassLayer class contains data specific to each type of grass.
 * 
 * @author Andreas
 */
public class GrassLayer {
    
    public enum MeshType {  QUADS,      //One static quad per patch of grass.
                            CROSSQUADS, //Two crossed static quads per patch of grass.
                            BILLBOARDS  //One billboarded quad per patch of grass.
                         }
    
    protected GrassLoader grassLoader;
    protected final SPLightMaterial material;
    protected MeshType type;
    protected GrassPlantingAlgorithm pa;
    
    protected float densityMultiplier = 1f;
    
    //The individual grass-patches height and width range.
    protected float maxHeight = 1.2f, minHeight = 1f;
    protected float maxWidth = 1.2f, minWidth = 1f;
    
    //Material parameters.
    protected boolean swaying = false;
    protected Vector3f swayData = new Vector3f();
    protected Vector2f wind = new Vector2f();
    protected boolean vertexLighting = true;
    protected boolean vertexColors;
    protected boolean selfShadowing;
    protected Texture colorMap;
    protected Texture alphaNoiseMap;
    
    protected ShadowMode shadowMode = ShadowMode.Off;
    
    //Related to density maps.
    protected Channel dmChannel = Channel.Luminance;
    protected int dmTexNum = 0;
    
    protected boolean needsUpdate;
    
    /**
     * Don't use this constructor. Create new instances of this class only 
     * through the GrassLoaders addLayer-method.
     * 
     * @param mat The material for the grass.
     * @param type The type of mesh to use.
     * @param grassLoader The grassloader used to instantiate the layer.
     */
    protected GrassLayer(Material mat, MeshType type, GrassLoader grassLoader)
    {
        if(mat.getMaterialDef().getName().equals("BillboardGrass") || mat.getMaterialDef().getName().equals("Grass")){
            material = new SPLightMaterial(Forester.getInstance().getApp().getAssetManager(),mat.getMaterialDef().getAssetName());
        } else {
            material = null;
            throw new RuntimeException("Material base not supported");
        }
        this.type = type;
        this.grassLoader = grassLoader;
        pa = new GPAUniform();
        initMaterial(mat);
    }
    
    /**
     * Internal method.
     */
    protected final void initMaterial(Material mat){
        this.colorMap = mat.getTextureParam("ColorMap").getTextureValue();
        this.alphaNoiseMap = mat.getTextureParam("AlphaNoiseMap").getTextureValue();
        
        if(mat.getParam("VertexLighting") == null){
            this.vertexLighting = false;
        } else {
            this.vertexLighting = (Boolean)mat.getParam("VertexLighting").getValue();
        }
        
        if(mat.getParam("VertexColors") == null){
            this.vertexColors = false;
        } else {
            this.vertexColors = (Boolean)mat.getParam("VertexColors").getValue();
        }
        
        if(mat.getParam("SelfShadowing") == null){
            this.selfShadowing = false;
        } else {
            this.selfShadowing = (Boolean)mat.getParam("SelfShadowing").getValue();
        }
        
        if(mat.getParam("Swaying") == null){
            this.swaying = false;
        } else {
            this.swaying = (Boolean) mat.getParam("Swaying").getValue();
        }
        if(mat.getParam("SwayData") == null){
            swayData = new Vector3f(1.0f,0.5f,300f);
        } else {
            swayData = (Vector3f) mat.getParam("SwayData").getValue();
        }
        if(mat.getParam("Wind") == null){
            wind = new Vector2f(0,0);
        } else {
            wind = (Vector2f) mat.getParam("Wind").getValue();
        }
        material.getAdditionalRenderState().setBlendMode(mat.getAdditionalRenderState().getBlendMode());
        material.getAdditionalRenderState().setAlphaFallOff(mat.getAdditionalRenderState().getAlphaFallOff());
        material.getAdditionalRenderState().setAlphaTest(mat.getAdditionalRenderState().isAlphaTest());
        material.getAdditionalRenderState().setFaceCullMode(mat.getAdditionalRenderState().getFaceCullMode());
        material.getAdditionalRenderState().setColorWrite(mat.getAdditionalRenderState().isColorWrite());
        material.getAdditionalRenderState().setDepthTest(mat.getAdditionalRenderState().isDepthTest());
        material.getAdditionalRenderState().setDepthWrite(mat.getAdditionalRenderState().isDepthWrite());
        material.setTransparent(true);
        updateMaterial();
    }
    
    protected void updateMaterial(){
        
        material.setTextureParam("AlphaNoiseMap",VarType.Texture2D,alphaNoiseMap);
        material.setTextureParam("ColorMap",VarType.Texture2D,colorMap);
        material.setBoolean("VertexLighting",vertexLighting);
        material.setBoolean("VertexColors", vertexColors);
        material.setBoolean("SelfShadowing", selfShadowing);
        material.setBoolean("Swaying",swaying);
        material.setVector3("SwayData",swayData);
        material.setVector2("Wind", wind);
        material.setInt("NumLights", 4);
    }
    
    public void update(){
        if(needsUpdate){
            updateMaterial();
            needsUpdate = false;
        }
    }
    
    public void setMeshType(MeshType type){
        this.type = type;
    }
    
    public MeshType getMeshType(){
        return type;
    }

    public float getDensityMultiplier() {
        return densityMultiplier;
    }

    public void setDensityMultiplier(float density) {
        densityMultiplier = density;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(float maxHeight) {
        this.maxHeight = maxHeight;
    }

    public float getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
    }

    public float getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(float minHeight) {
        this.minHeight = minHeight;
    }

    public float getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(float minWidth) {
        this.minWidth = minWidth;
    }
    
    public boolean isSwaying(){
        return swaying;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public void setSwaying(boolean swaying) {
        this.swaying = swaying;
        needsUpdate = true;
    }
    
    public void setWind(Vector2f wind) {
        this.wind = wind;
        needsUpdate = true;
    }

    public void setSwayData(Vector3f swayData) {
        this.swayData = swayData;
        needsUpdate = true;
    }
    
    public void setSwayingFrequency(float distance){
        swayData.x = distance;
        needsUpdate = true;
    }
    
    public void setSwayingVariation(float distance){
        swayData.y = distance;
        needsUpdate = true;
    }
    
    public void setMaxSwayDistance(float distance){
        swayData.z = distance;
        needsUpdate = true;
    }

    public Texture getAlphaNoiseMap() {
        return alphaNoiseMap;
    }

    public void setAlphaNoiseMap(Texture alphaNoiseMap) {
        this.alphaNoiseMap = alphaNoiseMap;
        needsUpdate = true;
    }

    public Texture getColorMap() {
        return colorMap;
    }

    public void setColorMap(Texture colorMap) {
        this.colorMap = colorMap;
        needsUpdate = true;
    }

    public GrassLoader getGrassLoader() {
        return grassLoader;
    }

    public void setGrassLoader(GrassLoader grassLoader) {
        this.grassLoader = grassLoader;
    }

    public boolean isVertexLighting() {
        return vertexLighting;
    }

    public void setVertexLighting(boolean vertexLighting) {
        this.vertexLighting = vertexLighting;
        needsUpdate = true;
    }
    
    public GrassPlantingAlgorithm getPlantingAlgorithm() {
        return pa;
    }

    public void setPlantingAlgorithm(GrassPlantingAlgorithm plantingAlgorithm) {
        this.pa = plantingAlgorithm;
    }

    public Channel getDmChannel() {
        return dmChannel;
    }
    
    public int getDmTexNum(){
        return dmTexNum;
    }
    public void setDensityTextureData(int dmTexNum, Channel channel){
        this.dmChannel = channel;
        this.dmTexNum = dmTexNum;
    }

    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
    }
    
}//GrassLayer
