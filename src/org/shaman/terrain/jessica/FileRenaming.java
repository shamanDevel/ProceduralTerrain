/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.jessica;

import java.io.File;
import java.text.DecimalFormat;

/**
 *
 * @author Sebastian Weiss
 */
public class FileRenaming {
	private static final String FOLDER = "F:\\TMP\\";
	private static final String PREFIX = "JessicaIsland";
	private static final DecimalFormat FORMAT = new DecimalFormat("00000");

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		File folder = new File(FOLDER);
		
		for (int i=1; ;++i) {
			File f = new File(folder, PREFIX+i+".png");
			if (!f.exists()) {
				return;
			}
			File target = new File(folder, PREFIX+FORMAT.format(i)+".png");
			f.renameTo(target);
			System.out.println("rename "+f+" to "+target);
		}
	}
	
}
