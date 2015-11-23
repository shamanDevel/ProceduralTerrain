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
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import se.fojob.grid.Cell2D;
import se.fojob.grid.Grid2D;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import se.fojob.paging.DetailLevel;

/**
 * This is the paging engine interface.
 * 
 * @author Andreas
 */
public interface PagingEngine {
    
    /**
     * This method connects the engine with the tileLoader. It has to 
     * be called before the update method is called.
     * 
     * @param tileLoader The tile-loader associated with this engine.
     */
    public void setTileLoader(TileLoader tileLoader);
    
    /**
     * The update method. Should be called every frame.
     * 
     * @param tpf The number of seconds passed since the last frame.
     */
    public void update(float tpf);
        
    /**
     * Getter for page size.
     * 
     * @return The page size used by the engine.
     */
    public float getPageSize();
    
    /**
     * Getter for tile size.
     * 
     * @return The tile size used by the engine.
     */
    public short getTileSize();
    
    /**
     * Getter for resolution
     * 
     * @return The resolution used by the engine.
     */
    public short getResolution();
    
    /**
     * Getter for the current cell.
     * 
     * @return The curent cell.
     */
    public Cell2D getCurrentCell();
    
    /**
     * Getter for the camera.
     * 
     * @return The camera used by the paging engine.
     */
    public Camera getCamera();
    
    /**
     * Getter for the executor.
     * 
     * @return The executor object used by the engine.
     */
    public ExecutorService getExecutor();
    
    /**
     * Getter for the pageloader.
     * 
     * @return The pageLoader used by the engine.
     */
    public TileLoader getTileLoader();
    
    /**
     * Getter for the current grid size
     * 
     * @return The amount of cells in the grid.
     */
    public int getCurrentGridSize();
    
    /**
     * Checks whether or not the paged geometry is visible.
     * 
     * @return True or false. 
     */
    public boolean isVisible();
    
    /**
     * Gets the paging node.
     * 
     * @return The paging node. 
     */
    public Node getPagingNode();
    
    /**
     * Get the ArrayList containing the different detail levels.
     * 
     * @return The detail levels.
     */
    public ArrayList<DetailLevel> getDetailLevels();
    
    /**
     * 
     * @return The tile grid.
     */
    public Grid2D<Tile> getGrid();
    
    /**
     * 
     * @param radius The radius of the tile grid.
     */
    public void setRadius(float radius);
    
    /**
     * 
     * @param tileSize The size of tiles.
     */
    public void setTileSize(int tileSize);
    
    /**
     * 
     * @param resolution The resolution of tiles (number of subdivisions).
     * The total amount is resolution^2.
     */
    public void setResolution(int resolution);
    
    /**
     * Check whether or not fading is enabled.
     * 
     * @return True if fading is enabled, false otherwise.
     */
    public boolean isFadeEnabled();
    
    /**
     * Set whether or not to use a built-in cache to store pages for a
     * set period (in milis) before discarding them completely. This mechanic
     * is most useful when using small grids and/or massive pages that takes
     * a significant amount of time to load, and thus should not be discarded.
     * 
     * @param useCache Whether or not to use the cache.
     */
    public void setUseCache(boolean useCache);
    
    /**
     * Set the time period that pages should stay alive after being removed from
     * the grid.
     * 
     * @param time The time in miliseconds.
     */
    public void setCacheTime(int time);
    
    /**
     * Sets the visibility of the paged geometry globally.
     * 
     * @param visible True or false.
     */
    public void setVisible(boolean visible);
    
    /**
     * Sets the paging node.
     * 
     * @param pagingNode The paging node.
     */
    public void setPagingNode(Node pagingNode);
    
    /**
     * Adds a detail level to the geometry. The engine will do fading- and
     * visibilitycalculations based on the provided data.
     * 
     * @param farDist The far viewing-distance of this detail-level.
     * @param fadingRange The length of the fade transitions (in world units).
     */
    public void addDetailLevel(float farDist, float fadingRange);
    
    public void reloadTiles(float l, float r, float t, float b);
    public void reloadTiles(Vector3f center, float radius);
    
    /**
     * Reloads the tile corresponding to the given location.
     * 
     * @param loc The location in world space.
     */
    public void reloadTile(Vector3f loc);
    
    /**
     * Reloads a tile based on its x and z coordinates.
     * 
     * @param x The x-coordinate of the tile.
     * @param z The z-coordinate of the tile.
     */
    public void reloadTile(int x, int z);
    
    /**
     * Reloads the whole grid.
     * 
     */
    public void reloadTiles();

}//PagingEngine