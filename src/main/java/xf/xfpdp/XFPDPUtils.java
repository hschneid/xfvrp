package xf.xfpdp;

import java.util.Arrays;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

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
	
	/**
	 * 
	 * @param giantRoute
	 * @param srcA
	 * @param srcB
	 * @param dstA
	 * @param dstB
	 */
	public static void move2(Node[] giantRoute, int srcA, int srcB, int dstA, int dstB) {
		Node nSrcA = giantRoute[srcA];
		Node nSrcB = giantRoute[srcB];
		
		Node[] arr = new Node[giantRoute.length];
		System.arraycopy(giantRoute, 0, arr, 0, giantRoute.length);
		
		int j = 0;
		for (int i = 0; i < giantRoute.length; i++) {
			// Es wird vor einem Knoten i eingefügt.
			if(i == dstA)
				giantRoute[j++] = nSrcA;
			
			// dstA und dstB k�nnen den gleichen Index haben, wenn A vor B direkt
			// eingef�gt werden soll.
			if(i == dstB)
				giantRoute[j++] = nSrcB;
			
			// Packe einen Knoten nur dann zurück in giant route, 
			// falls der Index nicht auf einem Source-Knoten liegt.
			if(i != srcA && i != srcB) 
				giantRoute[j++] = arr[i];
			
		}
	}
	
	public static void testPDPPerRouteConstraint(Node[] route) {
		int[][] shipArr = new int[route.length][2];
		for (int i = 0; i < shipArr.length; i++)
			Arrays.fill(shipArr[i], -1);
		
		
		int[] routeIdxArr = new int[route.length];
		int id = 0;
		for (int i = 1; i < route.length; i++) {
			if(route[i].getSiteType() == SiteType.DEPOT)
				id++;
			routeIdxArr[i] = id;
		}
		
		for (int i = 0; i < route.length; i++) {
			if(route[i].getSiteType() == SiteType.DEPOT)
				continue;
			
			if(route[i].getDemand()[0] > 0) {
				shipArr[route[i].getShipmentIdx()][0] = i;
			}
			if(route[i].getDemand()[0] < 0) {
				shipArr[route[i].getShipmentIdx()][1] = i;
			}
		}
		
		for (int i = 0; i < shipArr.length; i++) {
			if(shipArr[i][0] == -1 && shipArr[i][1] == -1)
				continue;
			if(shipArr[i][0] == -1 || shipArr[i][1] == -1) {
				System.out.println("Error in route NO_PAIR");
				continue;
			}
			
			int s0TIdx = routeIdxArr[shipArr[i][0]];
			int s1TIdx = routeIdxArr[shipArr[i][1]];
			
			if(s0TIdx != s1TIdx)
				System.out.println("Error in route DIFFERENT_ROUTES");
		}
	}
}
