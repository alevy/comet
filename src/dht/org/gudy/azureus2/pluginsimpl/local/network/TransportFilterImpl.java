package org.gudy.azureus2.pluginsimpl.local.network;

import org.gudy.azureus2.plugins.network.TransportFilter;

import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;

public class TransportFilterImpl implements TransportFilter {
	public TransportHelperFilter filter;
	public TransportFilterImpl(TransportHelperFilter filter) {
		this.filter = filter;
	}
}
