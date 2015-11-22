/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.erosion;

import com.jme3.scene.Geometry;
import java.io.Serializable;

/**
 *
 * @author Sebastian Weiss
 */
public class RiverSource implements Serializable {
	private static final long serialVersionUID = -6156570240252394893L;
	public int x;
	public int y;
	public float radius;
	public float intensity;
	public transient Geometry geom;
	
}
