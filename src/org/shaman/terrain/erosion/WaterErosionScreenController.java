/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.erosion;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.*;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.Arrays;

/**
 *
 * @author Sebastian Weiss
 */
public class WaterErosionScreenController implements ScreenController {

	private final WaterErosionSimulation simulation;
	private Nifty nifty;
	private Screen screen;
	private int originalSize;
	
	private DropDown<Integer> upscaleDropDown;
	private CheckBox temperatureCheckBox;
	private CheckBox moistureCheckBox;
	private Slider brushSizeSlider;
	private CheckBox addSourceCheckBox;
	private CheckBox editSourceCheckBox;
	private Button deleteSourceButton;
	private Slider sourceRadiusSlider;
	private Slider sourceIntensitySlider;
	private Button runButton;
	private Button stopButton;
	private Button resetButton;
	private Label iterationsLabel;
	private CheckBox heightDifferenceCheckBox;
	private CheckBox rainCheckBox;
	private CheckBox riverCheckBox;
	private Button deleteWaterButton;
	private Label messageLabel;

	public WaterErosionScreenController(WaterErosionSimulation simulation, int mapSize) {
		this.simulation = simulation;
		this.originalSize = mapSize;
	}
	
	
	@Override
	public void bind(Nifty nifty, Screen screen) {
		this.nifty = nifty;
		this.screen = screen;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onStartScreen() {
		upscaleDropDown = screen.findNiftyControl("UpscaleDropDown", DropDown.class);
		temperatureCheckBox = screen.findNiftyControl("TemperatureCheckBox", CheckBox.class);
		moistureCheckBox = screen.findNiftyControl("MoistureCheckBox", CheckBox.class);
		brushSizeSlider = screen.findNiftyControl("BrushSizeSlider", Slider.class);
		addSourceCheckBox = screen.findNiftyControl("AddSourceCheckBox", CheckBox.class);
		editSourceCheckBox = screen.findNiftyControl("EditSourceCheckBox", CheckBox.class);
		deleteSourceButton = screen.findNiftyControl("DeleteSourceButton", Button.class);
		sourceRadiusSlider = screen.findNiftyControl("SourceRadiusSlider", Slider.class);
		sourceIntensitySlider = screen.findNiftyControl("SourceIntensitySlider", Slider.class);
		runButton = screen.findNiftyControl("RunButton", Button.class);
		stopButton = screen.findNiftyControl("StopButton", Button.class);
		resetButton = screen.findNiftyControl("ResetButton", Button.class);
		rainCheckBox = screen.findNiftyControl("RainCheckBox", CheckBox.class);
		riverCheckBox = screen.findNiftyControl("RiverCheckBox", CheckBox.class);
		deleteWaterButton = screen.findNiftyControl("DeleteWaterButton", Button.class);
		iterationsLabel = screen.findNiftyControl("IterationsLabel", Label.class);
		heightDifferenceCheckBox = screen.findNiftyControl("HeightDifferenceCheckBox", CheckBox.class);
		messageLabel = screen.findNiftyControl("MessageLabel", Label.class);
		
		upscaleDropDown.addAllItems(Arrays.asList(originalSize, originalSize*2, originalSize*4, originalSize*8));
		upscaleDropDown.selectItemByIndex(0);
		stopButton.setEnabled(false);
		resetButton.setEnabled(false);
		heightDifferenceCheckBox.setEnabled(false);
		brushSizeSlider.setValue(10);
		rainCheckBox.setChecked(true);
		riverCheckBox.setChecked(true);
		deleteWaterButton.setEnabled(false);
	}

	@Override
	public void onEndScreen() {
	}
	
	void setSolving(boolean solving) {
		upscaleDropDown.setEnabled(!solving);
		temperatureCheckBox.setEnabled(!solving);
		moistureCheckBox.setEnabled(!solving);
		brushSizeSlider.setEnabled(!solving);
		addSourceCheckBox.setEnabled(!solving);
		editSourceCheckBox.setEnabled(!solving);
		deleteSourceButton.setEnabled(!solving);
		sourceRadiusSlider.setEnabled(!solving);
		sourceIntensitySlider.setEnabled(!solving);
		runButton.setEnabled(!solving);
		stopButton.setEnabled(solving);
		resetButton.setEnabled(!solving);
		heightDifferenceCheckBox.setEnabled(!solving);
		heightDifferenceCheckBox.setChecked(false);
		deleteWaterButton.setEnabled(solving);
	}
	void setIteration(int iteration) {
		iterationsLabel.setText("Iteration: "+iteration);
	}
	
	void setMessageLabelText(String text) {
		messageLabel.setText(text);
	}
	
	boolean isRaining() {
		return rainCheckBox.isChecked();
	}
	boolean isRiverActive() {
		return riverCheckBox.isChecked();
	}
	
	@NiftyEventSubscriber(pattern = ".*Button")
	public void onButtonClick(String id, ButtonClickedEvent e) {
		System.out.println("button "+id+" clicked: "+e);
		if (runButton == e.getButton()) {
			simulation.guiRun();
		} else if (stopButton == e.getButton()) {
			simulation.guiStop();
		} else if (resetButton == e.getButton()) {
			simulation.guiReset();
		} else if (deleteWaterButton == e.getButton()) {
			simulation.guiDeleteWater();
		} else if (deleteSourceButton == e.getButton()) {
			simulation.guiDeleteRiverSource();
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*CheckBox")
	public void onCheckBoxClick(String id, CheckBoxStateChangedEvent e) {
		System.out.println("checkbox "+id+" changed: "+e);
		if (temperatureCheckBox==e.getCheckBox()) {
			if (e.isChecked()) {
				moistureCheckBox.setChecked(false);
				addSourceCheckBox.setChecked(false);
				editSourceCheckBox.setChecked(false);
				simulation.guiDisplayMode(1);
				simulation.guiRiverMode(0);
			} else if (!moistureCheckBox.isChecked() 
					&& !addSourceCheckBox.isChecked() && !editSourceCheckBox.isChecked()) {
				simulation.guiDisplayMode(0);
			}
		} else if (moistureCheckBox==e.getCheckBox()) {
			if (e.isChecked()) {
				temperatureCheckBox.setChecked(false);
				addSourceCheckBox.setChecked(false);
				editSourceCheckBox.setChecked(false);
				simulation.guiDisplayMode(2);
				simulation.guiRiverMode(0);
			} else if (!temperatureCheckBox.isChecked()
					&& !addSourceCheckBox.isChecked() && !editSourceCheckBox.isChecked()) {
				simulation.guiDisplayMode(0);
			}
		} else if (addSourceCheckBox==e.getCheckBox()) {
			if (e.isChecked()) {
				temperatureCheckBox.setChecked(false);
				moistureCheckBox.setChecked(false);
				editSourceCheckBox.setChecked(false);
				simulation.guiDisplayMode(0);
				simulation.guiRiverMode(1);
			} else if (!temperatureCheckBox.isChecked() && !moistureCheckBox.isChecked()
					&& !editSourceCheckBox.isChecked()) {
				simulation.guiRiverMode(0);
			}
		} else if (editSourceCheckBox==e.getCheckBox()) {
			if (e.isChecked()) {
				temperatureCheckBox.setChecked(false);
				moistureCheckBox.setChecked(false);
				addSourceCheckBox.setChecked(false);
				simulation.guiDisplayMode(0);
				simulation.guiRiverMode(2);
			} else if (!temperatureCheckBox.isChecked() && !moistureCheckBox.isChecked()
					&& !addSourceCheckBox.isChecked()) {
				simulation.guiRiverMode(0);
			}
		} else if (heightDifferenceCheckBox==e.getCheckBox()) {
			simulation.guiShowHeightDifference(e.isChecked());
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*Slider")
	public void onSliderChange(String id, SliderChangedEvent e) {
		System.out.println("slider "+id+" changed: "+e);
		if (brushSizeSlider==e.getSlider()) {
			simulation.guiBrushSizeChanged(e.getValue());
		}
	}
	
	@NiftyEventSubscriber(pattern = ".*DropDown")
	public void onSelectionChange(String id, DropDownSelectionChangedEvent e) {
		System.out.println("dropdown "+id+" changed: "+e.getSelection());
		if (upscaleDropDown==e.getDropDown()) {
			simulation.guiUpscaleMap(e.getSelectionItemIndex());
		}
	}
}
