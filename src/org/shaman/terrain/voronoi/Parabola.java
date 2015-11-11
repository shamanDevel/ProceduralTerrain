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
class Parabola {
	boolean isLeaf;
	Vector2f site;
	Edge edge;
	Event cEvent;
	Parabola parent;

	private Parabola left, right;
	
	Parabola() {
		site = null;
		isLeaf = false;
		cEvent = null;
		edge = null;
		parent = null;
	}
	
	Parabola(Vector2f s) {
		site = s;
		isLeaf = true;
		cEvent = null;
		edge = null;
		parent = null;
	}
	
	void setLeft(Parabola p) {
		left = p;
		p.parent = this;
	}
	void setRight(Parabola p) {
		right = p;
		p.parent = this;
	}
	Parabola left() {
		return left;
	}
	Parabola right() {
		return right;
	}
	static Parabola getLeft(Parabola p) {
		return getLeftChild(getLeftParent(p));
	}
	static Parabola getRight(Parabola p) {
		return getRightChild(getRightParent(p));
	}
	static Parabola getLeftParent(Parabola p) {
		Parabola par = p.parent;
		Parabola pLast = p;
		while (par.left() == pLast) {
			if (par.parent==null) {
				return null;
			}
			pLast = par;
			par = par.parent;
		}
		return par;
	}
	static Parabola getRightParent(Parabola p) {
		Parabola par = p.parent;
		Parabola pLast = p;
		while (par.right() == pLast) {
			if (par.parent==null) {
				return null;
			}
			pLast = par;
			par = par.parent;
		}
		return par;
	}
	static Parabola getLeftChild(Parabola p) {
		if (p==null) {
			return null;
		}
		Parabola par = p.left();
		while (!par.isLeaf) {
			par = par.right();
		}
		return par;
	}
	static Parabola getRightChild(Parabola p) {
		if (p==null) {
			return null;
		}
		Parabola par = p.right();
		while (!par.isLeaf) {
			par = par.left();
		}
		return par;
	}
}
