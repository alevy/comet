package com.aelitis.azureus.core.dht.db.impl;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class DHTDBValueFactory {
	private static FactoryInterface factory = new DefaultFactoryAdapter();
	private static boolean was_initialized = false;
	private static Object lock = new Object();
	
	/**
	 * Sets up this factory to use the given factory object as the underlying
	 * factory for producing DHTDBValueImpl's.
	 * @param _factory
	 */
	public static void
	init(
		FactoryInterface _factory)
	{
		synchronized(lock) {
			if (! was_initialized) {
				factory = _factory;
				was_initialized = true;
			}  // else, nothing to do (don't initialize repeatedly).
		}
	}
	
	public static DHTDBValueImpl
	create(
		long _creation_time,
		byte[] _value,
		int	_version,
		DHTTransportContact _originator,
		DHTTransportContact _sender,
		boolean _local,
		int	_flags )
	{
		return factory.create(
				_creation_time, _value, _version,
				_originator,
				_sender,
				_local,
				_flags );
	}
	
	public static DHTDBValueImpl
	create(
		DHTTransportContact	_sender,
		DHTTransportValue _other,
		boolean _local)
	{
		return factory.create(_sender, _other, _local);
	}
	
	public static interface FactoryInterface {
		public DHTDBValueImpl
		create(
			long _creation_time,
			byte[] _value,
			int	_version,
			DHTTransportContact _originator,
			DHTTransportContact _sender,
			boolean _local,
			int	_flags );
		
		public DHTDBValueImpl
		create(
			DHTTransportContact	_sender,
			DHTTransportValue _other,
			boolean _local );
	}
	
	// Default factory class:
	
	private static class DefaultFactoryAdapter implements FactoryInterface {
		public DHTDBValueImpl
		create(
			long _creation_time, byte[] _value,
			int _version,
			DHTTransportContact _originator,
			DHTTransportContact _sender,
			boolean _local,
			int _flags )
		{
			return new DHTDBValueImpl(
					_creation_time,
					_value, _version,
					_originator, _sender,
					_local, _flags );
		}

		public DHTDBValueImpl
		create(
			DHTTransportContact _sender,
			DHTTransportValue _other,
			boolean _local)
		{
			return new DHTDBValueImpl(_sender, _other, _local);
		}
	}
}
