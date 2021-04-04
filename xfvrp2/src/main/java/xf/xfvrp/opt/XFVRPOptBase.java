package xf.xfvrp.opt;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPBase;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.evaluation.EvaluationService;


/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This class contains utility methods for all extending classes.
 * 
 * There are methods for changing a solution (giant tour)
 * like move, swap or relocate and evaluating a solution by
 * simulating the route plan node by node (Lineare evaluation time).
 * 
 * All classes inheriting this class have access to an instance of XFVRPModel.
 * 
 * @author hschneid
 * 
 */
public abstract class XFVRPOptBase extends XFVRPBase<XFVRPModel> {

	public boolean isSplittable = false;
	protected Random rand = new Random(1234);
	protected static final float epsilon = 0.001f;
	protected EvaluationService evaluationService = new EvaluationService();

	/**
	 * 
	 * Inverts the node sequence in the range
	 * from start to end, both inclusive. 
	 */
	protected void swap(Solution solution, int start, int end) {
		Node[] giantTour = solution.getGiantRoute();
		
		swap(giantTour, start, end);
		
		solution.setGiantRoute(giantTour);
	}
	
	protected void swap(Node[] nodes, int start, int end) {
		int offset = 0;
		while (end - offset > start + offset) {
			Node tmp = nodes[end - offset];
			nodes[end - offset] = nodes[start + offset];
			nodes[start + offset] = tmp;
			offset++;
		}
	}

	/**
	 * Moves the src-node from its position in giant tour
	 * to before the dst-node.
	 */
	protected void move(Solution solution, int src, int dst) {
		Node[] giantTour = solution.getGiantRoute();
		
		// If src is equal to dst, than nothing can be done.
		if(src == dst)
			return;

		Node[] arr = new Node[1];
		arr[0] = giantTour[src];

		if(dst < src) {
			System.arraycopy(giantTour, dst, giantTour, dst + 1, src - dst);
			System.arraycopy(arr, 0, giantTour, dst, arr.length);
		} else {
			// Verschiebe den Block zwischen src und dst um eins nach vorne
			// um die L�cke zu stopfen, die sich durch das Entfernen von src
			// �ffnet.
			System.arraycopy(giantTour, src + 1, giantTour, src, dst - src);
			System.arraycopy(arr, 0, giantTour, dst - 1, arr.length);
		}
		
		solution.setGiantRoute(giantTour);
	}

	/**
	 * Moves the nodes in the range from srcStart and srcEnd, both inclusive, 
	 * before the position dst. 
	 */
	protected void pathMove(Solution solution, int srcStart, int srcEnd, int dst) {
		if(srcEnd < srcStart)
			throw new IllegalStateException();
		
		Node[] giantTour = solution.getGiantRoute();
		
		Node[] arr = new Node[srcEnd - srcStart + 1];
		System.arraycopy(giantTour, srcStart, arr, 0, arr.length);

		if(srcStart < dst) {
			System.arraycopy(giantTour, srcEnd + 1, giantTour, srcStart, dst - srcEnd);
			System.arraycopy(arr, 0, giantTour, dst - ((srcEnd - srcStart) + 1), arr.length);
		} else {
			System.arraycopy(giantTour, dst, giantTour, dst + (srcEnd - srcStart) + 1, srcStart - dst);
			System.arraycopy(arr, 0, giantTour, dst, arr.length);
		}
		
		solution.setGiantRoute(giantTour);
	}

	/**
	 * Exchanges the node positions of node i and j with
	 * their counterparts.
	 */
	protected void exchange(Solution solution, int i, int j) {
		Node[] giantTour = solution.getGiantRoute();
		
		Node tmp = giantTour[i];
		giantTour[i] = giantTour[j];
		giantTour[j] = tmp;
		
		solution.setGiantRoute(giantTour);
	}

	/**
	 * Exchanges two segments of the giant tour. First segment
	 * starts at position a and includes la many nodes. Second segments
	 * starts at position b and includes lb many nodes. The two
	 * segments must not overlap each other.
	 */
	protected void exchange(Solution solution, int a, int b, int la, int lb) {
		if(((a < b) && (a + la) >= b) || ((b < a) && (b + lb) >= a))
			throw new IllegalStateException("Segements are overlapping.");
		
		if(la == lb) {
			for (int i = 0; i <= la; i++)
				exchange(solution, a+i, b+i);
		} else {
			Node[] giantTour = solution.getGiantRoute();
			
			if(b < a) {
				int tmp = a; a = b; b = tmp;
				tmp = la; la = lb; lb = tmp;
			}
			
			Node[] nodesOfA = new Node[la + 1];
			System.arraycopy(giantTour, a , nodesOfA, 0, nodesOfA.length);
			Node[] nodesOfB = new Node[lb + 1];
			System.arraycopy(giantTour, b , nodesOfB, 0, nodesOfB.length);
			Node[] intermediates = new Node[b - (a + la + 1)];
			System.arraycopy(giantTour, a + la + 1, intermediates, 0, intermediates.length);

			System.arraycopy(nodesOfB, 0 , giantTour, a, nodesOfB.length);
			System.arraycopy(intermediates, 0 , giantTour, a + nodesOfB.length, intermediates.length);
			System.arraycopy(nodesOfA, 0 , giantTour, a + nodesOfB.length + intermediates.length, nodesOfA.length);
			
			solution.setGiantRoute(giantTour);
		}
	}
	
	protected void shipmentMove(Solution solution, int srcShipmentPickup, int srcShipmentDelivery, int dstShipmentPickup, int dstShipmentDelivery) {
		move(solution, srcShipmentPickup, dstShipmentPickup);
		
		if(dstShipmentPickup > srcShipmentDelivery) srcShipmentDelivery--;
		if(dstShipmentPickup < srcShipmentPickup && dstShipmentDelivery < srcShipmentPickup) dstShipmentDelivery++;
		
		move(solution, srcShipmentDelivery, dstShipmentDelivery);
	}

	/**
	 * Internal method of all improvment heuristics to bring to list of improving steps
	 * in the rigth ordering. 
	 */
	protected void sort(List<float[]> list, final int position) {
		// Sort descending for potential
		Collections.sort(list, 
				(o1, o2) -> {
					if(o1[position] > o2[position]) return -1;
					if(o1[position] < o2[position]) return 1;
					return 0;
				}
		);
	}

	/**
	 * Processes a check evaluation.
	 */
	public Quality check(Solution solution) {	
		return evaluationService.check(solution, model);
	}
	
	public Random getRandom() {
		return rand;
	}

	/**
	 * Returns the distance between two given XFNodes
	 */
	protected float getDistanceForOptimization(Node n1, Node n2) {
		return model.getDistanceForOptimization(n1, n2);
	}
}
