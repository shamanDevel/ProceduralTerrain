/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.sketch;

/**
 *
 * @author Sebastian Weiss
 */
public class ControlPoint {
	public float x;
	public float y;
	/**
	 * the target height at that control point.
	 * h_i in the paper.
	 */
	public float height;
	/**
	 * True if the control point has an elevation constraint.
	 * This enables the height- and plateau-property.
	 */
	public boolean hasElevation = true;
	/**
	 * The radius of the plateau on both sides of the feature curve at this point.
	 * r_i in the paper.
	 */
	public float plateau;
	public float angle1;
	public float extend1;
	public float angle2;
	public float extend2;
	public float noiseAmplitude;
	public float noiseRoughness;

	public ControlPoint() {
	}

	public ControlPoint(float x, float y, float height, float plateau, float angle1, float extend1, float angle2, float extend2, float noiseAmplitude, float noiseRoughness) {
		this.x = x;
		this.y = y;
		this.height = height;
		this.plateau = plateau;
		this.angle1 = angle1;
		this.extend1 = extend1;
		this.angle2 = angle2;
		this.extend2 = extend2;
		this.noiseAmplitude = noiseAmplitude;
		this.noiseRoughness = noiseRoughness;
	}

	@Override
	public String toString() {
		return "ControlPoint{" + "x=" + x + ", y=" + y + ", height=" + height + ", plateau=" + plateau + ", angle1=" + angle1 + ", extend1=" + extend1 + ", angle2=" + angle2 + ", extend2=" + extend2 + ", noiseAmplitude=" + noiseAmplitude + ", noiseRoughness=" + noiseRoughness + '}';
	}
	
}
