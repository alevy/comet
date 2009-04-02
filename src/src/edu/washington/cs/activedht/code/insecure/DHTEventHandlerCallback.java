package edu.washington.cs.activedht.code.insecure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;

import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.exceptions.NotAnActiveObjectException;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectInputStream;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;

/**
 * DHTEvent handler.
 *
 * This class is DANGEROUS. It contains functions that access ActiveCode,
 * which must only be called from within the sandbox. The only operation
 * that this class allows from outside the sandbox is instantiation.
 * 
 * See the documentation for each method, in order to identify where each
 * method needs to run.
 *
 * @author roxana
 */

public abstract class DHTEventHandlerCallback {
	private InputStreamSecureClassLoader class_loader;
	
	// Trusted methods:
	
	/**
	 * Constructor.
	 * 
	 * NOTE: This function is accessed from outside the sandbox, so it MUST NOT
	 * do anything dangerous (e.g., instantiate any ActiveCode objects, or
	 * access them).
	 */
	public DHTEventHandlerCallback() { }
	
	
	/**
	 * NOTE: This function is accessed from outside the sandbox, so it MUST NOT
	 * do anything dangerous (e.g., instantiate any ActiveCode objects, or
	 * access them).
	 * @param class_loader
	 */
	public final void init(InputStreamSecureClassLoader class_loader) {
		this.class_loader = class_loader;
	}
	
	/**
	 * Returns the imposed preactions map, or null if none is imposed.
	 * Some event handlers (e.g., InitialPutCb) impose a preactions map, as
	 * the value that they will be unpacking is a remote value. The preactions
	 * map of such remote values must not be trusted, as we can't guarantee
	 * at this time that the serialization is currect. If we controlled more
	 * of the serialization process, then we could eliminate this somewhat
	 * confusing function and its uses.
	 * 
	 * NOTE: This function is accessed from outside the sandbox, so it MUST NOT
	 * do anything dangerous (e.g., instantiate any ActiveCode objects, or
	 * access them).
	 *
	 */
	public abstract DHTActionMap getImposedPreactionsMap();
	
	/**
	 * Returns the event to which this handler is associated.
	 * 
	 * NOTE: This function is accessed from outside the sandbox, so it MUST NOT
	 * do anything dangerous (e.g., instantiate any ActiveCode objects, or
	 * access them).
	 * 
	 * @return  the event with which this handler is associated.
	 */
	public abstract DHTEvent getEvent();
	
	//
	// DANGEROUS methods:
	//
	// These methods access the client-provided ActiveCode objects and
	// must therefore be run in the sandbox.
	//

	/**
	 * Executes the event on the active object given by the active_value
	 * parameter.
	 * This method is DANGEROUS, so it *MUST* be run from within the sandbox.
	 * 
	 * This function assumes that it has been previously initialized.
	 * 
	 * @param active_value        the bytes representing the value
	 * @param executed_preactions preactions with results.
	 * @param postactions         postactions.
	 * @return the serialization of the object to save back. It returns null
	 *         if nothing needs to be saved back locally.
	 */
	public final byte[] executeEventOnActiveObject(
			byte[] value_bytes,
			DHTActionList executed_preactions,
			DHTActionList postactions)
	throws ClassNotFoundException, IOException {
		// Instantiate the ActiveObject from the InputStream, if any.
		ActiveCode active_code = null;
		try {
			active_code = instantiateActiveObject(value_bytes);
		} catch (NotAnActiveObjectException e) {
			// It's a plain value, so nothing to do.
			// TODO(roxana): For performance, avoid calling this for
			// non-active values.
			return null;
		}
		if (active_code == null) return null;  // nothing to do
		
		// Execute the event on the object.
		this.executeEventOnActiveObject(active_code, executed_preactions,
					postactions);
		
		// Serialize the object back and return it.
		byte[] object_to_save_back = null;
		try { object_to_save_back = serializeActiveObject(active_code); }
		catch (IOException e) { e.printStackTrace(); }
		
		return object_to_save_back;
	}
	
	/**
	 * Executes the event on the given ActiveObject.
	 * This method is DANGEROUS, so it *MUST* be run from within the sandbox.
	 * 
	 * @param active_code         the bytes representing the value
	 * @param executed_preactions preactions with results.
	 * @param postactions         postactions.
	 * @return true if the active object needs to be re-serialized to disk;
	 *         or false if the active object shouldn't be re-serialized.
	 */	
	protected abstract void executeEventOnActiveObject(
			ActiveCode active_code,
			DHTActionList executed_preactions,
			DHTActionList postactions);
	
	private final boolean isInitialized() { return class_loader != null; }
		
	/**
	 * Instantiates the active object from its bytes.
	 * This method is DANGEROUS, so it *MUST* be run from within the sandbox.
	 * 
	 * @param  value_bytes   bytes containing the serialization of the
	 *                       ActiveCode object.
	 * @throws NotAnActiveObjectException  if the value is not active, but
	 *                                     rather it's just a set of bytes.
	 * @throws ClassNotFoundException      if the class cannot be loaded from
	 *                                     the byte buffer or from the allowed
	 *                                     classpath.
	 */
	protected final ActiveCode instantiateActiveObject(
			byte[] value_bytes)
	throws NotAnActiveObjectException, IOException {
		assert(isInitialized());

		ByteArrayInputStream bais = new ByteArrayInputStream(value_bytes);
		ClassObjectInputStream cois = new ClassObjectInputStream(bais,
				                                                 class_loader);
		
		ActiveCode deserialized_object = null;
		try {
			deserialized_object = (ActiveCode)cois.readObject();
		} catch(StreamCorruptedException e) {
			throw new NotAnActiveObjectException("Value is not active.");
		} catch(ClassCastException e) {
			throw new NotAnActiveObjectException("Invalid object type.");
		} catch(ClassNotFoundException e) {
			throw new NotAnActiveObjectException("Class not found.");
		} catch(OptionalDataException e) {
			throw new NotAnActiveObjectException("Invalid object type.");
		} catch(InvalidClassException e) {
			throw new NotAnActiveObjectException("Invalid object type.");
		}
		
		cois.close();
		
		return deserialized_object;
	}
	
	/**
	 * Serializes the active object.
	 * This method is DANGEROUS, so it *MUST* be run from within the sandbox.
	 */
	private final byte[] serializeActiveObject(ActiveCode active_code)
	throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ClassObjectOutputStream coos = new ClassObjectOutputStream(baos);
		
		coos.writeObject(active_code);
		
		byte[] serialized_object = baos.toByteArray();
		
		baos.close();
		
		return serialized_object;
	}
	
	//
    // Helper DHTEventHandlerCallback classes:
	//
    // All functions in these classes *MUST* always be accessed from inside the
    // sandbox. However, the constructors of these classes can be accessed from
    // outside the sandbox.
	//

	public static abstract class AbstractCb extends DHTEventHandlerCallback {
		private String caller_ip;
		
		public AbstractCb(String caller_ip) { this.caller_ip = caller_ip; }
		
		public String getCallerIP() { return caller_ip; }
	}

	public static class ValueChangedCb extends AbstractCb {
		private byte[] new_value_bytes;

		// Called from outside the sandbox.
		public ValueChangedCb(String caller_ip, byte[] new_value_bytes) {
			super(caller_ip);
			this.new_value_bytes = new_value_bytes;
		}

	    // Called from within the sandbox.
		@Override
		public void executeEventOnActiveObject(
				ActiveCode active_code,
				DHTActionList executed_preactions,
				DHTActionList postactions) {
			ActiveCode new_active_code = null;
			if (new_value_bytes != null) {
				try {
					new_active_code = this.instantiateActiveObject(
							new_value_bytes);
				} catch (Exception e) {
					// Some exception has occurred during decapsulation.
					// Ignore it and treat the value as a non-active value.
				}
			}
			
			// Execute onPut() on the given active object. 
			if (new_active_code != null) {
				active_code.onValueChanged(getCallerIP(),
						                   new_active_code,
						                   executed_preactions,
						                   postactions);
			} else {
				active_code.onValueChanged(getCallerIP(),
						                   new_value_bytes,
				           		           executed_preactions,
				           		           postactions);
			}
		}

		@Override
		public DHTEvent getEvent() { return DHTEvent.PUT; }

		@Override
		public DHTActionMap getImposedPreactionsMap() {
			return null;
		}
	}

	public static class ValueAddedCb extends AbstractCb {
		private String this_node_ip;
		DHTActionMap all_preactions;
		
		// Called from outside the sandbox.
		public ValueAddedCb(String this_node_ip, String caller_ip,
				            int max_action_list_size) {
			super(caller_ip);
			all_preactions = new DHTActionMap(max_action_list_size);
		}

	    // Called from within the sandbox.
		@Override
		public void executeEventOnActiveObject(
				ActiveCode active_code,
				DHTActionList executed_preactions,
				DHTActionList postactions) {
			active_code.onValueAdded(this_node_ip,
					                 getCallerIP(),
					                 all_preactions, 
					                 postactions);
		}
		
		@Override
		public DHTActionMap getImposedPreactionsMap() {
			return all_preactions;
		}

		@Override
		public DHTEvent getEvent() { return DHTEvent.PUT; }
	}

	public static class GetCb extends AbstractCb {
		// Called from outside the sandbox.
		public GetCb(String caller_ip) { super(caller_ip); }
		
	    // Called from within the sandbox.
		@Override
		protected void executeEventOnActiveObject(ActiveCode active_code,
				DHTActionList executed_preactions,
				DHTActionList postactions) {
			active_code.onGet(getCallerIP(), executed_preactions, postactions);		
		}

		@Override
		public DHTEvent getEvent() { return DHTEvent.GET; }

		@Override
		public DHTActionMap getImposedPreactionsMap() {
			return null;
		}
	}

	public static class DeleteCb extends AbstractCb {
		// Called from outside the sandbox.
		public DeleteCb(String caller_ip) { super(caller_ip); }
		
	    // Called from within the sandbox.
		@Override
		protected void executeEventOnActiveObject(ActiveCode active_code,
				DHTActionList executed_preactions,
				DHTActionList postactions) {
			active_code.onDelete(getCallerIP(), executed_preactions,
					             postactions);		
		}
		
		@Override
		public DHTEvent getEvent() { return DHTEvent.DELETE; }

		@Override
		public DHTActionMap getImposedPreactionsMap() {
			return null;
		}
	}

	public static class TimerCb extends DHTEventHandlerCallback {
		// Called from outside the sandbox.
		public TimerCb() { }
		
	    // Called from within the sandbox.
		@Override
		protected void executeEventOnActiveObject(ActiveCode active_code,
				DHTActionList executed_preactions,
				DHTActionList postactions) {
			active_code.onTimer(executed_preactions, postactions);
		}
		
		@Override
		public DHTEvent getEvent() { return DHTEvent.TIMER; }

		@Override
		public DHTActionMap getImposedPreactionsMap() {
			return null;
		}
	}
}
