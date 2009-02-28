package edu.washington.cs.activedht.code;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Enumeration;

import sun.applet.AppletClassLoader;
import sun.awt.AppContext;
import sun.misc.Launcher;

import com.sun.org.apache.bcel.internal.util.ClassPath.ClassFile;

public class ClassObjectInputStream  extends ObjectInputStream {
	public ClassObjectInputStream() throws IOException, SecurityException {
		super();
	}
	
	public ClassObjectInputStream(InputStream input)
	throws IOException, SecurityException { super(input); }

	@Override
	protected Class resolveClass(ObjectStreamClass serialized_class)
	throws ClassNotFoundException, IOException {
		super.resolveClass(serialized_class);
		try {
			AppletClassLoader a.;
			a.loadClass("");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
