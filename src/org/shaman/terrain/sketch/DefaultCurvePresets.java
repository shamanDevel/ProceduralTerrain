/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

import com.jme3.math.FastMath;
import org.shaman.terrain.TerrainHeighmapCreator;
import org.shaman.terrain.heightmap.Heightmap;

/**
 *
 * @author Sebastian Weiss
 */
public class DefaultCurvePresets {
	
	public static CurvePreset[] DEFAULT_PRESETS = new CurvePreset[] {
		new Hill(),
		new Ridge(),
		new RidgePlateau(),
		new CliffLeft(),
		new CliffRight(),
		new Riverbed(),
		new Crack()
	};
	
	public static class Hill implements CurvePreset {

		@Override
		public String getName() {
			return "Hill";
		}

		@Override
		public ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap) {
			return new ControlPoint(x, y, height, 5, 0, 0, 0, 0, 0, 0);
		}
		
	}
	
	public static class Ridge implements CurvePreset {

		@Override
		public String getName() {
			return "Ridge";
		}

		@Override
		public ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap) {
			float angle = 30 * FastMath.DEG_TO_RAD;
			float plateau = 1;
			float extend = (float) ((height - heightmap.getHeightInterpolating(x, y)) / Math.cos(angle) * 0.7);
			extend *= TerrainHeighmapCreator.HEIGHMAP_HEIGHT_SCALE;
			return new ControlPoint(x, y, height, plateau, angle, extend, angle, extend, 0, 0);
		}
		
	}
	
	public static class RidgePlateau implements CurvePreset {

		@Override
		public String getName() {
			return "Ridge + Plateau";
		}

		@Override
		public ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap) {
			float angle = 30 * FastMath.DEG_TO_RAD;
			float plateau = 10;
			float extend = (float) ((height - heightmap.getHeightInterpolating(x, y)) / Math.cos(angle) * 0.7);
			extend *= TerrainHeighmapCreator.HEIGHMAP_HEIGHT_SCALE;
			return new ControlPoint(x, y, height, plateau, angle, extend, angle, extend, 0, 0);
		}
		
	}
	
	public static class Riverbed implements CurvePreset {

		@Override
		public String getName() {
			return "Riverbed";
		}

		@Override
		public ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap) {
			float angle = -30 * FastMath.DEG_TO_RAD;
			float plateau = 10;
			float extend = (float) ((height - heightmap.getHeightInterpolating(x, y)) / Math.cos(angle) * 0.7);
			extend *= TerrainHeighmapCreator.HEIGHMAP_HEIGHT_SCALE;
			extend = Math.abs(extend);
			return new ControlPoint(x, y, height, plateau, angle, extend, angle, extend, 0, 0);
		}
		
	}
	
	public static class CliffLeft implements CurvePreset {

		@Override
		public String getName() {
			return "Cliff Left";
		}

		@Override
		public ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap) {
			float angle = 80 * FastMath.DEG_TO_RAD;
			float extend = (float) ((height - heightmap.getHeightInterpolating(x, y)) * 2.0);
			extend *= TerrainHeighmapCreator.HEIGHMAP_HEIGHT_SCALE;
			return new ControlPoint(x, y, height, 1, angle, extend, 0, 30, 0, 0);
		}
		
	}
	
	public static class CliffRight implements CurvePreset {

		@Override
		public String getName() {
			return "Cliff Right";
		}

		@Override
		public ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap) {
			float angle = 80 * FastMath.DEG_TO_RAD;
			float extend = (float) ((height - heightmap.getHeightInterpolating(x, y)) * 2.0);
			extend *= TerrainHeighmapCreator.HEIGHMAP_HEIGHT_SCALE;
			return new ControlPoint(x, y, height, 1, 0, 30, angle, extend, 0, 0);
		}
		
	}
	
	public static class Crack implements CurvePreset {

		@Override
		public String getName() {
			return "Crack";
		}

		@Override
		public ControlPoint createControlPoint(float x, float y, float height, ControlPoint[] oldPoints, Heightmap heightmap) {
			float angle = -80 * FastMath.DEG_TO_RAD;
			float extend = 20;
			ControlPoint p = new ControlPoint(x, y, height, 0, angle, extend, angle, extend, 0, 0);
			p.hasElevation = false;
			return p;
		}
		
	}
}
