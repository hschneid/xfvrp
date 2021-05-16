package xf.xfvrp.base;

import java.io.Serializable;

/** 
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This class holds all general planning parameters, which are
 * not allocated to certain data objects like Node or Container.
 * 
 * @author hschneid
 *
 */
public class XFVRPParameter implements Serializable {
	
	private static final long serialVersionUID = 8822219362823890492L;
	
	private boolean allowsRouteSplitting = false;

	/* 
	 * Parameters for Open VRP
	 * 
	 * Open routes start at the first customer and ends at the last customer. Here the
	 * start or the end of a route can be decided to be open.
	 */
	private boolean openRouteAtStart = false;
	private boolean openRouteAtEnd = false;
	
	/*
	 * Parameters for time window planning
	 * 
	 * In default, the loading und unloading time at the depot is not considered. With these
	 * parameters the loading times will be part of the time window planning. So the loading at
	 * the depot may lead to different routings.
	 */
	private boolean loadingTimeAtDepot = false;
	private boolean unloadingTimeAtDepot = false;
	
	/*
	 * Parameter for pre-defined solutions
	 */
	private String predefinedSolutionString = null;
	
	/*
	 * Parameter for the ILS optimization algorithm
	 */
	private int nbrOfILSLoops = 50;
	private long maxRunningTimeInSec = Long.MAX_VALUE;
	
	/*
	 * Parameter for planning a pickup-and-delivery problem (dial-a-ride)
	 */
	private boolean isWithPDP = false;
	
	/**
	 * Reset of all parameters to default value
	 */
	public void clear() {
		allowsRouteSplitting = false;
		openRouteAtStart = false;
		openRouteAtEnd = false;
		loadingTimeAtDepot = false;
		unloadingTimeAtDepot = false;
		isWithPDP = false;
		predefinedSolutionString = null;
		nbrOfILSLoops = 50;
		maxRunningTimeInSec = Long.MAX_VALUE;
	}

	/**
	 * @return the allowsRouteSplitting
	 */
	public final boolean isRouteSplittingAllowed() {
		return allowsRouteSplitting;
	}
	/**
	 * @param allowsRouteSplitting the allowsRouteSplitting to set
	 */
	public final void setRouteSplitting(boolean allowsRouteSplitting) {
		this.allowsRouteSplitting = allowsRouteSplitting;
	}

	/**
	 * @return the openRouteAtStart
	 */
	public final boolean isOpenRouteAtStart() {
		return openRouteAtStart;
	}

	/**
	 * @param openRouteAtStart the openRouteAtStart to set
	 */
	public final void setOpenRouteAtStart(boolean openRouteAtStart) {
		this.openRouteAtStart = openRouteAtStart;
	}

	/**
	 * @return the openRouteAtEnd
	 */
	public final boolean isOpenRouteAtEnd() {
		return openRouteAtEnd;
	}

	/**
	 * @param openRouteAtEnd the openRouteAtEnd to set
	 */
	public final void setOpenRouteAtEnd(boolean openRouteAtEnd) {
		this.openRouteAtEnd = openRouteAtEnd;
	}

	/**
	 * @return the predefinedSolutionString
	 */
	public final String getPredefinedSolutionString() {
		return predefinedSolutionString;
	}

	/**
	 * @param predefinedSolutionString the predefinedSolutionString to set
	 */
	public final void setPredefinedSolutionString(String predefinedSolutionString) {
		this.predefinedSolutionString = predefinedSolutionString;
	}

	/**
	 * 
	 * @param b
	 */
	public void setLoadingTimeAtDepot(boolean b) {
		this.loadingTimeAtDepot = b;
	}

	/**
	 * 
	 * @param b
	 */
	public void setUnloadingTimeAtDepot(boolean b) {
		unloadingTimeAtDepot = b;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isWithLoadingTimeAtDepot() {
		return loadingTimeAtDepot;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isWithUnloadingTimeAtDepot() {
		return unloadingTimeAtDepot;
	}

	/**
	 * Returns the number of loops for the ILS optimization algorithm
	 * 
	 * @return
	 */
	public int getILSLoops() {
		return nbrOfILSLoops;
	}

	/**
	 * 
	 * @param nbrOfLoops
	 */
	public void setNbrOfILSLoops(int nbrOfLoops) {
		this.nbrOfILSLoops = nbrOfLoops;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isWithPDP() {
		return isWithPDP;
	}
	
	/**
	 * 
	 * @param isWithPDP
	 */
	public void setWithPDP(boolean isWithPDP) {
		this.isWithPDP = isWithPDP;
	}

	/**
	 * @return the maxRunningTimeInSec
	 */
	public long getMaxRunningTimeInSec() {
		return maxRunningTimeInSec;
	}

	/**
	 * @param maxRunningTimeInSec the maxRunningTimeInSec to set
	 */
	public void setMaxRunningTimeInSec(long maxRunningTimeInSec) {
		this.maxRunningTimeInSec = maxRunningTimeInSec;
	}
}
