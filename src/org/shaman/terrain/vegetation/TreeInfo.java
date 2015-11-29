/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.material.Material;
import java.io.Serializable;
import org.shaman.terrain.Biome;

/**
 *
 * @author Sebastian Weiss
 */
public class TreeInfo implements Serializable {
	private static final long serialVersionUID = -4799746574620732770L;
	
	public String name;
	public int impostorCount;
	
	public float treeSize;
	public float impostorFadeNear;
	public float impostorFadeFar;
	public float highResStemFadeNear;
	public float highResStemFadeFar;
	public float highResLeavesFadeNear;
	public float highResLeavesFadeFar;
	
	public Biome biome;
	public float probability;
	
	public transient Material impostorMaterial;
}
