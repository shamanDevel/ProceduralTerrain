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

import org.shaman.terrain.sketch.SketchTerrain;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;
import de.lessvoid.nifty.Nifty;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.shaman.terrain.erosion.WaterErosionSimulation;
import org.shaman.terrain.heightmap.*;
import org.shaman.terrain.polygonal.PolygonalMapGenerator;


/**
 * Uses the terrain's lighting texture with normal maps and lights.
 *
 * @author bowens
 */
public class TerrainHeighmapCreator extends SimpleApplication {
	private static final Logger LOG = Logger.getLogger(TerrainHeighmapCreator.class.getName());
	private static final float SLOPE_SCALE = 200f;
	private static final float SLOPE_POWER = 2f;
	public static final float HEIGHMAP_HEIGHT_SCALE = 48;
	public static final float TERRAIN_SCALE = 16;
	private static final boolean RECORDING = false;
	private static final int RECORDING_FRAMES = 1000 / 10; //20 FPS
	
	@SuppressWarnings("unchecked")
	private static final Class<? extends AbstractTerrainStep>[] STEPS = new Class[] {
		//RandomHeightmapGenerator.class,
		PolygonalMapGenerator.class,
		SketchTerrain.class,
		WaterErosionSimulation.class
	};
	private static final int FIRST_STEP_INDEX = 0;
	private AbstractTerrainStep[] steps;
	private static Class<? extends AbstractTerrainStep> loadedStep;
	private static Map<Object, Object> loadedProperties;
	
	private long recordingTime;
	private ScreenshotAppState screenshotAppState;
	
	private Thread renderThread;
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
	private Spatial sky;
	private WaterFilter water;
	private FilterPostProcessor waterFilter;
	
	private NiftyJmeDisplay niftyDisplay;
	private Nifty nifty;
	
	private Heightmap heightmap;
	private Texture2D alphaMap;

	@SuppressWarnings("unchecked")
    public static void main(String[] args) {
		//load save
		File root = new File("./saves/");
		JFileChooser chooser = new JFileChooser(root);
		chooser.addChoosableFileFilter(new FileNameExtensionFilter(null, "save"));
		chooser.setFileFilter(new FileNameExtensionFilter(null, "save"));
		int result = chooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(chooser.getSelectedFile())))) {
				loadedStep = (Class<? extends AbstractTerrainStep>) in.readObject();
				loadedProperties = (Map<Object, Object>) in.readObject();
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, "unable to load save file", ex);
			}
		}
		
		//create app
        TerrainHeighmapCreator app = new TerrainHeighmapCreator();
		app.getStateManager().detach(app.getStateManager().getState(FlyCamAppState.class));
		AppSettings settings = new AppSettings(true);
		settings.setUseJoysticks(true);
		settings.setResolution(1280, 800);
		settings.setVSync(true);
		app.setSettings(settings);
		app.setShowSettings(true);
        app.start();
    }
	
    @Override
    public void simpleInitApp() {
		renderThread = Thread.currentThread();
		steps = new AbstractTerrainStep[STEPS.length];
		for (int i=0; i<STEPS.length; ++i) {
			try {
				steps[i] = STEPS[i].newInstance();
			} catch (InstantiationException | IllegalAccessException ex) {
				Logger.getLogger(TerrainHeighmapCreator.class.getName()).log(Level.SEVERE, "Unable to create step", ex);
				stop();
				return;
			}
			stateManager.attach(steps[i]);
		}
		if (loadedStep!=null && loadedProperties!=null) {
			int index = ArrayUtils.indexOf(STEPS, loadedStep);
			if (index==-1) {
				LOG.log(Level.INFO, "unknown loaded step: {0}", loadedStep);
				steps[FIRST_STEP_INDEX].properties = new HashMap<>();
				steps[FIRST_STEP_INDEX].setEnabled(true);
			} else {
				steps[index].properties = loadedProperties;
				steps[index].setEnabled(true);
			}
		} else {
			steps[FIRST_STEP_INDEX].properties = new HashMap<>();
			steps[FIRST_STEP_INDEX].setEnabled(true);
		}
		
        setupKeys();
//		loadHintText();
		
		initNifty();
		initScene();
//		initPropertyUI();
		
//		polygonalMapGenerator = new PolygonalMapGenerator(this);
		
		//nextStep();
		screenshotAppState = new ScreenshotAppState();
		stateManager.attach(screenshotAppState);
		if (RECORDING) {
			recordingTime = System.currentTimeMillis();
		}
    }

	@Override
	public void handleError(String errMsg, Throwable t) {
		// Print error to log.
        LOG.log(Level.SEVERE, errMsg, t);
        // Display error message on screen if not in headless mode
        if (context.getType() != JmeContext.Type.Headless) {
            if (t != null) {
                JmeSystem.showErrorDialog(errMsg + "\n" + t.getClass().getSimpleName() +
                        (t.getMessage() != null ? ": " +  t.getMessage() : ""));
            } else {
                JmeSystem.showErrorDialog(errMsg);
            }
        }
		//do not stop application
	}
	
	private void initNifty() {
		niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
        nifty = niftyDisplay.getNifty();
        guiViewPort.addProcessor(niftyDisplay);
	}
	
	private void initScene() {
		cam.setFrustumFar(10000 * TERRAIN_SCALE);
		camera = new CustomFlyByCamera(cam);
		camera.registerWithInput(inputManager);
//		camera.setDragToRotate(true);
		inputManager.setCursorVisible(true);
		
		// TERRAIN TEXTURE material
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", true);
        matTerrain.setFloat("Shininess", 0.0f);

        // ALPHA map (for splat textures)
		alphaMap = new Texture2D(256, 256, Image.Format.ABGR8);
//		updateAlphaMap();
        
        // DARK ROCK texture
        Texture darkRock = assetManager.loadTexture("org/shaman/terrain/rock2.jpg");
        darkRock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap", darkRock);
        matTerrain.setFloat("DiffuseMap_0_scale", darkRockScale/(float)256);
        
        // GRASS texture
        Texture grass = assetManager.loadTexture("org/shaman/terrain/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_1", grass);
        matTerrain.setFloat("DiffuseMap_1_scale", grassScale/(float)256);
        
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
		createWater();

//		updateTerrain();
        
        //Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        //terrain.generateDebugTangents(debugMat);

        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.1f, -0.1f, -0.1f)).normalize());
        rootNode.addLight(light);
		AmbientLight ambientLight = new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1));
		rootNode.addLight(ambientLight);

        cam.setLocation(new Vector3f(0, 100 * TERRAIN_SCALE, -100 * TERRAIN_SCALE));
        cam.lookAtDirection(new Vector3f(0, -1.5f, -1).normalizeLocal(), Vector3f.UNIT_Y);
        
        rootNode.attachChild(createAxisMarker(20));
	}
	
	private void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setSize(guiFont.getCharSet().getRenderedSize());
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Hit T to switch to wireframe");
        guiNode.attachChild(hintText);
    }
	
	/**
	 * Sets the terrain to the new heightmap, updates the sizes, alpha map and
	 * terrain mesh.
	 * If you pass {@code null}, the heightmap is disabled.
	 * @param map the new map
	 */
	public void setTerrain(Heightmap map) {
		this.heightmap = map;
		if (map == null) {
			if (terrain != null) {
				rootNode.detachChild(terrain);
			}
			terrain = null;
			return;
		}
		alphaMap = new Texture2D(map.getSize(), map.getSize(), Image.Format.ABGR8);
		updateAlphaMap();
		updateTerrain();
	}
	public void forceTerrainMaterial(Material mat) {
		if (mat == null) {
			terrain.setMaterial(matTerrain);
		} else {
			terrain.setMaterial(mat);
		}
	}
	
	public void updateAlphaMap() {		
		Image image = alphaMap.getImage();
		ByteBuffer data = image.getData(0);
		if (data == null) {
			data = BufferUtils.createByteBuffer(heightmap.getSize()*heightmap.getSize()*4);
		}
		data.rewind();
		for (int x=0; x<heightmap.getSize(); ++x) {
			for (int y=0; y<heightmap.getSize(); ++y) {
				float slope = heightmap.getSlopeAt(y, heightmap.getSize()-x-1);
				slope = (float) Math.pow(slope * SLOPE_SCALE, SLOPE_POWER);
				float g = Math.max(0, 1-slope);
				float r = 1-g;
				data.put((byte) (255*r)).put((byte) (255*g)).put((byte) 0).put((byte) 0);
			}
		}
		data.rewind();
		image.setFormat(Image.Format.RGBA8);
		image.setWidth(heightmap.getSize());
		image.setHeight(heightmap.getSize());
		image.setData(0, data);
		alphaMap.setMagFilter(Texture.MagFilter.Bilinear);
		matTerrain.setTexture("AlphaMap", alphaMap);
	}
	
	public void updateTerrain() {
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
        terrain = new TerrainQuad("terrain", 65, heightmap.getSize()+1, heightmap.getJMEHeightmap(HEIGHMAP_HEIGHT_SCALE));
//        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
//        control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
//        terrain.addControl(control);
        terrain.setMaterial(matTerrain);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
        terrain.setLocalTranslation(0, -HEIGHMAP_HEIGHT_SCALE/2, 0);
        terrain.setLocalScale(TERRAIN_SCALE);
		rootNode.attachChild(terrain);
	}
	
	/**
	 * Converts a heightmap position in a real-world position
	 * @param x the heightmap x-coordinate
	 * @param y the heightmap y-coordinate
	 * @return the position of that corner in real-world
	 */
	public Vector3f getHeightmapPoint(int x, int y) {
		float h = heightmap.getHeightAt(x, y);
		Vector3f v = new Vector3f(x - heightmap.getSize()/2, h*HEIGHMAP_HEIGHT_SCALE -HEIGHMAP_HEIGHT_SCALE/2, y - heightmap.getSize()/2);
		v.multLocal(TERRAIN_SCALE);
		return v;
	}
	
	/**
	 * Converts a heightmap position in a real-world position
	 * @param x the heightmap x-coordinate
	 * @param y the heightmap y-coordinate
	 * @return the position of that corner in real-world
	 */
	public Vector3f getHeightmapPoint(float x, float y) {
		float h = heightmap.getHeightInterpolating(x, y);
		Vector3f v = new Vector3f(x - heightmap.getSize()/2, h*HEIGHMAP_HEIGHT_SCALE -HEIGHMAP_HEIGHT_SCALE/2, y - heightmap.getSize()/2);
		v.multLocal(TERRAIN_SCALE);
		return v;
	}
	
	public Vector3f mapHeightmapToWorld(float x, float y, float h) {
		Vector3f v = new Vector3f(x - heightmap.getSize()/2, h*HEIGHMAP_HEIGHT_SCALE -HEIGHMAP_HEIGHT_SCALE/2, y - heightmap.getSize()/2);
		v.multLocal(TERRAIN_SCALE);
		return v;
	}
	
	public Vector3f mapWorldToHeightmap(Vector3f world) {
		float x = world.x/TERRAIN_SCALE + heightmap.getSize()/2;
		float y = world.z/TERRAIN_SCALE + heightmap.getSize()/2;
		float h = (world.y/TERRAIN_SCALE + HEIGHMAP_HEIGHT_SCALE/2) / HEIGHMAP_HEIGHT_SCALE;
		return new Vector3f(x, y, h);
	}
	
	public Spatial getHeightmapSpatial() {
		return terrain;
	}
	
	public Nifty getNifty() {
		return nifty;
	}
	
	public void save(Map<Object, Object> properties, Class<? extends AbstractTerrainStep> step,
			@Nullable String fileName) {
		File file;
		File root = new File("./saves/");
		//select file
		if (fileName==null) {
			JFileChooser chooser = new JFileChooser(root);
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(null, "save"));
			chooser.setFileFilter(new FileNameExtensionFilter(null, "save"));
			int result = chooser.showSaveDialog(null);
			if (result != JFileChooser.APPROVE_OPTION) {
				return;
			}
			file = chooser.getSelectedFile();
		} else {
			file = new File(root, fileName);
		}
		//save file
		try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			out.writeObject(step);
			out.writeObject(properties);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "unable to save file", ex);
			return;
		}
		LOG.info("file saved to "+file);
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

        sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        rootNode.attachChild(sky);
    }
	private void createWater() {
		water = new WaterFilter(rootNode, new Vector3f(-0.1f, -0.1f, -0.1f));
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
        water.setWaterHeight(0);
		waterFilter = new FilterPostProcessor(assetManager);
		waterFilter.addFilter(water);
	}
	
	/**
	 * Enables or disables the sky.
	 * @param enabled {@code true} to display the sky, {@code false} to hide it
	 */
	public void setSkyEnabled(boolean enabled) {
		sky.setCullHint(enabled ? Spatial.CullHint.Never : Spatial.CullHint.Always);
	}
	
	public void enableWater(float waterHeight) {
		water.setWaterHeight(waterHeight);
		if (!viewPort.getProcessors().contains(waterFilter)) {
			viewPort.addProcessor(waterFilter);
		}
	}
	public void disableWater() {
		viewPort.removeProcessor(waterFilter);
	}
	
	/**
	 * Enables or disables the camera.
	 * If the camera is disabled, you cannot longer move around.
	 * @param enabled 
	 */
	public void setCameraEnabled(boolean enabled) {
		camera.setEnabled(enabled);
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
			camera.setMoveSpeed(200 * TERRAIN_SCALE);
		}
		if (RECORDING) {
			long time = System.currentTimeMillis();
			if (time > recordingTime + RECORDING_FRAMES) {
				recordingTime = time;
				screenshotAppState.takeScreenshot();
				System.out.println("shot");
			}
		}
	}
	
}
