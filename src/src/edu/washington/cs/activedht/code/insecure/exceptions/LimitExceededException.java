package edu.washington.cs.activedht.code.insecure.exceptions;

import java.io.IOException;

public class LimitExceededException extends IOException {
	private static final long serialVersionUID = -2494661274146883277L;
	public LimitExceededException(String msg) { super(msg); }
}
