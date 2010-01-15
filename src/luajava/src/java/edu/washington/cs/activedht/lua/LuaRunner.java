/**
 * 
 */
package edu.washington.cs.activedht.lua;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

/**
 * @author levya
 *
 */
public class LuaRunner {

	public static void main(String[] args) throws Exception {
		LuaState luaState = LuaStateFactory.newLuaState();
				
		luaState.LdoString("result = {hello = function() return 1234 end, foo = \"bar\"}");
		LuaObject result = luaState.getLuaObject("result");
		System.out.println(result);
		Serializer serializer = new Serializer(luaState);
		byte[] bytes = serializer.serialize(result);
		
		LuaObject resultObject = serializer.deserialize(bytes);
		
		
		System.out.println(resultObject.getField("hello").call(new Object[]{}));
	}
	
}
