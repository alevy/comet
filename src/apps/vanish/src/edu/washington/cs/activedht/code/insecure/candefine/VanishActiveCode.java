package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.LocalDeleteDHTAction;

public class VanishActiveCode extends ActiveObjectAdapter {
	private static final long serialVersionUID = -2646731843470828602L;
	
	private static final long SELF_DESTRUCT_AFTER_FIRST_READ_GRACE_PERIOD =
		20 * 60000L;  // 20 min
	
	// Inputs:
	private boolean self_destroy_after_first_read = false;  // one-time arg.
	private long self_destruction_date;  // time-based self-destruction arg.
	private long release_date;  // release argument
	
	// State:
	private long one_time_self_destruction_init_time = 0;  // not initiated yet
	
	public VanishActiveCode(byte[] value,
			boolean self_destroy_after_first_read,
			long self_destruction_date,
			long release_date) {
		super(value);
		// One-time-read params:
		this.self_destroy_after_first_read = self_destroy_after_first_read;
		// Timeout-based self-destruction params:
		this.self_destruction_date = self_destruction_date;
		// Release params:
		this.release_date = release_date;
	}

	// ActiveCode interface:
	
	@Override
	public void onGet(String caller_ip, DHTActionList executed_preactions,
			          DHTActionList postactions) {
		System.out.println("VanishActiveCode: onGet()");
		long now = System.currentTimeMillis();
		boolean releaseAlreadyCanceled = false;
		if (isTimeToSelfDestruct(now)) {
			selfDestruct(postactions);
			cancelRelease(postactions);
			releaseAlreadyCanceled = true;
		}
		
		if (! releaseAlreadyCanceled && shouldntReleaseData(now)) {
			cancelRelease(postactions);
			releaseAlreadyCanceled = true;
			System.out.println("VanishActiveCode: Release canceled 2");
		}
		
		if (! releaseAlreadyCanceled) setValueWasRead(now);

		System.out.println("VanishActiveCode: willRead = " +
				           (! releaseAlreadyCanceled));
	}
	
	@Override
	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) {
		System.out.println("VanishActiveCode: onTimer()");
		long now = System.currentTimeMillis();
		if (shouldBcastSelfDestructionToReplicas()) {
			bcastSelfDestructionToReplicas(postactions);
		}
		
        // If it's time to self-destruct, do it.
		if (isTimeToSelfDestruct(now)) selfDestruct(postactions);
	}
	
	// Helper functions:
	
	private void cancelRelease(DHTActionList postactions) {
		try { postactions.addAction(new AbortOperationAction()); }
		catch (Exception e) { }
	}
	
	private void selfDestruct(DHTActionList postactions) {
		try { postactions.addAction(new LocalDeleteDHTAction()); }
		catch (Exception e) { }
	}
	
	private void bcastSelfDestructionToReplicas(DHTActionList postactions) {
		if (self_destroy_after_first_read) {
			try { postactions.addAction(new GetDHTAction(20)); }
			catch (Exception e) { }
		}
	}
	
	private void setValueWasRead(long now) {
		if (self_destroy_after_first_read) { 
			one_time_self_destruction_init_time = now;
		}
	}
	
	private boolean shouldntReleaseData(long now) {
		return release_date > now || one_time_self_destruction_init_time > 0;
	}
	
	private boolean isTimeToSelfDestruct(long now) {
		return ((self_destruction_date < now) ||
				(selfDestructionInitiated() &&
		         ((one_time_self_destruction_init_time +
		          SELF_DESTRUCT_AFTER_FIRST_READ_GRACE_PERIOD) < now)));
	}
	
	private boolean selfDestructionInitiated() {
		return one_time_self_destruction_init_time > 0;
	}
	
	private boolean shouldBcastSelfDestructionToReplicas() {
		return selfDestructionInitiated();
	}
}
