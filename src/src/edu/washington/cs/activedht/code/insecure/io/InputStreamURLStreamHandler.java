package edu.washington.cs.activedht.code.insecure.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;


class InputStreamURLStreamHandler extends URLStreamHandler {
	Map<String, InputStreamURLConnection> urls_to_connections;
	
	protected InputStreamURLStreamHandler() {
		urls_to_connections = new HashMap<String, InputStreamURLConnection>();
	}
	
	protected URLConnection openConnection(URL u)
	throws IOException {
		String key = getKeyFromURL(u);
		return urls_to_connections.get(key);
	}
	
	protected void initConnectionInputStream(URL u, InputStream is) {
		String key = getKeyFromURL(u);
		
		// Get the connection object from the local map. 
		InputStreamURLConnection conn = urls_to_connections.get(u);
		if (conn == null) {
			// No such connection, so add it.
			conn = new InputStreamURLConnection(u);
			urls_to_connections.put(key, conn);
		}

		// Initialize the connection. Hopefully, this was the first time...
		conn.init(is);
	}
	
	private static String getKeyFromURL(URL u) {
		return u.getHost();  // should I also put the port?
	}
}
