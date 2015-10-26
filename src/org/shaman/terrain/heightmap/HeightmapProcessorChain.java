/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.heightmap;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Sebastian Weiss
 */
public class HeightmapProcessorChain {
	private final Random rand = new Random();
	private final ArrayList<HeightmapProcessor> processors;
	private final Heightmap original;
	private Heightmap map;

	public HeightmapProcessorChain(Heightmap original) {
		this.processors = new ArrayList<>();
		this.original = original;
	}
	
	public void addProcessor(HeightmapProcessor p) {
		processors.add(p);
	}
	
	public boolean removeProcessor(HeightmapProcessor p) {
		return processors.remove(p);
	}
	
	public void clearProcessors() {
		processors.clear();
	}
	
	public Heightmap applyAll() {
		Heightmap current = original.clone();
		for (HeightmapProcessor p : processors) {
			current = p.apply(current);
		}
		return current;
	}
	
	public void setSeeds() {
		for (HeightmapProcessor p : processors) {
			p.setSeed(rand.nextLong());
		}
	}
}
