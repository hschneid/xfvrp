package xf.xfvrp.base.xfvrp;

import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.monitor.StatusMonitor;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public abstract class XFVRPBase {

	/* Manages internal status messages to external observer */
	protected StatusManager statusManager = new StatusManager();

	/**
	 * With this method a user can place a specifed status monitor object, where
	 * news from the optimization are communicated.
	 * 
	 * A full transparent information flow is not given, because the loss of speed is huge.
	 * 
	 * @param monitor User defined monitor object
	 */
	public void setStatusMonitor(StatusMonitor monitor) {
		statusManager.addObserver(monitor);
	}
	
	/**
	 * Removes all accounted StatusMonitors
	 */
	public void clearStatusMonitor() {
		statusManager.clearObserver();
	}
}
