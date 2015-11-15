/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.voronoi;

import javax.vecmath.Vector2d;


/**
 *
 * @author Sebastian Weiss
 */
public class Edge {
	Vector2d start;
	Vector2d end;
	Vector2d direction;
	Vector2d left;
	Vector2d right;
	double f;
	double g;
	Edge neighbour;

	public Edge() {
	}
	
	Edge(Vector2d s, Vector2d a, Vector2d b) {
		start = s;
		left = a;
		right = b;
		neighbour = null;
		end = null;
		f = (b.x - a.x) / (a.y - b.y);
		g = s.y - f*s.x;
		direction = new Vector2d(b.y - a.y, -(b.x-a.x));
	}

	public Vector2d getStart() {
		return start;
	}

	public Vector2d getEnd() {
		return end;
	}

	public Vector2d getLeft() {
		return left;
	}

	public Vector2d getRight() {
		return right;
	}

	@Override
	public String toString() {
		return "Edge{" + "start=" + start + ", end=" + end + ", direction=" + direction + ", left=" + left + ", right=" + right + ", neighbour=" + neighbour + '}';
	}
	
}
