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
public class Event implements Comparable<Event> {
	Vector2f point;
	boolean pe;
	float y;
	Parabola arch;
	
	Event(Vector2f p, boolean pev) {
		point = p;
		pe = pev;
		y = p.y;
		arch = null;
	}

	@Override
	public int compareTo(Event o) {
		return -Float.compare(this.y, o.y);
	}
}
