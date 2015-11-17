/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import java.io.Serializable;

/**
 * Represents an n-dimensional vector field
 * @author Sebastian Weiss
 */
public class Vectorfield implements Cloneable, Serializable {
	private static final long serialVersionUID = 8072220924471909437L;
	private int size;
	private int dimensions;
	private float data[][][];

	public Vectorfield(int size, int dimensions) {
		setSize(size, dimensions);
	}
	
	public float[][][] getRawData() {
		return data;
	}
	
	public void setSize(int size, int dimensions) {
		this.size = size;
		this.dimensions = dimensions;
		this.data = new float[size][size][dimensions];
	}
	
	public int getSize() {
		return size;
	}
	
	public int getDimensions() {
		return dimensions;
	}
	
	/**
	 * Returns the height at the specific coordinates without checking for
	 * array boundaries.
	 * @param x
	 * @param y
	 * @param i
	 * @return 
	 */
	public float getScalarAt(int x, int y, int i) {
		return data[x][y][i];
	}
	
	public float[] getVectorAt(int x, int y) {
		return data[x][y];
	}
	
	/**
	 * Returns the height at the specific coordinates.
	 * If the coordinates are outside of the boundary, they are wrapped around.
	 * @param x
	 * @param y
	 * @return 
	 */
	public float[] getVectorAtWrapping(int x, int y) {
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
	public float[] getVectorAtClamping(int x, int y) {
		return data[Math.min(size-1, Math.max(0, x))][Math.min(size-1, Math.max(0, y))];
	}
	
	/**
	 * Performs a biliniar interpolation to get the height between grid positions.
	 * @param x
	 * @param y
	 * @return 
	 */
	public float[] getVectorInterpolating(float x, float y, float[] store) {
		if (store==null || store.length!=dimensions) {
			store = new float[dimensions];
		}
		x = Math.max(0, Math.min(size-1, x));
		y = Math.max(0, Math.min(size-1, y));
		int ax = (int) Math.floor(x);
		int bx = ax+1;//(int) Math.ceil(x);
		int ay = (int) Math.floor(y);
		int by = ay+1;//(int) Math.ceil(y);
		float fx = x%1;
		float fy = y%1;
		float[] q11 = getVectorAtClamping(ax, ay);
		float[] q12 = getVectorAtClamping(ax, by);
		float[] q21 = getVectorAtClamping(bx, ay);
		float[] q22 = getVectorAtClamping(bx, by);
		for (int i=0; i<dimensions; ++i) {
			float v1 = (1-fx)*q11[i] + fx*q12[i];
			float v2 = (1-fx)*q21[i] + fx*q22[i];
			store[i] = (1-fy)*v1 + fy*v2;
		}
		return store;
	}
	
	/**
	 * Sets the height at the specific coordinate.
	 * If the coordinates are outside of the boundary, nothing changes
	 * @param x
	 * @param y
	 * @param vector 
	 */
	public void setVectorAt(int x, int y, float[] vector) {
		if (x>=0 && x<size && y>=0 && y<size) {
			for (int i=0; i<dimensions; ++i) {
				data[x][y][i] = vector[i];
			}
		}
	}
	
	public void setScalarAt(int x, int y, int i, float scalar) {
		if (x>=0 && x<size && y>=0 && y<size && i>=0 && i<dimensions) {
			data[x][y][i] = scalar;
		}
	}
	
	public void setLayer(int i, float[][] layer) {
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				data[x][y][i] = layer[x][y];
			}
		}
	}
	public float[][] getLayer(int i, float[][] store) {
		if (store==null || store.length!=size || store[0].length!=size) {
			store = new float[size][size];
		}
		for (int x=0; x<size; ++x) {
			for (int y=0; y<size; ++y) {
				store[x][y] = data[x][y][i];
			}
		}
		return store;
	}

	@Override
	@SuppressWarnings("CloneDeclaresCloneNotSupported")
	public Vectorfield clone() {
		try {
			Vectorfield map = (Vectorfield) super.clone();
			map.data = new float[map.size][map.size][dimensions];
			for (int x=0; x<map.size; ++x) {
				for (int y=0; y<map.size; ++y) {
					for (int i=0; i<map.dimensions; ++i) {
						map.data[x][y][i] = this.data[x][y][i];
					}
				}
			}
			return map;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException("unable to clone heightmap", ex);
		}
	}

}
