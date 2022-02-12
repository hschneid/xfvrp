package xf.xfvrp.opt.init.precheck;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.init.precheck.pdp.PDPPreCheckService;
import xf.xfvrp.opt.init.precheck.vrp.VRPPreCheckService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class PreCheckService {

	public Node[] precheck(Node[] nodes, Vehicle vehicle, XFVRPParameter parameter) throws XFVRPException {
		checkExternId(nodes);

		if(parameter.isWithPDP())
			return new PDPPreCheckService().precheck(nodes, vehicle);

		return new VRPPreCheckService().precheck(nodes, vehicle);
	}

	/**
	 * External Id's must be unique for each node.
	 */
	private void checkExternId(Node[] nodes) throws XFVRPException {
		List<String> externIdsWithMultipleOccurences = Arrays
				.stream(nodes)
				.collect(Collectors.groupingBy(Node::getExternID))
				.values().stream()
				// Check, that there more nodes per ID
				.filter(nodesPerId -> nodesPerId.size() > 1)
				.map(nodesPerId -> nodesPerId.get(0).getExternID())
				.collect(Collectors.toList());

		if(externIdsWithMultipleOccurences.size() == 0)
			return;

		throw new XFVRPException(
				XFVRPExceptionType.ILLEGAL_INPUT, String.format("External id's of nodes are not unique and cause irregular behaviour: %s", String.join(", ", externIdsWithMultipleOccurences))
		);

	}
}
