package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-present Holger Schneider
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
	 * In single depot problems a giant tour contains only one depot multiple times. But each
	 * version of the depot in giant tour is a deep copy with a unique depotId.
	 * 
	 * This method creates the deep copy instance and assigns the depotId.
	 * 
	 * @param depot Current depot
	 * @param newId Last assigned depotId
	 * @return The copy of depot object
	 */
	public static Node createIdNode(Node depot, int newId) {
		Node n = depot.copy();
		n.setDepotId(newId);
		return n;
	}
}
