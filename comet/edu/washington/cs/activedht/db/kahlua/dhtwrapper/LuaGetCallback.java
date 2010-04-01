package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import java.util.List;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Deserializer;

import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.dhtwrapper.GetCallback;

/**
 * 
 * @author levya
 * 
 */
class LuaGetCallback implements GetCallback {
	final LuaClosure closure;
	final LuaState state;

	public LuaGetCallback(LuaClosure closure, LuaState state) {
		this.closure = closure;
		this.state = state;
	}

	public void call(List<DHTTransportValue> values) {
		LuaTable vals = new LuaMapTable();
		for (int i = 0; i < values.size(); ++i) {
			byte[] value = values.get(i).getValue();
			if (value != null) {
				Object val = Deserializer.deserializeBytes(value, state
						.getEnvironment());
				vals.rawset(i + 1, val);
			}
		}
		state.call(closure, new Object[] { vals });
	}
}