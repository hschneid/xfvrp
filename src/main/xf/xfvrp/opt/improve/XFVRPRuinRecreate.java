package xf.xfvrp.opt.improve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import util.collection.GiantRouteIterator;
import util.collection.NodeNeighborhood;
import util.collection.Route;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.opt.XFVRPOptBase;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class XFVRPRuinRecreate extends XFVRPOptBase {

	private final double ruinFraction = 0.01;
	//	OpenIntMap<Node> nodeIndices;

	private NodeNeighborhood metricNeighborhood;
	private List<Node> customers = new ArrayList<>();

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.XFVRPBase#execute(de.fhg.iml.vlog.xfvrp.base.Node[])
	 */
	@Override
	protected Node[] execute(Node[] giantRoute) {
		init();

		Node[] bestRoute = giantRoute;
		Quality bestQuality = check(giantRoute);

		statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" is starting with "+model.getParameter().getILSLoops()+" loops and ruin fraction " + (ruinFraction * 100f) + "%.");
		statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" starts with solution "+bestQuality);

		for (int i = 0; i < model.getParameter().getILSLoops(); i++) {
			// Select customers for removal
			Set<Node> unassigned = radialRuin();
			// Remove selected customers from solution
			List<Node> ruinedRoute = removeFromRoute(bestRoute, unassigned);
			// Insert customers per FirstBestInsert 
			Node[] recreatedRoute = bestInsertion(ruinedRoute, unassigned);

			Quality qNew = check(recreatedRoute);

			if (qNew.getPenalty() == 0)
				if (qNew.getCost() < bestQuality.getCost()){
					statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" loop "+i+"\t last cost : "+bestQuality.getCost()+"\t new cost : "+qNew.getCost());

					bestQuality = qNew;
					bestRoute = Util.normalizeRoute(recreatedRoute, model);
				}
		}
		return bestRoute;
	}

	/**
	 * 
	 */
	public void init() {
		// Build up the metric neighborhood of each customer
		// c -> {nearest neighbor, second nearest neighbor, ...}
		metricNeighborhood = new NodeNeighborhood(model);

		for (int i = model.getNbrOfDepots()+model.getNbrOfReplenish(); i < model.getNbrOfNodes(); i++)
			customers.add(model.getNodeArr()[i]);
	}

	/**
	 * 
	 * @return
	 */
	protected Set<Node> radialRuin() {
		Collections.shuffle(customers, rand);

		// Number of bombing actions and bombing radius
		int ruinCount = (int)(ruinFraction * customers.size());

		// Collect the bombed customers of a ruin region
		Set<Node> unassigned = new HashSet<>();
		for (int i = 0; i < ruinCount; i++) {
			Iterator<Node> it = metricNeighborhood.getNeighborIterator(customers.get(i), ruinCount - 1);
			while (it.hasNext())
				unassigned.add(it.next());			
		}		

		return unassigned;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param unassigned
	 * @return
	 */
	private List<Node> removeFromRoute(Node[] giantRoute, Set<Node> unassigned) {
		List<Node> al = new ArrayList<>();
		for (int i = 0; i < giantRoute.length; i++)
			if (!unassigned.contains(giantRoute[i])) al.add(giantRoute[i]);
		return al;
	}

	/**
	 * 
	 * @param ruinedGiantRoute
	 * @param unassigned
	 * @return
	 */
	private Node[] bestInsertion(List<Node> ruinedGiantRoute, Set<Node> unassigned) {
		List<Node> badJobs = new ArrayList<>();
		List<Node> alUnassigned = new ArrayList<>(unassigned);
		Collections.shuffle(alUnassigned, rand);

		GiantRouteIterator routeIt = new GiantRouteIterator();
		alUnassigned.forEach(unassignedNode -> {
			int bestInsPos = -1;
			float bestInsCosts = Float.MAX_VALUE;

			routeIt.init(ruinedGiantRoute);
			while (routeIt.hasNext()) {
				Route route = routeIt.next();
				float[] best = calcBestInsertPoint(route, unassignedNode);

				if (best != null) {
					if ((bestInsPos == -1 || best[1] < bestInsCosts)) {
						bestInsPos =  route.getRouteStart() + (int)best[0];
						bestInsCosts = best[1];
					}
				}
			}

			if (bestInsPos == -1)
				badJobs.add(unassignedNode); // TODO: badjobs handling -> z.B. wenn Stop nicht mal in Stichstrecke erreichbar
			else
				ruinedGiantRoute.add(bestInsPos, unassignedNode);
		});

		return ruinedGiantRoute.toArray(new Node[0]);
	}

	/**
	 * 
	 * @param route
	 * @param insert
	 * @return
	 */
	private float[] calcBestInsertPoint(Route route, Node unassignedNode) {
		List<Node> nodes = route.getNodes();
		List<float[]> list = new ArrayList<>();

		for (int i = 1; i < nodes.size(); i++) {
			float addCosts = 0;
			addCosts += model.getDistanceForOptimization(nodes.get(i-1),unassignedNode);
			addCosts += model.getDistanceForOptimization(unassignedNode, nodes.get(i));
			addCosts -= model.getDistanceForOptimization(nodes.get(i-1), nodes.get(i));
			list.add(new float[]{i, addCosts});
		}
		list.sort((a,b) -> {
			return (int)((a[1] - b[1])*1000f); 
		});		

		for (int i = 0; i < list.size(); i++) {
			float[] p = list.get(i);

			// Insert in list
			nodes.add((int)p[0], unassignedNode);

			// Evaluate the insertion

			Quality q = check(nodes.toArray(new Node[0]));
			if (q.getPenalty() == 0)
				return p;

			// If not feasable, delete the insertion 
			nodes.remove((int)p[0]);
		}
		return null;
	}
}
