package xf.xfvrp.opt.fleetmix;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

import java.util.List;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @author hschneid
 */
public interface IMixedFleetSelector {
    List<RouteReport> getBestRoutes(Vehicle veh, Report rep);
}
