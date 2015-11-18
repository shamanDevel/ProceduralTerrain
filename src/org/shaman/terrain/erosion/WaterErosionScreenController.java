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
		deleteSourceButton = screen.findNiftyControl("DeleteSourceCheckBox", Button.class);
		sourceRadiusSlider = screen.findNiftyControl("SourceRadiusSlider", Slider.class);
		sourceIntensitySlider = screen.findNiftyControl("SourceIntensitySlider", Slider.class);
		runButton = screen.findNiftyControl("RunButton", Button.class);
		stopButton = screen.findNiftyControl("StopButton", Button.class);
		resetButton = screen.findNiftyControl("ResetButton", Button.class);
		iterationsLabel = screen.findNiftyControl("IterationsLabel", Label.class);
		
		upscaleDropDown.addAllItems(Arrays.asList(originalSize, originalSize*2, originalSize*4, originalSize*8));
		upscaleDropDown.selectItemByIndex(0);
	}

	@Override
	public void onEndScreen() {
	}
	
	@NiftyEventSubscriber(pattern = ".*Button")
	public void onButtonClick(String id, ButtonClickedEvent e) {
		System.out.println("button "+id+" clicked: "+e);
	}
	
	@NiftyEventSubscriber(pattern = ".*CheckBox")
	public void onCheckBoxClick(String id, CheckBoxStateChangedEvent e) {
		System.out.println("checkbox "+id+" changed: "+e);
	}
	
	@NiftyEventSubscriber(pattern = ".*Slider")
	public void onSliderChange(String id, SliderChangedEvent e) {
		System.out.println("slider "+id+" changed: "+e);
	}
	
	@NiftyEventSubscriber(pattern = ".*DropDown")
	public void onSelectionChange(String id, DropDownSelectionChangedEvent e) {
		System.out.println("dropdown "+id+" changed: "+e.getSelection());
		if (upscaleDropDown==e.getDropDown()) {
			simulation.guiUpscaleMap(e.getSelectionItemIndex());
		}
	}
}
