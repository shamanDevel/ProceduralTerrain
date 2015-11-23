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

import se.fojob.paging.interfaces.Page;
import se.fojob.paging.interfaces.Tile;
import se.fojob.paging.interfaces.PagingEngine;
import com.jme3.math.Vector3f;
import se.fojob.grid.GenericCell2D;
import java.util.ArrayList;
import java.util.concurrent.Future;

/**
 * Base class for all tiles.
 * 
 * @author Andreas
 */
public class GeometryTile extends GenericCell2D implements Tile {

    protected Future<Boolean> future;
    protected int cacheTimer = 0;
    protected ArrayList<Page> pages;
    protected short resolution;
    protected short tileSize;
    protected float pageSize;
    protected float halfPageSize;
    protected PagingEngine engine;
    protected Vector3f centerPoint;
    protected boolean loaded;
    protected boolean pending;
    protected boolean idle;

    public GeometryTile(int x, int z, PagingEngine engine) {
        super(x, z);
        this.engine = engine;
        this.resolution = engine.getResolution();
        this.tileSize = engine.getTileSize();
        this.pageSize = engine.getPageSize();
        this.halfPageSize = pageSize*0.5f;
        this.centerPoint = new Vector3f(x * engine.getTileSize(), 0, z * engine.getTileSize());
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public Page getPage(int i) {
        return pages.get(i);
    }

    @Override
    public ArrayList<Page> getPages() {
        return pages;
    }

    @Override
    public void createPages() {
        pages = new ArrayList<Page>(resolution * resolution);

        Page page = null;
        for (int j = 0; j < resolution; j++) {
            for (int i = 0; i < resolution; i++) {
                float posX = (i + 0.5f) * pageSize + (x - 0.5f) * tileSize;
                float posZ = (j + 0.5f) * pageSize + (z - 0.5f) * tileSize;
                Vector3f center = new Vector3f(posX, 0, posZ);
                page = createPage(i, j, center, engine);
                pages.add(page);
            }
        }
    }

    @Override
    public void process(Vector3f camPos) {

        for (Page page : pages) {
            //If the pagingNode is hidden, don't make any visibility
            //calculations.
            if (!engine.isVisible()) {
                return;
            }
            if(page.getNodes() == null){
                continue;
            }
            //Get the distance to the page center.
            float dx = page.getCenterPoint().x - camPos.x;
            float dz = page.getCenterPoint().z - camPos.z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);            

            ArrayList<DetailLevel> levels = engine.getDetailLevels();
            //Start with the detail-level furthest away
            for (int l = levels.size() - 1; l >= 0; l--) {
                if(page.getNode(l).getChildren().isEmpty()){
                    continue;
                }
                DetailLevel thisLvl = levels.get(l);
                DetailLevel nextLvl = null;

                if (l > 0) {
                    nextLvl = levels.get(l - 1);
                }

                boolean vis = false;

                boolean fadeEnable = false;
                float fadeStart = 0;
                float fadeEnd = 0;

                //Standard visibility check.
                if (dist < thisLvl.farDist && dist >= thisLvl.nearDist) {
                    vis = true;
                }
                
                if (engine.isFadeEnabled()) {

                    //This is the diameter of the (smallest) circle enclosing
                    //the page and all its geometry in the xz plane.
                    float halfPageDiag = page.getOverlap()*1.414214f;
                    float pageMin = dist - halfPageDiag;
                    float pageMax = dist + halfPageDiag;
                    //Fading visibility check.
                    if (pageMax >= thisLvl.nearDist && pageMin < thisLvl.farTransDist) {
                        if (thisLvl.fadeEnabled && pageMax >= thisLvl.farDist) {
                            vis = true;
                            fadeEnable = true;
                            fadeStart = thisLvl.farDist;
                            fadeEnd = thisLvl.farTransDist;
                        } else if (nextLvl != null && nextLvl.fadeEnabled && pageMin < nextLvl.farTransDist) {
                            vis = true;
                            fadeEnable = true;
                            fadeStart = nextLvl.farTransDist;
                            fadeEnd = nextLvl.farDist;
                        }
                    }
                    page.setFade(fadeEnable, fadeStart, fadeEnd, l);
                } //If fade enabled

                page.setVisible(vis, l);
            }//Detail level loop
        }//Page loop
    }//Process method

    @Override
    public void unload() {
        if (pages == null) {
            return;
        }
        //TODO Clean up better.
        if (future != null) {
            future.cancel(false);
        }
        if (pages != null) {
            for (Page page : pages) {
                if (page != null) {
                    page.unload();
                }
            }
        }
        pages = null;
    }

    @Override
    public void increaseCacheTimer(int num) {
        cacheTimer += num;
    }

    @Override
    public int getCacheTimer() {
        return cacheTimer;
    }

    @Override
    public void resetCacheTimer() {
        cacheTimer = 0;
    }

    @Override
    public boolean isIdle() {
        return idle;
    }

    @Override
    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public boolean isPending() {
        return pending;
    }

    @Override
    public void setPending(boolean pending) {
        this.pending = pending;
    }

    @Override
    public Future<Boolean> getFuture() {
        return future;
    }

    @Override
    public void setFuture(Future<Boolean> future) {
        this.future = future;
    }

    @Override
    public Page createPage(int x, int z, Vector3f center, PagingEngine engine) {
        return new GeometryPage(x,z,center,engine);
    }
}
