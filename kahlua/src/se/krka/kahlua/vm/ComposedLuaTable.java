package se.krka.kahlua.vm;

public class ComposedLuaTable implements LuaTable {

	private final LuaTable primary;
	private final LuaTable[] backends;

	public ComposedLuaTable(LuaTable primary, LuaTable... backends) {
		this.primary = primary;
		this.backends = backends;

	}

	@Override
	public LuaTable getMetatable() {
		return primary.getMetatable();
	}

	@Override
	public int len() {
		int i = 0;
		while (rawget(i + 1) != null) {
			++i;
		}
		return i;
	}

	@Override
	public Object next(Object key) {
		return primary.next(key);
	}

	@Override
	public Object rawget(Object key) {
		Object result = primary.rawget(key);
		if (result != null) {
			return result;
		}
		for (LuaTable table : backends) {
			result = table.rawget(key);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public Object rawget(int key) {
		Object result = primary.rawget(key);
		if (result != null) {
			return result;
		}
		for (LuaTable table : backends) {
			result = table.rawget(key);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public void rawset(Object key, Object value) {
		primary.rawset(key, value);
	}

	@Override
	public void rawset(int key, Object value) {
		primary.rawset(key, value);
	}

	@Override
	public void setMetatable(LuaTable metatable) {
		primary.setMetatable(metatable);
	}

	@Override
	public void updateWeakSettings(boolean weakKeys, boolean weakValues) {
		primary.updateWeakSettings(weakKeys, weakValues);
	}

}
