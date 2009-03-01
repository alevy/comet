package edu.washington.cs.activedht.code;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

class InputStreamURLStreamHandler extends URLStreamHandler {
	Map<URL, InputStreamURLConnection> urls_to_connections;
	
	protected InputStreamURLStreamHandler() {
		urls_to_connections = new HashMap<URL, InputStreamURLConnection>();
	}
	
	protected URLConnection openConnection(URL u)
	throws IOException {
		return urls_to_connections.get(u);
	}
	
	protected void initConnectionInputStream(URL u, InputStream is) {
		InputStreamURLConnection conn = urls_to_connections.get(u);
		if (conn == null) {
			// No such connection, so add it.
			conn = new InputStreamURLConnection(u);
			urls_to_connections.put(u, conn);
		}
		// Initialize the connection. Hopefully, this was the first time...
		conn.init(is);
	}
}
