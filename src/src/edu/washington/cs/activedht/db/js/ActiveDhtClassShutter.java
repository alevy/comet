/**
 * 
 */
package edu.washington.cs.activedht.db.js;

import java.util.Collections;

import org.mozilla.javascript.ClassShutter;

public class ActiveDhtClassShutter implements ClassShutter {
	
	private JSActiveDHTDBValue value;

	public ActiveDhtClassShutter(JSActiveDHTDBValue value) {
		this.value = value;
	}
	
	public boolean visibleToScripts(String fullClassName) {
		if (fullClassName.equals(value.getDhtWrapper().getClass().getName())) {
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