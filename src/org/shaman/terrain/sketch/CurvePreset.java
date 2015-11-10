/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

import org.shaman.terrain.heightmap.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public interface CurvePreset {
	String getName();
	
	ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap);
}
