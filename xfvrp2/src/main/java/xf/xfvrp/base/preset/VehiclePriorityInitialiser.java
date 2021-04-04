package xf.xfvrp.base.preset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import xf.xfvrp.base.Vehicle;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class VehiclePriorityInitialiser {

	/**
	 * A vehicle may have a given priority from user for the ordering in
	 * mixed fleet algorithm. This method checks and orders the priority
	 * of the vehicles. 
	 * 
	 * @param vehicles The imported vehicles
	 * @return List of vehicles with right ordering by priorities
	 */
	public static Vehicle[] execute(Vehicle[] vehicles) {
		// Split vehicles into 2 list with and without given priorities
		
		// With Priorities (sorted ascending by priority)
		List<Vehicle> withPrios = Arrays
			.stream(vehicles)
			.filter(v -> (v.priority != Vehicle.PRIORITY_UNDEF))
			.sorted((a, b) -> (a.priority - b.priority))
			.collect(Collectors.toList());
		// Get the highest given priority value from user
		// ( all automatically filled priorities must be greater than this value) 
		int maxPresetPriority = 0;
		if(withPrios.size() > 0) 
			maxPresetPriority = withPrios.stream().mapToInt(v -> v.priority).max().getAsInt();
		
		// Without priorities (sorted descend by capacity)
		List<Vehicle> withoutPrios = Arrays
				.stream(vehicles)
				.filter(v -> (v.priority == Vehicle.PRIORITY_UNDEF))
				.sorted((a, b) -> {
					int c = 0;
					for (int i = 0; i < b.capacity.length; i++)
						c += b.capacity[i] - a.capacity[i];
					return c * 1000;	
				})
				.collect(Collectors.toList());
		// Set the automatically filled priority
		for (Vehicle v : withoutPrios)
			v.priority = maxPresetPriority++;
		
		List<Vehicle> newList = new ArrayList<>();
		newList.addAll(withPrios);
		newList.addAll(withoutPrios);
		return newList.toArray(new Vehicle[0]);
	}

}
