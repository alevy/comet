package data;

import java.io.FileOutputStream;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectInputStreamTest;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;

public class ClassObjectOutputStreamTestGenerator {
	private static void serialize() throws Exception {
		ActiveCode my_object = new TestDataActiveCode(
				ClassObjectInputStreamTest.OBJECT_VALUE);
		FileOutputStream os = new FileOutputStream(
			ClassObjectInputStreamTest.TEST_INSTANCE_FILENAME);
		ClassObjectOutputStream coos = null;
		coos = new ClassObjectOutputStream(os);

		coos.writeObject(my_object);

		os.close();
	}

	public static void main(String args[]) {
		try { serialize(); }
		catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
