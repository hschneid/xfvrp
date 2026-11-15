package xf.xfvrp.opt.construct;

import xf.xfvrp.base.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Service-mode enumeration for the VRP with Trailers and Transshipments
 * (VRPTT), spec §2.1. For each trailer customer, the possible ways of
 * service are enumerated as derived tasks:
 * <ul>
 * <li>DIRECT(c) - direct visit by a lorry (optionally with its trailer),</li>
 * <li>VIA_TDP(c, t) - visit of a transshipment location t followed by a
 * visit of customer c, for each candidate transshipment location.</li>
 * </ul>
 * <p>
 * In Phase 1 (MVP) the candidate set of transshipment locations is fixed:
 * all transshipment locations of the model are candidates for each customer.
 * Lorry customers (which are not trailer accessible) are served directly
 * only by a vehicle type, which pulls no trailer (preset vehicle block
 * mechanism).
 *
 * @author hschneid
 */
public class VRPTTTaskEnumerator {

	/**
	 * Enumerates all ways of service for the given customer.
	 *
	 * @param customer       The customer to serve
	 * @param transshipments Candidate transshipment locations
	 * @return One task per way of service (first task is the direct mode)
	 */
	public List<VRPTTTask> enumerate(Node customer, List<Node> transshipments) {
		List<VRPTTTask> tasks = new ArrayList<>();

		tasks.add(new VRPTTTask(VRPTTTask.Mode.DIRECT, customer, null));

		for (Node transshipment : transshipments)
			tasks.add(new VRPTTTask(VRPTTTask.Mode.VIA_TDP, customer, transshipment));

		return tasks;
	}
}
