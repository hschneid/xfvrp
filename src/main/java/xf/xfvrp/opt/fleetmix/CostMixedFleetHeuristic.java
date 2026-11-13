package xf.xfvrp.opt.fleetmix;

import java.util.*;
import java.util.stream.Collectors;

import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

/**
 * Mixed fleet heuristic that decides between vehicle types by cost.
 * <p>
 * The {@link DefaultMixedFleetHeuristic} plans the vehicle types one after the other: each type keeps as many routes as
 * it has vehicles and leaves the rest of the customers to the next type. Routes of different types are never compared,
 * so the plan often holds short routes of several types that one vehicle could drive together.
 * </p>
 * <p>
 * This heuristic starts from that plan and improves it across the types, each move planned again by the normal route
 * planning (all constraints apply) and accepted only if the new route is valid (all its customers on one route, no
 * delay, no overload, within the vehicle's maximum route duration) and cheaper:
 * </p>
 * <ul>
 *   <li><b>merge</b>: the customers of two routes as one route of the vehicle of either; frees a vehicle;</li>
 *   <li><b>change</b>: the customers of a route with a vehicle of another type that has a vehicle left.</li>
 * </ul>
 * <p>
 * A route costs its vehicle's fix cost plus var cost × distance ({@code RouteReportSummary.getCost()}). The number of
 * vehicles per type never grows beyond the type's count. {@link #setMaxEvaluations(int)} bounds the trial plannings.
 * Unplanned customers keep their own solution.
 * </p>
 */
public class CostMixedFleetHeuristic extends DefaultMixedFleetHeuristic {

	private static final float EPS = 1e-3f;

	private int maxEvaluations = 1000;

	/** Trial plannings already found invalid or not cheaper (vehicle and customers), not tried again. */
	private final Set<String> rejected = new HashSet<>();

	/** A planned route with what the improvement needs of it. */
	private static final class Entry {
		final Vehicle vehicle;
		final List<String> customers;
		final float cost;
		final float duration;
		final float[] deliveries;
		final float[] pickups;
		final RouteReport report;
		final XFVRPModel model;

		Entry(Vehicle vehicle, RouteReport report, XFVRPModel model) {
			this.vehicle = vehicle;
			this.report = report;
			this.model = model;
			this.customers = report.getEvents().stream().filter(e -> e.getSiteType() == SiteType.CUSTOMER).map(Event::getID)
					.collect(Collectors.toList());
			this.cost = report.getSummary().getCost();
			this.duration = report.getSummary().getDuration();
			this.deliveries = report.getSummary().getDeliveries().clone();
			this.pickups = report.getSummary().getPickups().clone();
		}
	}

	/** @param maxEvaluations the most trial plannings per call (default 1000) */
	public void setMaxEvaluations(int maxEvaluations) {
		this.maxEvaluations = maxEvaluations;
	}

	@Override
	public List<Solution> execute(Node[] nodes, CompartmentType[] compartmentTypes, Vehicle[] vehicles,
								  RoutePlanningFunction routePlanningFunction, Metric metric, XFVRPParameter parameter,
								  StatusManager statusManager) throws XFVRPException {
		List<Solution> solutions = super.execute(nodes, compartmentTypes, vehicles, routePlanningFunction, metric, parameter,
				statusManager);
		if (vehicles.length == 0) return solutions;

		Map<String, Vehicle> types = new LinkedHashMap<>();
		Map<String, Integer> available = new HashMap<>();
		for (Vehicle v : vehicles) {
			types.putIfAbsent(v.getName(), v);
			available.merge(v.getName(), v.getNbrOfAvailableVehicles(), Integer::sum);
		}

		List<Solution> keep = new ArrayList<>();
		List<Entry> entries = new ArrayList<>();
		for (Solution s : solutions) {
			XFVRPModel model = s.getModel();
			if (model == null || !types.containsKey(model.getVehicle().getName())) {
				keep.add(s);
				continue;
			}
			Report rep = getReportBuilder().getReport(s);
			for (RouteReport r : rep.getRoutes())
				if (r.getEvents().stream().anyMatch(e -> e.getSiteType() == SiteType.CUSTOMER))
					entries.add(new Entry(r.getVehicle(), r, model));
		}
		if (entries.size() < 2 && types.size() < 2) return solutions;

		Map<String, Node> byId = new HashMap<>();
		List<Node> others = new ArrayList<>();
		for (Node n : nodes) {
			if (n.getSiteType() == SiteType.CUSTOMER) byId.putIfAbsent(n.getExternID(), n);
			else others.add(n);
		}

		Map<String, Integer> used = new HashMap<>();
		for (Entry e : entries) used.merge(e.vehicle.getName(), 1, Integer::sum);

		rejected.clear();
		int[] budget = {maxEvaluations};
		int merges = 0, changes = 0;
		boolean improved = true;
		while (improved && budget[0] > 0) {
			improved = false;
			Entry[] merge = bestMerge(entries, byId, others, compartmentTypes, routePlanningFunction, budget);
			if (merge != null) {
				entries.remove(merge[0]);
				entries.remove(merge[1]);
				entries.add(merge[2]);
				for (Entry e : List.of(merge[0], merge[1])) used.merge(e.vehicle.getName(), -1, Integer::sum);
				used.merge(merge[2].vehicle.getName(), 1, Integer::sum);
				merges++;
				improved = true;
				continue;
			}
			Entry[] change = bestChange(entries, types, available, used, byId, others, compartmentTypes, routePlanningFunction, budget);
			if (change != null) {
				entries.remove(change[0]);
				entries.add(change[1]);
				used.merge(change[0].vehicle.getName(), -1, Integer::sum);
				used.merge(change[1].vehicle.getName(), 1, Integer::sum);
				changes++;
				improved = true;
			}
		}
		statusManager.fireMessage(StatusCode.RUNNING,
				"Cost mixed fleet: " + merges + " routes merged, " + changes + " vehicle types changed");

		// one solution per route, rebuilt on the model it was planned with
		List<Solution> out = new ArrayList<>();
		for (Entry e : entries) out.add(reconstructSolution(List.of(e.report), e.model));
		out.addAll(keep);
		return out;
	}

	/** The first merge of two routes that is valid and cheaper, the pairs with the shortest total duration first. */
	private Entry[] bestMerge(List<Entry> entries, Map<String, Node> byId, List<Node> others, CompartmentType[] compartmentTypes,
							  RoutePlanningFunction plan, int[] budget) {
		List<Entry[]> pairs = new ArrayList<>();
		for (int i = 0; i < entries.size(); i++)
			for (int j = i + 1; j < entries.size(); j++) pairs.add(new Entry[] {entries.get(i), entries.get(j)});
		pairs.sort(Comparator.comparingDouble(p -> p[0].duration + p[1].duration));
		for (Entry[] p : pairs) {
			float before = p[0].cost + p[1].cost;
			for (Entry host : p[0].vehicle.getFixCost() <= p[1].vehicle.getFixCost() ? p : new Entry[] {p[1], p[0]}) {
				Vehicle v = host.vehicle;
				if (!fits(v, p[0], p[1])) continue;
				if (Math.max(p[0].duration, p[1].duration) > v.getMaxRouteDuration() + EPS) continue;
				if (v.getFixCost() >= before) continue;
				if (budget[0]-- <= 0) return null;
				List<String> customers = new ArrayList<>(p[0].customers);
				customers.addAll(p[1].customers);
				Entry merged = planOne(v, customers, byId, others, compartmentTypes, plan);
				if (merged != null && merged.cost < before - EPS) return new Entry[] {p[0], p[1], merged};
			}
		}
		return null;
	}

	/** The first change of a route to another type with a vehicle left that is valid and cheaper. */
	private Entry[] bestChange(List<Entry> entries, Map<String, Vehicle> types, Map<String, Integer> available,
							   Map<String, Integer> used, Map<String, Node> byId, List<Node> others,
							   CompartmentType[] compartmentTypes, RoutePlanningFunction plan, int[] budget) {
		List<Entry> byCost = new ArrayList<>(entries);
		byCost.sort(Comparator.comparingDouble((Entry e) -> e.cost).reversed());
		for (Entry e : byCost)
			for (Vehicle v : types.values()) {
				if (v.getName().equals(e.vehicle.getName())) continue;
				if (used.getOrDefault(v.getName(), 0) >= available.getOrDefault(v.getName(), 0)) continue;
				if (v.getFixCost() >= e.cost || !fits(v, e, null)) continue;
				if (e.duration > v.getMaxRouteDuration() * 2) continue;
				if (budget[0]-- <= 0) return null;
				Entry changed = planOne(v, e.customers, byId, others, compartmentTypes, plan);
				if (changed != null && changed.cost < e.cost - EPS) return new Entry[] {e, changed};
			}
		return null;
	}

	/** Whether the loads of one or two routes fit a vehicle, compartment by compartment. */
	private static boolean fits(Vehicle v, Entry a, Entry b) {
		float[] cap = v.getCapacity();
		for (int c = 0; c < a.deliveries.length; c++) {
			float capacity = c < cap.length ? cap[c] : Float.MAX_VALUE;
			float deliveries = a.deliveries[c] + (b == null ? 0 : b.deliveries[c]);
			float pickups = a.pickups[c] + (b == null ? 0 : b.pickups[c]);
			if (deliveries > capacity + EPS || pickups > capacity + EPS) return false;
		}
		return true;
	}

	/**
	 * Plans customers as one route of a vehicle. The nodes are copies: planning indexes its nodes, and the originals
	 * still belong to the routes of the current plan.
	 *
	 * @return the route, or {@code null} if the planning does not give exactly one valid route with all of them
	 */
	private Entry planOne(Vehicle vehicle, List<String> customers, Map<String, Node> byId, List<Node> others,
						  CompartmentType[] compartmentTypes, RoutePlanningFunction plan) {
		String key = vehicle.getName() + "|" + customers.stream().sorted().collect(Collectors.joining(","));
		if (rejected.contains(key)) return null;
		Entry planned = plan(vehicle, customers, byId, others, compartmentTypes, plan);
		if (planned == null) rejected.add(key);
		return planned;
	}

	private Entry plan(Vehicle vehicle, List<String> customers, Map<String, Node> byId, List<Node> others,
					   CompartmentType[] compartmentTypes, RoutePlanningFunction plan) {
		List<Node> trial = new ArrayList<>();
		for (Node n : others) trial.add(n.copy());
		for (String id : customers) {
			Node n = byId.get(id);
			if (n == null) return null;
			trial.add(n.copy());
		}
		Vehicle one = new Vehicle(vehicle);
		one.setNbrOfAvailableVehicles(1);
		try {
			Solution solution = plan.apply(new RoutingDataBag(trial.toArray(new Node[0]), compartmentTypes, one));
			Report rep = getReportBuilder().getReport(solution);
			List<RouteReport> routes = rep.getRoutes().stream()
					.filter(r -> r.getEvents().stream().anyMatch(e -> e.getSiteType() == SiteType.CUSTOMER)).toList();
			if (routes.size() != 1) return null;
			RouteReport route = routes.getFirst();
			long served = route.getEvents().stream().filter(e -> e.getSiteType() == SiteType.CUSTOMER).count();
			if (served != customers.size()) return null;
			if (route.getSummary().getDelay() > EPS) return null;
			for (float o : route.getSummary().getOverloads()) if (o > EPS) return null;
			if (route.getSummary().getDuration() > vehicle.getMaxRouteDuration() + EPS) return null;
			return new Entry(route.getVehicle(), route, solution.getModel());
		} catch (RuntimeException e) { // XFVRPException is one: a planning the constraints refuse is no route
			return null;
		}
	}
}
