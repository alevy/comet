/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.List;

import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * 
 * @author levya
 *
 */
public interface GetCallback {
	void call(List<DHTTransportValue> values);
}