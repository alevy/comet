package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import se.krka.kahlua.vm.LuaTable;

public class LuaTableWrapper implements LuaTable {

	private final LuaTable table;

	public LuaTableWrapper(LuaTable table) {
		this.table = table;
	}
	
	@Override
	public LuaTable getMetatable() {
		return table.getMetatable();
	}

	@Override
	public int len() {
		return table.len();
	}

	@Override
	public Object next(Object key) {
		return table.next(key);
	}

	@Override
	public Object rawget(Object key) {
		return table.rawget(key);
	}

	@Override
	public Object rawget(int key) {
		return table.rawget(key);
	}

	@Override
	public void rawset(Object key, Object value) {
		table.rawset(key, value);
	}

	@Override
	public void rawset(int key, Object value) {
		table.rawset(key, value);
	}

	@Override
	public void setMetatable(LuaTable metatable) {
		table.setMetatable(metatable);
	}

	@Override
	public void updateWeakSettings(boolean weakKeys, boolean weakValues) {
		table.updateWeakSettings(weakKeys, weakValues);
	}

}
