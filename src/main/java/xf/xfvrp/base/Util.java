package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Utility class holds indifferent methods for processing on
 * the giant tour array. All methods are static.
 * 
 * @author hschneid
 *
 */
public class Util {

	/**
	 * In single node problems a giant tour contains only one node multiple times. But each
	 * version of the node in giant tour is a deep copy with a unique depotId.
	 * 
	 * This method creates the deep copy instance and assigns the depotId.
	 * 
	 * @param node Current node
	 * @param newId Last assigned depotId
	 * @return The copy of node object
	 */
	public static Node createIdNode(Node node, int newId) {
		Node n = node.copy();
		n.setDepotId(newId);
		return n;
	}
}
