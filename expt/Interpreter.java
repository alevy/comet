import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Simple;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;
import se.krka.kahlua.vm.serialize.Deserializer;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;
import edu.washington.cs.activedht.expt.ActivePeer;
import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

public class Interpreter {

	private final LuaState state;
	private final BufferedReader in;
	private final DHTControl dhtControl;

	private class Require implements JavaFunction {

		public int call(LuaCallFrame callFrame, int nArguments) {
			String filename = callFrame.get(0).toString();
			File f = new File(filename);
			try {
				LuaClosure require = LuaCompiler.loadis(new FileInputStream(f),
						filename, state.getEnvironment());
				state.call(require);
			} catch (Exception e) {
				callFrame.push(e.getMessage());
				return 1;
			}
			return 0;
		}

	}

	private class Get implements JavaFunction {

		public int call(final LuaCallFrame callFrame, int nArguments) {
			if (nArguments < 1) {
				BaseLib.fail("get takes at least one argument");
			}
			byte[] key = encodeKey(callFrame.get(0).toString().getBytes());
			byte[] payload = null;
			if (nArguments > 1) {
				payload = Serializer.serialize(callFrame.get(1), state
						.getEnvironment());
			}
			final LuaTable values = new LuaTableImpl();
			final Semaphore sema = new Semaphore(0);

			if (nArguments > 2) {
				LuaTable contacts = (LuaTable) callFrame.get(2);
				for (int i = 1; i <= contacts.len(); ++i) {
					DHTTransportContact contact = ((NodeWrapper) contacts
							.rawget(i)).contact;
					contact.sendFindValue(
							new DHTTransportReplyHandlerAdapter() {

								@Override
								public void failed(DHTTransportContact contact,
										Throwable error) {
									System.err
											.print(contact.getString() + ": ");
									System.err.println(error.getMessage());
									sema.release();
								}

								@Override
								public void findValueReply(
										DHTTransportContact contact,
										DHTTransportValue[] vals,
										byte diversificationType,
										boolean moreToCome) {
									values.rawset(new NodeWrapper(contact),
											Deserializer.deserializeBytes(
													vals[0].getValue(), state
															.getEnvironment()));
									if (!moreToCome) {
										sema.release();
									}
								}
								
								@Override
								public void findValueReply(
										DHTTransportContact contact,
										DHTTransportContact[] contacts) {
								}

							}, key, dhtControl.getTransport().getLocalContact().getID(), payload, 1, (byte) 0);
				}
				sema.acquireUninterruptibly(contacts.len());
				callFrame.push(values);
				return 1;
			}

			dhtControl.getEncodedKey(key, dhtControl.getTransport()
					.getLocalContact().getID(), payload, "", (byte) 0, 20,
					60000, false, true, new DHTOperationAdapter() {
						public void read(DHTTransportContact contact,
								DHTTransportValue value) {
							Object obj = Deserializer.deserializeBytes(value
									.getValue(), state.getEnvironment());
							values.rawset(new NodeWrapper(contact), obj);
						}

						public void complete(boolean timeout) {
							sema.release();
						}
					});
			sema.acquireUninterruptibly();
			if (values.len() == 0) {
				callFrame.pushNil();
				return 1;
			}
			callFrame.push(values);
			return values.len();
		}

	}

	private class Lookup implements JavaFunction {

		public int call(LuaCallFrame callFrame, int nArguments) {
			if (nArguments < 1) {
				BaseLib.fail("lookup takes at least 1 argument");
			}
			Object obj = callFrame.get(0);
			byte[] key;
			if (String.class.isInstance(obj)) {
				key = ((String) obj).getBytes();
				final LuaTable contacts = new LuaTableImpl();
				final Semaphore sema = new Semaphore(0);
				dhtControl.lookup(key, "", 60000, new DHTOperationAdapter() {
					int i = 1;

					public void found(DHTTransportContact contact,
							boolean isClosest) {
						contacts.rawset(i, new NodeWrapper(contact));
						++i;
					}

					public void complete(boolean timeout) {
						sema.release();
					}
				});
				sema.acquireUninterruptibly();
				callFrame.push(contacts);
				return 1;
			} else {
				key = ((HashWrapper) obj).getBytes();

				final LuaTable contacts = new LuaTableImpl();
				final Semaphore sema = new Semaphore(0);
				dhtControl.lookupEncoded(key, "", 60000, true,
						new DHTOperationAdapter() {
							int i = 1;

							public void found(DHTTransportContact contact,
									boolean isClosest) {
								contacts.rawset(i, new NodeWrapper(contact));
								++i;
							}

							public void complete(boolean timeout) {
								sema.release();
							}
						});
				sema.acquireUninterruptibly();
				callFrame.push(contacts);
				return 1;
			}
		}

	}

	private class Put implements JavaFunction {

		public int call(LuaCallFrame callFrame, int nArguments) {
			if (nArguments < 2) {
				BaseLib.fail("put takes at least two argument");
			}
			String key = callFrame.get(0).toString();
			byte[] payload = Serializer.serialize(callFrame.get(1), state
					.getEnvironment());
			if (nArguments > 2) {
				LuaTable contacts = (LuaTable) callFrame.get(2);
				final Semaphore sema = new Semaphore(0);
				for (int i = 1; i <= contacts.len(); ++i) {
					DHTTransportContact contact = ((NodeWrapper) contacts
							.rawget(i)).contact;
					contact
							.sendStore(
									new DHTTransportReplyHandlerAdapter() {

										public void failed(
												DHTTransportContact contact,
												Throwable error) {
											System.err.print(contact
													.getString()
													+ ": ");
											System.err.println(error
													.getMessage());
											sema.release();
										}

										public void storeReply(
												DHTTransportContact contact,
												byte[] diversifications) {
											System.out.println("Wrote to: "
													+ contact.getString());
											sema.release();
										}
									},
									new byte[][] { encodeKey(key.getBytes()) },
									new DHTTransportValue[][] { new DHTTransportValue[] { new BasicDHTTransportValue(
											System.currentTimeMillis(),
											payload, "", 1, dhtControl
													.getTransport()
													.getLocalContact(), false,
											0) } }, true);
				}
				sema.acquireUninterruptibly(contacts.len());
				return 0;
			}

			final LuaTable contacts = new LuaTableImpl();
			final Semaphore sema = new Semaphore(0);
			dhtControl.put(key.getBytes(), "", payload, (byte) 0, (byte) 8,
					DHT.REP_FACT_DEFAULT, true, new DHTOperationAdapter() {
						int i = 1;

						public void wrote(DHTTransportContact contact,
								DHTTransportValue value) {
							contacts.rawset(i, new NodeWrapper(contact));
							++i;
						}

						public void complete(boolean timeout) {
							sema.release();
						}
					});
			sema.acquireUninterruptibly();
			callFrame.push(contacts);
			return 1;
		}

	}

	public Interpreter(LuaState state, InputStream in, DHTControl dhtControl) {
		this.state = state;
		this.dhtControl = dhtControl;
		this.in = new BufferedReader(new InputStreamReader(in));
		state.getEnvironment().rawset("require", new Require());
		state.getEnvironment().rawset("get", new Get());
		state.getEnvironment().rawset("put", new Put());
		state.getEnvironment().rawset("lookup", new Lookup());
		state.getEnvironment().rawset("localNode",
				new NodeWrapper(dhtControl.getTransport().getLocalContact()));
	}

	public void run() throws Exception {
		while (true) {
			System.out.print(">> ");
			System.out.flush();
			LuaClosure closure = getClosure(in.readLine());
			if (closure != null) {
				Object[] result = state.pcall(closure);
				if (result[0] == Boolean.TRUE) {
					printResults(result);
				} else {
					for (Object o : result) {
						System.out.println(o);
					}
				}
			}
		}
	}

	private void printResults(Object[] result) {
		for (int i = 1; i < result.length; i++) {
			if (i > 1) {
				System.out.print("\t");
			}
			System.out.print(BaseLib.tostring(result[i], state));
		}
		System.out.println();
	}

	private LuaClosure getClosure(String line) throws IOException {
		if (line == null) {
			System.out.println();
			System.exit(0);
			return null;
		}
		if (line.length() == 0) {
			return null;
		}
		if (line.charAt(0) == '=') {
			line = "return " + line.substring(1);
		}
		try {
			return LuaCompiler
					.loadstring(line, "stdin", state.getEnvironment());
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return null;
		} catch (RuntimeException e) {
			if (e.getMessage().contains("<eof>")) {
				System.out.print(">> ");
				System.out.flush();
				return getClosure(line + "\n" + in.readLine());
			}
			System.out.println(e.getMessage());
			System.out.flush();
			return null;
		}
	}

	public byte[] encodeKey(byte[] key) {
		byte[] temp = new SHA1Simple().calculateHash(key);

		int keylen = dhtControl.getTransport().getLocalContact().getID().length;
		byte[] result = new byte[keylen];

		System.arraycopy(temp, 0, result, 0, keylen);

		return (result);
	}

	public static void main(String[] args) throws Exception {
		ActivePeer peer = new ActivePeer(1234, "dht.aelitis.com:6881");
		// ActivePeer peer = new ActivePeer(5432,
		// "nethack.cs.washington.edu:5432");
		peer.init("granville.cs.washington.edu");
		Thread.sleep(5000);
		new Interpreter(new LuaState(), System.in, peer.dht.getControl()).run();
		peer.stop();
	}
}
