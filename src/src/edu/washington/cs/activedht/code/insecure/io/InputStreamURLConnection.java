package edu.washington.cs.activedht.code.insecure.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * URLConnection object that actually reads from an InputStream.
 * This class is used by the InputStreamSecureClassLoader to load a class
 * from an InputStream securely.
 * 
 * @author roxana
 */
public class InputStreamURLConnection extends URLConnection {
	private InputStream input = null;
	private boolean was_initialized = false;
	
	public InputStreamURLConnection(URL url) { super(url); }
	
	public void init(InputStream input) {
		assert(!isInitialized());
		this.input = input;
		markAsInitialized();
	}
	
	@Override
	public void connect() throws IOException {
		assert(isInitialized());
		// Nothing to do.
		setAllowUserInteraction(false);
		setDoOutput(false);
		setDoInput(true);
		setUseCaches(false);
	}
	
	private boolean isInitialized() { return was_initialized; }
	private void markAsInitialized() { this.was_initialized = true; }

	@Override
	public InputStream getInputStream() throws IOException {
		assert(isInitialized());
		return input;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}
	
	@Override
	public void setAllowUserInteraction(boolean allowUserInteraction) {
		// Nothing.
	}
	
	@Override
	public void setDoOutput(boolean doOutput) {
		// Nothing.
	}
	
	@Override
	public void setDoInput(boolean doInput) {
		// Nothing.
	}
	
	@Override
	public void setUseCaches(boolean useCashes) {
		// Nothing.
	}
	
	public void setConnectTimeout(int timeout) {
			// Nothing to do.
	}

	public int getConnectTimeout() {
		return 0;
    }

	@Override
	public void setReadTimeout(int timeout) {
		// Nothing to do.
	}

	@Override
	public int getReadTimeout() {
		return 0;
	}

	@Override
	public int getContentLength() {
		assert(isInitialized());
		try {
			return input.available();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public String getHeaderField(String name) {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String,List<String>> getHeaderFields() {
	    return Collections.EMPTY_MAP;
	}

	@Override
	public int getHeaderFieldInt(String name, int Default) {
		String value = getHeaderField(name);
		if (value == null) return Default;
	 	try { return Integer.parseInt(value); }
	 	catch (Exception e) { }
	   	return Default;
	}

	@Override
	public long getHeaderFieldDate(String name, long Default) {
		return 0;
	}

	@Override
	public String getHeaderFieldKey(int n) {
		return null;
	}

	@Override
	public String getHeaderField(int n) {
		return null;
	}

	@Override
	public Object getContent() throws IOException {
		assert(isInitialized());
	    byte[] content = new byte[getContentLength()];
	    input.read(content);
	    return content;
	}

	@Override
	public Object getContent(Class[] classes) throws IOException {
		assert(isInitialized());
	    Object object = getContent();
	    if (object == null) return object;
	    for (Class c: classes) {
	    	if (object.getClass().getName().equals(c.getName()))
	    		return object;
	    }
	    return null;  // nothing matched.
	}

	@Override
	public void setIfModifiedSince(long ifmodifiedsince) {
		// Nothing to do.
    }

	@Override
	public void setDefaultUseCaches(boolean defaultusecaches) {
		setDefaultUseCaches(false);
	}

	@Override
	public void setRequestProperty(String key, String value) {
		// Nothing to do.
	}

	@Override
	public void addRequestProperty(String key, String value) {
		// Nothing to do.
	}

	@Override
	public String getRequestProperty(String key) {
	  	return null;  // nothing to do.
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String,List<String>> getRequestProperties() {
	    return Collections.EMPTY_MAP;  // nothing to do.
	}
}
