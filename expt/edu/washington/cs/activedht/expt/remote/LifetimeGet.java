/**
 * 
 */
package edu.washington.cs.activedht.expt.remote;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Deserializer;
import se.krka.kahlua.vm.serialize.Type;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author levya
 * 
 */
public class LifetimeGet extends RemoteNodeAction {

	private PrintStream out = System.out;

	public LifetimeGet(String[] args) throws Exception {
		super(args);
		if (args.length > 1) {
			File file = new File(args[1]);
			out = new PrintStream(file);
		}
	}

	public String getArrayString(LuaMapTable table) {
		StringBuilder builder = new StringBuilder();
		for (Entry<Object, Object> entry : table.table.entrySet()) {
			LuaTable t = (LuaTable)entry.getValue();
			builder.append(((Double) t.rawget(1)).intValue());
			builder.append('-');
			builder.append(((Double) t.rawget(2)).longValue());
			builder.append(',');
		}

		builder.setLength(builder.length() == 0 ? 0 : builder.length() - 1);
		return builder.toString();
	}

	public void printValue(byte[] value) {
		if (value[0] == Type.TABLE) {
			LuaMapTable table = (LuaMapTable) Deserializer.deserializeBytes(
					value, new LuaMapTable());
			for (Map.Entry<Object, Object> entry : table.table.entrySet()) {
				out.print(entry.getKey());
				out.print(":");
				out.println(getArrayString((LuaMapTable) entry.getValue()));
			}
		}
	}

	protected void run() throws Exception {
		final Semaphore sema = new Semaphore(0);
		contact.sendFindValue(new DHTTransportReplyHandlerAdapter() {

			@Override
			public void findValueReply(DHTTransportContact contact,
					DHTTransportValue[] values, byte diversificationType,
					boolean moreToCome) {
				if (values.length > 0) {
					printValue(values[0].getValue());
				}
				sema.release();
			}

			@Override
			public void failed(DHTTransportContact contact, Throwable error) {
				error.printStackTrace();
				sema.release();
			}
		}, contact.getID(), new byte[] {}, new byte[] {}, 1, (byte) 0);
		sema.acquire();
	}

	public static void main(String[] args) throws Exception {
		new LifetimeGet(args).run();
	}

}
