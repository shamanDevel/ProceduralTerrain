/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

import com.jme3.math.FastMath;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.*;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.Arrays;
import java.util.List;
import org.shaman.terrain.TerrainHeighmapCreator;

/**
 *
 * @author Sebastian Weiss
 */
public class SketchTerrainScreenController implements ScreenController {
	private static final float SLOPE_SIZE_SCALE = 4f;
	private static final float SLOPE_ANGLE_SCALE = FastMath.RAD_TO_DEG * 1.2f;
	
	private final SketchTerrain sketchTerrain;
	
	private Nifty nifty;
	private Screen screen;
	private CheckBox addCurveCheckBox;
	private CheckBox editCurveCheckBox;
	private Button deleteCurveButton;
	private Button deleteControlPointButton;
	private CheckBox elevationConstraintCheckBox;
	private Slider plateauSizeSlider;
	private Slider slopeSizeLeftSlider;
	private Slider slopeAngleLeftSlider;
	private Slider slopeSizeRightSlider;
	private Slider slopeAngleRightSlider;
	private ListBox<String> presetListBox;
	private Button solveButton;
	private Label messageLabel;
	private String[] presetNames;
	
	private boolean curveSelected;
	private ControlPoint controlPoint;
	private boolean disableInput = false;

	public SketchTerrainScreenController(SketchTerrain sketchTerrain) {
		this.sketchTerrain = sketchTerrain;
	}	
	
	@Override
	public void bind(Nifty nifty, Screen screen) {
		this.nifty = nifty;
		this.screen = screen;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onStartScreen() {
		addCurveCheckBox = screen.findNiftyControl("AddCurveCheckBox", CheckBox.class);
		editCurveCheckBox = screen.findNiftyControl("EditCurveCheckBox", CheckBox.class);
		deleteCurveButton = screen.findNiftyControl("DeleteCurveButton", Button.class);
		deleteControlPointButton = screen.findNiftyControl("DeleteControlPointButton", Button.class);
		elevationConstraintCheckBox = screen.findNiftyControl("ElevationCheckBox", CheckBox.class);
		plateauSizeSlider = screen.findNiftyControl("PlateauSlider", Slider.class);
		slopeSizeLeftSlider = screen.findNiftyControl("SlopeSizeLeftSlider", Slider.class);
		slopeAngleLeftSlider = screen.findNiftyControl("SlopeAngleLeftSlider", Slider.class);
		slopeSizeRightSlider = screen.findNiftyControl("SlopeSizeRightSlider", Slider.class);
		slopeAngleRightSlider = screen.findNiftyControl("SlopeAngleRightSlider", Slider.class);
		presetListBox = screen.findNiftyControl("PresetListBox", ListBox.class);
		solveButton = screen.findNiftyControl("SolveButton", Button.class);
		messageLabel = screen.findNiftyControl("MessageLabel", Label.class);
		
		if (presetNames != null) {
			presetListBox.addAllItems(Arrays.asList(presetNames));
			presetListBox.selectItemByIndex(0);
		}
		
		addCurveCheckBox.setChecked(true);
		updatePhase();
	}

	@Override
	public void onEndScreen() {
		
	}
	
	//Commands from SketchTerrain
	
	public void setMessage(String message) {
		messageLabel.setText(message);
	}
	
	public void selectCurve(int curveIndex, ControlPoint point) {
		if (curveIndex==-1) {
			curveSelected = false;
			controlPoint = null;
		} else {
			controlPoint = point;
			curveSelected = true;
			if (point != null) {
				disableInput = true;
				elevationConstraintCheckBox.setChecked(point.hasElevation);
				plateauSizeSlider.setValue(point.plateau);
				slopeSizeLeftSlider.setValue(point.extend1 * SLOPE_SIZE_SCALE);
				slopeAngleLeftSlider.setValue(point.angle1 * SLOPE_ANGLE_SCALE);
				slopeSizeRightSlider.setValue(point.extend2 * SLOPE_SIZE_SCALE);
				slopeAngleRightSlider.setValue(point.angle2 * SLOPE_ANGLE_SCALE);
				disableInput = false;
			}
		}
		updatePhase();
	}
	public void setAvailablePresets(String[] presets) {
		if (presetListBox == null) {
			presetNames = presets;
		} else {
			presetListBox.addAllItems(Arrays.asList(presets));
		}
	}
	
	private void updatePhase() {
		boolean newCurve = addCurveCheckBox.isChecked();
		boolean editCurve = editCurveCheckBox.isChecked() && curveSelected;
		boolean editPoint = editCurveCheckBox.isChecked() && curveSelected && controlPoint!=null;
		deleteCurveButton.setEnabled(editCurve);
		deleteControlPointButton.setEnabled(editPoint);
		elevationConstraintCheckBox.setEnabled(editPoint);
		plateauSizeSlider.setEnabled(editPoint);
		slopeSizeLeftSlider.setEnabled(editPoint);
		slopeAngleLeftSlider.setEnabled(editPoint);
		slopeSizeRightSlider.setEnabled(editPoint);
		slopeAngleRightSlider.setEnabled(editPoint);
		presetListBox.setEnabled(newCurve);
	}
	
	//Events
	
	@NiftyEventSubscriber(pattern = ".*Button")
	public void onButtonClick(String id, ButtonClickedEvent e) {
		System.out.println("button "+id+" clicked: "+e);
		if (e.getButton()==solveButton) {
			sketchTerrain.guiSolve();
		} else if (e.getButton()==deleteCurveButton) {
			sketchTerrain.guiDeleteCurve();
		} else if (e.getButton()==deleteControlPointButton) {
			sketchTerrain.guiDeleteControlPoint();
		}
	}
	@NiftyEventSubscriber(pattern = ".*CheckBox")
	public void onCheckBoxClick(String id, CheckBoxStateChangedEvent e) {
		System.out.println("checkbox "+id+" changed: "+e);
		if (e.getCheckBox()==elevationConstraintCheckBox && controlPoint!=null && !disableInput) {
			controlPoint.hasElevation = e.isChecked();
			sketchTerrain.guiControlPointChanged();
		} else if (e.getCheckBox()==addCurveCheckBox && e.isChecked()) {
			editCurveCheckBox.setChecked(false);
			curveSelected = false;
			controlPoint = null;
			updatePhase();
			sketchTerrain.guiAddCurves();
		} else if (e.getCheckBox()==editCurveCheckBox && e.isChecked()) {
			addCurveCheckBox.setChecked(false);
			updatePhase();
			sketchTerrain.guiEditCurves();
		}
	}
	@NiftyEventSubscriber(pattern = ".*Slider")
	public void onSliderChange(String id, SliderChangedEvent e) {
		System.out.println("slider "+id+" changed: "+e);
		if (controlPoint==null || disableInput) {
			return;
		}
		if (e.getSlider()==plateauSizeSlider) {
			controlPoint.plateau = e.getValue();
		} else if (e.getSlider()==slopeSizeLeftSlider) {
			controlPoint.extend1 = e.getValue() / SLOPE_SIZE_SCALE;
		} else if (e.getSlider()==slopeAngleLeftSlider) {
			controlPoint.angle1 = e.getValue() / SLOPE_ANGLE_SCALE;
		} else if (e.getSlider()==slopeSizeRightSlider) {
			controlPoint.extend2 = e.getValue() / SLOPE_SIZE_SCALE;
		} else if (e.getSlider()==slopeAngleRightSlider) {
			controlPoint.angle2 = e.getValue() / SLOPE_ANGLE_SCALE;
		}
		sketchTerrain.guiControlPointChanged();
	}
	@NiftyEventSubscriber(pattern = ".*ListBox")
	public void onSelectionChange(String id, ListBoxSelectionChangedEvent e) {
		System.out.println("list box "+id+" selection changed: "+e);
		if (e.getListBox()==presetListBox) {
			List<Integer> indices = e.getSelectionIndices();
			if (!indices.isEmpty()) {
				sketchTerrain.guiPresetChanged(indices.get(0));
			}
		}
	}
	
	
}
