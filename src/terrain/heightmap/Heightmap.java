/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package terrain.heightmap;

import com.jme3.math.FastMath;

/**
 * A class for storing heightmaps
 * @author Sebastian Weiss
 */
public class Heightmap implements Cloneable {
	private int size;
	private float data[][];

	public Heightmap(int size) {
		setSize(size);
	}
	
	public void setSize(int size) {
		this.size = size;
		this.data = new float[size][size];
	}
	
	public int getSize() {
		return size;
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
