package se.krka.kahlua.annotation;

import se.krka.kahlua.integration.annotations.LuaConstructor;

import se.krka.kahlua.integration.annotations.Desc;

import se.krka.kahlua.integration.annotations.LuaClass;
import se.krka.kahlua.integration.annotations.LuaMethod;

@LuaClass
@Desc("This is a base class description")
public class BaseAnnotationClass {

	@LuaConstructor(name="NewBase")
	@Desc("This is a base class constructor description")
	public BaseAnnotationClass() {
		
	}
	
	public int foo;
	public String bar;

	@LuaMethod
	public void baseDoStuff() {

	}

	@LuaMethod
	public void baseMethodWithArgs(int foo, String bar) {
		this.foo = foo;
		this.bar = bar;

	}
	
	
	@LuaMethod	
	@Desc("This is a base class method description")
	public String baseMethod2() {
		return "Base";
	}
}
