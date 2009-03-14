package edu.washington.cs.activedht.code.insecure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
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
	 * Tells whether the value on top of which this DHTEventHandler will run
	 * is a local or remote value. A local value is taken from the local
	 * database, where it was inserted some time ago.
	 * A remote value is received from a remote note and the event is 
	 * (e.g., via a put). The initial put for a value is surely a remote value.
	 * 
	 * NOTE: This function is accessed from outside the sandbox, so it MUST NOT
	 * do anything dangerous (e.g., instantiate any ActiveCode objects, or
	 * access them).
	 * 
	 * @return  true if the value is local; false if it's remote.
	 */
	public abstract boolean isValueLocal();
	
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
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions)
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
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions);
		
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
	throws NotAnActiveObjectException, ClassNotFoundException, IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(value_bytes);
		ClassObjectInputStream cois = new ClassObjectInputStream(bais,
				class_loader);
		
		ActiveCode deserialized_object = null;
		try { deserialized_object = (ActiveCode)cois.readObject(); }
		catch(ClassCastException e) {
			throw new NotAnActiveObjectException("Value is not active.");
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
		
		public AbstractCb(String caller_ip) {
			this.caller_ip = caller_ip;
		}
		
		public String getCallerIP() { return caller_ip; }
	}

	public static class PutCb extends AbstractCb {
		private byte[] new_value_bytes;

		// Called from outside the sandbox.
		public PutCb(String caller_ip, byte[] new_value_bytes) {
			super(caller_ip);
			this.new_value_bytes = new_value_bytes;
		}

	    // Called from within the sandbox.
		@Override
		public void executeEventOnActiveObject(
				ActiveCode active_code,
				DHTActionList<DHTPreaction> executed_preactions,
				DHTActionList<DHTPostaction> postactions) {
			ActiveCode new_active_code = null;
			if (new_value_bytes != null) {
				try {
					new_active_code = this.instantiateActiveObject(
							new_value_bytes);
				} catch (Exception e) {
					// Some exception has occurred while decapsulating the
					// new value object.
					// Just treat the new value as non-active code.
				}
			}
			
			// Execute onPut() on the given active object. 
			if (new_active_code != null) {
				active_code.onPut(getCallerIP(), new_active_code,
						          executed_preactions, postactions);
			} else {
				active_code.onPut(getCallerIP(), new_value_bytes,
				           		  executed_preactions, postactions);
			}
		}

		@Override
		public boolean isValueLocal() { return false; }

		@Override
		public DHTEvent getEvent() { return DHTEvent.PUT; }
	}

	public static class InitialPutCb extends PutCb {
		DHTActionMap<DHTPreaction> all_preactions;
		
		// Called from outside the sandbox.
		public InitialPutCb(String caller_ip,
				DHTActionMap<DHTPreaction> all_preactions) {
			super(caller_ip, null);
			this.all_preactions = all_preactions;
		}

	    // Called from within the sandbox.
		@Override
		public void executeEventOnActiveObject(
				ActiveCode active_code,
				DHTActionList<DHTPreaction> executed_preactions,
				DHTActionList<DHTPostaction> postactions) {
			active_code.onInitialPut(getCallerIP(), all_preactions);
		}
	}

	public static class GetCb extends AbstractCb {
		// Called from outside the sandbox.
		public GetCb(String caller_ip) { super(caller_ip); }
		
	    // Called from within the sandbox.
		@Override
		protected void executeEventOnActiveObject(ActiveCode active_code,
				DHTActionList<DHTPreaction> executed_preactions,
				DHTActionList<DHTPostaction> postactions) {
			active_code.onGet(getCallerIP(), executed_preactions, postactions);		
		}

		@Override
		public boolean isValueLocal() { return true; }

		@Override
		public DHTEvent getEvent() { return DHTEvent.GET; }
	}

	public static class DeleteCb extends AbstractCb {
		// Called from outside the sandbox.
		public DeleteCb(String caller_ip) { super(caller_ip); }
		
	    // Called from within the sandbox.
		@Override
		protected void executeEventOnActiveObject(ActiveCode active_code,
				DHTActionList<DHTPreaction> executed_preactions,
				DHTActionList<DHTPostaction> postactions) {
			active_code.onDelete(getCallerIP(), executed_preactions,
					             postactions);		
		}

		@Override
		public boolean isValueLocal() { return true; }
		
		@Override
		public DHTEvent getEvent() { return DHTEvent.DELETE; }
	}

	public static class TimerCb extends DHTEventHandlerCallback {
		// Called from outside the sandbox.
		public TimerCb() { }
		
	    // Called from within the sandbox.
		@Override
		protected void executeEventOnActiveObject(ActiveCode active_code,
				DHTActionList<DHTPreaction> executed_preactions,
				DHTActionList<DHTPostaction> postactions) {
			active_code.onTimer(executed_preactions, postactions);
		}

		@Override
		public boolean isValueLocal() { return true; }
		
		@Override
		public DHTEvent getEvent() { return DHTEvent.TIMER; }
	}
}
