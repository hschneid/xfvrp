package xf.xfvrp.opt;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPBase;
import xf.xfvrp.base.XFVRPModel;


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
	private EvaluationService check = new EvaluationService();

	/**
	 * Inverts the node sequence in the range
	 * from start to end, both inclusive. 
	 * 
	 * @param giantRoute node sequence
	 * @param start starting index of range (incl.)
	 * @param end ending index of range (incl.)
	 */
	protected void swap(Solution solution, int start, int end) {
		Node[] giantTour = solution.getGiantRoute();
		
		int offset = 0;
		while (end - offset > start + offset) {
			Node tmp = giantTour[end - offset];
			giantTour[end - offset] = giantTour[start + offset];
			giantTour[start + offset] = tmp;
			offset++;
		}
		
		solution.setGiantRoute(giantTour);
	}

	/**
	 * Moves the src-node from its position in giant tour
	 * to before the dst-node.
	 * 
	 * @param giantRoute node sequence
	 * @param src index of the node, which will be moved.
	 * @param dst index of the node, where src-node will be inserted before.
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
	 * 
	 * @param giantRoute node sequence
	 * @param srcStart index of the node, which is the starting point of the segment, which will be moved.
	 * @param srcEnd index of the node, which is the ending point of the segment, which will be moved.
	 * @param dst index of the node, where the segment will be inserted before.
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
	 * 
	 * @param giantRoute node sequence
	 * @param i index of the node, which will be moved to position j.
	 * @param j index of the node, which will be moved to position i.
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
	 * 
	 * @param giantRoute node sequence
	 * @param a index of the first node in first segment
	 * @param b index of the first node in second segment
	 * @param la number of nodes in first segment
	 * @param lb number of nodes in second segment
	 */
	protected void exchange(Solution solution, int a, int b, int la, int lb) {
		if(((a < b) && (a + la - 1) >= b) || ((b < a) && (b + lb - 1) >= a))
			throw new IllegalStateException("Segements are overlapping.");
		
		if(la == lb) {
			for (int i = 0; i < la; i++)
				exchange(solution, a+i, b+i);
		} else {
			Node[] giantTour = solution.getGiantRoute();
			
			if(b < a) {
				int tmp = a; a = b; b = tmp;
				tmp = la; la = lb; lb = tmp;
			}
			
			Node[] aArr = new Node[la];
			System.arraycopy(giantTour, a , aArr, 0, aArr.length);
			Node[] bArr = new Node[lb];
			System.arraycopy(giantTour, b , bArr, 0, bArr.length);
			Node[] iArr = new Node[b - (a + la)];
			System.arraycopy(giantTour, a + la , iArr, 0, b - (a + la));

			System.arraycopy(bArr, 0 , giantTour, a, bArr.length);
			System.arraycopy(iArr, 0 , giantTour, a + bArr.length, iArr.length);
			System.arraycopy(aArr, 0 , giantTour, a + bArr.length + iArr.length, aArr.length);
			
			solution.setGiantRoute(giantTour);
		}
	}

	/**
	 * Internal method of all improvment heuristics to bring to list of improving steps
	 * in the rigth ordering. 
	 * 
	 * @param list A list of improving steps, where each step is indicated by an float array. A certain position in the array has to contain the amount of improvement.
	 * @param position The position of the improving amount of a step in the improving step list.
	 */
	protected void sort(List<float[]> list, final int position) {
		// Sortier absteigend nach Potenzial
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
	 * 
	 * @param giantRoute sequence of nodes
	 * @return
	 */
	public Quality check(Solution solution) {		
		return check.check(solution, model);
	}

	/**
	 * Returns the distance between two given XFNodes
	 * 
	 * @param n1
	 * @param n2
	 * @return
	 */
	protected float getDistanceForOptimization(Node n1, Node n2) {
		return model.getDistanceForOptimization(n1, n2);
	}
}
