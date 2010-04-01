/**
 * 
 */
package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import java.util.SortedSet;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaTable;
import edu.washington.cs.activedht.db.dhtwrapper.UpdateNeighborsCallback;
import edu.washington.cs.activedht.db.kahlua.KahluaActiveDHTDBValue;

/**
 * @author levya
 * 
 */
public class LuaUpdateNeighborsCallback implements UpdateNeighborsCallback {

	private final LuaClosure closure;
	private final KahluaActiveDHTDBValue value;

	public LuaUpdateNeighborsCallback(LuaClosure closure,
			KahluaActiveDHTDBValue value) {
		this.closure = closure;
		this.value = value;
	}

	@Override
	public void call(SortedSet<NodeWrapper> neighbors) {
		LuaTable vals = new LuaMapTable();
		for (NodeWrapper neighbor : neighbors) {
			if (neighbor != null) {
				vals.rawset(vals.len() + 1, neighbor);
			}
		}
		value.call(closure, new Object[] { vals });
	}

}
