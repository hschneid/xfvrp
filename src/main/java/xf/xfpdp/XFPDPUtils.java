package xf.xfpdp;

import xf.xfvrp.base.Node;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class XFPDPUtils {
	
	
	/**
	 * 
	 * @param giantRoute
	 * @param srcA
	 * @param srcB
	 * @param dstA
	 * @param dstB
	 */
	public static void move(Node[] giantRoute, int srcA, int srcB, int dstA, int dstB) {
		Node nSrcA = giantRoute[srcA];
		Node nSrcB = giantRoute[srcB];
		
		Node[] arr = new Node[giantRoute.length];
		System.arraycopy(giantRoute, 0, arr, 0, giantRoute.length);
		
		int j = 0;
		for (int i = 0; i < giantRoute.length; i++) {
			// Packe einen Knoten nur dann zurück in giant route, 
			// falls der Index nicht auf einem Source-Knoten liegt.
			if(i != srcA && i != srcB) 
				giantRoute[j++] = arr[i];
			
			// Es wird nach einem Knoten i eingefügt.
			if(i == dstA)
				giantRoute[j++] = nSrcA;
			// dstA und dstB k�nnen den gleichen Index haben, wenn A vor B direkt
			// eingef�gt werden soll.
			if(i == dstB)
				giantRoute[j++] = nSrcB;
		}
	}
}
