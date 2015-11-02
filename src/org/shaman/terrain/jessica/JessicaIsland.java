/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.shaman.terrain.jessica;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;
import java.nio.ByteBuffer;

/**
 * Demonstrates how to use terrain.
 * The base terrain class it uses is TerrainQuad, which is a quad tree of actual
 * meshes called TerainPatches.
 * There are a couple options for the terrain in this test:
 * The first is wireframe mode. Here you can see the underlying trianglestrip structure.
 * You will notice some off lines; these are degenerate triangles and are part of the
 * trianglestrip. They are only noticeable in wireframe mode.
 * Second is Tri-Planar texture mode. Here the textures are rendered on all 3 axes and
 * then blended together to reduce distortion and stretching.
 * Third, which you have to modify the code to see, is Entropy LOD calculations.
 * In the constructor for the TerrainQuad, un-comment the final parameter that is
 * the LodPerspectiveCalculatorFactory. Then you will see the terrain flicker to start
 * while it calculates the entropies. Once it is done, it will pick the best LOD value
 * based on entropy. This method reduces "popping" of terrain greatly when LOD levels
 * change. It is highly suggested you use it in your app.
 *
 * @author bowens
 */
public class JessicaIsland extends SimpleApplication {
	private Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);

    private TerrainQuad terrain;
    Material matRock;
    Material matWire;
    boolean wireframe = false;
    boolean triPlanar = false;
    protected BitmapText hintText;
    PointLight pl;
    Geometry lightMdl;
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;

    public static void main(String[] args) {
        JessicaIsland app = new JessicaIsland();
        app.start();
    }

    @Override
    public void initialize() {
        super.initialize();

        loadHintText();
    }

    @Override
    public void simpleInitApp() {
        setupKeys();

        // First, we load up our textures and the heightmap texture for the terrain

        // TERRAIN TEXTURE material
        Material matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", true);

		Texture heightMapImage = assetManager.loadTexture("org/shaman/terrain/jessica/Heightmap2.png");
		
        // ALPHA map (for splat textures)
		Texture2D alphaMap = new Texture2D(heightMapImage.getImage().getWidth(), 
				heightMapImage.getImage().getHeight(), Image.Format.ABGR8);
        
        // DARK ROCK texture
        Texture darkRock = assetManager.loadTexture("org/shaman/terrain/rock2.jpg");
        darkRock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap", darkRock);
        matTerrain.setFloat("DiffuseMap_0_scale", 16/256f);
        
        // GRASS texture
        Texture grass = assetManager.loadTexture("org/shaman/terrain/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_1", grass);
        matTerrain.setFloat("DiffuseMap_1_scale", 32/256f);
        
        // NORMAL MAPS
        Texture normalMapRock = assetManager.loadTexture("org/shaman/terrain/rock_normal.png");
        normalMapRock.setWrap(WrapMode.Repeat);
        Texture normalMapGrass = assetManager.loadTexture("org/shaman/terrain/grass_normal.jpg");
        normalMapGrass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("NormalMap", normalMapRock);
        matTerrain.setTexture("NormalMap_1", normalMapGrass);


        // CREATE HEIGHTMAP
        AbstractHeightMap heightmap = null;
        try {
            //heightmap = new HillHeightMap(1025, 1000, 50, 100, (byte) 3);

            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 1f);
            heightmap.load();

        } catch (Exception e) {
            e.printStackTrace();
        }
		
		Image image = alphaMap.getImage();
		ByteBuffer data = image.getData(0);
		if (data == null) {
			data = BufferUtils.createByteBuffer(512*512*4);
		}
		data.rewind();
		for (int x=0; x<512; ++x) {
			for (int y=0; y<512; ++y) {
				float r = 1, g=0;
				data.put((byte) (255*r)).put((byte) (255*g)).put((byte) 0).put((byte) 0);
			}
		}
		data.rewind();
		image.setFormat(Image.Format.RGBA8);
		image.setWidth(512);
		image.setHeight(512);
		image.setData(0, data);
		alphaMap.setMagFilter(Texture.MagFilter.Bilinear);
		matTerrain.setTexture("AlphaMap", alphaMap);

        /*
         * Here we create the actual terrain. The tiles will be 65x65, and the total size of the
         * terrain will be 513x513. It uses the heightmap we created to generate the height values.
         */
        /**
         * Optimal terrain patch size is 65 (64x64).
         * The total size is up to you. At 1025 it ran fine for me (200+FPS), however at
         * size=2049, it got really slow. But that is a jump from 2 million to 8 million triangles...
         */
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matTerrain);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 0.5f, 2f);
        rootNode.attachChild(terrain);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(1f));
        rootNode.addLight(sun);
        
        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        rootNode.addLight(al);

        cam.setLocation(new Vector3f(0, 10, -10));
        cam.lookAtDirection(new Vector3f(0, -1.5f, -1).normalizeLocal(), Vector3f.UNIT_Y);
		
		Spatial sky = SkyFactory.createSky(assetManager, "Scenes/Beach/FullskiesSunset0068.dds", false);
        sky.setLocalScale(350);
		rootNode.attachChild(sky);
		
		//Water Filter
        WaterFilter water = new WaterFilter(rootNode, lightDir);
        water.setWaterColor(new ColorRGBA().setAsSrgb(0.0078f, 0.3176f, 0.5f, 1.0f));
        water.setDeepWaterColor(new ColorRGBA().setAsSrgb(0.0039f, 0.00196f, 0.145f, 1.0f));
        water.setUnderWaterFogDistance(80);
        water.setWaterTransparency(0.12f);
        water.setFoamIntensity(0.4f);        
        water.setFoamHardness(0.3f);
        water.setFoamExistence(new Vector3f(0.8f, 8f, 1f));
        water.setReflectionDisplace(50);
        water.setRefractionConstant(0.25f);
        water.setColorExtinction(new Vector3f(30, 50, 70));
        water.setCausticsIntensity(0.4f);        
        water.setWaveScale(0.003f);
        water.setMaxAmplitude(2f);
        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
        water.setRefractionStrength(0.2f);
        water.setWaterHeight(-50);
		cam.setFrustumFar(10000);
        
        //Bloom Filter
        BloomFilter bloom = new BloomFilter();        
        bloom.setExposurePower(55);
        bloom.setBloomIntensity(1.0f);
        
        //Light Scattering Filter
        LightScatteringFilter lsf = new LightScatteringFilter(lightDir.mult(-300));
        lsf.setLightDensity(0.5f);   
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        
        fpp.addFilter(water);
//        fpp.addFilter(bloom);
//        fpp.addFilter(lsf);
        fpp.addFilter(new FXAAFilter());
		viewPort.addProcessor(fpp);
    }

    public void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setSize(guiFont.getCharSet().getRenderedSize());
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Hit T to switch to wireframe,  P to switch to tri-planar texturing");
        guiNode.attachChild(hintText);
    }

    private void setupKeys() {
        flyCam.setMoveSpeed(50);
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "wireframe");
        inputManager.addMapping("triPlanar", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(actionListener, "triPlanar");
    }
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("wireframe") && !pressed) {
                wireframe = !wireframe;
                if (!wireframe) {
                    terrain.setMaterial(matWire);
                } else {
                    terrain.setMaterial(matRock);
                }
            } else if (name.equals("triPlanar") && !pressed) {
                triPlanar = !triPlanar;
                if (triPlanar) {
                    matRock.setBoolean("useTriPlanarMapping", true);
                    // planar textures don't use the mesh's texture coordinates but real world coordinates,
                    // so we need to convert these texture coordinate scales into real world scales so it looks
                    // the same when we switch to/from tr-planar mode
                    matRock.setFloat("Tex1Scale", 1f / (float) (512f / grassScale));
                    matRock.setFloat("Tex2Scale", 1f / (float) (512f / dirtScale));
                    matRock.setFloat("Tex3Scale", 1f / (float) (512f / rockScale));
                } else {
                    matRock.setBoolean("useTriPlanarMapping", false);
                    matRock.setFloat("Tex1Scale", grassScale);
                    matRock.setFloat("Tex2Scale", dirtScale);
                    matRock.setFloat("Tex3Scale", rockScale);
                }
            }
        }
    };
}