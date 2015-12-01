/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import org.apache.commons.lang3.ArrayUtils;
import org.shaman.terrain.vegetation.ImpositorCreator;

/**
 *
 * @author Sebastian Weiss
 */
public class ProceduralTerrain {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		if (ArrayUtils.indexOf(args, "generate-trees") >= 0) {
			ImpositorCreator.main(args);
		} else if (ArrayUtils.indexOf(args, "arbaro") >= 0) {
			ArbaroTreeGenerator.main(args);
		} else {
			TerrainHeighmapCreator.main(args);
		}
	}
	
}
