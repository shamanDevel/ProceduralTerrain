/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.voronoi;

import com.jme3.math.Vector2f;

/**
 *
 * @author Sebastian Weiss
 */
public class Edge {
	Vector2f start;
	Vector2f end;
	Vector2f direction;
	Vector2f left;
	Vector2f right;
	float f;
	float g;
	Edge neighbour;
	
	Edge(Vector2f s, Vector2f a, Vector2f b) {
		start = s;
		left = a;
		right = b;
		neighbour = null;
		end = null;
		f = (b.x - a.x) / (a.y - b.y);
		g = s.y - f*s.x;
		direction = new Vector2f(b.y - a.y, -(b.x-a.x));
	}

	public Vector2f getStart() {
		return start;
	}

	public Vector2f getEnd() {
		return end;
	}
}
