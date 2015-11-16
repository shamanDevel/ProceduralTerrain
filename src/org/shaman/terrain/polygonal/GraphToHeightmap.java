/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.polygonal;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.shaman.terrain.AbstractTerrainStep;
import org.shaman.terrain.Heightmap;

/**
 * Converts the graph to a heightmap.<br>
 * Input: a {@link Graph} with all the informations of elevation, temperature, moisture, biomes.<br>
 * Output: a {@link Map} object with the heightmap, temperature and moisture stored
 * in {@link Heightmap} instances, using the keys from {@link AbstractTerrainStep}.
 * @author Sebastian Weiss
 */
public class GraphToHeightmap {
	//Input
	private final Graph graph;
	private final int size;
	//Output
	private final Heightmap heightmap;
	private final Heightmap temperature;
	private final Heightmap moisture;
	private final Map<Object, Object> properties;

	public GraphToHeightmap(Graph graph, int size) {
		this.graph = graph;
		this.size = size;
		
		heightmap = new Heightmap(size);
		temperature = new Heightmap(size);
		moisture = new Heightmap(size);
		properties = new HashMap<>();
		properties.put(AbstractTerrainStep.KEY_HEIGHTMAP, heightmap);
		properties.put(AbstractTerrainStep.KEY_TEMPERATURE, temperature);
		properties.put(AbstractTerrainStep.KEY_MOISTURE, moisture);
		properties.put("PolygonalGraph", graph); //for backup
		
		calculate();
	}
	public Map<Object, Object> getResult() {
		return properties;
	}
	
	private void calculate() {
		
	}
}
