package edu.washington.cs.activedht.db;

/**
 * Exception raised by a DHTAction if it wants to cancel all the subsequent
 * operations that follow it.
 * 
 * This exception can be thrown by a DHTPostAction to state that the operations
 * being executed on an active object must be aborted.
 * 
 * @author roxana
 *
 */
@SuppressWarnings("serial")
public class AbortDHTActionException extends Exception {
	public AbortDHTActionException() { }
	public AbortDHTActionException(String msg) { super(msg); }
}
