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
import se.fojob.forester.RectBounds;
import se.fojob.forester.trees.TreeData;
import se.fojob.forester.trees.TreeDataBlock;
import se.fojob.forester.trees.TreeDataList;
import se.fojob.forester.trees.TreeLayer;
import se.fojob.forester.trees.TreeLoader;
import se.fojob.forester.trees.TreePage;
import se.fojob.forester.trees.TreeTile;
import se.fojob.forester.util.FastRandom;
import se.fojob.grid.Grid2D;
import java.util.ArrayList;

/**
 *
 * @author Andreas
 */
public class UDTreeProvider implements DataProvider {

    protected float density;
    protected TreeLoader treeLoader;
    int xMin, xMax, zMin, zMax;
    boolean useBounds;

    public UDTreeProvider(float density, TreeLoader treeLoader) {
        this.density = density;
        this.treeLoader = treeLoader;
    }

    @Override
    public TreeDataBlock getData(TreeTile tile) {
        int x = tile.getX();
        int z = tile.getZ();
        if (x < xMin || x > xMax || z < zMin || z > zMax) {
            return null;
        }
        TreeDataBlock block = new TreeDataBlock();
        ArrayList<TreeLayer> layers = treeLoader.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            TreeLayer layer = layers.get(i);
            Grid2D<TreeDataList> list = generateTreeData(layer, tile);
            block.put(layer, list);
        }
        return block;
    }

    protected Grid2D<TreeDataList> generateTreeData(TreeLayer layer, TreeTile tile) {

        FastRandom random = new FastRandom();
        float scaleDiff = layer.getMaximumScale() - layer.getMinimumScale();
        Grid2D<TreeDataList> grid = new Grid2D<TreeDataList>();
        
        int resolution = treeLoader.getPagingEngine().getResolution();
        
        for (int k = 0; k < resolution; k++) {
            for (int j = 0; j < resolution; j++) {
                TreePage page = (TreePage) tile.getPage(j + resolution*k);
                RectBounds bounds = page.getBounds();
                float length = bounds.getWidth();
                int dens = (int) (density * length * length * 0.001f);
                TreeDataList dataList = new TreeDataList(j,k);

                for (int i = 0; i < dens; i++) {
                    TreeData data = new TreeData();
                    float x = bounds.getxMin() + random.unitRandom() * length;
                    float z = bounds.getzMin() + random.unitRandom() * length;
                    
                    data.y = treeLoader.getTerrain().getHeight(new Vector2f(x, z));
                    
                    data.x = x - page.getCenterPoint().x;
                    data.z = z - page.getCenterPoint().z;
                    
                    data.rot = (-1f + 2 * random.unitRandom()) * 3.141593f;
                    data.scale = layer.getMinimumScale() + random.unitRandom() * (scaleDiff);
                    dataList.add(data);
                }
                grid.add(dataList);
            }
        }
        return grid;
    }

    public float getDensity() {
        return density;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public void setBounds(int xMin, int xMax, int zMin, int zMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.zMin = zMin;
        this.zMax = zMax;
        useBounds = true;
    }

    public void setUseBounds(boolean useBounds) {
        this.useBounds = useBounds;
    }
} //UniformDistribution