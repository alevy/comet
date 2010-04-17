import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

import org.gudy.azureus2.core3.util.SHA1Simple;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import edu.washington.cs.activedht.expt.remote.StoreLua;

/**
 * 
 */

/**
 * @author levya
 * 
 */
public class PutWorkload {

	static DHTTransportContact contact;

	public static byte[] encodeKey(byte[] key) {
		byte[] temp = new SHA1Simple().calculateHash(key);

		int keylen = 20;
		byte[] result = new byte[keylen];

		System.arraycopy(temp, 0, result, 0, keylen);

		return (result);
	}

	public static void main(String[] args) throws Exception {
		InputStream input = System.in;
		int numObjects = 10;
		String bootstrapLoc = "granville.cs.washington.edu:9876";

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-f")) {
				input = new FileInputStream(args[++i]);
			} else if (args[i].equals("-n")) {
				numObjects = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-b")) { // bootstrap host:port
				bootstrapLoc = args[++i];
			}
		}

		Scanner scanner = new Scanner(input);
		StoreLua storeLua = new StoreLua(new String[] { bootstrapLoc });

		byte[][] values = new byte[numObjects][];
		for (int i = 0; i < numObjects; ++i) {
			String line = scanner.nextLine();
			int numBytes = (int) (Double.parseDouble(line.trim()) * 1024);
			String code = "obj = {value = " + numBytes + ", onStore = function(self) local str = \"\"; for i = 1,count do str = str .. \"a\" end return str end}";
			LuaMapTable env = new LuaMapTable();
			byte[] bytes = Serializer.serialize(LuaCompiler.loadstring(code, "stin", env), env);
			/*byte[] bytes = new byte[numBytes];
			for (int j = 0; j < numBytes; ++j) {
				bytes[j] = 'a';
			}*/
			values[i] = Serializer.serialize(new String(bytes), null);
		}
		for (int i = 0; i < numObjects; ++i) {
			storeLua.setKey(("" + i).getBytes());
			storeLua.setPayload(values[i]);
			System.err.println(values[i].length);
			if (!storeLua.run()) {
			}
			if (i % 1 == 0) {
				System.err.println("Finished putting " + i + " of "
						+ numObjects);
			}
		}
		System.err.println("Done.");

	}

}
