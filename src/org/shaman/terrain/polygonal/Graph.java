/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import com.jme3.math.Vector2f;
import java.util.ArrayList;

/**
 *
 * @author Sebastian Weiss
 */
public class Graph {
	public ArrayList<Center> centers = new ArrayList<>();
	public ArrayList<Corner> corners = new ArrayList<>();
	public ArrayList<Edge> edges = new ArrayList<>();
	
	public static class Center {
		public int index;
		public Vector2f location;
		public boolean water;
		public boolean ocean;
		public boolean border;
		public Biome biome;
		public float elevation;
		public float moisture;
		public float temperature;
		
		public ArrayList<Center> neighbors = new ArrayList<>();
		public ArrayList<Edge> borders = new ArrayList<>();
		public ArrayList<Corner> corners = new ArrayList<>();

		@Override
		public String toString() {
			return "Center{" + "index=" + index + ", location=" + location + ", water=" + water + ", ocean=" + ocean + ", border=" + border + ", biome=" + biome + ", elevation=" + elevation + ", moisture=" + moisture + ", temperature=" + temperature + '}';
		}
		
	}
	
	public static class Edge {
		public int index;
		/**
		 * Delaunay edge.
		 */
		public Center d0, d1;
		/**
		 * Voronoi edge.
		 */
		public Corner v0, v1;
		public Vector2f midpoint;
		public int riverVolume;

		@Override
		public String toString() {
			return "Edge{" + "index=" + index + ", d0=" + d0 + ", d1=" + d1 + ", v0=" + v0 + ", v1=" + v1 + ", midpoint=" + midpoint + ", riverVolume=" + riverVolume + '}';
		}
		
	}
	
	public static class Corner {
		public int index;
		public Vector2f point;
		public boolean ocean;
		public boolean water;
		public boolean coast;
		public boolean border;
		public float elevation;
		public float moisture;
		public float temperature;
		public int river;
		
		public ArrayList<Center> touches = new ArrayList<>();
		public ArrayList<Edge> protrudes = new ArrayList<>();
		public ArrayList<Corner> adjacent = new ArrayList<>();

		@Override
		public String toString() {
			return "Corner{" + "index=" + index + ", point=" + point + ", ocean=" + ocean + ", water=" + water + ", coast=" + coast + ", border=" + border + ", elevation=" + elevation + ", moisture=" + moisture + ", temperature=" + temperature + ", river=" + river + '}';
		}
	}
}
