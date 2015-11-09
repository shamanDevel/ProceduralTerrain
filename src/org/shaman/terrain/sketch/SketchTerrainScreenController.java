/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 *
 * @author Sebastian Weiss
 */
public class SketchTerrainScreenController implements ScreenController {
	private final SketchTerrain sketchTerrain;

	public SketchTerrainScreenController(SketchTerrain sketchTerrain) {
		this.sketchTerrain = sketchTerrain;
	}	
	
	@Override
	public void bind(Nifty nifty, Screen screen) {
		
	}

	@Override
	public void onStartScreen() {
		
	}

	@Override
	public void onEndScreen() {
		
	}
	
}
