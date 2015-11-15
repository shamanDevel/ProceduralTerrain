/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.voronoi;

import java.util.*;
import javax.vecmath.Vector2d;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.DefaultedMap;
import org.apache.commons.collections4.map.MultiValueMap;

/**
 * C++ to Java port of the voronoi diagram algorithm from
 * http://blog.ivank.net/fortunes-algorithm-and-implementation.html .
 * @author Sebastian Weiss
 */
public class Voronoi {
	private ArrayList<Vector2d> places;
	private ArrayList<Edge> edges;
	private double width, height;
	private Parabola root;
	private double ly;
	private HashSet<Event> deleted;
	private ArrayList<Vector2d> points;
	private PriorityQueue<Event> queue;
	
	public Voronoi() {
		edges = new ArrayList<>();
		points = new ArrayList<>();
		deleted = new HashSet<>();
		queue = new PriorityQueue<>();
	}
	/**
	 * Computes the voronoi diagram of the specified set of points.
	 * It returns a list of edges.
	 * Note that the points should not be too close, recommended values
	 * for w and h are 10000. All points must lie inside [0,w[ x [0,h[.
	 * @param v the list of points
	 * @param w the width of the sweeping area
	 * @param h the height of the sweeping area
	 * @return a list of edges
	 */
	public List<Edge> getEdges(List<Vector2d> v, float w, float h) {
		places = new ArrayList<>(v);
		width = w;
		height = h;
		root = null;
		
		points.clear();
		edges.clear();
		queue.clear();
		deleted.clear();
		
		for (Vector2d i : places) {
			queue.add(new Event(i, true));
		}
		
		while (!queue.isEmpty()) {
			Event e = queue.poll();
			ly = e.point.y;
			if (deleted.contains(e)) {
				deleted.remove(e);
				continue;
			}
			if (e.pe) {
				insertParabola(e.point);
			} else {
				removeParabola(e);
			}
		}
		
		finishEdge(root);
		
		for (Edge i : edges) {
			if (i.neighbour!=null) {
				i.start = i.neighbour.end;
				i.neighbour = null;
			}
		}
		
		return new ArrayList<>(edges);
	}
	
	/**
	 * Closes the box: the cells are surrounded by a box with the specified
	 * dimensions. All border cells that were previous at infinity are capped
	 * at that box.
	 * @param edges the list of edges, this list is destroyed
	 * @param w the width of the box
	 * @param h the height of the box
	 * @return the list with the edges clamped to the borders
	 */
	public static List<Edge> closeEdges(List<Edge> edges, float w, float h) {
		ArrayList<Edge> output = new ArrayList<>();
		MultiMap<Vector2d, Vector2d> borders = new MultiValueMap<>();
		for (Edge edge : edges) {
			boolean sInside = isInside(edge.start, w, h);
			boolean eInside = isInside(edge.end, w, h);
			if (sInside && eInside) {
				output.add(edge); //normal edge inside
				continue;
			} else if (!sInside && !eInside) {
				continue; //completely outside, ignore it
			}
			//border edge, cut with border
			Vector2d p = edgeBoxIntersection(edge, w, h);
			if (sInside) {
				edge.end = p;
			} else {
				edge.start = p;
			}
			output.add(edge);
			borders.put(edge.left, p);
			borders.put(edge.right, p);
		}
		//connect borders
//		ArrayList<Vector2d> borderCenters = new ArrayList<>();
//		borderCenters.addAll(borders.keySet());
//		for (Vector2d center : borders.keySet()) {
//			@SuppressWarnings("unchecked")
//			Collection<Vector2d> ex = (Collection<Vector2d>) borders.get(center);
//			if (ex.size() == 2) {
//				Vector2d[] points = ex.toArray(new Vector2d[2]);
//				Edge e = new Edge();
//				e.start = points[0];
//				e.end = points[1];
//				e.left = center;
//				output.add(e);
//			} else {
//				System.err.println("illegal edge borders: "+center+" -> "+ex);
//				errors.add(center); //for some reason, these cannot be processed, use extra step
//			}
//		}
//		//process erounous centers
//		for (Vector2d center : borderCenters) {
//			//Find corners
//			DefaultedMap<Vector2d, Integer> cornerCounter = new DefaultedMap<>(0);
//			for (Edge e : output) {
//				if (e.left == center || e.right == center) {
//					cornerCounter.put(e.start, cornerCounter.get(e.start) + 1);
//					cornerCounter.put(e.end, cornerCounter.get(e.end) + 1);
//				}
//			}
////			System.err.println("corner counter for center "+center+": "+cornerCounter);
//			ArrayList<Vector2d> corners = new ArrayList<>(2);
//			for (Map.Entry<Vector2d, Integer> entry : cornerCounter.entrySet()) {
//				if (entry.getValue()==1) {
//					corners.add(entry.getKey());
//				}
//			}
//			if (corners.size() == 2) {
//				Edge e = new Edge();
//				e.start = corners.get(0);
//				e.end = corners.get(1);
//				e.left = center;
//				output.add(e);
//			} else {
//				System.err.println("illegal corner map");
//			}
//		}
		
		return output;
	}
	private static boolean isInside(Vector2d v, float w, float h) {
		if (v.x<0 || v.y<0 || v.x>=w || v.y>=h) {
			return false;
		} else {
			return true;
		}
	}
	private static Vector2d edgeBoxIntersection(Edge e, float w, float h) {
		double a,b,x,y;
		if (isInside(e.end, w, h)) {
			a = e.end.x;
			b = e.end.y;
			x = e.start.x;
			y = e.start.y;
		} else {
			a = e.start.x;
			b = e.start.y;
			x = e.end.x;
			y = e.end.y;
		}
		double t1 = a/(a-x);
		double t2 = b/(b-y);
		double t3 = (w-a)/(x-a);
		double t4 = (h-b)/(y-b);
		if (t1<0 || t1>1) t1 = Double.POSITIVE_INFINITY;
		if (t2<0 || t2>1) t2 = Double.POSITIVE_INFINITY;
		if (t3<0 || t3>1) t3 = Double.POSITIVE_INFINITY;
		if (t4<0 || t4>1) t4 = Double.POSITIVE_INFINITY;
		double t = Math.min(Math.min(t1, t2), Math.min(t3, t4));
		if (t == Double.POSITIVE_INFINITY) {
			return null;
		}
		return new Vector2d(a + t*(x-a), b + t*(y-b));
	}
	
	private void insertParabola(Vector2d p) {
		if (root==null) {
			root = new Parabola(p);
			return;
		}
		
		if (root.isLeaf && root.site.y-p.y < 1) {
			Vector2d fp = root.site;
			root.isLeaf = false;
			root.setLeft(new Parabola(fp));
			root.setRight(new Parabola(p));
			Vector2d s = new Vector2d((p.x+fp.x)/2, height);
			points.add(s);
			if (p.x > fp.x) {
				root.edge = new Edge(s, fp, p);
			} else {
				root.edge = new Edge(s, p, fp);
			}
			edges.add(root.edge);
			return;
		}
		
		Parabola par = getParabolaByX(p.x);
		if (par.cEvent != null) {
			deleted.add(par.cEvent);
			par.cEvent = null;
		}
		
		Vector2d start = new Vector2d(p.x, getY(par.site, p.x));
		points.add(start);
		Edge el = new Edge(start, par.site, p);
		Edge er = new Edge(start, p, par.site);
		
		el.neighbour = er;
		edges.add(el);
		
		par.edge = er;
		par.isLeaf = false;
		
		Parabola p0 = new Parabola(par.site);
		Parabola p1 = new Parabola(p);
		Parabola p2 = new Parabola(par.site);
		
		par.setRight(p2);
		par.setLeft(new Parabola());
		par.left().edge = el;
		par.left().setLeft(p0);
		par.left().setRight(p1);
		
		checkCircle(p0);
		checkCircle(p2);
	}
	private void removeParabola(Event e) {
		Parabola p1 = e.arch;
		Parabola xl = Parabola.getLeftParent(p1);
		Parabola xr = Parabola.getRightParent(p1);
		Parabola p0 = Parabola.getLeftChild(xl);
		Parabola p2 = Parabola.getRightChild(xr);
		
		assert (p0 != p2);
		
		if (p0.cEvent != null) {
			deleted.add(p0.cEvent);
			p0.cEvent = null;
		}
		if (p2.cEvent != null) {
			deleted.add(p2.cEvent);
			p2.cEvent = null;
		}
		
		Vector2d p = new Vector2d(e.point.x, getY(p1.site, e.point.x));
		points.add(p);
		
		xl.edge.end = p;
		xr.edge.end = p;
		
		Parabola higher = null;
		Parabola par = p1;
		while (par != root) {
			par = par.parent;
			if (par == xl) higher = xl;
			if (par == xr) higher = xr;
		}
		higher.edge = new Edge(p, p0.site, p2.site);
		edges.add(higher.edge);
		
		Parabola gparent = p1.parent.parent;
		if (p1.parent.left() == p1) {
			if (gparent.left() == p1.parent) {
				gparent.setLeft(p1.parent.right());
			}
			if (gparent.right() == p1.parent) {
				gparent.setRight(p1.parent.right());
			}
		} else {
			if (gparent.left() == p1.parent) {
				gparent.setLeft(p1.parent.left());
			}
			if (gparent.right() == p1.parent) {
				gparent.setRight(p1.parent.left());
			}
		}
		
		checkCircle(p0);
		checkCircle(p2);
	}
	private void finishEdge(Parabola n) {
		if (n.isLeaf) {
			return;
		}
		double mx;
		if (n.edge.direction.x > 0) {
			mx = Math.max(width, n.edge.start.x + 10);
		} else {
			mx = Math.min(0, n.edge.start.x - 10);
		}
		Vector2d end = new Vector2d(mx, mx*n.edge.f + n.edge.g);
		n.edge.end = end;
		points.add(end);
		
		finishEdge(n.left());
		finishEdge(n.right());
	}
	private double getXOfEdge(Parabola par, double y) {
		Parabola left = Parabola.getLeftChild(par);
		Parabola right = Parabola.getRightChild(par);
		
		Vector2d p = left.site;
		Vector2d r = right.site;
		
		double dp = 2 * (p.y - y);
		double a1 = 1/dp;
		double b1 = -2 * p.x / dp;
		double c1 = y + dp/4 + p.x*p.x/dp;
		dp = 2*(r.y-y);
		double a2 = 1/dp;
		double b2 = -2*r.x/dp;
		double c2 = ly + dp/4 + r.x*r.x/dp;
		
		double a = a1-a2;
		double b = b1-b2;
		double c = c1-c2;
		
		double disc = b*b - 4*a*c;
		double x1 = (-b + Math.sqrt(disc)) / (2*a);
		double x2 = ((-b - Math.sqrt(disc)) / (2*a));
		
		double ry;
		if (p.y < r.y) {
			ry = Math.max(x1, x2);
		} else {
			ry = Math.min(x1, x2);
		}
		return ry;
	}
	private Parabola getParabolaByX(double xx) {
		Parabola par = root;
		double x = 0;
		while (!par.isLeaf) {
			x = getXOfEdge(par, ly);
			if (x>xx) {
				par = par.left();
			} else {
				par = par.right();
			}
		}
		return par;
	}
	private double getY(Vector2d p, double x) {
		double dp = 2*(p.y - ly);
		double a1 = 1/dp;
		double b1 = -2*p.x/dp;
		double c1 = ly+dp/4 + p.x*p.x/dp;
		return a1*x*x + b1*x + c1;
	}
	private void checkCircle(Parabola b) {
		Parabola lp = Parabola.getLeftParent(b);
		Parabola rp = Parabola.getRightParent(b);
		Parabola a = Parabola.getLeftChild(lp);
		Parabola c = Parabola.getRightChild(rp);
		if (a==null || c==null || a.site==c.site) {
			return;
		}
		Vector2d s = getEdgeIntersection(lp.edge, rp.edge);
		if (s==null) {
			return;
		}
		double dx = a.site.x - s.x;
		double dy = a.site.y - s.y;
		double d = Math.sqrt(dx*dx + dy*dy);
		if (s.y - d >= ly) {
			return;
		}
		Event e = new Event(new Vector2d(s.x, s.y - d), false);
		points.add(e.point);
		b.cEvent = e;
		e.arch = b;
		queue.add(e);
	}
	private Vector2d getEdgeIntersection(Edge a, Edge b) {
		double x = (b.g - a.g) / (a.f - b.f);
		double y = a.f * x + a.g;
		if ((x-a.start.x) / a.direction.x < 0) return null;
		if ((y-a.start.y) / a.direction.y < 0) return null;
		if ((x-b.start.x) / b.direction.x < 0) return null;
		if ((y-b.start.y) / b.direction.y < 0) return null;
		Vector2d p = new Vector2d(x, y);
		points.add(p);
		return p;
	}
}
