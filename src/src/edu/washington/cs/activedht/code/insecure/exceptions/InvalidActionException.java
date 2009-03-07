package edu.washington.cs.activedht.code.insecure.exceptions;

import java.io.IOException;

@SuppressWarnings("serial")
public class InvalidActionException extends IOException {
	public InvalidActionException(String msg) { super(msg); }
}
