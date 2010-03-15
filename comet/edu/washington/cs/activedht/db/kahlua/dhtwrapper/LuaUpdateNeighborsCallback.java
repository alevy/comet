/**
 * 
 */
package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import java.util.SortedSet;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;
import edu.washington.cs.activedht.db.dhtwrapper.UpdateNeighborsCallback;

/**
 * @author levya
 *
 */
public class LuaUpdateNeighborsCallback implements UpdateNeighborsCallback {

	private final LuaClosure closure;
	private final LuaState state;

	public LuaUpdateNeighborsCallback(LuaClosure closure, LuaState state) {
		this.closure = closure;
		this.state = state;
	}

	@Override
	public void call(SortedSet<NodeWrapper> neighbors) {
		LuaTable vals = new LuaTableImpl(neighbors.size());
		for (NodeWrapper neighbor : neighbors) {
			if (neighbor != null) {
				vals.rawset(vals.len() + 1, neighbor);
			}
		}
		synchronized (state) {
			state.call(closure, new Object[] { vals });
		}
	}

}
