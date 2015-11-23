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
package se.fojob.forester;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import java.util.ArrayList;

/**
 * Forester Material is a material with a few extra properties. 
 * 
 * @author Andreas
 */
public class SPLightMaterial extends Material {

    protected ArrayList<Light> lightList = new ArrayList<Light>(4);
    
    public SPLightMaterial(AssetManager contentMan, String defName) {
        super(contentMan, defName);
    }

    //@Override
	@Deprecated
    protected void updateLightListUniforms(Shader shader, Geometry g, int numLights) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return;
        }
        
        LightList worldLightList = g.getWorldLightList();
        ColorRGBA ambLightColor = new ColorRGBA(0f, 0f, 0f, 1f);
        lightList.ensureCapacity(worldLightList.size());
        
        for (int i = 0; i < worldLightList.size(); i++) {
            Light light = worldLightList.get(i);
            if (light instanceof AmbientLight) {
                ambLightColor.addLocal(light.getColor());
            } else {
                lightList.add(light);
            }
        }
        numLights = lightList.size();
        this.setInt("NumLights", numLights);
        Uniform lightColor = shader.getUniform("g_LightColor");
        Uniform lightPos = shader.getUniform("g_LightPosition");
        Uniform lightDir = shader.getUniform("g_LightDirection");
        lightColor.setVector4Length(4);
        lightPos.setVector4Length(4);
        lightDir.setVector4Length(4);

        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");
        ambLightColor.a = 1.0f;
        ambientColor.setValue(VarType.Vector4, ambLightColor);
        
        for (int i = 0; i < numLights; i++) {
            
            Light l = lightList.get(i);
            ColorRGBA color = l.getColor();
            lightColor.setVector4InArray(color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    l.getType().getId(),
                    i);

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();
                    lightPos.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), -1, i);
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    lightPos.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, i);
                    break;
                case Spot:
                    SpotLight sl = (SpotLight) l;
                    Vector3f pos2 = sl.getPosition();
                    Vector3f dir2 = sl.getDirection();
                    float invRange = sl.getInvSpotRange();
                    float spotAngleCos = sl.getPackedAngleCos();

                    lightPos.setVector4InArray(pos2.getX(), pos2.getY(), pos2.getZ(), invRange, i);
                    lightDir.setVector4InArray(dir2.getX(), dir2.getY(), dir2.getZ(), spotAngleCos, i);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
        }
        lightList.clear();
    }
    
}