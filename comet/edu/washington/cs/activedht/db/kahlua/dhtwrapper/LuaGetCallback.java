package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import java.util.List;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaTable;

import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.dhtwrapper.GetCallback;
import edu.washington.cs.activedht.db.kahlua.KahluaActiveDHTDBValue;

/**
 * 
 * @author levya
 * 
 */
class LuaGetCallback implements GetCallback {
	final LuaClosure closure;
	final KahluaActiveDHTDBValue activeValue;

	public LuaGetCallback(LuaClosure closure, KahluaActiveDHTDBValue activeValue) {
		this.closure = closure;
		this.activeValue = activeValue;
	}

	public void call(List<DHTTransportValue> values) {
		LuaTable vals = new LuaMapTable();
		for (int i = 0; i < values.size(); ++i) {
			byte[] value = values.get(i).getValue();
			if (value != null) {
				Object val = activeValue.deserialize(value);
				vals.rawset(i + 1, val);
			}
		}
		activeValue.call(closure, new Object[] { vals });
	}
}