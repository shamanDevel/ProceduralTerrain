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
package se.fojob.forester.trees.datagrids;

import com.jme3.math.Vector2f;
import com.jme3.texture.Texture;
import se.fojob.forester.MapBlock;
import se.fojob.forester.RectBounds;
import se.fojob.forester.image.ColorMap;
import se.fojob.forester.image.DensityMap;
import se.fojob.forester.trees.TreeData;
import se.fojob.forester.trees.TreeDataBlock;
import se.fojob.forester.trees.TreeDataList;
import se.fojob.forester.trees.TreeLayer;
import se.fojob.forester.trees.TreeLoader;
import se.fojob.forester.trees.TreePage;
import se.fojob.forester.trees.TreeTile;
import se.fojob.forester.util.FastRandom;
import se.fojob.grid.GenericCell2D;
import se.fojob.grid.Grid2D;
import java.util.ArrayList;

/**
 *
 * @author Andreas
 */
public class MapGrid implements DataProvider {

    protected TreeLoader treeLoader;
    protected Grid2D<MapCell> grid;
    protected int tileSize;

    public MapGrid(int tileSize, TreeLoader treeLoader) {
        this.tileSize = tileSize;
        this.treeLoader = treeLoader;
        grid = new Grid2D<MapCell>();
    }

    @Override
    public TreeDataBlock getData(TreeTile tile) {
        TreeDataBlock block = new TreeDataBlock();
        ArrayList<TreeLayer> layers = treeLoader.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            TreeLayer layer = layers.get(i);
            Grid2D<TreeDataList> list = generateTreeData(layer, tile);
            if (list != null) {
                block.put(layer, list);
            }
        }
        return block;
    }

    protected Grid2D<TreeDataList> generateTreeData(TreeLayer layer, TreeTile tile) {

        FastRandom random = new FastRandom();
        float scaleDiff = layer.getMaximumScale() - layer.getMinimumScale();
        Grid2D<TreeDataList> tGrid = new Grid2D<TreeDataList>();

        int resolution = treeLoader.getPagingEngine().getResolution();

        MapCell cell = grid.getCell(tile.getX(), tile.getZ());
        if (cell == null) {
            return null;
        }
        DensityMap densityMap = cell.maps.getDensityMaps().get(layer.getDmTexNum());

        for (int k = 0; k < resolution; k++) {
            for (int j = 0; j < resolution; j++) {
                TreePage page = (TreePage) tile.getPage(j + resolution * k);
                RectBounds bounds = page.getBounds();
                float width = bounds.getWidth();
                int count = (int) (layer.getDensityMultiplier() * width * width * 0.001f);
                TreeDataList dataList = new TreeDataList(j, k);

                float[] dens = densityMap.getDensityUnfiltered(page, layer.getDmChannel());

                for (int i = 0; i < count; i++) {

                    float x = random.unitRandom() * width;
                    float z = random.unitRandom() * width;
                    
                    float d = dens[(int)x + (int)width * (int)z];

                    if (random.unitRandom() < d) {

                        TreeData data = new TreeData();
                        data.x = x + bounds.getxMin();
                        data.z = z + bounds.getzMin();
                        data.y = treeLoader.getTerrain().getHeight(new Vector2f(data.x, data.z));

                        data.x -= page.getCenterPoint().x;
                        data.z -= page.getCenterPoint().z;

                        data.rot = (-1f + 2 * random.unitRandom()) * 3.141593f;
                        data.scale = layer.getMinimumScale() + random.unitRandom() * (scaleDiff);
                        dataList.add(data);
                    }
                }
                tGrid.add(dataList);
            }
        }
        return tGrid;
    }

    /**
     * Load a texture as densitymap.
     * 
     * @param tex The texture.
     * @param x The grasstile x-index.
     * @param z The grasstile z-index.
     * @param index The order of the map (use 0 for first density map, 1 for second etc.).
     */
    public void addDensityMap(Texture tex, int x, int z, int index) {
        loadMapCell(x, z).addDensityMap(tex, index);
    }

    /**
     * Adds a colormap to the grid.
     * 
     * @param tex The texture to be used.
     * @param x The x-index (or coordinate) within the grid.
     * @param z The z-index within the grid.
     * @param index The index of the grass-layer using this densitymap.
     */
    public void addColorMap(Texture tex, int x, int z, int index) {
        MapCell cell = grid.getCell(x, z);
        if (cell == null) {
            throw new RuntimeException("Tried loading color map to empty cell: " + cell.toString());
        }
        cell.addColorMap(tex, index);
    }

    protected MapCell loadMapCell(int x, int z) {
        MapCell mapCell = grid.getCell(x, z);
        if (mapCell == null) {
            mapCell = new MapCell(x, z);
            grid.add(mapCell);
        }
        return mapCell;
    }

    /**
     * This class is used to store density and colormaps.
     */
    protected class MapCell extends GenericCell2D {

        MapBlock maps;

        protected MapCell(int x, int z) {
            super(x, z);
            maps = new MapBlock();
        }

        protected void addDensityMap(Texture tex, int idx) {
            DensityMap map = new DensityMap(tex, tileSize);
            maps.getDensityMaps().put(idx, map);
        }

        protected void addColorMap(Texture tex, int index) {
            ColorMap map = new ColorMap(tex, tileSize);
            maps.getColorMaps().put(index, map);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GenericCell2D other = (GenericCell2D) obj;
            if (this.hash != other.hashCode()) {
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
            return "MapCell: (" + Short.toString(x) + ',' + Short.toString(z) + ')';
        }
    }
}
