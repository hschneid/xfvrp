package xf.xfvrp.opt.fleetmix;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.evaluation.EvaluationService;
import xf.xfvrp.opt.improve.ils.VRPTTILS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Copyright (c) 2026 Lars Hackstein
 * <p>
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Optimizes transshipment records between the routes of DIFFERENT vehicles
 * (Phase 3): The mixed fleet planning produces one single-vehicle solution
 * per vehicle, so that the Phase 2 records span two trips of the same
 * vehicle and never reduce costs. This optimizer builds the joint plan of
 * all per-vehicle solutions and adds records between two vehicles:
 * <ul>
 * <li><b>Candidate generation:</b> for each collected customer of a
 * supplier route, the nearest transshipment locations and the taker route
 * positions with the least detour are candidates,</li>
 * <li><b>joint evaluation:</b> both affected solutions are evaluated with
 * the routes of the other solutions as record context (load and temporal
 * consistency over vehicles),</li>
 * <li><b>acceptance:</b> a candidate record is only kept, if both solutions
 * stay feasible and the joint costs stay within the given bound,</li>
 * <li><b>local re-planning:</b> after a record is applied, the affected
 * solutions are re-optimized with the VRPTT ILS (the freed capacity of the
 * supplier can consolidate its routes),</li>
 * <li><b>cleanup:</b> records, which can be removed without violating the
 * synchronization, are removed, because they only add detours.</li>
 * </ul>
 * A whole pass is only kept, if the joint plan improved, otherwise it is
 * reverted. The joint plan itself stays a list of per-vehicle solutions, so
 * the report building and all downstream consumers work unchanged.
 *
 * @author lhackste
 */
public class VRPTTJointRecordOptimizer {

	private static final float EPSILON = 0.001f;

	/** Maximum number of apply/replan passes */
	private static final int MAX_PASSES = 8;

	/** Maximum number of candidates, which are fully evaluated per pass */
	private static final int MAX_CANDIDATES_PER_PASS = 60;

	/** Maximum number of transshipment locations per customer */
	private static final int MAX_TL_CANDIDATES = 3;

	private final EvaluationService evaluationService = new EvaluationService();

	/**
	 * Improves the joint plan of the given per-vehicle solutions by
	 * inter-vehicle transshipment records. The list is modified in place.
	 */
	public void optimize(List<Solution> solutions, StatusManager statusManager) throws XFVRPException {
		if (solutions == null || solutions.size() < 2)
			return;

		float totalBefore = totalQuality(solutions).getCost();

		for (int pass = 0; pass < MAX_PASSES; pass++) {
			List<Solution> backups = copies(solutions);

			Candidate candidate = bestCandidate(solutions);
			if (candidate == null)
				break;

			// Apply the best candidate (its trial run was reverted)
			addRecord(solutions, candidate.supplierSolutionIdx, candidate.customerRouteIdx, candidate.customerPos,
					candidate.tlGlobalIdx, candidate.takerSolutionIdx, candidate.takeRouteIdx, candidate.takePos,
					Float.MAX_VALUE);

			// Local re-planning: the freed capacity of the supplier can
			// consolidate its routes
			replan(solutions.get(candidate.supplierSolutionIdx), solutions, statusManager);
			replan(solutions.get(candidate.takerSolutionIdx), solutions, statusManager);

			removeUnprofitableRecords(solutions);

			Quality total = totalQuality(solutions);
			if (total.getPenalty() == 0 && total.getCost() < totalBefore - EPSILON) {
				totalBefore = total.getCost();
			} else {
				restore(solutions, backups);
				break;
			}
		}
	}

	/**
	 * Adds a transshipment record between two different vehicles: a drop
	 * visit of the transshipment location is inserted after the customer of
	 * the supplier solution and a take visit of the same amount into the
	 * taker solution. The record is only kept, if both solutions stay
	 * feasible and the joint costs stay within the given bound.
	 *
	 * @param solutions           Joint plan of per-vehicle solutions
	 * @param supplierSolutionIdx Index of the solution of the supplier
	 * @param customerRouteIdx    Index of the route of the customer in the supplier solution
	 * @param customerPos         Position of the customer in its route
	 * @param tlGlobalIdx         Global index of the transshipment location
	 * @param takerSolutionIdx    Index of the solution of the taker
	 * @param takeRouteIdx        Index of the route of the take visit in the taker solution
	 * @param takePos             Insert position of the take visit
	 * @param maxCostIncrease     Maximal allowed increase of the joint costs
	 * @return the new total quality, or null, if the record was not kept
	 */
	public Quality addRecord(List<Solution> solutions, int supplierSolutionIdx, int customerRouteIdx, int customerPos,
	                         int tlGlobalIdx, int takerSolutionIdx, int takeRouteIdx, int takePos,
	                         float maxCostIncrease) throws XFVRPException {
		Solution supplierSolution = solutions.get(supplierSolutionIdx);
		Solution takerSolution = solutions.get(takerSolutionIdx);

		Node[] supplierRoute = supplierSolution.getRoutes()[customerRouteIdx];
		Node[] takerRoute = takerSolution.getRoutes()[takeRouteIdx];
		if (supplierRoute.length < 3 || takerRoute.length < 3)
			return null;

		Node customer = supplierRoute[customerPos];
		if (customer == null || customer.getSiteType() != SiteType.CUSTOMER || customer.getDemand().length == 0)
			return null;

		float amount = customer.getDemand()[0];
		if (amount <= EPSILON)
			return null;

		// The take visit must be inside the taker route (before its closing depot)
		if (takePos < 1 || takePos > takerRoute.length - 1)
			return null;

		Node supplierTl = findNode(supplierSolution.getModel(), tlGlobalIdx);
		Node takerTl = findNode(takerSolution.getModel(), tlGlobalIdx);
		if (supplierTl == null || takerTl == null)
			return null;

		float costsBefore = totalQuality(solutions).getCost();

		int depotId = maxDepotId(solutions) + 1;
		// The copies come from the model of their own solution, so their node
		// indices fit the metric of the evaluated vehicle
		Node drop = Util.createIdNode(supplierTl, depotId++);
		drop.setDemands(demandArray(supplierSolution.getModel(), -amount));
		Node take = Util.createIdNode(takerTl, depotId);
		take.setDemands(demandArray(takerSolution.getModel(), amount));

		Node[] oldSupplierRoute = supplierRoute;
		Node[] oldTakerRoute = takerRoute;

		supplierSolution.setRoute(customerRouteIdx, insert(supplierRoute, customerPos + 1, drop));
		takerSolution.setRoute(takeRouteIdx, insert(takerRoute, takePos, take));

		Quality total = totalQuality(solutions);
		if (total.getPenalty() == 0 && total.getCost() <= costsBefore + maxCostIncrease) {
			return total;
		}

		supplierSolution.setRoute(customerRouteIdx, oldSupplierRoute);
		takerSolution.setRoute(takeRouteIdx, oldTakerRoute);
		return null;
	}

	/**
	 * Removes the record of the given drop visit (drop and take visit), even
	 * if they belong to different solutions. The removal is only kept, if the
	 * joint plan stays feasible and the joint costs decrease.
	 *
	 * @return true, if the record was removed
	 */
	public boolean removeRecord(List<Solution> solutions, int dropSolutionIdx, int dropRouteIdx, int dropPos) throws XFVRPException {
		Solution dropSolution = solutions.get(dropSolutionIdx);
		Node[] dropRoute = dropSolution.getRoutes()[dropRouteIdx];
		Node drop = dropRoute[dropPos];

		if (drop == null || drop.getSiteType() != SiteType.TRANSSHIPMENT || demandOf(drop) >= 0)
			return false;

		int[] takePos = findPairedTake(solutions, drop);
		if (takePos == null)
			return false;

		Solution takeSolution = solutions.get(takePos[0]);
		Node[] oldDropRoute = dropRoute;
		Node[] oldTakeRoute = takeSolution.getRoutes()[takePos[1]];

		// If the take visit is in the same route behind the drop visit, its
		// position shifts by the removal of the drop visit
		int takeRemovePos = takePos[2];
		if (dropSolutionIdx == takePos[0] && dropRouteIdx == takePos[1] && takeRemovePos > dropPos)
			takeRemovePos--;

		float costsBefore = totalQuality(solutions).getCost();

		dropSolution.setRoute(dropRouteIdx, remove(dropRoute, dropPos));
		takeSolution.setRoute(takePos[1], remove(oldTakeRoute, takeRemovePos));

		Quality total = totalQuality(solutions);
		if (total.getPenalty() == 0 && total.getCost() < costsBefore - EPSILON)
			return true;

		dropSolution.setRoute(dropRouteIdx, oldDropRoute);
		takeSolution.setRoute(takePos[1], oldTakeRoute);
		return false;
	}

	/**
	 * Evaluates all feasible record candidates and returns the best one
	 * (least joint costs). The candidates are ordered by the transferred
	 * amount, because the freed supplier capacity drives the consolidation of
	 * the following re-planning. The trials are reverted.
	 */
	private Candidate bestCandidate(List<Solution> solutions) throws XFVRPException {
		List<Candidate> candidates = generateCandidates(solutions);
		candidates.sort(Comparator.comparingDouble((Candidate c) -> c.amount).reversed());

		Candidate best = null;
		float bestCosts = Float.MAX_VALUE;

		int evaluations = 0;
		for (Candidate candidate : candidates) {
			if (evaluations++ >= MAX_CANDIDATES_PER_PASS)
				break;

			Quality total = addRecord(solutions, candidate.supplierSolutionIdx, candidate.customerRouteIdx,
					candidate.customerPos, candidate.tlGlobalIdx, candidate.takerSolutionIdx, candidate.takeRouteIdx,
					candidate.takePos, Float.MAX_VALUE);
			if (total == null)
				continue;

			if (total.getCost() < bestCosts) {
				bestCosts = total.getCost();
				best = candidate;
			}

			// Revert the trial, the best candidate is applied afterwards
			Solution supplier = solutions.get(candidate.supplierSolutionIdx);
			Solution taker = solutions.get(candidate.takerSolutionIdx);
			supplier.setRoute(candidate.customerRouteIdx,
					remove(supplier.getRoutes()[candidate.customerRouteIdx], candidate.customerPos + 1));
			taker.setRoute(candidate.takeRouteIdx,
					remove(taker.getRoutes()[candidate.takeRouteIdx], candidate.takePos));
		}

		return best;
	}

	/**
	 * Generates all inter-vehicle record candidates of the joint plan: for
	 * each customer of a supplier route, the nearest transshipment locations
	 * and the take positions with the least detour in each other solution.
	 */
	private List<Candidate> generateCandidates(List<Solution> solutions) {
		List<Candidate> candidates = new ArrayList<>();

		for (int supplierSolutionIdx = 0; supplierSolutionIdx < solutions.size(); supplierSolutionIdx++) {
			Solution supplierSolution = solutions.get(supplierSolutionIdx);
			List<Node> transshipments = findTransshipments(supplierSolution.getModel());

			for (int routeIdx = 0; routeIdx < supplierSolution.getRoutes().length; routeIdx++) {
				Node[] route = supplierSolution.getRoutes()[routeIdx];

				for (int pos = 1; pos < route.length - 1; pos++) {
					Node customer = route[pos];
					if (customer.getSiteType() != SiteType.CUSTOMER || customer.getDemand().length == 0)
						continue;

					float amount = customer.getDemand()[0];
					if (amount <= EPSILON)
						continue;

					for (Node tl : nearestTransshipments(customer, transshipments, supplierSolution.getModel())) {
						for (int takerSolutionIdx = 0; takerSolutionIdx < solutions.size(); takerSolutionIdx++) {
							if (takerSolutionIdx == supplierSolutionIdx)
								continue;

							int[] takePos = bestTakePosition(solutions.get(takerSolutionIdx), tl.getGlobalIdx());
							if (takePos == null)
								continue;

							candidates.add(new Candidate(
									supplierSolutionIdx, routeIdx, pos,
									tl.getGlobalIdx(),
									takerSolutionIdx, takePos[0], takePos[1], amount));
						}
					}
				}
			}
		}
		return candidates;
	}

	/**
	 * Returns the route index and position, where a take visit of the
	 * transshipment location adds the least detour.
	 */
	private int[] bestTakePosition(Solution takerSolution, int tlGlobalIdx) {
		int bestRouteIdx = -1;
		int bestPos = -1;
		float bestDelta = Float.MAX_VALUE;

		Node tl = findNode(takerSolution.getModel(), tlGlobalIdx);
		if (tl == null)
			return null;

		Node[][] routes = takerSolution.getRoutes();
		XFVRPModel model = takerSolution.getModel();
		for (int routeIdx = 0; routeIdx < routes.length; routeIdx++) {
			Node[] route = routes[routeIdx];
			if (route.length < 3)
				continue;

			// All positions from the first node to the final depot
			for (int pos = 1; pos < route.length; pos++) {
				float delta = model.getDistance(route[pos - 1], tl) + model.getDistance(tl, route[pos])
						- model.getDistance(route[pos - 1], route[pos]);
				if (delta < bestDelta) {
					bestDelta = delta;
					bestRouteIdx = routeIdx;
					bestPos = pos;
				}
			}
		}

		if (bestRouteIdx < 0)
			return null;
		return new int[]{bestRouteIdx, bestPos};
	}

	/**
	 * Removes all transshipment records, which can be removed without
	 * violating the synchronization and with lower joint costs. Records only
	 * free capacity, so their detours are not needed anymore, if the freed
	 * capacity was not used by the re-planning.
	 */
	private void removeUnprofitableRecords(List<Solution> solutions) throws XFVRPException {
		boolean removed = true;
		while (removed) {
			removed = false;

			for (int solutionIdx = 0; solutionIdx < solutions.size(); solutionIdx++) {
				for (int routeIdx = 0; routeIdx < solutions.get(solutionIdx).getRoutes().length; routeIdx++) {
					Node[] route = solutions.get(solutionIdx).getRoutes()[routeIdx];
					for (int pos = 1; pos < route.length - 1; pos++) {
						Node visit = route[pos];
						if (visit.getSiteType() != SiteType.TRANSSHIPMENT || demandOf(visit) >= 0)
							continue;

						if (removeRecord(solutions, solutionIdx, routeIdx, pos)) {
							removed = true;
							break;
						}
					}
					if (removed) break;
				}
				if (removed) break;
			}
		}
	}

	/**
	 * Re-optimizes the given solution with the VRPTT ILS, which evaluates the
	 * synchronization constraints over the whole joint plan. The optimized
	 * routes are written back to the solution of the joint plan.
	 */
	private void replan(Solution solution, List<Solution> all, StatusManager statusManager) throws XFVRPException {
		if (solution.getModel() == null)
			return;

		List<Solution> others = new ArrayList<>();
		for (Solution s : all)
			if (s != solution)
				others.add(s);

		Node[][] contextRoutes = contextRoutes(otherSolutions(all, solution));
		XFVRPModel[] contextModels = contextModels(otherSolutions(all, solution));

		VRPTTILS ils = new VRPTTILS();
		ils.setRecordContext(contextRoutes, contextModels);
		Solution optimized = ils.execute(solution, solution.getModel(), statusManager);

		// The ILS optimizes a copy of the solution, so the routes are
		// written back to the solution of the joint plan
		Node[][] optimizedRoutes = optimized.getRoutes();
		for (int i = 0; i < Math.min(optimizedRoutes.length, solution.getRoutes().length); i++)
			solution.setRoute(i, optimizedRoutes[i]);
	}

	/**
	 * Evaluates all solutions of the joint plan with the other solutions as
	 * record context and returns the total quality.
	 */
	private Quality totalQuality(List<Solution> solutions) throws XFVRPException {
		Quality total = new Quality(null);
		for (Solution solution : solutions)
			total.add(evaluationService.check(solution, contextRoutes(otherSolutions(solutions, solution)),
					contextModels(otherSolutions(solutions, solution))));
		return total;
	}

	private List<Solution> otherSolutions(List<Solution> all, Solution own) {
		List<Solution> others = new ArrayList<>();
		for (Solution s : all)
			if (s != own)
				others.add(s);
		return others;
	}

	private Node[][] contextRoutes(List<Solution> others) {
		List<Node[]> routes = new ArrayList<>();
		for (Solution s : others)
			for (Node[] route : s.getRoutes())
				if (route.length > 0)
					routes.add(route);
		return routes.toArray(new Node[0][]);
	}

	private XFVRPModel[] contextModels(List<Solution> others) {
		List<XFVRPModel> models = new ArrayList<>();
		for (Solution s : others)
			for (int j = 0; j < s.getRoutes().length; j++)
				models.add(s.getModel());
		return models.toArray(new XFVRPModel[0]);
	}

	/**
	 * Searches the take visit of the given drop visit over all solutions of
	 * the joint plan.
	 *
	 * @return array with solution index, route index and visit position or null
	 */
	private int[] findPairedTake(List<Solution> solutions, Node drop) {
		float takeAmount = -demandOf(drop);

		for (int solutionIdx = 0; solutionIdx < solutions.size(); solutionIdx++) {
			Node[][] routes = solutions.get(solutionIdx).getRoutes();
			for (int routeIdx = 0; routeIdx < routes.length; routeIdx++) {
				for (int pos = 1; pos < routes[routeIdx].length - 1; pos++) {
					Node visit = routes[routeIdx][pos];

					if (visit.getSiteType() != SiteType.TRANSSHIPMENT)
						continue;

					if (visit.getGlobalIdx() != drop.getGlobalIdx())
						continue;

					if (Math.abs(demandOf(visit) - takeAmount) > EPSILON)
						continue;

					return new int[]{solutionIdx, routeIdx, pos};
				}
			}
		}
		return null;
	}

	private float demandOf(Node node) {
		return (node.getDemand().length > 0) ? node.getDemand()[0] : 0;
	}

	private List<Node> findTransshipments(XFVRPModel model) {
		List<Node> transshipments = new ArrayList<>();
		for (Node node : model.getNodes())
			if (node.getSiteType() == SiteType.TRANSSHIPMENT)
				transshipments.add(node);
		return transshipments;
	}

	private List<Node> nearestTransshipments(Node customer, List<Node> transshipments, XFVRPModel model) {
		List<Node> sorted = new ArrayList<>(transshipments);
		sorted.sort((a, b) -> Float.compare(model.getDistance(customer, a), model.getDistance(customer, b)));
		return sorted.subList(0, Math.min(MAX_TL_CANDIDATES, sorted.size()));
	}

	private Node findNode(XFVRPModel model, int globalIdx) {
		for (Node node : model.getNodes())
			if (node.getGlobalIdx() == globalIdx)
				return node;
		return null;
	}

	private float[] demandArray(XFVRPModel model, float amount) {
		float[] demand = new float[Math.max(1, model.getCompartments().length)];
		demand[0] = amount;
		return demand;
	}

	private int maxDepotId(List<Solution> solutions) {
		int maxId = 0;
		for (Solution solution : solutions)
			for (Node[] route : solution.getRoutes())
				for (Node node : route)
					maxId = Math.max(maxId, node.getDepotId());
		return maxId;
	}

	private List<Solution> copies(List<Solution> solutions) {
		List<Solution> copies = new ArrayList<>();
		for (Solution solution : solutions)
			copies.add(solution.copy());
		return copies;
	}

	private void restore(List<Solution> solutions, List<Solution> backups) {
		for (int i = 0; i < solutions.size(); i++) {
			Node[][] routes = solutions.get(i).getRoutes();
			Node[][] backupRoutes = backups.get(i).getRoutes();
			for (int j = 0; j < routes.length; j++)
				solutions.get(i).setRoute(j, backupRoutes[j]);
		}
	}

	private Node[] insert(Node[] route, int pos, Node node) {
		Node[] newRoute = new Node[route.length + 1];
		System.arraycopy(route, 0, newRoute, 0, pos);
		newRoute[pos] = node;
		System.arraycopy(route, pos, newRoute, pos + 1, route.length - pos);
		return newRoute;
	}

	private Node[] remove(Node[] route, int pos) {
		Node[] newRoute = new Node[route.length - 1];
		System.arraycopy(route, 0, newRoute, 0, pos);
		System.arraycopy(route, pos + 1, newRoute, pos, route.length - pos - 1);
		return newRoute;
	}

	/**
	 * A candidate inter-vehicle record: the customer of the supplier solution
	 * hands its collected amount at a transshipment location over to the
	 * taker solution.
	 */
	private static class Candidate {
		final int supplierSolutionIdx;
		final int customerRouteIdx;
		final int customerPos;
		final int tlGlobalIdx;
		final int takerSolutionIdx;
		final int takeRouteIdx;
		final int takePos;
		final float amount;

		Candidate(int supplierSolutionIdx, int customerRouteIdx, int customerPos,
		          int tlGlobalIdx, int takerSolutionIdx, int takeRouteIdx, int takePos, float amount) {
			this.supplierSolutionIdx = supplierSolutionIdx;
			this.customerRouteIdx = customerRouteIdx;
			this.customerPos = customerPos;
			this.tlGlobalIdx = tlGlobalIdx;
			this.takerSolutionIdx = takerSolutionIdx;
			this.takeRouteIdx = takeRouteIdx;
			this.takePos = takePos;
			this.amount = amount;
		}
	}
}
