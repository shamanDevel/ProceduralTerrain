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
package org.shaman.terrain;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.jme3.util.SkyFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.shaman.terrain.heightmap.*;


/**
 * Uses the terrain's lighting texture with normal maps and lights.
 *
 * @author bowens
 */
public class TerrainHeighmapCreator extends SimpleApplication {
	private static final Logger LOG = Logger.getLogger(TerrainHeighmapCreator.class.getName());
	private static final int SIZE = 512;//1024;
	private static final float SLOPE_SCALE = 200f;
	private static final float SLOPE_POWER = 2f;
	
    private TerrainQuad terrain;
    private Material matTerrain;
    private Material matWire;
    private boolean wireframe = false;
    private boolean triPlanar = false;
    private boolean wardiso = false;
    private boolean minnaert = false;
    protected BitmapText hintText;
    private PointLight pl;
    private Geometry lightMdl;
    private float darkRockScale = 16;
    private float grassScale = 32;
	private CustomFlyByCamera camera;
    
	private ArrayList<HeightmapProcessor.PropItem> properties = new ArrayList<>();
	private int property = 0;
	private boolean changed;
	private BitmapText titleText;
	private BitmapText selectionText;
	private BitmapText propText;
	private HeightmapProcessor processors;
	private Heightmap heightmap;
	private Texture2D alphaMap;

    public static void main(String[] args) {
        TerrainHeighmapCreator app = new TerrainHeighmapCreator();
		app.getStateManager().detach(app.getStateManager().getState(FlyCamAppState.class));
        app.start();
    }

	private void updateAlphaMap() {
		Image image = alphaMap.getImage();
		ByteBuffer data = image.getData(0);
		if (data == null) {
			data = BufferUtils.createByteBuffer(SIZE*SIZE*4);
		}
		data.rewind();
		for (int x=0; x<SIZE; ++x) {
			for (int y=0; y<SIZE; ++y) {
				float slope = heightmap.getSlopeAt(y, SIZE-x-1);
				slope = (float) Math.pow(slope * SLOPE_SCALE, SLOPE_POWER);
				float g = Math.max(0, 1-slope);
				float r = 1-g;
				data.put((byte) (255*r)).put((byte) (255*g)).put((byte) 0).put((byte) 0);
			}
		}
		data.rewind();
		image.setFormat(Image.Format.RGBA8);
		image.setWidth(SIZE);
		image.setHeight(SIZE);
		image.setData(0, data);
		alphaMap.setMagFilter(Texture.MagFilter.Bilinear);
		matTerrain.setTexture("AlphaMap", alphaMap);
	}
	
    @Override
    public void simpleInitApp() {
        setupKeys();
//		loadHintText();
		
		initHeightmap();
		initScene();
		initPropertyUI();
    }
	
	private void initHeightmap() {
		//create processors
		ChainProcessor noiseChain = new ChainProcessor();
		float initFrequency = 2;
		for (int i=0; i<7; ++i) {
			PerlinNoiseProcessor noise = new PerlinNoiseProcessor(i+1, Math.pow(2, initFrequency+i), Math.pow(0.3, i+1));
			noiseChain.add(noise);
		}
		noiseChain.add(new NormalizationProcessor());
		ChainProcessor voronoiChain = new ChainProcessor();
		voronoiChain.add(new VoronoiProcessor());
		voronoiChain.add(new NormalizationProcessor());
		processors = new SplitCombineProcessor(
				new HeightmapProcessor[]{noiseChain, voronoiChain}, 
				new float[]{0.7f, 0.3f});
		properties.addAll(processors.getProperties());
		processors.reseed();
		heightmap = processors.apply(new Heightmap(SIZE));
		changed = false;
	}
	
	private void updateHeightmap() {
		long time1 = System.currentTimeMillis();
		if (!changed) {
			processors.reseed();
		}
		heightmap = processors.apply(new Heightmap(SIZE));
		changed = false;
		long time2 = System.currentTimeMillis();
		System.out.println("Time to apply processors: "+(time2-time1)/1000.0+" sec");
		
		time1 = System.currentTimeMillis();
		updateAlphaMap();
		time2 = System.currentTimeMillis();
		System.out.println("Time to update alpha map: "+(time2-time1)/1000.0+" sec");
		
		time1 = System.currentTimeMillis();
		updateTerrain();
		time2 = System.currentTimeMillis();
		System.out.println("Time to update mesh: "+(time2-time1)/1000.0+" sec");
	}
	
	private void initScene() {
		cam.setFrustumFar(10000);
		camera = new CustomFlyByCamera(cam);
		camera.registerWithInput(inputManager);
		
		// TERRAIN TEXTURE material
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", true);
        matTerrain.setFloat("Shininess", 0.0f);

        // ALPHA map (for splat textures)
		alphaMap = new Texture2D(SIZE, SIZE, Image.Format.ABGR8);
		updateAlphaMap();
        
        // DARK ROCK texture
        Texture darkRock = assetManager.loadTexture("org/shaman/terrain/rock2.jpg");
        darkRock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap", darkRock);
        matTerrain.setFloat("DiffuseMap_0_scale", darkRockScale/(float)SIZE);
        
        // GRASS texture
        Texture grass = assetManager.loadTexture("org/shaman/terrain/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_1", grass);
        matTerrain.setFloat("DiffuseMap_1_scale", grassScale/(float)SIZE);
        
        // NORMAL MAPS
        Texture normalMapRock = assetManager.loadTexture("org/shaman/terrain/rock_normal.png");
        normalMapRock.setWrap(WrapMode.Repeat);
        Texture normalMapGrass = assetManager.loadTexture("org/shaman/terrain/grass_normal.jpg");
        normalMapGrass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("NormalMap", normalMapRock);
        matTerrain.setTexture("NormalMap_1", normalMapGrass);
        
        // WIREFRAME material (used to debug the terrain, only useful for this test case)
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);
        
        createSky();

		updateTerrain();
        
        //Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        //terrain.generateDebugTangents(debugMat);

        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.1f, -0.1f, -0.1f)).normalize());
        rootNode.addLight(light);

        cam.setLocation(new Vector3f(0, 10, -10));
        cam.lookAtDirection(new Vector3f(0, -1.5f, -1).normalizeLocal(), Vector3f.UNIT_Y);
        
        rootNode.attachChild(createAxisMarker(20));
	}
	private void updateTerrain() {
		if (terrain != null) {
			rootNode.detachChild(terrain);
		}
		/*
         * Here we create the actual terrain. The tiles will be 65x65, and the total size of the
         * terrain will be 513x513. It uses the heightmap we created to generate the height values.
         */
        /**
         * Optimal terrain patch size is 65 (64x64).
         * The total size is up to you. At 1025 it ran fine for me (200+FPS), however at
         * size=2049 it got really slow. But that is a jump from 2 million to 8 million triangles...
         */
        terrain = new TerrainQuad("terrain", 65, SIZE+1, heightmap.getJMEHeightmap(128));
		terrain.setHeight(Vector2f.ZERO, speed);
//        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
//        control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
//        terrain.addControl(control);
        terrain.setMaterial(matTerrain);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
        terrain.setLocalTranslation(0, -SIZE/2, 0);
        terrain.setLocalScale(1f, 1f, 1f);
        rootNode.attachChild(terrain);
	}
	
	private void initPropertyUI() {		
		//acreate ui
		BitmapFont font = assetManager.loadFont("Interface/Fonts/Console.fnt");
		titleText = new BitmapText(font);
		titleText.setText(
				"Use arrow keys to select the property and modify it\n"
				+ "Press Enter to apply changes, press Enter again to generate new seeds");
		titleText.setLocalTranslation(0, settings.getHeight(), 0);
		guiNode.attachChild(titleText);
		selectionText = new BitmapText(font);
		selectionText.setText("->");
		selectionText.setLocalTranslation(0, settings.getHeight() - titleText.getHeight() - 5, 0);
		guiNode.attachChild(selectionText);
		propText = new BitmapText(font);
		propText.setText("");
		propText.setLocalTranslation(selectionText.getLineWidth() + 5, settings.getHeight() - titleText.getHeight() - 5, 0);
		guiNode.attachChild(propText);
		//add action listener
		inputManager.addMapping("PropUp", new KeyTrigger(KeyInput.KEY_RIGHT));
		inputManager.addMapping("PropDown", new KeyTrigger(KeyInput.KEY_LEFT));
		inputManager.addMapping("NextProp", new KeyTrigger(KeyInput.KEY_DOWN));
		inputManager.addMapping("PrevProp", new KeyTrigger(KeyInput.KEY_UP));
		inputManager.addMapping("ApplyChanges", new KeyTrigger(KeyInput.KEY_RETURN));
		inputManager.addListener(new ActionListener() {
			@Override
			public void onAction(String name, boolean isPressed, float tpf) {
				if (!isPressed) return;
				switch (name) {
					case "NextProp":
						property = Math.min(properties.size() - 1, property + 1);
						break;
					case "PrevProp":
						property = Math.max(0, property - 1);
						break;
					case "PropUp":
						changed |= properties.get(property).change(true);
//						updateHeightmap();
						break;
					case "PropDown":
						changed |= properties.get(property).change(false);
//						updateHeightmap();
						break;
					case "ApplyChanges":
						updateHeightmap();
						break;
					default:
						return;
				}
			}
		}, "PropUp", "PropDown", "NextProp", "PrevProp", "ApplyChanges");
	}
	private void updatePropertyUI() {
		StringBuilder str = new StringBuilder();
		StringBuilder str2 = new StringBuilder();
		for (int i=0; i<properties.size(); ++i) {
			if (i>0) {
				str.append('\n');
				str2.append('\n');
			}
			HeightmapProcessor.PropItem item = properties.get(i);
			str.append(item.getText());
			if (i == property) {
				str2.append("->");
			}
		}
		propText.setText(str);
		selectionText.setText(str2);
	}

    private void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setSize(guiFont.getCharSet().getRenderedSize());
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Hit T to switch to wireframe");
        guiNode.attachChild(hintText);
    }

    private void setupKeys() {
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "wireframe");
        inputManager.addMapping("WardIso", new KeyTrigger(KeyInput.KEY_9));
        inputManager.addListener(actionListener, "WardIso");
        inputManager.addMapping("DetachControl", new KeyTrigger(KeyInput.KEY_0));
        inputManager.addListener(actionListener, "DetachControl");
    }
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("wireframe") && !pressed) {
                wireframe = !wireframe;
                if (!wireframe) {
                    terrain.setMaterial(matWire);
                } else {
                    terrain.setMaterial(matTerrain);
                }
            } else if (name.equals("DetachControl") && !pressed) {
                TerrainLodControl control = terrain.getControl(TerrainLodControl.class);
                if (control != null)
                    control.detachAndCleanUpControl();
                else {
                    control = new TerrainLodControl(terrain, cam);
                    terrain.addControl(control);
                }
                    
            }
        }
    };

    private void createSky() {
        Texture west = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_west.jpg");
        Texture east = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_east.jpg");
        Texture north = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_north.jpg");
        Texture south = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_south.jpg");
        Texture up = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_up.jpg");
        Texture down = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_down.jpg");

        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        rootNode.attachChild(sky);
    }
    
    protected Node createAxisMarker(float arrowSize) {

        Material redMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMat.getAdditionalRenderState().setWireframe(true);
        redMat.setColor("Color", ColorRGBA.Red);
        
        Material greenMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        greenMat.getAdditionalRenderState().setWireframe(true);
        greenMat.setColor("Color", ColorRGBA.Green);
        
        Material blueMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blueMat.getAdditionalRenderState().setWireframe(true);
        blueMat.setColor("Color", ColorRGBA.Blue);

        Node axis = new Node();

        // create arrows
        Geometry arrowX = new Geometry("arrowX", new Arrow(new Vector3f(arrowSize, 0, 0)));
        arrowX.setMaterial(redMat);
        Geometry arrowY = new Geometry("arrowY", new Arrow(new Vector3f(0, arrowSize, 0)));
        arrowY.setMaterial(greenMat);
        Geometry arrowZ = new Geometry("arrowZ", new Arrow(new Vector3f(0, 0, arrowSize)));
        arrowZ.setMaterial(blueMat);
        axis.attachChild(arrowX);
        axis.attachChild(arrowY);
        axis.attachChild(arrowZ);

        //axis.setModelBound(new BoundingBox());
        return axis;
    }

	private boolean firstUpdate = true;
	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
		if (firstUpdate) {
			firstUpdate = false;
			camera.setEnabled(true);
			camera.setMoveSpeed(200);
		}
		updatePropertyUI();
	}
	
}
