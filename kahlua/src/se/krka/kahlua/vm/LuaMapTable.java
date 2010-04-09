package se.krka.kahlua.vm;

import java.util.ArrayList;
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

	private ArrayList<Object> list = null;

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
		return table.size() + getList().size();
	}

	@Override
	public Object next(Object key) {
		if (table.isEmpty() && getList().isEmpty()) {
			return null;
		}
		if (key == null) {
			if (table.isEmpty()) {
				return getList().isEmpty() ? null : 1.0;
			}
			return table.firstKey();
		}
		Integer keyInt = keyToInt(key);
		if (keyInt != null) {
			return keyInt < getList().size() ? keyInt.doubleValue() + 1 : null;
		}
		Object higherTableKey = table.higherKey(key);
		if (higherTableKey != null) {
			return higherTableKey;
		} else {
			return getList().isEmpty() ? null : 1.0;
		}
	}

	@Override
	public Object rawget(Object key) {
		Integer intKey = keyToInt(key);
		if (intKey != null) {
			return rawget((int)intKey);
		} else {
			return table.get(key);
		}
	}

	@Override
	public Object rawget(int key) {
		if (key - 1 < getList().size()) {
			return getList().get(key - 1);
		}
		return null;
	}

	@Override
	public void rawset(Object key, Object value) {
		Integer intKey = keyToInt(key);
		if (intKey != null) {
			rawset((int)intKey, value);
		} else {
			table.put(key, value);
		}
	}

	@Override
	public void rawset(int index, Object value) {
		--index;
		if (index > getList().size()) {
			getList().ensureCapacity(index + 1);
			for (int i = getList().size(); i < index; ++i) {
				getList().add(i, null);
			}
		}
		getList().add(index, value);
	}

	@Override
	public void setMetatable(LuaTable metatable) {
		this.metatable = metatable;
	}

	@Override
	public void updateWeakSettings(boolean weakKeys, boolean weakValues) {
		return;
	}

	@Override
	public String toString() {
		return table.toString();
	}

	private Integer keyToInt(Object key) {
		if (Double.class.isInstance(key)) {
			Double dKey = Double.class.cast(key);
			int intValue = dKey.intValue();
			if (intValue > 0 && intValue == dKey.doubleValue()) {
				return intValue;
			}
		}
		return null;
	}

	private ArrayList<Object> getList() {
		if (list == null) {
			list = new ArrayList<Object>(10);
		}
		return list;
	}

}
