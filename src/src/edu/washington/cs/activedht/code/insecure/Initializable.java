package edu.washington.cs.activedht.code.insecure;

import edu.washington.cs.activedht.code.insecure.exceptions.InitializationException;

public interface Initializable {
	public void init() throws InitializationException;
	
	public boolean isInitialized();
	
	public void stop();
}
