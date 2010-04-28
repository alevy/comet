/**
 * 
 */
package edu.washington.cs.activedht.expt.remote;

import se.krka.kahlua.vm.serialize.Serializer;

/**
 * @author levya
 *
 */
public class TrackerGet extends RemoteGet {

	public TrackerGet(String[] args) throws Exception {
		super(args);
	}

	@Override
	public byte[] getKey() {
		return contact.getID();
	}

	@Override
	public byte[] getPayload() {
		return null;
	}

	@Override
	public byte[] getReaderID() {
		return null;
	}

	@Override
	public void handleValue(byte[] value) {
		out.println(Serializer.serialize(value, null));
	}

}
