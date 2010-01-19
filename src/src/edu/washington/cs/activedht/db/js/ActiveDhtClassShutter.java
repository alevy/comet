/**
 * 
 */
package edu.washington.cs.activedht.db.js;

import java.util.Collections;

import org.mozilla.javascript.ClassShutter;

public class ActiveDhtClassShutter implements ClassShutter {
	
	public boolean visibleToScripts(String fullClassName) {
		if (fullClassName.startsWith("edu.washington.cs.activedht.db")) {
			return true;
		}
		if (fullClassName.equals(String.class.getName())) {
			return true;
		}
		if (fullClassName.startsWith(Collections.class.getName())) {
			return true;
		}
		return false;
	}
}