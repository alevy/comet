package edu.washington.cs.activedht.code.insecure.exceptions;

public class ActiveCodeExecutionInterruptedException extends Exception {
	private static final long serialVersionUID = 40475702810008949L;

	public ActiveCodeExecutionInterruptedException(String reason) {
		super(reason);
	}
}
