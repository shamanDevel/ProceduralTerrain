/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.shaman.terrain.heightmap.Noise;

/**
 *
 * @author Sebastian Weiss
 */
public class NoiseGenerator {
	private static final int SIZE = 512;
	private static final String OUTPUT = "Paper/images/Noise";

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		Noise noise = new Noise(1);
		
		//octaves
		float[] octaves = {2,4,8,16,32};
		double[] ampl = {1/2.0, 1/4.0, 1/8.0, 1/16.0, 1/32.0};
		for (int i=0; i<octaves.length; ++i) {
			BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
			for (int x=0; x<SIZE; ++x) {
				for (int y=0; y<SIZE; ++y) {
					float xx = x*octaves[i]/SIZE;
					float yy = y*octaves[i]/SIZE;
					double v = (noise.noise(xx, yy)+1)/2;
					int c = Math.max(0, Math.min(255, (int) (v*0xff)));
					c = c + (c<<8) + (c<<16);
					c |= (0xff000000);
					image.setRGB(x, y, c);
				}
			}
			ImageIO.write(image, "png", new File(OUTPUT+i+".png"));
		}
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
		for (int x=0; x<SIZE; ++x) {
			for (int y=0; y<SIZE; ++y) {
				double v = 0;
				for (int i=0; i<octaves.length; ++i) {
					float xx = x*octaves[i]/SIZE;
					float yy = y*octaves[i]/SIZE;
					v += noise.noise(xx, yy, 0) * ampl[i];
				}
				v += 0.5;
				int c = Math.max(0, Math.min(255, (int) (v*0xff)));
				c = c + (c<<8) + (c<<16);
				c |= (0xff000000);
				image.setRGB(x, y, c);
			}
		}
		ImageIO.write(image, "png", new File(OUTPUT + "fractal.png"));
	}
	
}
