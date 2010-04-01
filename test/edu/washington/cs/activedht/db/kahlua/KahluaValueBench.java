package edu.washington.cs.activedht.db.kahlua;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import se.krka.kahlua.vm.serialize.Serializer;
import junit.framework.TestCase;

public class KahluaValueBench extends TestCase {

	public void testInitialaztionSpeed() throws Exception {
		byte[] bytes = Serializer.serialize("Hello World", null);
		int iters = 10000;
		ActiveDHTDBValue[] values = new ActiveDHTDBValue[iters];
		for (int i = 0; i < iters; ++i) {
			values[i] = new KahluaActiveDHTDBValue(0L,
					bytes, 1, null, false, 0);
			values[i].executeCallback("mrp");
		}
		int i = 0;
		long currentTime = System.currentTimeMillis();
		while (i < iters) {
			values[i].executeCallback("onGet");
			++i;
		}
		System.out.println(1.0 * (System.currentTimeMillis() - currentTime) / iters / 0.0022 );
	}
}
