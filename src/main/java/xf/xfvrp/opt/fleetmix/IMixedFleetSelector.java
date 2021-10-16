package xf.xfvrp.opt.fleetmix;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

import java.util.List;

public interface IMixedFleetSelector {
    List<RouteReport> getBestRoutes(Vehicle veh, Report rep);
}
