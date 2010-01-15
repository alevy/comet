package edu.washington.cs.activedht.db;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.DHTDBValue;


/**
 * @author levya
 *
 */
public interface ActiveDHTDBValue extends DHTDBValue {

	public ActiveDHTDBValue executeCallback(String string, DhtWrapper dhtWrapper, Object...args);
	
	public DhtWrapper getDhtWrapper(DHTControl control, HashWrapper key);
	
	public byte[] serialize(Object object);
	
	public Object deserialize(byte[] value);

}
