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
package se.fojob.forester;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import se.fojob.paging.interfaces.TileLoader;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import se.fojob.forester.grass.GrassLoader;
import se.fojob.forester.image.normdisp.Filter;
import se.fojob.forester.image.normdisp.NormDispGenerator;
import se.fojob.forester.trees.TreeLoader;
import se.fojob.forester.util.FastRandom;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides quick access to grass/tree-loaders and utilities.
 * 
 * @author Andreas
 */
public final class Forester
{
    protected static final Logger log = Logger.getLogger(Forester.class.getName());
    protected Node rootNode;
    protected Camera camera;
    protected Terrain terrain;
    protected Application app;
    protected PhysicsSpace physicsSpace;
    protected boolean physicsEnabled;
    //Using this to index new tileloaders that aren't named.
    protected int tlIdx = Short.MAX_VALUE;
    
    protected ArrayList<TileLoader> list;
    
    protected static Forester instance;
    
    /**
     * Creates a new Forester object.
     * 
     * @param rootNode The rootNode of the scene.
     * @param camera The camera.
     * @param terrain A terrain object.
     * @param app The application.
     */
    synchronized
    public void initialize(Node rootNode, Camera camera, Terrain terrain, Application app)
    {
        this.rootNode = rootNode;
        this.camera = camera;
        this.terrain = terrain;
        this.app = app;
        BulletAppState bas = app.getStateManager().getState(BulletAppState.class);
        if(bas == null){
            log.log(Level.INFO,"No BulletAppState found, physics is disabled.");
        } else {
            physicsSpace = bas.getPhysicsSpace();
            physicsEnabled = true;
        }
        list = new ArrayList<TileLoader>(2);
    }
    
    synchronized
    public static Forester getInstance(){
        if(instance == null){
            instance = new Forester();
            log.log(Level.INFO, "Forester was instantiated.");
        }
        return instance;
    }
    
    synchronized
    public static void destroy(){
        if(instance != null){
            instance = null;
            log.log(Level.INFO, "Forester was destroyed.");
        }
    }
        
    /**
     * The update method. Call this method every frame to update the pageloaders.
     * 
     * @param tpf Time passed since the last update (in seconds).
     */
    public void update(float tpf)
    {
        for(TileLoader loader: list){
            loader.update(tpf);
        }
    }
    
    /**
     * A method for creating grassloaders.
     * 
     * @param tileSize The tile size.
     * @param resolution The resolution of each tile (the number of partition).
     * The final number of partitions = resolution^2.
     * @param farViewingDistance The far viewing distance of the grass.
     * @param fadingRange The distance over which fading takes place.
     * @return A reference to the GrassGrid object.
     */
    public GrassLoader createGrassLoader(   int tileSize,
                                            int resolution,
                                            float farViewingDistance,
                                            float fadingRange
                                        )
    {
        GrassLoader grassLoader= new GrassLoader(tileSize,
                                                resolution,
                                                farViewingDistance,
                                                fadingRange,
                                                terrain,
                                                rootNode,
                                                camera
                                                );
        grassLoader.setName("GrassLoader" + tlIdx++);
        list.add(grassLoader);
        return grassLoader;
    }
    
    /**
     * A method for creating grass grids.
     * 
     * @param tileSize The tile size.
     * @param farViewingDistance The far viewing distance of the grass.
     * @param fadingRange The distance over which fading takes place.
     * @return A reference to the GrassGrid object.
     */
    public TreeLoader createTreeLoader( int tileSize,
                                        int resolution,
                                        float farViewingDistance,
                                        float fadingRange
                                    )
    {
        TreeLoader treeLoader = new TreeLoader( tileSize,
                                                resolution,
                                                farViewingDistance,
                                                fadingRange,
                                                //Not implemented yet.
                                                0f,
                                                rootNode,
                                                terrain,
                                                camera
                                               );
        treeLoader.setName("GrassLoader" + tlIdx++);
        list.add(treeLoader);
        return treeLoader;
    }
    
    
    /**
     * The NormDispGenerator can be used to create displacement and normal
     * maps out of images.
     * 
     * @return A NormDispGenerator object.
     */
    public NormDispGenerator createNormDispGenerator(){
        return new NormDispGenerator();
    }
    
    /**
     * The NormDispGenerator can be used to create displacement and normal
     * maps out of images.
     * 
     * @param filter What type of edge-detection filter to use when generating the normals.
     * Standard is Central Difference.
     * @param normalStrength The strength of normals decides how "prominent" edges are.
     * Default is 1f.
     * @param useInvertedDisp If this option is checked, the displacement maps will be
     * inverted. Such maps are often used in parallax mapping.
     * @return A NormDispGenerator object.
     */
    public NormDispGenerator createNormDispGenerator(Filter filter, float normalStrength, boolean useInvertedDisp){
        return new NormDispGenerator(filter,normalStrength,useInvertedDisp);
    }
    
    /**
     * This method creates a random generator that is designed especially for
     * generating large amounts of medium-quality random numbers. It uses
     * System.nanoTime() by default.
     * 
     * @return A FastRandom object.
     */
    public FastRandom createRandomGenerator(){
        return new FastRandom();
    }
    
    /**
     * This method creates a random generator that is designed especially for
     * generating large amounts of medium-quality random numbers.
     * 
     * @param seed A long-number used to seed the generator.
     * @return A FastRandom object.
     */
    public FastRandom createRandomGenerator(long seed){
        return new FastRandom(seed);
    }
    
    /**
     * Remove a Grass/Treeloader from the list.
     * 
     * @param loader The loader.
     */
    public void removeLoader(TileLoader loader){
        boolean result = list.remove(loader);
        if(!result){
            log.log(Level.INFO, "Object: {0} was not found in the Forester list.", loader.toString());
        }
        else
            log.log(Level.INFO, "Object: {0} was successfuly removed the Forester list.", loader.toString());
    }
    
    public void removeLoader(String name){
        for(int i = 0; i < list.size(); i++){
            if(name == null){
                log.log(Level.INFO, "Please specify a non-null string for a name.");
                return;
            }
            TileLoader loader = list.get(i);
            if(loader.getName() == null){
                continue;
            }
            if(loader.getName().equals(name)){
                list.remove(i);
                log.log(Level.INFO, "Object: {0} was successfuly removed the Forester list.", loader.toString());
                return;
            }
        }
        log.log(Level.INFO, "Object: {0} was not found in the Forester list.", name); 
    }
    
    public Application getApp() {
        return app;
    }

    public Camera getCamera() {
        return camera;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public Terrain getTerrain() {
        return terrain;
    }
    
    public ArrayList<TileLoader> getLoaderList(){
        return list;
    }

    public boolean isPhysicsEnabled() {
        return physicsEnabled;
    }

    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    public void setPhysicsSpace(PhysicsSpace physicsSpace) {
        this.physicsSpace = physicsSpace;
        if(physicsSpace == null){
            physicsEnabled = false;
            return;
        }
        physicsEnabled = true;
    }
    
    
    
} //Forester
