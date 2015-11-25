/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.arbaro.params;

/**
 *
 * @author Sebastian Weiss
 */
public class StringConstantsParam extends StringParam {
	public final String[] items;

	public StringConstantsParam(String[] items, String nam, String def, String grp, int lev, int ord, String sh, String lng) {
		super(nam, def, grp, lev, ord, sh, lng);
		this.items = items;
	}
	
	public String[] getItems() {
		return items;
	}
}
