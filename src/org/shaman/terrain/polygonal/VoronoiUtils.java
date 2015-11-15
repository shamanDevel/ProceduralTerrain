/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Vector2d;
import org.shaman.terrain.voronoi.Edge;

/**
 *
 * @author Sebastian Weiss
 */
public class VoronoiUtils {
	private static final Logger LOG = Logger.getLogger(VoronoiUtils.class.getName());
	
	public static class Site {
		public Vector2d center;
		public Vector2d[] corners;
		public Edge[] edges;
	}
	
	public static List<Site> findSites(List<Vector2d> sites, List<Edge> edges) {
		ArrayList<Site> result = new ArrayList<>();
		ArrayList<Edge> tmpEdges = new ArrayList<>();
		HashSet<Vector2d> tmpCorners = new LinkedHashSet<>();
		//first trivial implementation
		for (Vector2d center : sites) {
			Site site = new Site();
			site.center = center;
			//collect bounding edges
			tmpEdges.clear();
			tmpCorners.clear();
			for (Edge e : edges) {
				if (e.getLeft()==center || e.getRight()==center) {
					tmpEdges.add(e);
					tmpCorners.add(e.getStart());
					tmpCorners.add(e.getEnd());
				}
			}
			if (tmpEdges.isEmpty() || tmpCorners.isEmpty()) {
				LOG.log(Level.WARNING, "no edges for point {0} found!", center);
				continue;
			}
			site.edges = tmpEdges.toArray(new Edge[tmpEdges.size()]);
			site.corners = tmpCorners.toArray(new Vector2d[tmpCorners.size()]);
			result.add(site);
		}
		return result;
	}
	
	public static List<Vector2d> generateRelaxedSites(List<Vector2d> centers, List<Edge> edges) {
		List<Site> sites = findSites(centers, edges);
		ArrayList<Vector2d> newCenters = new ArrayList<>(sites.size());
		for (Site site : sites) {
			Vector2d p = new Vector2d(0, 0);
			for (Vector2d c : site.corners) {
				p.x += c.x;
				p.y += c.y;
			}
			p.x /= site.corners.length;
			p.y /= site.corners.length;
			newCenters.add(p);
		}
		return newCenters;
	}
	
	/**
	 * For some reason, in some constellation the voronoi algorithm does not
	 * produce a correct result. This method checks by a heuristic if this
	 * situation is the case.
	 * 
	 * @param edges
	 * @param size
	 * @return 
	 */
	public static boolean isValid(List<Edge> edges, float size) {
		float epsilon = 0.05f * size;
		epsilon *= epsilon;
		for (Edge e : edges) {
			Vector2d v = new Vector2d(e.getStart().x - e.getEnd().x, e.getStart().y - e.getEnd().y);
			if (v.lengthSquared() > epsilon) {
				return false;
			}
		}
		return true;
	}
}
