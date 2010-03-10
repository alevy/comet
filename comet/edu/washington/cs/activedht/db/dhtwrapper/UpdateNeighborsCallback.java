/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.Set;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;

/**
 * @author levya
 *
 */
public interface UpdateNeighborsCallback {

	void call(Set<NodeWrapper> tmpNeighbors);

}
