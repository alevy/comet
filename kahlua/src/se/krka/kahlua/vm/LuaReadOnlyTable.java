package se.krka.kahlua.vm;

import java.util.NavigableMap;

/**
 * @author levya
 *
 */
public class LuaReadOnlyTable extends LuaMapTable {

	public LuaReadOnlyTable(NavigableMap<Object, Object> table,
			LuaTable metatable) {
		super(table, metatable);
	}
	
	public LuaReadOnlyTable() {
		super();
	}
	
	public LuaReadOnlyTable(LuaMapTable t) {
		super(t.table, t.getMetatable());
	}

	@Override
	public void rawset(Object key, Object value) {
		return;
	}
	
	@Override
	public void rawset(int key, Object value) {
		return;
	}
	
	@Override
	public void setMetatable(LuaTable metatable) {
		return;
	}

}
