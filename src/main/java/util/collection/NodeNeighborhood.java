package util.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class NodeNeighborhood {

	private Map<Node, TreeSet<JobDistance>> neighborhoods;
	private List<Node> customers;
	private XFVRPModel model;

	/**
	 * 
	 * @param model
	 * @param giantTour
	 */
	public NodeNeighborhood(XFVRPModel model) {
		this.model = model;
		this.neighborhoods = new HashMap<>();
		this.customers = new ArrayList<>();

		for (int i = model.getNbrOfDepots() + model.getNbrOfReplenish(); i < model.getNbrOfNodes(); i++)
			customers.add(model.getNodeArr()[i]);

		calculateDistancesFromJob2Job();
	}

	/**
	 * 
	 * @param neighborTo
	 * @param nbrOfNeighbors
	 * @return
	 */
	public Iterator<Node> getNeighborIterator(Node neighborTo, int nbrOfNeighbors) {
		return new NeighborhoodIterator(neighborhoods.get(neighborTo).iterator(), nbrOfNeighbors);
	}

	/**
	 * 
	 */
	private void calculateDistancesFromJob2Job() {
		customers.forEach(p -> {
			TreeSet<JobDistance> tree = new TreeSet<>((o1, o2) -> Double.compare(o1.getDistance(), o2.getDistance()));
			customers.forEach(q -> {
				double distance = model.getDistanceForOptimization(p, q);
				tree.add(new JobDistance(q, distance));
			});
			neighborhoods.put(p, tree);
		});    
	}

	class NeighborhoodIterator implements Iterator<Node> {
		private int nNodes;
		private int count = 0;

		private Iterator<JobDistance> iterator;

		public NeighborhoodIterator(Iterator<JobDistance> iterator, int nNodes) {
			this.iterator = iterator;
			this.nNodes = nNodes;
		}

		@Override
		public boolean hasNext() {
			if (count < nNodes){
				boolean hasNext = iterator.hasNext();				
				return hasNext;
			}
			return false;
		}

		@Override
		public Node next() {
			count++;
			JobDistance next = iterator.next();
			return next.node;
		}
	}

	class JobDistance {		
		Node node;
		double distance;

		public JobDistance(Node node, double distance) {
			this.node = node;
			this.distance = distance;
		}
		public Node getNode() {
			return node;
		}
		public void setNode(Node node) {
			this.node = node;
		}
		public double getDistance() {
			return distance;
		}
		public void setDistance(double distance) {
			this.distance = distance;
		}
	}
}
