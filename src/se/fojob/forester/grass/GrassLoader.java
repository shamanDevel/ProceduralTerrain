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

import se.fojob.forester.grass.datagrids.MapProvider;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import se.fojob.forester.MapBlock;
import se.fojob.forester.grass.GrassLayer.MeshType;
import se.fojob.forester.grass.datagrids.MapGrid;
import se.fojob.forester.grass.datagrids.UDGrassProvider;
import se.fojob.forester.image.ColorMap;
import se.fojob.forester.image.DensityMap;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.fojob.paging.GeometryTileLoader;
import se.fojob.paging.interfaces.Page;
import se.fojob.paging.interfaces.Tile;

/**
 * This class is used to create grass.
 * 
 * @author Andreas
 */
public class GrassLoader extends GeometryTileLoader {

    protected static final Logger log = Logger.getLogger(GrassLoader.class.getName());
    //List of grass-layers.
    protected ArrayList<GrassLayer> layers;
    protected Vector2f wind;
    protected GrassGeometryGenerator grassGen;
    protected MapProvider mapProvider;
    protected boolean useBinaries;
    protected String binariesDir = ".";

    /**
     * The only constructor.
     * 
     * @param tileSize The tile size should be same size as the density 
     * maps used to produce the grass.
     * @param resolution This value determines the amount of sub-meshes
     * within each tile. The total amount of meshes per tile is resolution^2.
     * @param farViewingDistance The far viewing distance for the grass. This is
     * also a factor in determining grid size.
     * @param fadingRange The distance over which grass is faded out 
     * (in world units).
     * @param terrain A terrain object.
     * @param rootNode The rootNode of the scene.
     * @param camera The camera used for rendering the scene.
     */
    public GrassLoader(int tileSize,
            int resolution,
            float farViewingDistance,
            float fadingRange,
            Terrain terrain,
            Node rootNode,
            Camera camera) {
        super(tileSize, resolution, farViewingDistance, rootNode, terrain, camera);
        pagingEngine.addDetailLevel(farViewingDistance, fadingRange);
        layers = new ArrayList<GrassLayer>();
        wind = new Vector2f(0, 0);
        grassGen = new GrassGeometryGenerator(terrain);
        init();
    }

    protected final void init() {
        pagingEngine.setTileLoader(this);
    }

    @Override
    public Callable<Boolean> loadTile(Tile tile) {
        return new LoadTask((GrassTile) tile);
    }

    /**
     * This method was used to initialize the paging engine, but that
     * takes place in the constructor from version 0.1.7 on.
     * 
     * @deprecated This method is no longer necessary to call.
     */
    @Deprecated
    public void build() {
    }

    @Override
    public void update(float tpf) {
        for (GrassLayer layer : layers) {
            layer.update();
        }
        pagingEngine.update(tpf);
    }

    @Override
    public GrassTile createTile(int x, int z) {
        Logger.getLogger(GrassLoader.class.getName()).log(Level.INFO, "GrassTile created at: ({0},{1})", new Object[]{x, z});
        return new GrassTile(x, z, pagingEngine);
    }

    public MapGrid createMapGrid() {
        MapGrid grid = new MapGrid(pagingEngine.getTileSize());
        this.mapProvider = grid;
        return grid;
    }

    public UDGrassProvider createUDProvider() {
        UDGrassProvider provider = new UDGrassProvider(this);
        this.mapProvider = provider;
        return provider;
    }

    /**
     * Adds a new layer of grass to the grassloader. 
     * 
     * @param material The material for the main geometry.
     * @param type The meshtype of the main geometry.
     * @return A reference to the GrassLayer object.
     */
    public GrassLayer addLayer(Material material, MeshType type) {
        GrassLayer layer = new GrassLayer(material, type, this);
        layers.add(layer);
        return layer;
    }

    //***************************Getters and setters***********************
    public ArrayList<GrassLayer> getLayers() {
        return layers;
    }

    public void setLayers(ArrayList<GrassLayer> layers) {
        this.layers = layers;
    }

    public Vector2f getWind() {
        return wind;
    }

    public void setWind(Vector2f wind) {
        for (GrassLayer layer : layers) {
            layer.setWind(wind);
        }
    }

    public void setUseBinaries(boolean useBinaries) {
        this.useBinaries = useBinaries;
    }

    public void setBinariesDir(String binariesDir) {
        this.binariesDir = binariesDir;
    }

    public MapProvider getMapProvider() {
        return mapProvider;
    }

    public void setMapProvider(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    protected class LoadTask implements Callable<Boolean> {

        GrassTile tile;

        protected LoadTask(GrassTile tile) {
            this.tile = tile;
        }

        @Override
        public Boolean call() {

            //Get the density and colormaps.
            MapBlock block = mapProvider.getMaps(tile);
            if (block == null) {
                return false;
            }
            if (block.getDensityMaps().isEmpty()) {
                return false;
            }

            //Creates the empty page objects.
            tile.createPages();

            ArrayList<Page> pages = tile.getPages();
            float ps = pagingEngine.getPageSize() * 0.5f;
            //Loads grass geometry to each page.
            for (Page p : pages) {
                GrassPage page = (GrassPage) p;
                Node[] nodes = new Node[1];
                nodes[0] = new Node("Grass");

                for (int i = 0; i < layers.size(); i++) {
                    
                    GrassLayer layer = layers.get(i);
                    DensityMap densityMap = block.getDensityMaps().get(layer.getDmTexNum());
                    if (densityMap == null) {
                        continue;
                    }
                    ColorMap colorMap = null;
                    if (block.getColorMaps() != null) {
                        colorMap = block.getColorMaps().get(i);
                    }

                    Geometry geom = grassGen.createGrassGeometry(layer,
                            page,
                            densityMap,
                            colorMap);
                    
                    geom.setQueueBucket(Bucket.Transparent);
                    geom.setShadowMode(layer.getShadowMode());
                    nodes[0].attachChild(geom);

                }//for each layer
                page.setNodes(nodes);
                page.calculateOverlap(ps, 0);
            }//for each page

            for (Page p : tile.getPages()) {
                p.calculateOverlap(ps, 0);
            }
            return true;
        }//call
    }
}//AbstractGrassLoader