/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.*;
import de.lessvoid.nifty.elements.Element;
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
	
	private Nifty nifty;
	private Screen screen;
	private Button skipStepButton;
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
	private Button generateMapButton;
	private Button nextStepButton;
	private Button continueEditingButton;
	private Element waitPopup;
	
	public PolygonalScreenController(PolygonalMapGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void bind(Nifty nifty, Screen screen) {
		this.nifty = nifty;
		this.screen = screen;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onStartScreen() {
		skipStepButton = screen.findNiftyControl("SkipStepButton", Button.class);
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
		generateMapButton = screen.findNiftyControl("GenerateMapButton", Button.class);
		nextStepButton = screen.findNiftyControl("NextStepButton", Button.class);
		continueEditingButton = screen.findNiftyControl("ContinueButton", Button.class);
		
		pointCountDropDown.addAllItems(Arrays.asList(500, 1000, 1500, 2000, 2500, 3000));
		relaxationDropDown.addAllItems(Arrays.asList("no relaxation", "1x", "2x", "3x", "4x"));
		coastlineDropDown.addAllItems(Arrays.asList("perlin", "circular"));
		mapSizeDropDown.addAllItems(Arrays.asList(512, 1024, 2048));
		nextStepButton.setEnabled(false);
		continueEditingButton.setEnabled(false);
		
		String seed1 = randomSeed();
		String seed2 = randomSeed();
		seedTextField.setText(seed1);
		pointCountDropDown.selectItemByIndex(3);
		relaxationDropDown.selectItemByIndex(2);
		coastlineDropDown.selectItemByIndex(0);
		mapSeedTextField.setText(seed2);
		mapSizeDropDown.selectItemByIndex(1);
		generator.guiInitialValues(seed1.hashCode(), 2000, 2, 
				PolygonalMapGenerator.Coastline.PERLIN, 1024, seed2.hashCode());
		
		biomesCheckBox.check();
	}
	
	private String randomSeed() {
		return Long.toHexString(Math.abs(rand.nextLong() % (1l<<48)));
	}

	@Override
	public void onEndScreen() {
		
	}
	
	void setEditingEnabled(boolean enabled) {
		seedTextField.setEnabled(enabled);
		seedButton.setEnabled(enabled);
		pointCountDropDown.setEnabled(enabled);
		relaxationDropDown.setEnabled(enabled);
		coastlineDropDown.setEnabled(enabled);
		generateElevationButton.setEnabled(enabled);
		elevationCheckBox.setEnabled(enabled);
		generateBiomesButton.setEnabled(enabled);
		temperatureCheckBox.setEnabled(enabled);
		moistureCheckBox.setEnabled(enabled);
		biomesCheckBox.setEnabled(enabled);
		mapSeedTextField.setEnabled(enabled);
		mapSeedButton.setEnabled(enabled);
		mapSizeDropDown.setEnabled(enabled);
		generateMapButton.setEnabled(enabled);
		nextStepButton.setEnabled(!enabled);
		continueEditingButton.setEnabled(!enabled);
	}
	
	void showWaitPopup(boolean show) {
		if (show) {
			waitPopup = nifty.createPopup("popupWait");
			nifty.showPopup(screen, waitPopup.getId(), null);
		} else {
			nifty.closePopup(waitPopup.getId());
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*Button")
	public void onButtonClick(String id, ButtonClickedEvent e) {
		System.out.println("button "+id+" clicked: "+e);
		if (skipStepButton == e.getButton()) {
			generator.guiSkipStep();
		} else if (seedButton == e.getButton()) {
			String seed = randomSeed();
			seedTextField.setText(seed);
			generator.guiSeedChanged(seed.hashCode());
		} else if (mapSeedButton == e.getButton()) {
			String seed = randomSeed();
			mapSeedTextField.setText(seed);
			generator.guiMapSeedChanged(seed.hashCode());
		} else if (generateElevationButton == e.getButton()) {
			generator.guiGenerateElevation();
			if (!elevationCheckBox.isChecked()) elevationCheckBox.check();
		} else if (generateBiomesButton == e.getButton()) {
			generator.guiGenerateBiomes();
			if (!biomesCheckBox.isChecked()) biomesCheckBox.check();
		} else if (generateMapButton == e.getButton()) {
			generator.guiGenerateMap();
		} else if (nextStepButton == e.getButton()) {
			generator.guiNextStep();
		} else if (continueEditingButton == e.getButton()) {
			generator.guiContinueEditing();
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
		} else if (temperatureCheckBox == e.getCheckBox()) {
			generator.guiShowDrawTemperature(e.isChecked());
			if (e.isChecked()) {
				elevationCheckBox.setChecked(false);
				biomesCheckBox.setChecked(false);
				moistureCheckBox.setChecked(false);
			}
		} else if (moistureCheckBox == e.getCheckBox()) {
			generator.guiShowDrawMoisture(e.isChecked());
			if (e.isChecked()) {
				elevationCheckBox.setChecked(false);
				biomesCheckBox.setChecked(false);
				temperatureCheckBox.setChecked(false);
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
		} else if (mapSizeDropDown == e.getDropDown()) {
			generator.guiMapSizeChanged(mapSizeDropDown.getSelection());
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*TextField")
	public void onSeedChange(String id, TextFieldChangedEvent e) {
		System.out.println("textfield "+id+" changed: "+e.getText());
		if (seedTextField==e.getTextFieldControl()) {
			generator.guiSeedChanged(e.getText().hashCode());
		} else if (mapSeedTextField == e.getTextFieldControl()) {
			generator.guiMapSeedChanged(e.getText().hashCode());
		}
	}
}
