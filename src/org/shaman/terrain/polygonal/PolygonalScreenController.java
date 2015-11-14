/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 *
 * @author Sebastian Weiss
 */
public class PolygonalScreenController implements ScreenController {
	private final PolygonalMapGenerator generator;
	
	private Screen screen;
	private Button nextStepButton;

	public PolygonalScreenController(PolygonalMapGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void bind(Nifty nifty, Screen screen) {
		this.screen = screen;
	}

	@Override
	public void onStartScreen() {
		nextStepButton = screen.findNiftyControl("NextStepButton", Button.class);
	}

	@Override
	public void onEndScreen() {
		
	}
	
	@NiftyEventSubscriber(pattern = ".*Button")
	public void onButtonClick(String id, ButtonClickedEvent e) {
		System.out.println("button "+id+" clicked: "+e);
		if (nextStepButton == e.getButton()) {
			generator.guiNextStep();
		}
	}
}
