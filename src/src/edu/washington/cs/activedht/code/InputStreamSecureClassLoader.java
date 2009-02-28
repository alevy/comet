package edu.washington.cs.activedht.code;

import java.io.InputStream;
import java.security.SecureClassLoader;

public class InputStreamSecureClassLoader extends SecureClassLoader {
	InputStream input;
	public InputStreamSecureClassLoader(InputStream input) {
		this.input = input;
	}
	
	
}
