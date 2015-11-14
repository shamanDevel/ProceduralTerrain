/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.sketch.SketchTerrain;

/**
 *
 * @author Sebastian Weiss
 */
public class RandomHeightmapGenerator extends AbstractTerrainStep {
	private static final int SIZE = 256;//1024;
	private static final Class<? extends AbstractTerrainStep> NEXT_STEP = SketchTerrain.class;
	
	private ArrayList<HeightmapProcessor.PropItem> propItems = new ArrayList<>();
	private int property = 0;
	private boolean propertiesEnabled = true;
	private boolean changed;
	private BitmapText titleText;
	private BitmapText selectionText;
	private BitmapText propText;
	private HeightmapProcessor processors;
	private Heightmap heightmap;
	private ActionListener listener;

	@Override
	protected void enable() {
		initHeightmap();
		initPropertyUI();
	}

	@Override
	protected void disable() {
		removePropertyUI();
	}

	@Override
	public void update(float tpf) {
		updatePropertyUI();
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
//		voronoiChain.add(new DistortionProcessor(0.01f, 8));
		voronoiChain.add(new NormalizationProcessor());
		ChainProcessor finalChain = new ChainProcessor();
		finalChain.add(new SplitCombineProcessor(
				new HeightmapProcessor[]{noiseChain, voronoiChain}, 
				new float[]{0.6f, 0.3f}));
		finalChain.add(new DistortionProcessor());
		finalChain.add(new ThermalErosionProcessor());
		processors = finalChain;
		propItems.addAll(processors.getProperties());
		processors.reseed();
		heightmap = processors.apply(new Heightmap(SIZE));
		changed = false;
		app.setTerrain(heightmap);
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
		app.setTerrain(heightmap);
		time2 = System.currentTimeMillis();
		System.out.println("Time to update alpha map and mesh: "+(time2-time1)/1000.0+" sec");
	}
	
	private void nextStep() {
		Map<Object, Object> prop = new HashMap<>(properties);
		prop.put(KEY_HEIGHTMAP, heightmap);
		nextStep(NEXT_STEP, prop);
	}
	
	private void initPropertyUI() {		
		//acreate ui
		guiNode.detachAllChildren();
		BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Console.fnt");
		titleText = new BitmapText(font);
		titleText.setText(
				"Use arrow keys to select the property and modify it\n"
				+ "Press Enter to apply changes, press Enter again to generate new seeds\n"
				+ "Press Space to end this step and continue to terrain sketching");
		titleText.setLocalTranslation(0, app.getCamera().getHeight(), 0);
		guiNode.attachChild(titleText);
		selectionText = new BitmapText(font);
		selectionText.setText("->");
		selectionText.setLocalTranslation(0, app.getCamera().getHeight() - titleText.getHeight() - 5, 0);
		guiNode.attachChild(selectionText);
		propText = new BitmapText(font);
		propText.setText("");
		propText.setLocalTranslation(selectionText.getLineWidth() + 5, app.getCamera().getHeight() - titleText.getHeight() - 5, 0);
		guiNode.attachChild(propText);
		//add action listener
		InputManager inputManager = app.getInputManager();
		inputManager.addMapping("PropUp", new KeyTrigger(KeyInput.KEY_RIGHT));
		inputManager.addMapping("PropDown", new KeyTrigger(KeyInput.KEY_LEFT));
		inputManager.addMapping("NextProp", new KeyTrigger(KeyInput.KEY_DOWN));
		inputManager.addMapping("PrevProp", new KeyTrigger(KeyInput.KEY_UP));
		inputManager.addMapping("ApplyChanges", new KeyTrigger(KeyInput.KEY_RETURN));
		inputManager.addMapping("NextStep", new KeyTrigger(KeyInput.KEY_SPACE));
		listener = new ActionListener() {
			@Override
			public void onAction(String name, boolean isPressed, float tpf) {
				if (!isPressed) return;
				if (!propertiesEnabled) return;
				switch (name) {
					case "NextProp":
						property = Math.min(propItems.size() - 1, property + 1);
						break;
					case "PrevProp":
						property = Math.max(0, property - 1);
						break;
					case "PropUp":
						changed |= propItems.get(property).change(true);
//						updateHeightmap();
						break;
					case "PropDown":
						changed |= propItems.get(property).change(false);
//						updateHeightmap();
						break;
					case "ApplyChanges":
						updateHeightmap();
						break;
					case "NextStep":
						nextStep();
						break;
					default:
						return;
				}
			}
		};
		inputManager.addListener(listener, "PropUp", "PropDown", "NextProp", "PrevProp", "ApplyChanges", "NextStep");
	}
	private void removePropertyUI() {
		InputManager inputManager = app.getInputManager();
		inputManager.removeListener(listener);
	}
	private void updatePropertyUI() {
		StringBuilder str = new StringBuilder();
		StringBuilder str2 = new StringBuilder();
		for (int i=0; i<propItems.size(); ++i) {
			if (i>0) {
				str.append('\n');
				str2.append('\n');
			}
			HeightmapProcessor.PropItem item = propItems.get(i);
			str.append(item.getText());
			if (i == property) {
				str2.append("->");
			}
		}
		propText.setText(str);
		selectionText.setText(str2);
	}

}
