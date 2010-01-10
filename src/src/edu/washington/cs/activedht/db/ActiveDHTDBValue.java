package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.db.DHTDBValue;

/**
 * @author levya
 *
 */
public interface ActiveDHTDBValue extends DHTDBValue {

	public ActiveDHTDBValue executeCallback(String string);

}
