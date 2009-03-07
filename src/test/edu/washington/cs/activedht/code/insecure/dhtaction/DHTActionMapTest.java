package edu.washington.cs.activedht.code.insecure.dhtaction;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.MyBogusDHTPreaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTPreaction;
import junit.framework.TestCase;

public class DHTActionMapTest extends TestCase {
	private DHTActionMap<DHTPreaction> preactions;
	
	@Override
	protected void setUp() {
		preactions = new DHTActionMap<DHTPreaction>(2);
	}
	
	@Override
	protected void tearDown() { }
	
	public void testAddToListStopsAddingAtLimit() {
		try {
			preactions.addPreactionToEvent(DHTEvent.GET,
					                       new GetDHTPreaction());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception before limit.");
		}
		assertEquals(1, preactions.getActionsForEvent(DHTEvent.GET).size());

		try {
			preactions.addPreactionToEvent(DHTEvent.GET,
					                       new GetDHTPreaction());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception before limit.");
		}
		assertEquals(2, preactions.getActionsForEvent(DHTEvent.GET).size());
		
		try {
			preactions.addPreactionToEvent(DHTEvent.GET,
					                       new GetDHTPreaction());
			fail("Exception at limit.");
		} catch (Exception e) { }  // Exception expected.
	}
	
	public void testAddToListFailsWhenTryingToAddBadPreactionType() {
		try {
			preactions.addPreactionToEvent(DHTEvent.GET,
					                       new MyBogusDHTPreaction());
			fail("Allowed addition of action of invalid type.");
		} catch (Exception e) { }  // Exception expected.
		assertEquals(null, preactions.getActionsForEvent(DHTEvent.GET));
	}
}
