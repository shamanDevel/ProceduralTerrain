/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sebastian Weiss
 */
public class HeightmapTest {
	
	public HeightmapTest() {
	}
	
	@Test
	public void testInterpolating() {
		Heightmap map = new Heightmap(4);
		for (int x=0; x<4; ++x) {
			for (int y=0; y<4; ++y) {
				map.setHeightAt(x, y, -10);
			}
		}
		map.setHeightAt(1, 1, 0);
		map.setHeightAt(1, 2, 1);
		map.setHeightAt(2, 1, 2);
		map.setHeightAt(2, 2, 3);
		float e = 0.0001f;
		float f = 0.000001f;
		
		assertEquals(0, map.getHeightInterpolating(1, 1), e);
		assertEquals(1, map.getHeightInterpolating(1, 2), e);
		assertEquals(2, map.getHeightInterpolating(2, 1), e);
		assertEquals(3, map.getHeightInterpolating(2, 2), e);
		
		assertEquals(0, map.getHeightInterpolating(1+f, 1), e);
		assertEquals(1, map.getHeightInterpolating(1+f, 2), e);
		assertEquals(2, map.getHeightInterpolating(2+f, 1), e);
		assertEquals(3, map.getHeightInterpolating(2+f, 2), e);
		assertEquals(0, map.getHeightInterpolating(1-f, 1), e);
		assertEquals(1, map.getHeightInterpolating(1-f, 2), e);
		assertEquals(2, map.getHeightInterpolating(2-f, 1), e);
		assertEquals(3, map.getHeightInterpolating(2-f, 2), e);
		assertEquals(0, map.getHeightInterpolating(1, 1+f), e);
		assertEquals(1, map.getHeightInterpolating(1, 2+f), e);
		assertEquals(2, map.getHeightInterpolating(2, 1+f), e);
		assertEquals(3, map.getHeightInterpolating(2, 2+f), e);
		assertEquals(0, map.getHeightInterpolating(1, 1-f), e);
		assertEquals(1, map.getHeightInterpolating(1, 2-f), e);
		assertEquals(2, map.getHeightInterpolating(2, 1-f), e);
		assertEquals(3, map.getHeightInterpolating(2, 2-f), e);
		
		assertEquals(0.5, map.getHeightInterpolating(1, 1.5f), e);
		assertEquals(1, map.getHeightInterpolating(1.5f, 1), e);
		assertEquals(2, map.getHeightInterpolating(1.5f, 2), e);
		assertEquals(2.5, map.getHeightInterpolating(2, 1.5f), e);
	}
}
