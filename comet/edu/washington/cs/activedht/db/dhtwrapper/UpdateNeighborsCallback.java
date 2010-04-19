/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.List;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;

/**
 * @author levya
 *
 */
public interface UpdateNeighborsCallback {

	void call(List<NodeWrapper> tmpNeighbors);

}
