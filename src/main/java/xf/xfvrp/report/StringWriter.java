package xf.xfvrp.report;

import xf.xfvrp.base.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * String Writer converts a report object into a text representation, where
 * the format is dedicated to technical readers for post processing.
 * 
 * @author hschneid
 *
 */
public class StringWriter {

	/**
	 * Converts a report object into a string by listing
	 * the values in the events.
	 * 
	 * @param report Current report
	 * @return String representation
	 */
	public static String write(Report report) {
		StringBuilder sb = new StringBuilder();

		Node[] nodes = report.getModel().getNodes();
		Map<String, Integer> blockMap = new HashMap<>();
		Map<String, Integer> posMap = new HashMap<>();
		for (Node n : nodes) {
			blockMap.put(n.getExternID(), n.getPresetBlockIdx());
			posMap.put(n.getExternID(), n.getPresetBlockPos());
		}
		
		ReportSummary sum = report.getSummary();
		sb.append("#; DISTANCE; TOUR_COUNT; DELAY; OVERLOAD\n");
		sb.append("-; "+sum.getDistance()+"; "+sum.getNbrOfUsedVehicles()+"; "+sum.getDelay()+"; "+Arrays.toString(sum.getOverloads())+"\n");
		for (int i = 0; i < report.getRoutes().size(); i++) {
			RouteReport tRep = report.getRoutes().get(i);
			RouteReportSummary tSum = tRep.getSummary();
			sb.append("#; TOUR_ID; VEHICLE; CUSTOMER_COUNT; DISTANCE; DELAY; PICKUP; DELIVERY\n");
			sb.append("=; "+(i+1)+"; "+ tRep.getVehicle().getName() +"; "+tSum.getNbrOfEvents()+"; "+tSum.getDistance()+"; "+tSum.getDelay()+"; "+Arrays.toString(tSum.getPickups())+"; "+Arrays.toString(tSum.getDeliveries())+"\n");
			sb.append("# STOP_ID; ID; DISTANCE; AMOUNT; ARRIVAL; DEPARTURE; SERVICE; WAITING; TYPE\n");

			for (int j = 0; j < tRep.getEvents().size(); j++) {
				Event e = tRep.getEvents().get(j);
				sb.append(
						"+; "+(j+1)+"; "+e.getID()+"; "+e.getDistance()+"; "+ Arrays.toString(e.getAmounts())+"; "+
								e.getArrival()+"; "+e.getDeparture()+"; "+e.getService()+"; "+e.getWaiting()+"; "+e.getLoadType()+"; "+blockMap.get(e.getID())+"; "+posMap.get(e.getID())+"\n"
				);
			}
		}
		
		return sb.toString().replaceAll("\\.", ",");
	}
}
