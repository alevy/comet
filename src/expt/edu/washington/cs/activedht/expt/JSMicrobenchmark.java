package edu.washington.cs.activedht.expt;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableOutputStream;

/**
 * @author levya
 * 
 */
public class JSMicrobenchmark extends Microbenchmark {

	private static final Context context = Context.enter();
	private static final ScriptableObject scope = context.initStandardObjects();
	static {
		context.setOptimizationLevel(-1);
	}

	public JSMicrobenchmark(ActivePeer peer, String js, int numCurRequests)
			throws Exception {
		super(peer, js, numCurRequests);
	}

	@Override
	public byte[] generateValue(String js) throws IOException {
		Object obj = context.evaluateString(scope, js, "", 1, null);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ScriptableOutputStream sos = new ScriptableOutputStream(os, scope);
		sos.excludeStandardObjectNames();
		sos.writeObject(obj);
		return os.toByteArray();
	}

	public static void main(String[] args) throws Exception {
		int numObjects = 100;
		PrintStream out = System.out;
		
		if (args.length > 0) {
			numObjects = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			out = new PrintStream(new FileOutputStream(args[1], true));
		}

		ActivePeer bootstrap = new ActivePeer(48386, "localhost:48386", false);
		ActivePeer peer = new ActivePeer(1234, "localhost:48386", false);
		JSMicrobenchmark microbenchmark = new JSMicrobenchmark(
				peer,
				"activeobject = {onGet: function(self) { return \"hello\" }}",
				numObjects);
		
		bootstrap.init("localhost");
		Thread.sleep(5000);
		peer.init("localhost");
		Thread.sleep(5000);

		Semaphore sema = new Semaphore(numObjects);
		microbenchmark.run(sema, 100, out);
		sema.acquire(numObjects);

		peer.stop();
		bootstrap.stop();
		out.close();
	}
}
