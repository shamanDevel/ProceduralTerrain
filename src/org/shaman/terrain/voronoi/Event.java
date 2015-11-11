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
public class Event implements Comparable<Event> {
	Vector2d point;
	boolean pe;
	double y;
	Parabola arch;
	
	Event(Vector2d p, boolean pev) {
		point = p;
		pe = pev;
		y = p.y;
		arch = null;
	}

	@Override
	public int compareTo(Event o) {
		return -Double.compare(this.y, o.y);
	}
}
