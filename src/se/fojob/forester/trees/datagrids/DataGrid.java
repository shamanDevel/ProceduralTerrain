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

import com.jme3.math.Vector3f;
import se.fojob.grid.GenericCell2D;
import se.fojob.grid.Grid2D;
import se.fojob.forester.trees.TreeData;
import se.fojob.forester.trees.TreeDataBlock;
import se.fojob.forester.trees.TreeDataList;
import se.fojob.forester.trees.TreeLayer;
import se.fojob.forester.trees.TreeTile;

/**
 * A tile-loader implementation for adding & loading trees. It uses
 * nested grids. The top grid store all blocks of tree data. It has one
 * element per tile (or none).
 * Each block is a hashmap with tree layers as keys, and grids of tree-data 
 * lists as values. These lower level grids are structured so that each cell 
 * contains a list of tree data corresponding to a page.
 * 
 * @author Andreas
 */
public class DataGrid implements DataProvider {
    
    protected Grid2D<TreeCell> grid;
    protected int tileSize;
    protected int resolution;
    protected float pageSize;

    public DataGrid(){
        
    }
    
    public DataGrid(int tileSize, int resolution) 
    {
        this.tileSize = tileSize;
        this.resolution = resolution;
        this.pageSize = tileSize/(float)resolution;
        grid = new Grid2D<TreeCell>();
    }

    /**
     * This method is very useful when generating page-sized blocks of tree 
     * data manually, instead of generating tree data over entire tiles, or
     * worlds.
     * 
     * When you generate tree data over an arbitrary area, the treegrid has
     * to parse each data post and place it in its proper page-block based
     * on the coordinates. If you do this manually, there's no need for
     * sorting.
     * 
     * @param layer The tree layer.
     * @param list The properly formated list.
     * @param x The x-coordinate of the tile.
     * @param z The z-coordinate of the tile.
     * 
     */
    public void addTrees(TreeLayer layer, Grid2D<TreeDataList> list, int x, int z){
        grid.getCell(x, z).dataBlock.put(layer, list);
    }
    
    /**
     * Bulk method for adding tree data.
     * 
     * @param layer
     * @param list 
     */
    public void addTrees(TreeLayer layer, TreeDataList list){
        for(TreeData data : list){
            addTree(layer,data);
        }
    }
    
    public void addTree(TreeLayer layer, TreeData data) {
        getTreeCell(data).addData(layer, data);
    }
    
    public void removeTree(TreeLayer layer, TreeData data){
        getTreeCell(data).removeData(layer,data);
    }
    
    public TreeCell getTreeCell(TreeData data){
        //Normalize coordinates to the tree grid.
        int t = (data.x >= 0) ? 1 : -1;
        float xx = data.x / (float) tileSize + t * 0.5f;
        t = (data.z >= 0) ? 1 : -1;
        float zz = data.z / (float) tileSize + t * 0.5f;
        TreeCell cell = grid.getCell((int)xx, (int)zz);
        
        if (cell == null) {
            cell = new TreeCell((int) xx, (int) zz);
            grid.add(cell);
        }
        
        return cell;
    }

    @Override
    public TreeDataBlock getData(TreeTile tile) {
        TreeCell cell = grid.getCell(tile.hashCode());
        if(cell == null){
            return null;
        }
        return cell.dataBlock;
    }

    //Used for creating a grid of tree-pages.
    protected class TreeCell extends GenericCell2D {

        protected TreeDataBlock dataBlock;
        protected Vector3f centerPoint;

        public TreeCell(int x, int z) {
            super(x, z);
            this.centerPoint = new Vector3f(x*tileSize,0,z*tileSize);
            dataBlock = new TreeDataBlock();
        }
        
        protected boolean addData(TreeLayer layer, TreeData data) {
            Grid2D<TreeDataList> dataGrid = null;
            //Get the correct dataList (the list of tree-data corresponding
            //to the given spatial).
            if (!dataBlock.containsKey(layer)) {
                dataGrid = new Grid2D<TreeDataList>(resolution,resolution);
                
                for(int j = 0; j < resolution; j++){
                    for(int i = 0; i < resolution; i++){
                        dataGrid.add(new TreeDataList(i,j));
                    }
                }
                dataBlock.put(layer, dataGrid);
                
            } else {
                dataGrid = dataBlock.get(layer);
            }
            
            return getDataList(layer,data).add(data);
        }
        
        protected boolean removeData(TreeLayer layer, TreeData data){
            
            return dataBlock.get(layer).get(0).remove(data);
        }
        
        /*
         * Get data. Note it alters the tree data. Should not be used
         * for anything other then adding/removing tree-data from the
         * lists.
         */
        protected TreeDataList getDataList(TreeLayer layer,TreeData data){
            //Align with tile. xz-positions is now in the range 0->tileSize.
            data.x -= centerPoint.x - tileSize*0.5f;
            data.z -= centerPoint.z - tileSize*0.5f;
            //Find the proper page indices based on the coordinates.
            int xx = (int) (data.x/pageSize);
            int zz = (int) (data.z/pageSize);
            
            //Align the coordinates with page.
            data.x -= (xx + 0.5f)*pageSize;
            data.z -= (zz + 0.5f)*pageSize;
            //Tree coordinates are now relative to the center of 
            //their corresponding page.
            int packed = (int)xx + resolution*(int)zz;
            //Get the appropriate list.
            return dataBlock.get(layer).get(packed);
        }

        public Vector3f getCenterPoint() {
            return centerPoint;
        }

        public TreeDataBlock getDataBlock() {
            return dataBlock;
        }
        
    }//TreeList
    
}//TreeGrid
