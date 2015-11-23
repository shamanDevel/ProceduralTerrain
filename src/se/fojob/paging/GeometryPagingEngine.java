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
package se.fojob.paging;

import se.fojob.paging.interfaces.Tile;
import se.fojob.paging.interfaces.TileLoader;
import se.fojob.paging.interfaces.PagingEngine;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import se.fojob.grid.GenericCell2D;
import se.fojob.grid.Grid2D;
import se.fojob.grid.Cell2D;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the base class of all paging engines. This class is used for
 * loading, unloading and processing tiles. Tiles are high-level objects
 * used for dividing a space into smaller entities. Each tile is in 
 * turn sub-divided into pages. The amount of pages in each tile depends
 * on the resolution of the tile.
 * 
 * @author Andreas
 */
public class GeometryPagingEngine implements PagingEngine{
    
    protected static final Logger log = Logger.getLogger(GeometryPagingEngine.class.getCanonicalName());
    protected Node pagingNode;
    protected Node rootNode;
    //Used to turn visibility off globally.
    protected boolean visible = true;
    
    protected ArrayList<DetailLevel> detailLevels;
    protected int numDetailLevels;
    protected boolean fadeEnabled = false;
    
    protected Camera camera;
    protected TileLoader tileLoader;
    protected ExecutorService executor;
    
    //The size of a tile.
    protected short tileSize;
    protected float radius;
    protected short gridSize;
    protected short halfGridSize;         //(size - 1) / 2
    protected float pageSize;
    protected short resolution;
    
    //Grid data
    protected Grid2D<Tile> grid;
    protected Grid2D<Tile> cache;
    protected boolean useCache = true;
    protected int cacheTime = 6000;
    
    protected Cell2D currentCell;
    
    protected boolean updateTiles = false;
    
    //Temporary variable
    protected Vector3f camPos;
    
    
    /**
     * Should generally be avoided. Need to set other values manually
     * and may have to re-load the entire grid several times.
     * 
     * @param rootNode The root node of the scene.
     * @param camera camera A jME <code>Camera</code> object. Most grid-calculations are based 
     * on it.
     */
    public GeometryPagingEngine(Node rootNode, Camera camera){
        this(512,4,512f,rootNode,camera);
    }
    
    /**
     * Constructor.
     * 
     * @param tileSize The size of tiles.
     * @param resolution Each tile contains resolution^2 pages.
     * @param radius Used to determine the number of pages in the grid.
     * @param rootNode The root node of the scene.
     * @param camera A jME <code>Camera</code> object. Most grid-calculations are based 
     * on it.
     */
    public GeometryPagingEngine(int tileSize, int resolution, float radius, Node rootNode, Camera camera){
        
        this.tileSize = (short) tileSize;
        this.resolution = (short) resolution;
        this.pageSize = (tileSize/(float)resolution);
        
        this.radius = radius;
        this.camera = camera;
        
        this.rootNode = rootNode;
        detailLevels = new ArrayList<DetailLevel>();
        pagingNode = new Node("Paging");
        rootNode.attachChild(pagingNode);
        
        if(useCache){
            //Start at 5*size, expands if needed.
            cache = new Grid2D<Tile>(5,gridSize);
        }
    }
    
    @Override
    public void setTileLoader(TileLoader tileLoader)
    {
        if(this.tileLoader == null){
            this.tileLoader = tileLoader;
            initGrid();
        } else {
            this.tileLoader = tileLoader;
            reloadTiles();
        }
            
    }
    
    protected void initGrid(){
        
        //Limit pagesize min, in case weird values are being used.
        if(pageSize < 32f){
            log.log(Level.INFO, "Very small page size (pageSize: {0}); setting to minimum value: 32f.", pageSize);
            this.resolution = (short)(tileSize/16f);
            this.pageSize = (tileSize/(float)resolution);
            
        }
        //Calculate gridsize.
        gridSize = (short) (2*((short)(radius/(float)tileSize) + 1) + 1);
        halfGridSize = (short)((gridSize - 1) / 2);
        //Create a new grid.
        grid = new Grid2D<Tile>(gridSize,gridSize);
        log.log(Level.INFO, "Grid created (number of tiles: {0}).",gridSize*gridSize);
        
        camPos = camera.getLocation();
        Cell2D camCell = getGridCell(camPos);
        currentCell = camCell;
        
        for (int k = -halfGridSize; k <= halfGridSize; k++) {
            for (int i =  -halfGridSize; i <= halfGridSize; i++) {
                int x = i + camCell.getX();
                int z = k + camCell.getZ();
                Tile tile = tileLoader.createTile(x, z);
                grid.add(tile);
            }
        }
        setVisible(true);
    }
    
    @Override
    public void update(float tpf)
    {
        camPos = camera.getLocation();
        Cell2D camCell = getGridCell(camPos);
        
        //Check if the grid should be scrolled.
        if(camCell.hashCode() != currentCell.hashCode()){
            scrollGrid(camCell);
        }
        
        Tile tile = null;
        for (int i = 0; i < grid.size(); i++){
            tile= grid.get(i);
            if(tile == null){
                //DEBUG
                throw new RuntimeException(tile.toString() + " is null");
            }
            
            if(!tile.isLoaded() && !tile.isIdle() && !tile.isPending()){
                Callable<Boolean> task = tileLoader.loadTile(tile);
                Future<Boolean> future = getExecutor().submit(task);
                tile.setFuture(future);
                tile.setPending(true); 
                continue;
                
            } else if(tile.isPending()){
                if(tile.getFuture().isDone()){
                    try {
                        boolean result = tile.getFuture().get();
                        if(result == true){
                            tile.setLoaded(true);
                        } else {
                            tile.setIdle(true);
                        }
                        tile.setPending(false);
                        tile.setFuture(null);
                    } catch (InterruptedException ex) {
                        log.log(Level.SEVERE, null, ex.getCause());
                    } catch (ExecutionException ex) {
                        log.log(Level.SEVERE, null, ex.getCause());
                    }
                    
                }
            } else if(tile.isLoaded()){
                //If the tile is loaded, update and process it every frame.
                if(updateTiles){
                    tile.update(tpf);
                }
                tile.process(camPos);
            }
        }
        
        //If the cache is being used.
        if(useCache){
            tile = null;
            int delta = (int)(tpf*1000);
            for(int i = 0; i < cache.size(); i++){
                tile = cache.get(i);
                if(tile.getCacheTimer() >= cacheTime){
                    cache.remove(i);
                    tile.unload();
                } else {
                    tile.increaseCacheTimer(delta);
                }
            }
        }
    }
    
    /**
     * Internal method.
     * 
     * This method is called whenever the camera moves from one grid-cell to
     * another, to move the grid along with the camera.
     */
    protected void scrollGrid(Cell2D camCell)
    {
        int dX = camCell.getX() - currentCell.getX();
        int dZ = camCell.getZ() - currentCell.getZ();
        
        Tile tile = null;
        
        if (dX == 1 || dX == -1){ 
            int oldX = currentCell.getX() - dX*halfGridSize;
            int newX = oldX + dX*gridSize;
            
            for(int k =  -halfGridSize; k <= halfGridSize; k++){
                int z = k + currentCell.getZ();
                
                if(useCache){
                    //Browse the cache to see if the page is there before
                    //creating a new one
                    tile = cache.getCell(newX,z);
                    if(tile == null){
                        Tile newTile = tileLoader.createTile(newX, z);
                        tile = grid.setCell(oldX,z,newTile);
                        cache.add(tile);
                    } else {
                        grid.setCell(oldX,z,tile);
                        log.log(Level.INFO, "Tile recycled from cache at: {0}", tile.toString());
                        cache.remove(tile);
                        tile.resetCacheTimer();
                    }
                }
                else{
                    //Just create a new page and loose the old one.
                    Tile newTile = tileLoader.createTile(newX, z);
                    Tile oldTile = grid.setCell(oldX,z,newTile);
                    oldTile.unload();
                }
            }
            tile = null;
        }
        
        if(dZ == 1 || dZ == -1){
            int oldZ = currentCell.getZ() - dZ*halfGridSize;
            int newZ = oldZ + dZ*gridSize;
            
            for(int i =  -halfGridSize; i <= halfGridSize; i++){
                //Check to make sure that this page was not checked in the
                //previous loop.
                if((dX == 1 && i == -halfGridSize) || (dX == -1 && i == halfGridSize)){
                    continue;
                }
                int x = i + currentCell.getX();
                if(useCache){
                    //Browse the cache to see if the page is there before
                    //creating a new one
                    tile = cache.getCell(x,newZ);
                    if(tile == null){
                        Tile newTile = tileLoader.createTile(x, newZ);
                        tile = grid.setCell(x,oldZ,newTile);
                        cache.add(tile);
                    } else {
                        grid.setCell(x,oldZ,tile);
                        log.log(Level.INFO, "Tile recycled from cache at: {0}", tile.toString());
                        cache.remove(tile);
                        tile.resetCacheTimer();
                    }
                }
                else{
                    Tile newTile = tileLoader.createTile(x, newZ);
                    Tile oldTile = grid.setCell(x,oldZ,newTile);
                    oldTile.unload();
                }
            }
            tile = null;
        }
        currentCell = camCell;
    }
    
    @Override
    public void reloadTiles(){
        for(Tile tile: grid){
            tile.unload();
        }
        grid.clear();
        if(useCache){
            cache.clear();
        }
        initGrid();
    }
    
    @Override
    public void reloadTiles(float l, float r, float t, float b){
        Vector3f tl = new Vector3f(l,0,t);
        Vector3f br = new Vector3f(r,0,b);
        Cell2D tlc = getGridCell(tl);
        Cell2D brc = getGridCell(br);
        for(int j = brc.getZ(); j <= tlc.getZ();j++){
            for(int i = tlc.getX(); i <= brc.getX();i++){
                reloadTile(i,j);
            }
        }
    }
    
    @Override
    public void reloadTiles(Vector3f center, float radius) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void reloadTile(Vector3f loc){
        Cell2D cell = getGridCell(loc);
        reloadTile(cell.getX(),cell.getZ());
    }
    
    @Override
    public void reloadTile(int x, int z){
        Tile tile = grid.getCell(x, z);
        if(tile != null){
            tile.unload();
            grid.removeCell(tile);
            Tile newTile = tileLoader.createTile(x, z);
            grid.add(newTile);
        }
    }
    
    /**
     * A method for getting the grid cell that matches a certain xyz-location.
     * 
     * @param loc The location-vector.
     * @return The cell matching the given location.
     */
    public Cell2D getGridCell(Vector3f loc){
        float x = loc.x;
        float z = loc.z;
        int t = (x >= 0) ? 1 : -1;
        x = x/(float)tileSize + t*0.5f;
        t = (z >= 0) ? 1 : -1;
        z = z/(float)tileSize + t*0.5f;
        return new GenericCell2D((int)x,(int)z);
    }
    
    /**
     * A convenience method for getting cells based on world x and z coordinates.
     * 
     * @param xIn The world x-coordinate.
     * @param zIn The world z-coordinate.
     * @return The cell matching the given location.
     */
    public Cell2D getGridCell(float xIn, float zIn){
        return getGridCell(new Vector3f(xIn,0,zIn));
    }

    @Override
    public TileLoader getTileLoader() {
        return tileLoader;
    }

    @Override
    public short getResolution() {
        return resolution;
    }

    @Override
    public short getTileSize() {
        return tileSize;
    }
    
    @Override
    public float getPageSize() {
        return pageSize;
    }

    @Override
    public Camera getCamera() {
        return camera;
    }
    
    @Override
    public Cell2D getCurrentCell() {
        return currentCell;
    }
    
    @Override
    public int getCurrentGridSize(){
        return grid.size();
    }

    public int getCacheTime() {
        return cacheTime;
    }

    public boolean isUseCache() {
        return useCache;
    }
    
    @Override
    public void setCacheTime(int cacheTime) {
        this.cacheTime = cacheTime;
    }

    @Override
    public void setUseCache(boolean useCache) {
        if(useCache == true && this.cache == null){
            cache = new Grid2D<Tile>(5,gridSize);
        }
        if(useCache == false && this.cache != null){
            cache = null;
        }
        this.useCache = useCache;
    }

    @Override
    public ExecutorService getExecutor() {
        if(executor == null){
            executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("Paging Thread");
                    th.setDaemon(true);
                    return th;
                }
            });
        }
        return executor;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        
        if(visible == true && this.visible == false){
            rootNode.attachChild(pagingNode);
            this.visible = visible;
        }
        
        else if(visible == false && this.visible == true){
            pagingNode.removeFromParent();
            this.visible = visible;
        }
    }
    
    @Override
    public Node getPagingNode(){
        return pagingNode;
    }
    
    @Override
    public void setPagingNode(Node pagingNode){
        this.pagingNode = pagingNode;
    }
    
    public void addDetailLevel(float farDist)
    {
        addDetailLevel(farDist,0);
    }
    
    
    @Override
    public void addDetailLevel(float farDist , float fadingRange)
    {
        float nearDist = 0;
        DetailLevel level = null;
        
        //If a detail level has previously been added, use its far distance as 
        //near distance for the new one.
        if(numDetailLevels != 0){
            level = detailLevels.get(numDetailLevels - 1);
            nearDist = level.farDist;
            if(nearDist >= farDist)
                throw new RuntimeException("The near viewing distance must be closer then the far viewing distance");
        }
        if(fadingRange > 0){
            this.fadeEnabled = true;
        }
        DetailLevel newLevel = new DetailLevel();
        newLevel.setFarDist(farDist);
        newLevel.setNearDist(nearDist);
        newLevel.setTransition(fadingRange);
        detailLevels.add(newLevel);
        numDetailLevels += 1;
        
        if(farDist > radius){
            radius = farDist;
            if(grid != null){
                reloadTiles();
            }
        }
    }
   
    public void removeDetailLevels() 
    {
        detailLevels.clear();
        setVisible(false);
    }
    
    public int getNumDetailLevels() {
        return numDetailLevels;
    }

    @Override
    public ArrayList<DetailLevel> getDetailLevels() {
        return detailLevels;
    }

    @Override
    public boolean isFadeEnabled() {
        return fadeEnabled;
    }

    @Override
    public Grid2D<Tile> getGrid() {
        return grid;
    }

    @Override
    public void setRadius(float radius) {
        this.radius = radius;
        reloadTiles();
        
    }

    @Override
    public void setResolution(int resolution) {
        this.resolution = (short) resolution;
        reloadTiles();
    }

    @Override
    public void setTileSize(int tileSize) {
        if(tileSize < 64){
            tileSize = 64;
        }
        this.tileSize = (short) tileSize;
        reloadTiles();
    }

    public boolean isUpdateTiles() {
        return updateTiles;
    }

    public void setUpdateTiles(boolean updateTiles) {
        this.updateTiles = updateTiles;
    }
    
    static{
        
        Slappy:
                Pappy:
                        Wae:
                                Waaae:;
    }
    
} //AbstractPagingEngine
