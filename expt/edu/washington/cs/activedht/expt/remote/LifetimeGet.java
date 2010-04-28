/**
 * 
 */
package edu.washington.cs.activedht.expt.remote;

import java.util.Map;

import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Deserializer;
import se.krka.kahlua.vm.serialize.Type;


/**
 * @author levya
 * 
 */
public class LifetimeGet extends RemoteGet {

	public LifetimeGet(String[] args) throws Exception {
		super(args);
	}

	public String getArrayString(LuaMapTable table) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < table.len(); ++i) {
			LuaTable t = (LuaTable) table.rawget(i + 1);
			builder.append(((Double) t.rawget(1)).intValue());
			builder.append('-');
			builder.append(((Double) t.rawget(2)).longValue());
			builder.append(',');
		}

		builder.setLength(builder.length() == 0 ? 0 : builder.length() - 1);
		return builder.toString();
	}

	public void handleValue(byte[] value) {
		if (value[0] == Type.TABLE) {
			LuaMapTable table = (LuaMapTable) Deserializer.deserializeBytes(
					value, new LuaMapTable());
			System.out.println(table);
			for (Map.Entry<Object, Object> entry : table.table.entrySet()) {
				out.print(entry.getKey());
				out.print(":");
				out.println(getArrayString((LuaMapTable) entry.getValue()));
			}
		} else {
			System.out.println(Deserializer.deserializeBytes(value, null));
		}
	}

	public byte[] getKey() {
		return contact.getID();
	}

	public byte[] getReaderID() {
		return new byte[] {};
	}

	public byte[] getPayload() {
		return new byte[] {};
	}

	public static void main(String[] args) throws Exception {
		new LifetimeGet(args).run();
	}

}
