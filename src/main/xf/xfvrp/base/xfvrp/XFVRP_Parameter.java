package xf.xfvrp.base.xfvrp;

import xf.xfvrp.base.XFVRPParameter;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public abstract class XFVRP_Parameter extends XFVRP_Data {

	/* Planning parameters */
	protected final XFVRPParameter parameter = new XFVRPParameter();

	/**
	 * All parameters are reset to default values (in most cases to false)
	 */
	public void clearParameters() {
		parameter.clear();
	}

	/**
	 * Activates the splitted optimization of route plans.
	 * 
	 * Instead of a full blown optimization of a single big Vehicle Routing Problem,
	 * the problem is splitted into smaller pieces. The pieces will be optimized and later
	 * combined to one result. This procedure is done iteratively, so that the splitted result
	 * is nearly as good as the full blown optimized result.
	 * 
	 * The speed up of splitting is much bigger than the loss of result quality.
	 */
	public void allowsRoutePlanSplitting() {
		parameter.setRouteSplitting(true);
	}

	/**
	 * Activates the checking of loading constraints (3D oriented loading problem)
	 * 
	 * The input data should contain the additional informations
	 */
	public void allowsLoadPlanning() {
		parameter.setLoadPlanning(true);
	}

	/**
	 * Disables the lifo restriction in load planning, which means that
	 * repacking along the route could be necessary.
	 */
	public void disableLifoInLoadPlanning() {
		parameter.setNoLifoInLoadPlanning(true);
	}

	/**
	 * By activating this flag, routes start with the first customer and ending at the depot.
	 */
	public void allowsOpenRoutesAtStart() {
		parameter.setOpenRouteAtStart(true);
	}

	/**
	 * By activating this flag, routes end with the last customer and starting at the depot.
	 */
	public void allowsOpenRoutesAtEnd() {
		parameter.setOpenRouteAtEnd(true);
	}

	/**
	 * By activating this flag, routes start with the first customer and ending at the depot.
	 */
	public void allowsFullOpenRoutes() {
		parameter.setOpenRouteAtStart(true);
		parameter.setOpenRouteAtEnd(true);
	}

	/**
	 * By activating this flag, the loading time at the depot is considered in time window planning.
	 */
	public void allowsLoadingTimeAtDepot() {
		parameter.setLoadingTimeAtDepot(true);
	}

	/**
	 * By activating this flag, the unloading time at the depot is considered in time window planning.
	 */
	public void allowsUnloadingTimeAtDepot() {
		parameter.setUnloadingTimeAtDepot(true);	
	}

	/**
	 * By activating this flag, the loading an unloading time at the depot is considered in time window planning.
	 */
	public void allowsHandlingTimeForUnAndLoadingAtDepot() {
		parameter.setLoadingTimeAtDepot(true);
		parameter.setUnloadingTimeAtDepot(true);
	}

	/**
	 * Activates the planning of a pickup-and-delivery problem (dial-a-ride) where
	 * a pair of two customers (a shipment with pickup node and delivery node) must
	 * be planned on the same route and the pickup must be before the delivery.
	 * 
	 */
	public void allowsPDPPlanning() {
		parameter.setWithPDP(true);
	}

	/**
	 * With this parameter a predefined solution can be inserted into the XFVRP. The solution string
	 * is strictly formated. The nodes are denotated by their name (ORT_ID). All unknown or invalid nodes
	 * are ignored by the reading routine. Double entries are resolved by ignoring later doublettes. 
	 * It is not needed to assign all customer nodes. The unassigned nodes are inserted in the default way by
	 * single trips.
	 * 
	 * Example:
	 * 
	 * 	{ (ID1, ID2), (ID4, ID3), (ID9) }
	 * 
	 * Attention:
	 * 
	 * A predefined solution can be manipulated by the optimization routines. So this feature can be used in
	 * combination with NO optimization routine to validate a result or to initialize the optimization with
	 * a good starting solution. 
	 * 
	 * @param predefinedSolutionString
	 */
	public void setPredefinedSolutionString(String predefinedSolutionString) {
		parameter.setPredefinedSolutionString(predefinedSolutionString);
	}

	/**
	 * Sets the parameter for the ILS optimization algorithm. The number of loops
	 * influences the duration of the search and the quality of the solution aswell.
	 * 
	 * @param nbrOfLoops More means longer runs and better results
	 */
	public void setNbrOfLoopsForILS(int nbrOfLoops) {
		parameter.setNbrOfILSLoops(nbrOfLoops);
	}
	
	/**
	 * Sets the maximal running time of the optimization process (currently only for ILS).
	 * 
	 * The time value needs to be in seconds.
	 * 
	 * @param seconds Maximal running time in seconds
	 */
	public void setMaxRunningTime(long seconds) {
		parameter.setMaxRunningTimeInSec(seconds);
	}

}
