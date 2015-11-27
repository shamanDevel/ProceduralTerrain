/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.math.FastMath;
import java.io.Serializable;
import java.util.Arrays;

/**
 * A class for storing heightmaps
 * @author Sebastian Weiss
 */
public class Heightmap implements Cloneable, Serializable {
	private static final long serialVersionUID = 4683025587440740836L;
	private int size;
	private float data[][];

	public Heightmap(int size) {
		setSize(size);
	}
	
	public float[][] getRawData() {
		return data;
	}
	
	public void setSize(int size) {
		this.size = size;
		this.data = new float[size][size];
	}
	
	public int getSize() {
		return size;
	}
	
	public void fillHeight(float h) {
		for (int x=0; x<size; ++x) {
			Arrays.fill(data[x], h);
		}
	}
	
	/**
	 * Returns the height at the specific coordinates without checking for
	 * array boundaries.
	 * @param x
	 * @param y
	 * @return 
	 */
	public float getHeightAt(int x, int y) {
		return data[x][y];
	}
	
	/**
	 * Returns the height at the specific coordinates.
	 * If the coordinates are outside of the boundary, they are wrapped around.
	 * @param x
	 * @param y
	 * @return 
	 */
	public float getHeightAtWrapping(int x, int y) {
		x = (x+size) % size;
		y = (y+size) % size;
		return data[x][y];
	}
	
	/**
	 * Returns the height at the specific coordinates.
	 * If the coordinates are outside of the boundary, they are clamped
	 * @param x
	 * @param y
	 * @return 
	 */
	public float getHeightAtClamping(int x, int y) {
		return data[Math.min(size-1, Math.max(0, x))][Math.min(size-1, Math.max(0, y))];
	}
	
	/**
	 * Performs a biliniar interpolation to get the height between grid positions.
	 * @param x
	 * @param y
	 * @return 
	 */
	public float getHeightInterpolating(float x, float y) {
		x = Math.max(0, Math.min(size-1, x));
		y = Math.max(0, Math.min(size-1, y));
		int ax = (int) Math.floor(x);
		int bx = ax+1;//(int) Math.ceil(x);
		int ay = (int) Math.floor(y);
		int by = ay+1;//(int) Math.ceil(y);
		float fx = x%1;
		float fy = y%1;
		float q11 = getHeightAtClamping(ax, ay);
		float q12 = getHeightAtClamping(ax, by);
		float q21 = getHeightAtClamping(bx, ay);
		float q22 = getHeightAtClamping(bx, by);
		float v1 = (1-fx)*q11 + fx*q21;
		float v2 = (1-fx)*q12 + fx*q22;
		return (1-fy)*v1 + fy*v2;
	}
	
	/**
	 * Performs a bicubic interpolation to get the height between grid positions.
	 * See http://www.paulinternet.nl/?page=bicubic
	 * @param tx
	 * @param ty
	 * @return 
	 */
	public float getHeightInterpolatingBicubic(float tx, float ty) {
		tx = Math.max(0, Math.min(size-1, tx));
		ty = Math.max(0, Math.min(size-1, ty));
		int x = (int) Math.floor(tx);
		int y = (int) Math.floor(ty);
		float fx = tx%1;
		float fy = ty%1;
		
		float q00 = getHeightAtClamping(x-1, y-1);
		float q01 = getHeightAtClamping(x-1, y);
		float q02 = getHeightAtClamping(x-1, y+1);
		float q03 = getHeightAtClamping(x-1, y+2);
		float q10 = getHeightAtClamping(x, y-1);
		float q11 = getHeightAtClamping(x, y);
		float q12 = getHeightAtClamping(x, y+1);
		float q13 = getHeightAtClamping(x, y+2);
		float q20 = getHeightAtClamping(x+1, y-1);
		float q21 = getHeightAtClamping(x+1, y);
		float q22 = getHeightAtClamping(x+1, y+1);
		float q23 = getHeightAtClamping(x+1, y+2);
		float q30 = getHeightAtClamping(x+2, y-1);
		float q31 = getHeightAtClamping(x+2, y);
		float q32 = getHeightAtClamping(x+2, y+1);
		float q33 = getHeightAtClamping(x+2, y+2);
		
		float q0 = q01 + 0.5f * fy*(q02 - q00 + fy*(2*q00 - 5*q01 + 4*q02 - q03 + fy*(3*(q01 - q02) + q03 - q00)));
		float q1 = q11 + 0.5f * fy*(q12 - q10 + fy*(2*q10 - 5*q11 + 4*q12 - q13 + fy*(3*(q11 - q12) + q13 - q10)));
		float q2 = q21 + 0.5f * fy*(q22 - q20 + fy*(2*q20 - 5*q21 + 4*q22 - q23 + fy*(3*(q21 - q22) + q23 - q20)));
		float q3 = q31 + 0.5f * fy*(q32 - q30 + fy*(2*q30 - 5*q31 + 4*q32 - q33 + fy*(3*(q31 - q32) + q33 - q30)));
		
		return q1 + 0.5f * fx*(q2 - q0 + fx*(2*q0 - 5*q1 + 4*q2 - q3 + fx*(3*(q1 - q2) + q3 - q0)));
	}
	
	/**
	 * Sets the height at the specific coordinate.
	 * If the coordinates are outside of the boundary, nothing changes
	 * @param x
	 * @param y
	 * @param height 
	 */
	public void setHeightAt(int x, int y, float height) {
		if (x>=0 && x<size && y>=0 && y<size) {
			data[x][y] = height;
		}
	}
	
	/**
	 * Sets a specific value to the height at the specific coordinate.
	 * If the coordinates are outside of the boundary, nothing changes
	 * @param x
	 * @param y
	 * @param toAdd the value to add
	 */
	public void adjustHeightAt(int x, int y, float toAdd) {
		if (x>=0 && x<size && y>=0 && y<size) {
			data[x][y] += toAdd;
		}
	}
	
	/**
	 * Returns the slope at the specified coordinates.
	 * @param x
	 * @param y
	 * @return 
	 */
	public float getSlopeAt(int x, int y) {
		if (x<0 || x>=size || y<0 || y>=size) {
			return 0;
		}
		float h = getHeightAtClamping(x, y);
		return Math.max(
				Math.max(Math.abs(h-getHeightAtClamping(x+1, y)),
						 Math.abs(h-getHeightAtClamping(x-1, y))),
				Math.max(Math.abs(h-getHeightAtClamping(x, y+1)),
						 Math.abs(h-getHeightAtClamping(x, y-1)))
		);
	}

	@Override
	@SuppressWarnings("CloneDeclaresCloneNotSupported")
	public Heightmap clone() {
		try {
			Heightmap map = (Heightmap) super.clone();
			map.data = new float[map.size][map.size];
			for (int x=0; x<map.size; ++x) {
				for (int y=0; y<map.size; ++y) {
					map.data[x][y] = this.data[x][y];
				}
			}
			return map;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException("unable to clone heightmap", ex);
		}
	}
	
	public float[] getJMEHeightmap(float scale) {
		int s = (FastMath.isPowerOfTwo(size) ? size : FastMath.nearestPowerOfTwo(size));
		s += 1;
		float[] a = new float[s*s];
		for (int x=0; x<s; ++x) {
			for (int y=0; y<s; ++y) {
				a[x + s*y] = getHeightAtClamping(x, y) * scale;
			}
		}
		return a;
	}
}
