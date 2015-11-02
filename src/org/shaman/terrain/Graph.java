/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.lang3.tuple.*;

/**
 * A simple graph class for undirected, weighted graphs.
 *
 * @author Sebastian Weiss
 */
public class Graph implements Iterable<Triple<Integer, Integer, Float>> {

	private final ArrayList<ArrayList<MutablePair<Integer, Float>>> graph;

	public Graph() {
		this.graph = new ArrayList<>();
	}

	public int addNode() {
		int i = graph.size();
		graph.add(new ArrayList<MutablePair<Integer, Float>>());
		return i;
	}

	public void addEdge(int u, int v, float weight) {
		ArrayList<MutablePair<Integer, Float>> n1 = graph.get(u);
		ArrayList<MutablePair<Integer, Float>> n2 = graph.get(v);
		n1.add(new MutablePair<>(v, weight));
		n2.add(new MutablePair<>(u, weight));
	}

	public void setEdgeWeight(int u, int v, float weight) {
		ArrayList<MutablePair<Integer, Float>> n1 = graph.get(u);
		ArrayList<MutablePair<Integer, Float>> n2 = graph.get(v);
		for (MutablePair<Integer, Float> p : n1) {
			if (p.getLeft() == v) {
				p.setRight(weight);
				break;
			}
		}
		for (MutablePair<Integer, Float> p : n2) {
			if (p.getLeft() == u) {
				p.setRight(weight);
				break;
			}
		}
	}

	public void deleteEdge(int u, int v) {
		ArrayList<MutablePair<Integer, Float>> n1 = graph.get(u);
		ArrayList<MutablePair<Integer, Float>> n2 = graph.get(v);
		for (Iterator<MutablePair<Integer, Float>> it = n1.iterator(); it.hasNext();) {
			if (it.next().getLeft() == v) {
				it.remove();
				break;
			}
		}
		for (Iterator<MutablePair<Integer, Float>> it = n2.iterator(); it.hasNext();) {
			if (it.next().getLeft() == u) {
				it.remove();
				break;
			}
		}
	}
	
	@Override
	public Iterator<Triple<Integer, Integer, Float>> iterator() {
		return new Iterator<Triple<Integer, Integer, Float>>() {
			Triple<Integer, Integer, Float> current = null;
			private int u=0;
			private int v=0;
			
			@Override
			public boolean hasNext() {
				while (true) {
					if (u==graph.size()) {
						return false;
					}
					ArrayList<MutablePair<Integer, Float>> n = graph.get(u);
					if (v==n.size()) {
						v=0;
						u++;
						continue;
					}
					MutablePair<Integer, Float> p = n.get(v);
					v++;
					if (p.getLeft()>u) {
						current = new ImmutableTriple<>(u, p.getLeft(), p.getRight());
						return true;
					}
				}
			}

			@Override
			public Triple<Integer, Integer, Float> next() {
				return current;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Not supported yet.");
			}
		};
	}

	// A recursive function that uses visited[] and parent to detect
// cycle in subgraph reachable from vertex v.
	boolean isCyclicUtil(int v, boolean visited[], int parent) {
		// Mark the current node as visited
		visited[v] = true;

		// Recur for all the vertices adjacent to this vertex
		for (MutablePair<Integer, Float> p : graph.get(v)) {
			int i = p.getLeft();
			if (!visited[i]) {
				if (isCyclicUtil(i, visited, parent)) {
					return true;
				} else if (i != parent) {
					return true;
				}
			}
		}
		return false;
	}

// Returns true if the graph contains a cycle, else false.
	boolean isCyclic() {
    // Mark all the vertices as not visited and not part of recursion
		// stack
		boolean[] visited = new boolean[graph.size()];

    // Call the recursive helper function to detect cycle in different
		// DFS trees
		for (int u = 0; u < graph.size(); u++) {
			if (!visited[u]) // Don't recur for u if it is already visited
			{
				if (isCyclicUtil(u, visited, -1)) {
					return true;
				}
			}
		}

		return false;
	}
}
