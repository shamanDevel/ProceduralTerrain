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
package se.fojob.forester.trees;

import com.jme3.bounding.BoundingSphere;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import se.fojob.forester.Forester;
import se.fojob.forester.image.FormatReader.Channel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for storing different tree types.
 * 
 * @author Andreas
 */
public class TreeLayer {

    protected static final Logger log = Logger.getLogger(TreeLayer.class.getName());
    protected String name;
    protected Node model;
    protected boolean usePhysics;
    protected CompoundCollisionShape collisionShape;
    protected float minimumScale = 0.8f;
    protected float maximumScale = 1.2f;
    
    protected ShadowMode shadowMode = ShadowMode.Off;
    
    static protected String impostorTextureDir = "./";
    protected Texture impostorTexture;
    
    //Related to density maps.
    protected Channel dmChannel = Channel.Luminance;
    protected int dmTexNum = 0;
    
    protected float densityMultiplier;

    public TreeLayer(Spatial model, boolean usePhysics) {
        this.model = (Node) model.clone(true);
        this.usePhysics = usePhysics;
        prepareModel();
    }

    //Set bounding spheres and prepare physics.
    protected final void prepareModel() {

        //Make sure a physics space is available. Otherwise physics woun't work.
        if (usePhysics && !(Forester.getInstance().isPhysicsEnabled())) {
            log.log(Level.SEVERE, "physics space not found. Physics has been disabled.");
            usePhysics = false;
        }

        int modelSize = model.getChildren().size();

        for (int i = 0; i < modelSize; i++) {
            Geometry geom = (Geometry) model.getChild(i);
            Mesh mesh = geom.getMesh();
            mesh.setBound(new BoundingSphere());
            mesh.updateBound();
            geom.updateGeometricState();
        }
        model.updateGeometricState();

        if (usePhysics) {
            collisionShape = new CompoundCollisionShape();

            RigidBodyControl control = model.getControl(RigidBodyControl.class);
            if (control != null) {
                CollisionShape shape = control.getCollisionShape();
                if (shape instanceof CompoundCollisionShape) {
                    CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
                    for (ChildCollisionShape cs : ccs.getChildren()) {
                        collisionShape.addChildShape(cs.shape, Vector3f.ZERO);
                    }
                } else {
                    collisionShape.addChildShape(shape, Vector3f.ZERO);
                }
            } else {
                for (int i = 0; i < modelSize; i++) {
                    Spatial spat = model.getChild(i);
                    control = spat.getControl(RigidBodyControl.class);
                    if (control != null) {
                        CollisionShape shape = control.getCollisionShape();
                        if (shape instanceof CompoundCollisionShape) {
                            CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
                            for (ChildCollisionShape cs : ccs.getChildren()) {
                                collisionShape.addChildShape(cs.shape, Vector3f.ZERO);
                            }
                        } else {
                            collisionShape.addChildShape(shape, Vector3f.ZERO);
                        }
                    }
                }
            }
            if(usePhysics && collisionShape.getChildren().isEmpty()){
                log.log(Level.SEVERE, "No collisionshape found on model: {0}, physics disabled for this TreeLayer.",model.toString());
                usePhysics = false;
            }
        }
    }

    public Node getModel() {
        return model;
    }

    public void setMaximumScale(float maxScale) {
        maximumScale = maxScale;
    }

    public float getMaximumScale() {
        return maximumScale;
    }

    public void setMinimumScale(float minScale) {
        minimumScale = minScale;
    }

    public float getMinimumScale() {
        return minimumScale;
    }

    public boolean isUsePhysics() {
        return usePhysics;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
    }

    public CompoundCollisionShape getCollisionShape() {
        return collisionShape;
    }
    
    public float getDensityMultiplier() {
        return densityMultiplier;
    }

    public void setDensityMultiplier(float density) {
        densityMultiplier = density;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TreeLayer other = (TreeLayer) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}//TreeLayer
