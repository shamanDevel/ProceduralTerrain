/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import org.shaman.terrain.Heightmap;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.text.DecimalFormat;
import java.util.*;

/**
 *
 * @author Sebastian Weiss
 */
public class VoronoiProcessor implements HeightmapProcessor {
	private final Random rand = new Random();
	private long seed = 0;
	private int cellCount;
	private int pointsPerCell;
	private float minHeight;
	private float maxHeight;
	private float d1, d2, d3;

	public VoronoiProcessor(int cellCount, int pointsPerCell, float minHeight, float maxHeight, float d1, float d2, float d3) {
		this.cellCount = cellCount;
		this.pointsPerCell = pointsPerCell;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.d1 = d1;
		this.d2 = d2;
		this.d3 = d3;
	}

	public VoronoiProcessor() {
		this(4, 2, 1, 3, -1, 1, 0);
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

	public float getMinHeight() {
		return minHeight;
	}

	public void setMinHeight(float minHeight) {
		this.minHeight = minHeight;
	}

	public float getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(float maxHeight) {
		this.maxHeight = maxHeight;
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
		rand.setSeed(seed);
		float cellSize = (float) s / (float) cellCount;
		List<Vector3f> points = new ArrayList<>();
		for (int x=0; x<cellCount; ++x) {
			for (int y=0; y<cellCount; ++y) {
				float nx = x*cellSize;
				float ny = y*cellSize;
				for (int i=0; i<pointsPerCell; ++i) {
					points.add(new Vector3f(
							nx + rand.nextFloat()*cellSize,
							ny + rand.nextFloat()*cellSize,
							rand.nextFloat()*(maxHeight-minHeight) + minHeight));
				}
			}
		}
		//now cycle through cells and find influencing hills
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
				float v = d1*dist(points.get(0), px, py) 
						+ d2*dist(points.get(1), px, py)
						+ d3*dist(points.get(2), px, py);
				v += map.getHeightAt(x, y);
				map.setHeightAt(x, y, v);
			}
		}
		return map;
	}
	private static float dist(Vector3f hillCenter, float px, float py) {
		return FastMath.sqrt((hillCenter.x-px)*(hillCenter.x-px) + (hillCenter.y-py)*(hillCenter.y-py))*hillCenter.z;
	}
	private static float smooth(float v) {
		float v3 = v*v*v;
		float v4 = v3*v;
		float v5 = v4*v;
		return 6*v5 - 15*v4 + 10*v3;
	}

	@Override
	public void reseed() {
		seed = new Random().nextLong();
	}

	@Override
	public List<? extends PropItem> getProperties() {
		final DecimalFormat format = new DecimalFormat("0.000");
		return Arrays.asList(
		new PropItem() {

			@Override
			public String getText() {
				return "Voronoi: cell count="+cellCount;
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					cellCount++;
					return true;
				} else {
					if (cellCount==1) return false;
					cellCount--;
					return true;
				}
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Voronoi: point per cell="+pointsPerCell;
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					pointsPerCell++;
					return true;
				} else {
					if (pointsPerCell==1) return false;
					pointsPerCell--;
					return true;
				}
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Voronoi: min hill height="+format.format(minHeight);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					if (minHeight >= maxHeight) return false;
					minHeight = Math.min(maxHeight, minHeight + 0.1f);
					return true;
				} else {
					if (minHeight <= 0.1f) return false;
					minHeight = Math.max(0.1f, minHeight - 0.1f);
					return true;
				}
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Voronoi: max hill height="+format.format(maxHeight);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					maxHeight += 0.1;
					return true;
				} else {
					if (maxHeight <= minHeight) return false;
					maxHeight = Math.max(minHeight, maxHeight - 0.1f);
					return true;
				}
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Voronoi: d1="+format.format(d1);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					d1 += 0.1f;
				} else {
					d1 -= 0.1f;
				}
				return true;
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Voronoi: d1="+format.format(d2);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					d2 += 0.1f;
				} else {
					d2 -= 0.1f;
				}
				return true;
			}
		},
		new PropItem() {

			@Override
			public String getText() {
				return "Voronoi: d3="+format.format(d3);
			}

			@Override
			public boolean change(boolean up) {
				if (up) {
					d3 += 0.1f;
				} else {
					d3 -= 0.1f;
				}
				return true;
			}
		}
		);
	}
	
}
