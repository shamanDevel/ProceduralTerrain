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

import java.util.concurrent.Callable;

/**
 * <Code>TileLoader</code>s are used by paging engines to load tiles.
 * Normally these classes are used as facades for the paging system
 * (they are the classes users interact with).
 * 
 * @author Andreas
 */
public interface TileLoader {
    
    /**
     * This method is called to load a tile. This method should return null if 
     * the tileloader has no data associated with the tile in question.
     * 
     * @param tile The tile to be loaded.
     * @return The runnable in which the actual loading takes place.
     */
    public Callable<Boolean> loadTile(Tile tile);
    
    /**
     * This is a method for updating the tileloader. Normally the paging engine
     * is updated from a tile loader instead of manually, in order not to expose
     * the engine to the user.
     * 
     * @param tpf The number of seconds since the last frame.
     */
    public void update(float tpf);
    
    /**
     * This method is used to create tiles.
     * 
     * @param x The x-coordinate of the tile.
     * @param z The z-coordinate of the tile.
     * @return A tile of the proper type.
     */
    public Tile createTile(int x, int z);
    
    public String getName();
    public void setName(String name);
    
}//TileLoader
