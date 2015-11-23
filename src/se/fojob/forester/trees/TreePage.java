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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import se.fojob.forester.Forester;
import se.fojob.forester.RectBounds;
import se.fojob.paging.GeometryPage;
import se.fojob.paging.interfaces.PagingEngine;

/**
 * This is the default page type for the tree-loader.
 * 
 * @author Andreas
 */
public class TreePage extends GeometryPage {
    protected RigidBodyControl control;

    public TreePage(int x, int z, Vector3f center, PagingEngine engine) {
        super(x, z, center, engine);
    }

    @Override
    public void setVisible(boolean visible, int detailLevel) {
        if (visible == true && stateVec[detailLevel] == false) {
            parentNode.attachChild(nodes[detailLevel]);
            stateVec[detailLevel] = visible;
            if (detailLevel == 0 && control != null) {
                PhysicsSpace phySpace = Forester.getInstance().getPhysicsSpace();
                phySpace.add(control);
            }
        } else if (visible == false && stateVec[detailLevel] == true) {
            nodes[detailLevel].removeFromParent();
            stateVec[detailLevel] = visible;
            if (detailLevel == 0 && control != null) {
                PhysicsSpace phySpace = Forester.getInstance().getPhysicsSpace();
                phySpace.remove(control);
            }
        }
    }

    public void initPhysics(CompoundCollisionShape ccs) {
        if (!nodes[0].getChildren().isEmpty()) {
            
            if (ccs != null ) {
                control = new RigidBodyControl(ccs, 0f);
                nodes[0].addControl(control);
            }
        }
    }

    @Override
    public void unload() {
        super.unload();
        if(control != null){
            PhysicsSpace phySpace = Forester.getInstance().getPhysicsSpace();
            phySpace.remove(control);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TreePage other = (TreePage) obj;
        if (other.hash != this.hash) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "TreePage (" + Short.toString(x) + ','
                + Short.toString(z) + ')';
    }
}//TreePage
