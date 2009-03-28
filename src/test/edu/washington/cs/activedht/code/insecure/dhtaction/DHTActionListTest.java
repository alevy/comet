package edu.washington.cs.activedht.code.insecure.dhtaction;

import edu.washington.cs.activedht.code.insecure.candefine.MyBogusDHTPreaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import junit.framework.TestCase;

public class DHTActionListTest extends TestCase {
	private DHTActionList preactions;

	@Override
	protected void setUp() { preactions = new DHTActionList(2); }
	
	@Override
	protected void tearDown() { }
	
	public void testAddToListStopsAddingAtLimit() {
		assertEquals(0, preactions.size());

		try { preactions.addAction(new TestPreaction(0)); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Exception before limit.");
		}
		assertEquals(1, preactions.size());

		try { preactions.addAction(new TestPreaction(0)); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Exception before limit.");
		}
		assertEquals(2, preactions.size());
		
		// Try to add over the limit now.
		try {
			preactions.addAction(new TestPreaction(0));
			fail("Allowed addition of action over limit.");
		} catch (Exception e) { }  // Exception expected.
		assertEquals(2, preactions.size());
	}
	
	public void testAddToListFailsWhenTryingToAddBadPreactionType() {
		try {
			preactions.addAction(new MyBogusDHTPreaction(0));
			fail("Allowed addition of action of invalid type.");
		} catch (Exception e) { }  // Exception expected.
		assertEquals(0, preactions.size());
	}
}

