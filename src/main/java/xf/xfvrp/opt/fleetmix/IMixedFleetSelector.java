package xf.xfvrp.opt.fleetmix;

import java.util.List;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

public interface IMixedFleetSelector {
    List<RouteReport> getBestRoutes(Vehicle veh, Report rep);
}
