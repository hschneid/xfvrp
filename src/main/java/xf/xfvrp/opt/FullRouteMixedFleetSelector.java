package xf.xfvrp.opt;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;
import xf.xfvrp.report.RouteReportSummary;

import java.util.List;
import java.util.stream.Collectors;

public class FullRouteMixedFleetSelector {

	/**
	 * This method searches the best k routes in a given solution
	 * for a given vehicle. k is the number of available vehicles. The
	 * objective for best route is the cost per amount.
	 * 
	 * @param veh Container with given parameters
	 * @param rep Solution as report object
	 * @return List of best routes in solution report
	 */
	List<RouteReport> getBestRoutes(Vehicle veh, Report rep) {
		return rep.getRoutes().stream()
				// Get the quality of routes by the report informations
				.map(route -> getQuality(route))
				// Sort the routes by their quality
				.sorted((o1, o2) -> {
					double v1 = o1.quality;
					double v2 = o2.quality;
					if(v1 > v2) return 1;
					if(v1 < v2) return -1;
					return 0;
				})
				// Reduce routes to the n best routes
				.map(val -> val.route)
				.limit(veh.nbrOfAvailableVehicles)
				.collect(Collectors.toList());
	}

	private RouteQuality getQuality(RouteReport route) {
		RouteReportSummary summary = route.getSummary();

		Vehicle veh = route.getVehicle();
		float time = summary.getDuration();
		float amount = sum(summary.getPickups()) + sum(summary.getDeliveries());
		float quality = (veh.fixCost + (veh.varCost * time)) / amount;
		
		if(summary.getDelay() > 0)
			quality = Float.MAX_VALUE;
		if(amount == 0)
			quality = 0;
		
		RouteQuality routeQuality = new RouteQuality();
		routeQuality.route = route;
		routeQuality.quality = quality;

		return routeQuality;
	}

	private float sum(float[] arr) {
		float sum = 0;
		for (int i = arr.length - 1; i >= 0; i--) {
			sum += arr[i];
		}

		return sum;
	}

	private class RouteQuality {
		RouteReport route;
		double quality;
	}
}
