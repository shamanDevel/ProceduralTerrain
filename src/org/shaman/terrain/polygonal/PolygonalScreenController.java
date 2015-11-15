/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.*;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author Sebastian Weiss
 */
public class PolygonalScreenController implements ScreenController {
	private final PolygonalMapGenerator generator;
	private final Random rand = new Random();
	
	private Screen screen;
	private Button nextStepButton;
	private TextField seedTextField;
	private Button seedButton;
	private DropDown<Integer> pointCountDropDown;
	private DropDown<String> relaxationDropDown;
	private DropDown<String> coastlineDropDown;
	private Button generateElevationButton;
	private CheckBox elevationCheckBox;
	private Button generateBiomesButton;
	private CheckBox temperatureCheckBox;
	private CheckBox moistureCheckBox;
	private CheckBox biomesCheckBox;
	private TextField mapSeedTextField;
	private Button mapSeedButton;
	private DropDown<Integer> mapSizeDropDown;

	public PolygonalScreenController(PolygonalMapGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void bind(Nifty nifty, Screen screen) {
		this.screen = screen;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onStartScreen() {
		nextStepButton = screen.findNiftyControl("NextStepButton", Button.class);
		seedTextField = screen.findNiftyControl("SeedTextField", TextField.class);
		seedButton = screen.findNiftyControl("SeedButton", Button.class);
		pointCountDropDown = screen.findNiftyControl("PointCountDropDown", DropDown.class);
		relaxationDropDown = screen.findNiftyControl("RelaxationDropDown", DropDown.class);
		coastlineDropDown = screen.findNiftyControl("CoastlineDropDown", DropDown.class);
		generateElevationButton = screen.findNiftyControl("GenerateElevationButton", Button.class);
		elevationCheckBox = screen.findNiftyControl("ElevationCheckBox", CheckBox.class);
		generateBiomesButton = screen.findNiftyControl("GenerateBiomesButton", Button.class);
		temperatureCheckBox = screen.findNiftyControl("TemperatureCheckBox", CheckBox.class);
		moistureCheckBox = screen.findNiftyControl("MoistureCheckBox", CheckBox.class);
		biomesCheckBox = screen.findNiftyControl("BiomesCheckBox", CheckBox.class);
		mapSeedTextField = screen.findNiftyControl("MapSeedTextField", TextField.class);
		mapSeedButton = screen.findNiftyControl("MapSeedButton", Button.class);
		mapSizeDropDown = screen.findNiftyControl("MapSizeDropDown", DropDown.class);
		
		pointCountDropDown.addAllItems(Arrays.asList(500, 1000, 2000, 4000, 6000, 8000));
		relaxationDropDown.addAllItems(Arrays.asList("no relaxation", "1x", "2x", "3x", "4x"));
		coastlineDropDown.addAllItems(Arrays.asList("perlin", "circular"));
		mapSizeDropDown.addAllItems(Arrays.asList(256, 512, 1024, 2048, 4096, 8192));
		
		String seed1 = randomSeed();
		String seed2 = randomSeed();
		seedTextField.setText(seed1);
		pointCountDropDown.selectItemByIndex(2);
		relaxationDropDown.selectItemByIndex(2);
		coastlineDropDown.selectItemByIndex(0);
		mapSeedTextField.setText(seed2);
		mapSizeDropDown.selectItemByIndex(1);
		generator.guiInitialValues(seed1.hashCode(), 2000, 2, PolygonalMapGenerator.Coastline.PERLIN);
		
		biomesCheckBox.check();
	}
	
	private String randomSeed() {
		return Long.toHexString(Math.abs(rand.nextLong() % (1l<<48)));
	}

	@Override
	public void onEndScreen() {
		
	}
	
	@NiftyEventSubscriber(pattern = ".*Button")
	public void onButtonClick(String id, ButtonClickedEvent e) {
		System.out.println("button "+id+" clicked: "+e);
		if (nextStepButton == e.getButton()) {
			generator.guiNextStep();
		} else if (seedButton == e.getButton()) {
			String seed = randomSeed();
			seedTextField.setText(seed);
			generator.guiSeedChanged(seed.hashCode());
		} else if (mapSeedButton == e.getButton()) {
			String seed = randomSeed();
			mapSeedTextField.setText(seed);
			
		} else if (generateElevationButton == e.getButton()) {
			generator.guiGenerateElevation();
			if (!elevationCheckBox.isChecked()) elevationCheckBox.check();
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*CheckBox")
	public void onCheckBoxClick(String id, CheckBoxStateChangedEvent e) {
		System.out.println("checkbox "+id+" changed: "+e);
		if (elevationCheckBox == e.getCheckBox()) {
			generator.guiShowDrawElevation(e.isChecked());
			if (e.isChecked()) {
				biomesCheckBox.setChecked(false);
				temperatureCheckBox.setChecked(false);
				moistureCheckBox.setChecked(false);
			}
		} else if (biomesCheckBox == e.getCheckBox()) {
			generator.guiShowBiomes(e.isChecked());
			if (e.isChecked()) {
				elevationCheckBox.setChecked(false);
				temperatureCheckBox.setChecked(false);
				moistureCheckBox.setChecked(false);
			}
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*DropDown")
	public void onSelectionChange(String id, DropDownSelectionChangedEvent e) {
		System.out.println("dropdown "+id+" changed: "+e.getSelection());
		if (pointCountDropDown == e.getDropDown()) {
			generator.guiPointCountChanged(pointCountDropDown.getSelection());
		} else if (relaxationDropDown == e.getDropDown()) {
			generator.guiRelaxationChanged(relaxationDropDown.getSelectedIndex());
		} else if (coastlineDropDown == e.getDropDown()) {
			generator.guiCoastlineChanged(PolygonalMapGenerator.Coastline.values()[coastlineDropDown.getSelectedIndex()]);
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*TextField")
	public void onSeedChange(String id, TextFieldChangedEvent e) {
		System.out.println("textfield "+id+" changed: "+e.getText());
		if (seedTextField==e.getTextFieldControl()) {
			generator.guiSeedChanged(e.getText().hashCode());
		}
	}
}
