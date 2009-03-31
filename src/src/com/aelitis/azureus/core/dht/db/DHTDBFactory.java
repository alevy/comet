/*
 * Created on 28-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
/**
 * @author parg
 * @author roxana
 */

package com.aelitis.azureus.core.dht.db;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;

public class DHTDBFactory {
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
		FactoryInterface _factory )
	{
		synchronized(lock) {
			if (! was_initialized) {
				factory = _factory;
				was_initialized = true;
			}  // else, nothing to do (don't initialize repeatedly).
		}
	}
	
	public static DHTDB
	create(
		DHTStorageAdapter	adapter,
		int					original_republish_interval,
		int					cache_republish_interval,
		DHTLogger			logger )
	{
		return factory.create(
				adapter,
				original_republish_interval,
				cache_republish_interval,
				logger );
	}
	
	public static interface FactoryInterface {
		public DHTDB
		create(
			DHTStorageAdapter	adapter,
			int					original_republish_interval,
			int					cache_republish_interval,
			DHTLogger			logger );
	}
	
	// Default factory class:
	
	private static class DefaultFactoryAdapter implements FactoryInterface {
		@Override
		public DHTDB
		create(
			DHTStorageAdapter	adapter,
			int					original_republish_interval,
			int					cache_republish_interval,
			DHTLogger			logger )
		{
			return ( new DHTDBImpl( 
					adapter,
					original_republish_interval, 
					cache_republish_interval, 
					logger ));
		}
	}
}

