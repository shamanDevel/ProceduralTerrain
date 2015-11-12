/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 *
 * @author Sebastian Weiss
 */
public class PolygonalScreenController implements ScreenController {
	private final PolygonalMapGenerator generator;

	public PolygonalScreenController(PolygonalMapGenerator generator) {
		this.generator = generator;
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
