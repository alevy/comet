package edu.washington.cs.activedht.db;

import java.util.List;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.sandbox.ActiveCodeSandboxImpl;
import edu.washington.cs.activedht.db.coderunner.ActiveCodeRunner;
import edu.washington.cs.activedht.db.dhtactionexecutor.DHTActionExecutorImpl;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTActionFactoryImpl;
import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.activedht.util.Pair;

/**
 * TODO(roxana): Need to figure out how to transmit ActiveDHTDBValues remotely.
 * I currently include their preactions map inside the binary. Shoulnd't do
 * that, as replication gets really confused.
 * 
 * Another problem: synchronization of setValue().
 * 
 * @author roxana
 *
 */
public class ActiveDHTDBImpl extends DHTDBImpl
implements ActiveDHTDB, Constants {
	// Initialize the system for ActiveDHTs.
	// TODO(roxana): This is pretty hacky now. Should have an init() function
	// that gets called, even if that means that we need to modify Vuze.
	static {
		ActiveDHTInitializer.prepareRuntimeForActiveCode();
	}
	
	private boolean should_init_underlying_db;
	
	/** Pointer to the active storage handler. */
	private ActiveDHTStorageAdapter active_storage_adapter_ptr;
	
	/** Pointer to the handler of active code. */
	private final ActiveCodeRunner active_code_handler;

	public ActiveDHTDBImpl(ActiveDHTStorageAdapter _adapter,
	                       int _original_republish_interval, 
	                       int _cache_republish_interval,
	                       DHTLogger _logger) {
		this(_adapter, _original_republish_interval,
			 _cache_republish_interval,
			 _logger,
			 false);
	}
	
	protected ActiveDHTDBImpl(ActiveDHTStorageAdapter _adapter,
                              int _original_republish_interval, 
                              int _cache_republish_interval,
                              DHTLogger _logger,
                              boolean should_init_underlying_db) {
		super(_adapter, _original_republish_interval,
	          _cache_republish_interval,
	          _logger,
	          should_init_underlying_db);
		
		this.should_init_underlying_db = should_init_underlying_db;
		
		this.active_storage_adapter_ptr = _adapter;
		
		ActiveCodeRunner.ActiveCodeRunnerParam param =
			new ActiveCodeRunner.ActiveCodeRunnerParam();

		active_code_handler = new ActiveCodeRunner(
				new ActiveCodeSandboxImpl<byte[]>(
						param.active_code_execution_timeout),
				new DHTActionExecutorImpl(this,
						new ExecutableDHTActionFactoryImpl()),
				param);
	}
	
	// Override DHTDBImpl methods:
	
	@Override
	protected void init() {
		// Initialize the underlying DB implementation if appropriate.
		if (should_init_underlying_db) super.init();
		
		// Initialize the ActiveDHTDB-specific part.
		SimpleTimer.addPeriodicEvent("DHTDB:ac",
				ACTIVE_CODE_PERIODIC_TIMER_INTERVAL,
				new TimerEventPerformer() {
					@Override
					public void perform(TimerEvent event) {
						fireMaintenanceTimerForActiveCode();		
					}
				});
	}
	
	// DHTDB interface:

	/**
	 * Looks up a key in the DB and returns a set of values associated with
	 * that key. In Vuze, each value is associated with a different originator.
	 * Each value in the result set is then announced that it is being read.
	 * Upon this, it can decide whether to request exclusion from the returned
	 * set or not.
	 * This is typically done by preparing and returning a AbortDHTPostaction,
	 * though other solutions may be possible, depending on the available range
	 * of DHTActions.
	 */
	@Override
	public DHTDBLookupResult get(final DHTTransportContact reader,
			                     final HashWrapper key,
			                     int max_values,
			                     byte flags,
			                     boolean external_request) {
		DHTDBLookupResult result = super.get(reader, key, max_values, flags,
				                             external_request);
		if (result == null || ! external_request) return result;  // nothing.

		// Announce all of the values that they're being read.
		final DHTDBValue[] all_values = result.getValues();
		final DHTDBValue[] values_allowing_access =
			active_code_handler.onGet(reader, key, all_values);
		
		// If not all values have decided to remain, create a result with
		// only those that have not denied access.
		if (values_allowing_access != null) {
			final byte div_type = result.getDiversificationType();
			result = new DHTDBLookupResult() {
				public DHTDBValue[] getValues() {
					return values_allowing_access;
				}
				public byte getDiversificationType() { return div_type; }
			};
		}

		return result;
	}

	/**
	 * Removes a value from the database and then announces it of the fact that
	 * it's being evicted. The value has the chance of re-inserting itself
	 * into the DB, if it so chooses.
	 * 
	 * The re-insert, however, changes the value somewhat: the value looks
	 * like a brand new value (e.g., its timestamp is reset).
	 */
	@Override
	public DHTDBValue remove(DHTTransportContact sender, HashWrapper key) {
		// Remove the object.
		DHTDBValue removed_value = super.remove(sender, key);
		if (removed_value == null) return removed_value;  // nothing to do.
		
		// Announce the object of its eviction and re-add if it so requests.
		DHTDBValue value_to_add_back = active_code_handler.onRemove(sender,
				key, removed_value);

		// If the value wants back in the DB, add it back.
		if (value_to_add_back != null) {
			// Call the super's store function, s.t we avoid cycles.
			super.store(value_to_add_back.getOriginator(),
					    key,
				        new DHTDBValue[] { value_to_add_back });
		}

		return removed_value;  // TODO(roxana): how to indicate refusal? 
	}
	
	/**
	 * Active-DB stores behave similarly to the other active-DB operations.
	 * They store the values in the DB, and then announce the overwritten and
	 * newly-added values of the action that's being done on them.
	 * <ul>
	 * <li><i>Overwritten values</i> are announced of the "put" action. They
	 *     have a chance to defend themselves and request the DB to undo the
	 *     overwriting. A request to add an overwritten value back into the DB
	 *     results in the immediate dropping of the value that attempted to
	 *     overwrite it.
	 *
	 * <li><i>Newly-added values</i> are announced of the "initialPut" action.
	 *     They have a chance to request that they not be added to the DB.
	 *     In that case, the DB will undo the changes. If an overwritten value
	 *     requests that it goes back to the DB, then the value that attempted
	 *     its overwriting will not be announced of anything.
	 * </ul>
	 *
	 * <h4>Implementation considerations:</h4>
	 * Currently, we first store all new values in the DB and then announce
	 * their overwritten values and the values themselves of the action being
	 * taken. If an overwritten value wants back into the DB, then we add
	 * it back immediately. This means that we had previously replaced it
	 * for nothing. This approach works OK when values don't have a merging
	 * semantic, but has poor performance and awkward semantic for
	 * applications where values incorporate updates into themselves.
	 * 
	 * <br>
	 * TODO(roxana): 
	 * Depending on how important is merging in applications, we may want to
	 * implement stores differently. For example, we could implement the
	 * merging by announcing a value of its being overwritten upon overwriting
	 * time. However, such an implementation, while better, requires
	 * restructuring of DHTDBImpl.store(), which we postpone a till later time.
	 * 
	 * <br>
	 * Note that a similar consideration applies for deletes, as well, where
	 * we first remove the value, then announce it, and then add it back, if
	 * it so asks.
	 *
	 * <br><br>
	 * NOTE: This DHTDB implementation doesn't store null-values. If a null
	 * value is found among the parameter values, then none of the values is
	 * stored.
	 * TODO(roxana): This seems similar to Vuze's DHTDBImpl semantic. Is it?
	 *
	 */
	@Override
	public byte store(DHTTransportContact sender, HashWrapper key,
	                  DHTTransportValue[] values) {
		StoreListener store_listener = doPreStore(key);
		if (store_listener == null) return -1;  // nothing to do -- what ret?
		
		// Perform the store of all the values; StoreOutcome's will accumulate
		// in the store listener above.
		byte ret = super.store(sender, key, values);
		
		doPostStore(sender, key, store_listener);
		
		return ret;  // TODO(roxana): not clear what I should return...
	}
	
	@Override
	public DHTDBValue store(HashWrapper key, byte[] value, byte flags) {
		StoreListener store_listener = doPreStore(key);
		if (store_listener == null) return null;  // nothing to do
		
		DHTDBValue stored_value = super.store(key, value, flags);
		
		doPostStore(getLocalContact(), key, store_listener);
		
		if (stored_value instanceof ActiveDHTDBValueImpl) {
			((ActiveDHTDBValueImpl)stored_value).prepareForRemote();
		}
		
		return stored_value;
	}
	
	private StoreListener doPreStore(HashWrapper key) {
		// Register the store listener.
		StoreListener store_listener = new StoreListener();
		active_storage_adapter_ptr.registerStoreListener(key, store_listener);
		return store_listener;
	}
	
	private void doPostStore(DHTTransportContact sender, HashWrapper key,
                             StoreListener store_listener) {
		// Unregister the listener.
		active_storage_adapter_ptr.unregisterStoreListener(key);
		
		// Announce all newly-added and replaced values of the action taken
		// upon them.
		Pair<List<DHTTransportValue>, List<DHTTransportValue>> p =
			active_code_handler.onStore(sender, key, store_listener);
		
		// Add back the ones that want in and remote the ones that want out.
		if (p != null) {
			List<DHTTransportValue> values_to_add_back = p.getFirst();
			// Add the values that want to go back.
			if (values_to_add_back != null) {
				for (DHTTransportValue value: values_to_add_back) {
					// Call the super's store function, s.t we avoid cycles.
					super.store(value.getOriginator(), key,
							    new DHTDBValue[] { (DHTDBValue)value });
				}
			}
			
			// Remove the values that refuse to be stored.
			List<DHTTransportValue> values_to_remove = p.getSecond();
			if (values_to_remove != null) {
				for (DHTTransportValue value: values_to_remove) {
					// Call the super's store function, s.t we avoid cycles.
					super.remove(value.getOriginator(), key);
				}
			}
		}
	}
	
	// Timer handler:
	
	protected void fireMaintenanceTimerForActiveCode() {
		
	}

	// ActiveDHTDB interface:
	
	@Override
	public DHTDBValue superGet(HashWrapper key) { return super.get(key); }

	@Override
	public DHTDBLookupResult superGet(DHTTransportContact reader,
			                          HashWrapper key,
			                          int max_values,
			                          byte flags,
			                          boolean external_request) {
		return super.get(reader, key, max_values, flags, external_request);
	}

	@Override
	public DHTDBValue superRemove(DHTTransportContact sender,
			                      HashWrapper key) {
		return super.remove(sender, key);
	}

	@Override
	public DHTDBValue superStore(HashWrapper key, byte[] value, byte flags) {
		return super.store(key, value, flags);
	}

	@Override
	public byte superStore(DHTTransportContact sender, HashWrapper key,
			               DHTTransportValue[] values) {
		return super.store(sender, key, values);
	}
}


