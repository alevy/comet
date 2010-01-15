package edu.washington.cs.activedht.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aelitis.azureus.core.dht.impl.Test;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.ForensicTrailActiveObject;
import edu.washington.cs.activedht.code.insecure.candefine.LocationAwareTrackerActiveObject;
import edu.washington.cs.activedht.code.insecure.candefine.OneTimeActiveObject;
import edu.washington.cs.activedht.code.insecure.candefine.SelfRegulatingReplicationObject;
import edu.washington.cs.activedht.code.insecure.candefine.SensitiveValueActiveObject;
import edu.washington.cs.activedht.code.insecure.candefine.TimeReleaseActiveObject;
import edu.washington.cs.activedht.code.insecure.candefine.TimeoutActiveObject;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;
import edu.washington.cs.activedht.db.coderunner.InvalidActiveObjectException;
import edu.washington.cs.activedht.db.java.JavaActiveDHTDBValue;

/**
 * USAGE:
 *   java -cp build/classes:dist/ActiveVuze.jar:lib/commons-cli.jar:lib/log4j.jar \
          edu.washington.cs.activedht.db.ActiveDHTTestShell 3
 * 
 * Examples:
 *   p x=OneTimeActiveObject(xval)
 *   p y=ForensicTrailActiveObject(yval)
 *   p z=TimeoutActiveObject(zval, 10)
 *   p t=TimeReleaseActiveObject(tval, 10)
 *   
 *   p u=SelfRegulatingReplicationObject(uval, 3, 1, 2)
 *   p v=SensitiveValueActiveObject(vval)
 *   
 *   # For TimeoutActiveObject and TimeReleaseActiveObject timeouts are in sec.
 * 
 * @author roxana
 *
 */
public class ActiveDHTTestShell extends Test {
	private static final String ACTIVE_CODE_PACKAGE_NAME =
		"edu.washington.cs.activedht.code.insecure.candefine.";
	private static final String CLASS_NAME_RE = "([A-z0-9\\.]+)";
	
	private static final String CONSTRUCTOR_PARAM_RE = "([^\\s,\\)]+)";
	
	private static final String ALL_CONSTRUCTOR_PARAMS_RE =
		"(" + CONSTRUCTOR_PARAM_RE + "\\s*,\\s*)*" +  // non-last params
		CONSTRUCTOR_PARAM_RE;  // last param.
	
	private static final String ACTIVE_VALUE_RE =
		"^\\s*" + CLASS_NAME_RE + "\\s*\\(\\s*" + ALL_CONSTRUCTOR_PARAMS_RE +
		"\\)\\s*$";
	
	private static final Pattern ACTIVE_VALUE_PATTERN =
		Pattern.compile(ACTIVE_VALUE_RE);
	
	protected ActiveDHTTestShell(int num_dhts) { super(num_dhts); }
	
	// Test function overrides:
	
	@Override
	protected String getTmpDirectory() { return "/tmp/dht/"; }
	
	/**
	 * Active value format:
	 *     CLASS_NAME(arg1, arg2, arg3, ...)
	 * where:
	 *     CLASS_NAME is the class' name.
	 *     arg1, arg2, ... are arguments of the constructor.
	 */
	@Override
	protected byte[] getBytes(String value) {
		Matcher matcher = ACTIVE_VALUE_PATTERN.matcher(value);
		if (! matcher.find()) return super.getBytes(value);  // not active
		
		// Create a test active value.
		ActiveCode ac = null;
		try {
			ac = instantiateActiveCode(matcher);
		} catch (InvalidActiveObjectException e1) {
			e1.printStackTrace();
			ActiveDHTTestShell.printShellUsage();
			return null;
		}
		if (ac == null) return super.getBytes(value);  // not an active value.
		
		// Value should be active.
		
		// Serialize it.
		byte[] value_bytes = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ClassObjectOutputStream oos = new ClassObjectOutputStream(baos);
			oos.writeObject(ac);
			value_bytes = baos.toByteArray();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value_bytes;
	}
	
	private static void printShellUsage() {
		System.out.println("USAGE: Syntax for active values: " +
				           "CLASS_NAME(arg1, arg2, ...)" );
	}
	
	// TODO(roxana): Pretty hacky now.
	private ActiveCode instantiateActiveCode(Matcher matcher)
	throws InvalidActiveObjectException {
		// assert(matcher.matches());
		if (matcher.groupCount() < 3) {
			throw new InvalidActiveObjectException("Can't instantiate " +
					"active object: invalid number of params");
		}

		String active_code_class = ACTIVE_CODE_PACKAGE_NAME + matcher.group(1);
		if (active_code_class.equals(
				ForensicTrailActiveObject.class.getName())) {
			return new ForensicTrailActiveObject(matcher.group(4).getBytes());
			
		} else if (active_code_class.equals(
				OneTimeActiveObject.class.getName())) {
			return new OneTimeActiveObject(matcher.group(4).getBytes());
			
		} else if (active_code_class.equals(
				LocationAwareTrackerActiveObject.class.getName())) {
			return new LocationAwareTrackerActiveObject();
			
		} else if (active_code_class.equals(
				SensitiveValueActiveObject.class.getName())) {
			return null;  // TODO
			
		} else if (active_code_class.equals(
				TimeoutActiveObject.class.getName())) {
			long timeout_date = System.currentTimeMillis();
			try {
				timeout_date += Long.parseLong(matcher.group(4)) * 1000L;
			} catch (NumberFormatException e) {
				throw new InvalidActiveObjectException("Can't instantiate " +
						"active object: invalid timeout");
			}
			return new TimeoutActiveObject(matcher.group(3).getBytes(),
					                       timeout_date);
			
		} else if (active_code_class.equals(
				TimeReleaseActiveObject.class.getName())) {
			long release_date = System.currentTimeMillis();
			try {
				release_date += Long.parseLong(matcher.group(4)) * 1000L;
			} catch (NumberFormatException e) {
				throw new InvalidActiveObjectException("Can't instantiate " +
						"active object: invalid timeout");
			}
			return new TimeReleaseActiveObject(matcher.group(3).getBytes(),
					                           release_date);
		} else if (active_code_class.equals(
				SelfRegulatingReplicationObject.class.getName())) {
			//if (matcher.groupCount() < 6) return null;
			int replication_threshold = 0;
			int replication_factor    = 3;
			long replication_interval  = 2;
			try {
				replication_threshold = Integer.parseInt(matcher.group(4));
				//replication_factor    = Integer.parseInt(matcher.group(5));
				//replication_interval  = Long.parseLong(matcher.group(6)) * 1000L;
			} catch (NumberFormatException e) {
				throw new InvalidActiveObjectException("Can't instantiate " +
						"active object: invalid timeout");
			}
			return new SelfRegulatingReplicationObject(matcher.group(3).getBytes(),
					                                   replication_threshold,
					                                   replication_factor,
					                                   replication_interval);
		}
		
		return null;
	}
	
	private String getString(ActiveCode ac) {
		if (ac == null) return null;
		String ret = "";
		if (ac instanceof ActiveObjectAdapter) {
			ret += new String(((ActiveObjectAdapter)ac).getValue());
		}
		if (ac instanceof ForensicTrailActiveObject) {
			ForensicTrailActiveObject o = (ForensicTrailActiveObject)ac;
			ret += " (replicated by: " + o.getReplicaIPs() +
			       "; read by: " + o.getAccessorIPs() + ")";
		}
		
		return ret;
	}
	
	@Override
	protected String getString(DHTTransportValue value) {		
		JavaActiveDHTDBValue active_val =
			new JavaActiveDHTDBValue(null, value, false);
				
		byte[] active_object = active_val.getValue();
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(
					new ByteArrayInputStream(active_object));
			ActiveCode ac = (ActiveCode)ois.readObject();
			if (ac != null) return getString(ac);
		} catch (Exception e) {
		} finally {
			if (ois != null) {
				try { ois.close(); }
				catch (IOException e) { }
			}
		}
		
		return super.getString(value);  // not an active value.
	}
	
	private static void printProgramUsageAndExit() {
		System.err.println("USAGE: java " +
				ActiveDHTTestShell.class.getName() + " <num dhts>");
		System.exit(1);
	}
	
	private static void doMain(int num_dhts) {
        // First, initialize the active engine of the DHT.
		ActiveDHTInitializer.prepareRuntimeForActiveCode();
		
		// Create a test shell using Azureus' one.
		new ActiveDHTTestShell(num_dhts);
	}
	
	public static void main(String args[]) {
		if (args.length != 1) printProgramUsageAndExit();

		int num_dhts = 0;
		try { num_dhts = Integer.parseInt(args[0]); }
		catch(NumberFormatException e) { printProgramUsageAndExit(); }

		doMain(num_dhts);
	}
}

