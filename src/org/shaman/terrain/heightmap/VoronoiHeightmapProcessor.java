/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.*;

/**
 *
 * @author Sebastian Weiss
 */
public class VoronoiHeightmapProcessor implements HeightmapProcessor {
	private final Random rand = new Random();
	private int cellCount;
	private int pointsPerCell;
	private float hillHeight;
	private float d1, d2, d3;
	private float exponent;

	public VoronoiHeightmapProcessor(int cellCount, int pointsPerCell, float hillHeight, float d1, float d2, float d3, float exponent) {
		this.cellCount = cellCount;
		this.pointsPerCell = pointsPerCell;
		this.hillHeight = hillHeight;
		this.d1 = d1;
		this.d2 = d2;
		this.d3 = d3;
		this.exponent = exponent;
	}

	public VoronoiHeightmapProcessor() {
		this(4, 2, 0.5f, -1, 1, 0, 3);
	}

	public int getCellCount() {
		return cellCount;
	}

	public void setCellCount(int cellCount) {
		this.cellCount = cellCount;
	}

	public int getPointsPerCell() {
		return pointsPerCell;
	}

	public void setPointsPerCell(int pointsPerCell) {
		this.pointsPerCell = pointsPerCell;
	}

	public float getHillHeight() {
		return hillHeight;
	}

	public void setHillHeight(float hillHeight) {
		this.hillHeight = hillHeight;
	}

	public float getD1() {
		return d1;
	}

	public void setD1(float d1) {
		this.d1 = d1;
	}

	public float getD2() {
		return d2;
	}

	public void setD2(float d2) {
		this.d2 = d2;
	}

	public float getD3() {
		return d3;
	}

	public void setD3(float d3) {
		this.d3 = d3;
	}
	
	@Override
	public Heightmap apply(Heightmap map) {
		int s = map.getSize();
		//generate hill centers: x,y,height
		float cellSize = (float) s / (float) cellCount;
		List<Vector3f> points = new ArrayList<>();
		for (int x=0; x<cellCount; ++x) {
			for (int y=0; y<cellCount; ++y) {
				float nx = x*cellSize;
				float ny = y*cellSize;
				for (int i=0; i<pointsPerCell; ++i) {
					points.add(new Vector3f(nx + rand.nextFloat()*cellSize, ny + rand.nextFloat()*cellSize, 1));
				}
			}
		}
		//now cycle through cells and find influencing hills
		float minH = Float.MAX_VALUE;
		float maxH = -Float.MAX_VALUE;
		for (int x=0; x<s; ++x) {
			for (int y=0; y<s; ++y) {
				//sort points by distance to (x,y)
				final float px = x;
				final float py = y;
				Collections.sort(points, new Comparator<Vector3f>() {

					@Override
					public int compare(Vector3f o1, Vector3f o2) {
						float dist1 = dist(o1, px, py);
						float dist2 = dist(o2, px, py);
						return Float.compare(dist1, dist2);
					}
				});
				//calc height
				float v = d1*dist(points.get(0), px, py)*points.get(0).z 
						+ d2*dist(points.get(1), px, py)*points.get(1).z
						+ d3*dist(points.get(2), px, py)*points.get(2).z;
				v += map.getHeightAt(x, y);
				map.setHeightAt(x, y, v);
				//update min,max for normalization
				minH = Math.min(minH, v);
				maxH = Math.max(maxH, v);
			}
		}
		//normalize
		float factor = hillHeight / (maxH - minH);
		for (int x=0; x<s; ++x) {
			for (int y=0; y<s; ++y) {
				float v = map.getHeightAt(x, y);
				v -= minH;
				v *= factor;
				v = 1-v;
//				v = (float) Math.pow(v, exponent);
				v = smooth(v);
				map.setHeightAt(x, y, 1-v);
			}
		}
		return map;
	}
	private static float dist(Vector3f hillCenter, float px, float py) {
		return FastMath.sqrt((hillCenter.x-px)*(hillCenter.x-px) + (hillCenter.y-py)*(hillCenter.y-py));
	}
	private static float smooth(float v) {
		float v3 = v*v*v;
		float v4 = v3*v;
		float v5 = v4*v;
		return 6*v5 - 15*v4 + 10*v3;
	}

	@Override
	public void setSeed(long seed) {
		rand.setSeed(seed);
	}

	@Override
	public List<? extends PropItem> getProperties() {
		return Collections.EMPTY_LIST;
	}
	
}
