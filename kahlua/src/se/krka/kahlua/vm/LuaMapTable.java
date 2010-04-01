package se.krka.kahlua.vm;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 
 * @author levya
 *
 */
public class LuaMapTable implements LuaTable {

	public final NavigableMap<Object, Object> table;

	private LuaTable metatable;

	public LuaMapTable(NavigableMap<Object, Object> table, LuaTable metatable) {
		this.table = table;
		this.metatable = metatable;
	}
	
	public LuaMapTable() {
		this.table = new TreeMap<Object, Object>(new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
	}

	@Override
	public LuaTable getMetatable() {
		return metatable;
	}

	@Override
	public int len() {
		return table.size();
	}

	@Override
	public Object next(Object key) {
		if (table.isEmpty()) {
			return null;
		}
		if (key == null) {
			return table.firstKey();
		}
		return table.higherKey(key);
	}

	@Override
	public Object rawget(Object key) {
		return table.get(key);
	}

	@Override
	public Object rawget(int key) {
		return rawget(LuaState.toDouble(key));
	}

	@Override
	public void rawset(Object key, Object value) {
		table.put(key, value);
	}

	@Override
	public void rawset(int key, Object value) {
		table.put(LuaState.toDouble(key), value);
	}

	@Override
	public void setMetatable(LuaTable metatable) {
		this.metatable = metatable;
	}

	@Override
	public void updateWeakSettings(boolean weakKeys, boolean weakValues) {
		return;
	}

}
