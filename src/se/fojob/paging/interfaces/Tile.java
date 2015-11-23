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
import se.fojob.grid.Cell2D;
import java.util.ArrayList;
import java.util.concurrent.Future;

/**
 * Base interface for tiles.
 * 
 * @author Andreas
 */
public interface Tile extends Cell2D {
    
    /**
     * This method is called every update on every page. It can be used to
     * do visibility calculations (as in geometry pages) or whatever.
     * 
     * @param camPos The position of the camera used by the paging engine.
     */
    public void process(Vector3f camPos);
    
    /**
     * This method could be used for anything regarding the tile itself.
     * 
     * @param tpf Time since last frame in seconds.
     */
    public void update(float tpf);
    
    /**
     * This method is called whenever tiles are being removed.
     */
    public void unload();
    
    /**
     * Set the future object. It is used internally by the paging engine, 
     * and normally contains work designated by the process-method.
     * 
     * @param future
     */
    public void setFuture(Future<Boolean> future);
    
    /**
     * Get the future object. Used internally.
     * 
     * @return The future object.
     */
    public Future<Boolean> getFuture();
    
    //******************* Page management ********************
    
    /**
     * Creates empty pages. This is normally done during tile loading, before
     * anything else.
     */
    public void createPages();
    
    /**
     * Creates a page object of the designated type. Tiles are also page factories.
     * 
     * @param x x-coordinate of the page.
     * @param z z-coordinate of the page.
     * @param center The center of the page in world coordinates.
     * @param engine The paging engine used with the tile loader.
     * @return A page.
     */
    public Page createPage(int x, int z, Vector3f center, PagingEngine engine);
    
    /**
     * Returns a page from the page grid.
     * 
     * @param i Index of the page.
     * @return The page.
     */
    public Page getPage(int i);
    
    /**
     * Get the arraylist containing all pages in the tile.
     * 
     * @return The pages.
     */
    public ArrayList<Page> getPages();
    
    //*********************** Status **************************
    
    public boolean isIdle();
    public void setIdle(boolean idle);
    public boolean isLoaded();
    public void setLoaded(boolean loaded);
    public boolean isPending();
    public void setPending(boolean pending);
    
    //*********************** Cache **************************
    
    /**
     * @param num Time in ms
     */
    public void increaseCacheTimer(int num);
    public int getCacheTimer();
    public void resetCacheTimer();
}//Tile
