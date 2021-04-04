package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * A load type describes the kind of a order.
 * Pickup orders are loaded at a certain location
 * and are brought to the end depot. Deliveries will
 * loaded at the start depot and unloaded at the specified
 * customer location.
 * 
 * This enumeration holds all possible LoadTypes, which are
 * recognized by the model.
 * 
 * @author hschneid
 *
 */
public enum LoadType {

	PICKUP(0),
	DELIVERY(1),
	REPLENISH(2), 
	PAUSE(3),
	UNDEF(4);
	
	public final int idx;
	
	/**
	 * 
	 * @param idx
	 */
	private LoadType(int idx) {
		this.idx = idx;
	}
}
