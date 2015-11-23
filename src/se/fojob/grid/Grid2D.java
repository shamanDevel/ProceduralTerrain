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
package se.fojob.grid;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * This class is a container for Cell2D. It is based on ArrayList, but has
 * additional methods to lookup elements (cells) based on hashcodes. It
 * should be considered a work in progress.
 * <br/><br/>
 * Grid2D implements Cell2D which makes it possible to make a grid of
 * grids.
 * 
 * @author Andreas
 */
public class Grid2D<T extends Cell2D> extends ArrayList<T> implements Cell2D{
    
    protected static final Logger log = Logger.getLogger(Grid2D.class.getName());
    //Cell data
    protected final short x,z;
    protected final int hash;
    
    protected static final short hashRadius = (1 << 14);
    
    /**
     * The default constructor. Creates a grid at position 0,0 with
     * x and z dimensions both 4 (the initial array size is 16).
     */
    public Grid2D(){
        this(4,4,0,0);
    }
    
    
    
    /**
     * Creates a grid with size sizeX*sizeZ at position x,z.
     * 
     * @param sizeX The x-size of the grid.
     * @param sizeZ The z-size of the grid.
     */
    public Grid2D(int sizeX, int sizeZ){
        this(sizeX,sizeZ,0,0);
    }
    
    /**
     * Creates a grid with size sizeX*sizeZ at position x,z.
     * 
     * @param sizeX The x-size of the grid.
     * @param sizeZ The z-size of the grid.
     * @param x The x-position of the grid.
     * @param z The z-position of the grid.
     */
    public Grid2D(int sizeX, int sizeZ, int x, int z){
        super(sizeX*sizeZ);
        this.x = (short) x;
        this.z = (short) z;
        this.hash = hash(x,z);
    }
    
    /**
     * Replaces the cell at the specified coords with the cell newCell, and 
     * returns the old cell. If no cell exists at the specified coords, or 
     * if it's the same cell as newCell, the method returns null.
     * 
     * @param x The x-coordinate of the cell.
     * @param z The z-coordinate of the cell.
     * @param newCell The new cell.
     * @return The cell currently at position (x,z), or null.
     */
    public T setCell(int x, int z, T newCell){
        return setCell(hash(x,z),newCell);
    }
    
    /**
     * Replaces the cell oldCell with the cell newCell, and returns the 
     * old cell. If no cell exists at the specified coords, or if it is 
     * the same cell as newCell, the method returns null.
     * 
     * @param oldCell The old cell.
     * @param newCell The new cell.
     * @return oldCell or null.
     */
    public T setCell(T oldCell, T newCell){
        return setCell(oldCell.hashCode(),newCell);
    }
    
    /**
     * Replaces the cell using the hashcode "hash", with the cell newCell, 
     * and returns the old cell. If no cell with hashcode "hash" exists, 
     * or if it is the same cell as newCell, the Method returns null.
     * 
     * @param hash The hash of the current cell.
     * @param newCell The new cell.
     * @return The gridcell with hashcode "hash", or null.
     */
    public T setCell(int hash, T newCell){
        if(hash == newCell.hashCode()){
            return null;
        }
        //Returns the old cell.
        T c = null;
        for(int i = 0; i < size(); i++){
            c = get(i);
            if(c.hashCode() == hash){
                return set(i,newCell);
            }
        }
        return null;
    }
    
    /**
     * Gets the cell at position x,z.
     * 
     * @param x The x-coordinate of the cell.
     * @param z The z-coordinate of the cell.
     * @return The cell at (x,z), or null if no such cell exists.
     */
    public T getCell(int x, int z){
        return getCell(hash(x,z));
    }
   
    /**
     * Gets the cell with hashcode "hash".
     * 
     * @param hash the hashcode to use for lookup.
     * @return The cell with hashCode "hash", or null if no such cell exists.
     */
    public T getCell(int hash){
        T c = null;
        for(int i = 0; i < size(); i++){
            c = get(i);
            if(c.hashCode() == hash){
                return c;
            }
        }
        return null;
    }
    
    /**
     * Removes the cell with the given coordinates if its in the grid.
     * 
     * @param x The x-coordinate of the cell.
     * @param z The z-coordinate of the cell.
     * @return The cell, or null if it's not in the grid.
     */
    public T removeCell(int x, int z){
        int hashCode = hash(x,z);
        T c = null;
        for(int i = 0; i < size(); i++){
            c = get(i);
            if(c.hashCode() == hashCode){
                return remove(i);
            }
        }
        return null;
    }
    
    /**
     * Removes a cell if its in the grid.
     * 
     * @param cell The cell to be removed.
     * @return The cell, or null if it's not in the grid.
     */
    public T removeCell(T cell){
        int hashCode = cell.hashCode();
        T c = null;
        for(int i = 0; i < size(); i++){
            c = get(i);
            if(c.hashCode() == hashCode){
                return remove(i);
            }
        }
        return null;
    }
    
    /**
     * Removes the cell with the given hashcode if its in the grid.
     * 
     * @param hash The hashcode of the cell.
     * @return The cell, or null if it's not in the grid.
     */
    public T removeCell(int hash){
        T c = null;
        for(int i = 0; i < size(); i++){
            c = get(i);
            if(c.hashCode() == hash){
                return remove(i);
            }
        }
        return null;
    }
    
    @Override
    public short getX() {
        return x;
    }

    @Override
    public short getZ() {
        return z;
    }
    
    /**
     * This method generates a 30 bit hashcode for each set of 
     * integers. The first 15 bits are used for x, and the rest for z.
     * The hash is unique assuming x and z are both contained in the interval
     * [-hashRadius,hashRadius - 1].
     * 
     * @param x the x-coordinate of the cell
     * @param z the z-coordinate of the cell
     */
    public static int hash(int x, int z){
        return x + hashRadius + ((z + hashRadius) << 15);
    }
    
}//Grid2D
