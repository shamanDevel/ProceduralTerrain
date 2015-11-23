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
package se.fojob.paging.interfaces;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import se.fojob.forester.RectBounds;

/**
 * This is the page interface. Pages are the leaves of the paging grid. They
 * contain all geometry.
 * 
 * @author Andreas
 */
public interface Page{
    
    /**
     * This method is called to update the page. It has to be implemented, but
     * it does not have to do anything.
     * 
     * @param tpf The number of seconds passed since the last frame. 
     */
    public void update(float tpf);
    
    /**
     * This method is called when the page is removed.
     */
    public void unload();
    
    /**
     * Get the centerpoint of the page.
     * 
     * @return The centerpoint.
     */
    public Vector3f getCenterPoint();
    
    public RectBounds getBounds();
    
    /**
     * Sets the nodes of the geometry page.
     * 
     * @param nodes The nodes to be set. 
     */
    public void setNodes(Node[] nodes);

    public Node[] getNodes();
    /**
     * Gets the node corresponding to the given level of detail.
     * 
     * @param detailLevel The level of detail.
     * @return The node.
     */
    public Node getNode(int detailLevel);
    
    /**
     * Gets the visibility status of a node.
     * 
     * @param detailLevel Detail levels are also used to index the nodes.
     * @return The visibility status of the node.
     */
    public boolean isVisible(int detailLevel);
    
    /**
     * Changes the visibility status of a node. Which node is determined by
     * the detailLevel parameter.
     * 
     * @param visible true or false.
     * @param detailLevel The level of detail.
     */
    public void setVisible(boolean visible, int detailLevel);
    
    /**
     * This mehod changes the fading status of a node. The fading status
     * is calculated by the paging engine each update. Whether or not this
     * method is called depends on the paging engines fade settings. See 
     * the addDetailLevel-methods of the <code>GeometryPagingEngine</code> 
     * interface for more information.
     * 
     * @param enabled Whether or not fading is enabled.
     * @param fadeStart The distance where fading starts.
     * @param fadeEnd The distance where fading ends.
     * @param detailLevel 
     */
    public void setFade(boolean enabled, float fadeStart, float fadeEnd, int detailLevel);
    
    public float getOverlap();
    public void calculateOverlap(float halfPageSize, int detailLevel);
    
}//Page
