package xf.xfvrp.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.opt.Solution;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This summary lists all errors in the route plan. Invalid nodes
 * are in the route plan on individual routes with an invalid vehicle type.
 * 
 * Each invalid node has an reason, which is detected in the XFVRP init method. So
 * for each invalid node with an invalid description, this is listed in the error 
 * descriptions. In the statistics for each invalid reason the number of invalid nodes
 * is counted.
 * 
 * @author hschneid
 *
 */
public class ErrorSummary {

	private final List<String> errorDescriptions = new ArrayList<>();
	private final Map<String, Integer> statistics = new HashMap<>();
	
	/**
	 * Adds the errors in an evaluated giant route. Evaluated route means,
	 * that the route is build up by the init routine in the XFVRPInit class.
	 * 
	 * @param route giant route with potential invalid customers.
	 */
	public void add(Solution solution) {
		Node[] route = solution.getGiantRoute();
		for (Node node : route) {
			if(node != null && node.getInvalidReason() != InvalidReason.NONE) {
				// Description
				if(node.getInvalidArguments().length() > 0)
					errorDescriptions.add(node.getInvalidArguments());
				
				// Statistics
				String invalidReason = node.getInvalidReason().toString();
				
				if(!statistics.containsKey(invalidReason))
					statistics.put(invalidReason, 0);
					
				statistics.put(
						invalidReason,
						statistics.get(invalidReason) + 1
						);
			}
		}
	}
	
	/**
	 * If multiple report are joined, the ErrorSummary objects must be joined too.
	 * 
	 * This is done in this method. The given ErrorSummary will be added to this object.
	 * 
	 * @param errors ErrorSummary of foreign Report
	 */
	public void importErrors(ErrorSummary errors) {
		errorDescriptions.addAll(errors.getErrorDescriptions());
		
		for (String invalidReason : errors.getStatistics().keySet()) {
			if(!statistics.containsKey(invalidReason))
				statistics.put(invalidReason, 0);
				
			statistics.put(
					invalidReason,
					statistics.get(invalidReason) + errors.getStatistics().get(invalidReason)
					);
		}
	}

	/**
	 * 
	 * @return Counted invalid customers for each invalid reason
	 */
	public Map<String, Integer> getStatistics() {
		return statistics;
	}
	
	/**
	 * 
	 * @return List of error descriptions, if the invalid reason allows a description.
	 */
	public List<String> getErrorDescriptions() {
		return errorDescriptions;
	}
}
