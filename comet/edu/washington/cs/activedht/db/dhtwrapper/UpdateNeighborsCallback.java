/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.SortedSet;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;

/**
 * @author levya
 *
 */
public interface UpdateNeighborsCallback {

	void call(SortedSet<NodeWrapper> tmpNeighbors);

}
