package edu.washington.cs.activedht.db.kahlua;

/**
 * Keeps track of instructions used for a particular call to execute of an ASO
 * 
 * @author alevy
 *
 */
public class InstructionCounter {
	
	private int budget;
	
	public InstructionCounter(int budget) {
		this.setBudget(budget);
	}

	public synchronized void setBudget(int budget) {
		this.budget = budget;
	}

	public synchronized int getBudget() {
		return budget;
	}
	

}
