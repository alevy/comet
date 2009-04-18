package edu.washington.cs.vanish.internal.metadata;

import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.vanish.internal.metadata.VDOParams;
/**
 * 
 * @author roxana
 *
 */
public class ActiveVDOParams extends VDOParams {
	private static final long serialVersionUID = -2396344193593137788L;
	
	private boolean destroy_after_first_read = false;  // by default, disabled.
	
	private long release_date = 0;  // by default, always released.
	
	private long timeout_date;  // by default, now + default timeout.
	
	public ActiveVDOParams() { super(); }
	
	public ActiveVDOParams(int num_shares, int threshold, int timeout_h,
                           boolean destroy_after_first_read,
                           long release_date) {
		super(num_shares, threshold, timeout_h);
		this.destroy_after_first_read = destroy_after_first_read;
		this.release_date = release_date;
		// timeout_date is initialized by setTimeoutInHours, which is called
		// by the super.constructor.
	}
	
	@Override
	public boolean validate() {
		return (super.validate() && (release_date < getTimeoutDate()));
	}
	
	// Accessors:
	
	public boolean destroyAfterFirstRead() { return destroy_after_first_read; }
	
	public void setDestryAfterFirstRead(boolean destroy_after_first_read) {
		this.destroy_after_first_read = destroy_after_first_read;
	}
	
	public long getReleaseDate() { return release_date; }
	
	public void setReleaseDate(long release_date) {
		this.release_date = release_date;
	}
	
	public long getTimeoutDate() { return timeout_date; }
	
	@Override
	public void setTimeoutInHours(int timeout_h) {
		super.setTimeoutInHours(timeout_h);
		this.timeout_date = System.currentTimeMillis() +
		                    getTimeout() * Constants.MINUTES;  // TODO: * Constants.HOURS;
	}
}
