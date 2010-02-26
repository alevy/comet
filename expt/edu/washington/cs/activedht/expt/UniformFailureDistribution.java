/**
 * 
 */
package edu.washington.cs.activedht.expt;


/**
 * @author levya
 *
 */
public class UniformFailureDistribution implements FailureDistribution {

	private final long max;

	public UniformFailureDistribution(long max) {
		this.max = max;
	}
	
	public long nextFailureInterval() {
		return (long) (Math.random() * max);
	}

}
