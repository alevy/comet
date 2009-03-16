package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.io.Serializable;

import edu.washington.cs.activedht.code.insecure.exceptions.InvalidActionException;

/**
 * All subclasses of the DHTAction must be entered for runtime checks
 * on their 
 * @author roxana
 */
public abstract class DHTAction implements Serializable {
	private static final String TRUSTED_DHTACTION_IMPL_PACKAGE_NAME =
		DHTAction.class.getPackage().getName();
	
	private boolean was_executed = false;
	
	public final boolean actionWasExecuted() { return was_executed; }
	
	// Not final, as it's being used in testing.
	// TODO(roxana): Should probably make it final and change testing.
	public void markAsExecuted() { this.was_executed = true; }
	
	public final void resetExecution() { this.was_executed = false; }
	
	/** 
	 * Validate the type of this DHTAction. This is to prevent the user from
	 * inserting his own malicious action type.
	 * 
	 * @param action  the action to verify.
	 * @throws InvalidActionException thrown if the action is invalid.
	 */
	public static final void validateActionType(DHTAction action)
	throws InvalidActionException {
		if (! action.getClass().getPackage().getName().equals(
				TRUSTED_DHTACTION_IMPL_PACKAGE_NAME)) {
			throw new InvalidActionException("Invalid action " +
				action.getClass().getName() + "(" +
				action.getClass().getPackage().getName() + ")");
		}
	}
	
	public abstract boolean equals(Object o);
	public abstract int hashCode();
}
