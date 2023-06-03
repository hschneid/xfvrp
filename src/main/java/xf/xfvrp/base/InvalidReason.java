package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * An invalid reason defines the reason why a node may be invalid
 * for the route planning. Several reasons are possible, but only the
 * last detected reason will be remembered in the Report.  
 * 
 * The invalid reason is an attribute of the Node (XFVRP) object.
 * 
 * @author hschneid
 *
 */
public enum InvalidReason {
	NONE, // Default in Node, Means that customer is valid.
	TRAVEL_TIME, // The customer is not reachable from any depot in given route length restrictions
	CAPACITY, // The demand of the customer exceeds any capacity restrictions
	WRONG_VEHICLE_TYPE, // The vehicle type preset for this customer is not applicable 
	TIME_WINDOW, // Any customer time window can not be reached by any depot or vehicle type.
	UNPLANNED, // The customer is not placed on a valid route due to restrictions for vehicle count
	PDP_INCOMPLETE,  // Whether pickup or delivery node of a pdp-shipment is missing
	PDP_IMPROPER_AMOUNTS,
	PDP_ILLEGAL_NUMBER_OF_CUSTOMERS_PER_SHIPMENT, 
	PDP_SOURCE_DEST_EQUAL, 
	PDP_NODE_MULTIPLE_MENTIONS
}
