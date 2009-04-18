package edu.washington.cs.vanish.apps;

import java.net.UnknownHostException;

import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.vanish.internal.VanishException;
import edu.washington.cs.vanish.internal.metadata.ActiveVDOParams;
import edu.washington.cs.vanish.internal.metadata.VDOParams;
import edu.washington.cs.vanish.util.InvalidArgumentException;

public class ActiveDHTVanishClientImpl extends VanishClientRunner {
	private ActiveVDOParams params;
	
	public ActiveDHTVanishClientImpl(String[] args)
	throws UnknownHostException, VanishException {
		super(args);
	}

	@Override
	protected VDOParams instantiateVDOParams() { 
		this.params = new ActiveVDOParams();
		return params;
	}
	
	@Override
	protected int decodeArgument(String arg, String args[], int i)
	throws InvalidArgumentException {
		if (arg.equals("onetime")) {
			params.setDestryAfterFirstRead(true);
		} else if (arg.equals("release")) {
			if (i < args.length) {
				try {
					params.setReleaseDate(System.currentTimeMillis() +
							Integer.parseInt(args[i++]) * Constants.MINUTES);
				} catch (NumberFormatException e) {
					throw new InvalidArgumentException(
							"release requires a number (hours)");
				}
			} else {
				throw new InvalidArgumentException(
						"release requires a number (hours)");
			}
		} else {
			i = super.decodeArgument(arg, args, i);
		}
		
		return i;
	}
	
	@Override
	public String usage() {
		return super.usage() 
			 + "\tonetime true|false\n"
			 + "\trelease <release delay (hours)>\n";
	}
	
	@Override
	public boolean validate() { return super.validate(); }
	
	
	public static void main(String[] args) {
		try {
			VanishClientRunner client_runner = new ActiveDHTVanishClientImpl(
					args);
			doWork(client_runner);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); // very important to exit with != 0; all plugins
			                 // depend on it.
		}
		System.exit(0);
	}
}
