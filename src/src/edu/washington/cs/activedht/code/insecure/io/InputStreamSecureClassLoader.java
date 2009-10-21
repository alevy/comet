package edu.washington.cs.activedht.code.insecure.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;


/**
 * Secure ClassLoader that reads the class from an InputStream.
 * This loader functions exactly as URLClassLoader functions, except
 * it reads from an InputStream, as opposed to opening a connection
 * to a host.
 * 
 * The same security properties exist.
 * 
 * Limitations: It can only be used to load classes, not jars.
 * 
 * @author roxana
 */
public class InputStreamSecureClassLoader extends URLClassLoader {
	public static final String VUZE_CLASSLOAD_PROTOCOL = "vuze";

    /* The input from where we get objects. */
	InputStreamURLStreamHandler handler = null;

    /**
     * Constructs a new InputStreamSecureClassLoader for the given URL and
     * input stream.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's <code>checkCreateClassLoader</code> method
     * to ensure creation of a class loader is allowed.
     * 
     * @param url    the URL from which to load classes and resources
     * @param parent the parent class loader for delegation
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkCreateClassLoader</code> method doesn't allow 
     *             creation of a class loader.
     * @see SecurityManager#checkCreateClassLoader
     */
    protected InputStreamSecureClassLoader(URL url,
    		InputStreamURLStreamHandler handler, ClassLoader parent) {
    	super(new URL[] { url }, parent);

		// This is to make the stack depth consistent with 1.1.
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkCreateClassLoader();
		}
		this.handler = handler;
    }
    
    public final synchronized void init(InputStream input) throws IOException {
    	// assert(getURLs().length == 1);  // due to constructor and addURL.
    	handler.initConnectionInputStream(this.getURLs()[0], input);
    }
    
    /**
     * Loads a class using the URLClassLoader code, but before that it
     * checks whether the caller has the right to access the loaded package.
     */
    @SuppressWarnings("unchecked")
	@Override
    public final synchronized Class loadClass(String name, boolean resolve)
	throws ClassNotFoundException {
    	// First check if we have permission to access the package. This
    	// should go away once we've added support for exported packages.
    	SecurityManager sm = System.getSecurityManager();
    	if (sm != null) {
    		int i = name.lastIndexOf('.');
    		if (i != -1) {
    			sm.checkPackageAccess(name.substring(0, i));
    		}
    	}
    	return super.loadClass(name, resolve);
    }
    
    /**
     * Returns an empty set of permissions.
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
    	return new Permissions();  // no permissions.
    }

    /**
     * Does nothing. We only support one URL for this type of class loader.
     */
    @Override
    protected final void addURL(URL url) { }

    /**
     * Creates a new instance of URLClassLoader for the specified
     * URLs and default parent class loader. If a security manager is
     * installed, the <code>loadClass</code> method of the URLClassLoader
     * returned by this method will invoke the
     * <code>SecurityManager.checkPackageAccess</code> before
     * loading the class.
     *
     * @param url the URL to search for classes and resources
     * @return the resulting class loader
     * @throws MalformedURLException 
     * 
     * TODO(roxana): Figure out what's the commented stuff and whether
     * there's any vulnerability by not having it.
     */
    public static InputStreamSecureClassLoader newInstance(final String host,
    		final int port, final ClassLoader parent)
    throws MalformedURLException {
    	// Save the caller's context.
    	//AccessControlContext acc = AccessController.getContext();
    	// Need a privileged block to create the class loader.
    	//InputStreamSecureClassLoader ucl = (InputStreamSecureClassLoader)
	    //	AccessController.doPrivileged(new PrivilegedAction() {
	    //		public Object run() {
	    //			return new FactoryInputStreamSecureClassLoader(url,
	    //					input, parent);
	    //		}
	    //	});
    	
    	// Now set the context on the loader using the one we saved,
    	// not the one inside the privileged block...
    	//ucl.acc = acc;
    	//return ucl;
    	
    	InputStreamURLStreamHandler handler =
    		new InputStreamURLStreamHandler();
    	
    	return new InputStreamSecureClassLoader(
    		new URL(VUZE_CLASSLOAD_PROTOCOL, host, port, "/", handler),
    		handler, parent);
    }
    
    public static InputStreamSecureClassLoader newInstance(final String host,
    		final int port) throws MalformedURLException {
    	return newInstance(host, port, ClassLoader.getSystemClassLoader());
    }
}
