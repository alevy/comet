/**
 * 
 */
package se.krka.kahlua.vm.serialize;

public class Type {
	public static final byte DONE = 0xf;
	
	public static final byte STRING = 0x1;
	public static final byte DOUBLE = 0x2;
	public static final byte TABLE = 0x3;
	public static final byte BOOLEAN = 0x4;
	public static final byte PROTOTYPE = 0x5;
}