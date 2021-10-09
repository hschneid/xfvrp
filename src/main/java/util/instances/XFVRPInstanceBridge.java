package util.instances;

import org.vrprep.model.instance.Instance;
import org.vrprep.model.instance.Instance.Fleet.VehicleProfile;
import org.vrprep.model.instance.Instance.Network.Nodes.Node;
import org.vrprep.model.instance.Instance.Requests.Request;
import org.vrprep.model.util.Instances;
import xf.xfvrp.XFVRP;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.EucledianMetric;
import xf.xfvrp.base.monitor.DefaultStatusMonitor;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.report.Report;

import javax.xml.bind.JAXBException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPInstanceBridge {

	public XFVRP build(String pathName) throws JAXBException {
		Instance instance = Instances.read(Paths.get(pathName));

		XFVRP vrp = new XFVRP();

		Map<String, Request> requests = instance.getRequests().getRequest().stream().collect(Collectors.toMap(n -> n.getNode().toString(), v -> v));

		for (Node node : instance.getNetwork().getNodes().getNode()) {
			int nodeType = node.getType().intValue();

			if(nodeType == 0) {
				addDepot(node, vrp);
			} else if(nodeType == 1) {
				addCustomer(node, requests, vrp);
			}
		}

		for (VehicleProfile vehicle : instance.getFleet().getVehicleProfile()) {
			vrp.getData().addVehicle()
			.setName(vehicle.getType().toString())
			.setCapacity(new float[]{vehicle.getCapacity().floatValue()});
		}

		vrp.setStatusMonitor(new DefaultStatusMonitor());

		return vrp;
	}

	private void addCustomer(Node node, Map<String, Request> requests, XFVRP vrp) {
		String nodeId = node.getId().toString();

		vrp.getData().addCustomer()
		.setExternID(nodeId)
		.setXlong(node.getCx().floatValue())
		.setYlat(node.getCy().floatValue())
		.setLoadType(LoadType.DELIVERY)
		.setDemand(new float[]{requests.get(nodeId).getQuantity().floatValue()});
	}

	private void addDepot(Node node, XFVRP vrp) {
		vrp.getData().addDepot()
		.setExternID(node.getId().toString())
		.setXlong(node.getCx().floatValue())
		.setYlat(node.getCy().floatValue());
	}

	private void opt(XFVRP vrp) throws XFVRPException {
		//vrp.addOptType(XFVRPOptType.CONST);
		/*vrp.addOptType(XFVRPOptType.RELOCATE);
		vrp.addOptType(XFVRPOptType.SWAP);
		vrp.addOptType(XFVRPOptType.PATH_RELOCATE);*/
		vrp.addOptType(XFVRPOptType.ILS);
		vrp.getParameters().setNbrOfILSLoops(20000);

		/*vrp.setPredefinedSolutionString(
				"{(200,194,158,192,184,190,43,199,197,136,1,191,196,66,200),"+
						"(200,16,159,104,183,23,116,62,185,89,137,91,141,22,186,200),"+
						"(200,93,156,114,142,113,42,68,143,41,90,115,160,53,198,195,200),"+
						"(200,6,61,96,105,33,193,112,2,157,200),"+
						"(200,86,101,28,64,94,140,121,82,173,21,172,139,54,152,200),"+
						"(200,125,59,5,103,88,37,138,36,155,47,48,30,200),"+
						"(200,45,29,79,15,154,124,20,166,122,174,171,120,200),"+
						"(200,175,46,65,167,99,179,27,58,111,4,87,200),"+
						"(200,34,176,8,102,178,78,19,70,128,123,13,83,153,98,200),"+
						"(200,149,51,7,132,180,108,69,14,133,177,35,200),"+
						"(200,50,169,119,38,165,164,85,134,84,170,11,52,150,200),"+
						"(200,126,17,76,40,130,12,168,81,26,60,127,200),"+
						"(200,9,109,39,57,189,131,80,10,77,129,71,100,200),"+
						"(200,187,97,161,110,25,56,118,72,32,106,44,55,3,200),"+
						"(200,75,162,31,163,148,92,135,145,73,146,18,147,181,200),"+
						"(200,95,151,117,63,107,24,144,74,49,182,67,188,200)}"
				);*/

		vrp.getData().setMetric(new EucledianMetric());
		vrp.executeRoutePlanning();

		Report report = vrp.getReport();
		System.out.println("Dist " + report.getSummary().getDistance());
		System.out.println("Nbr " + report.getSummary().getNbrOfUsedVehicles());
	}

	public static void main(String[] args) {
		try {
			XFVRPInstanceBridge i = new XFVRPInstanceBridge();
			XFVRP vrp = i.build("./src/test/resources/CMT01.xml");
			i.opt(vrp);
		} catch (JAXBException | XFVRPException e) {
			e.printStackTrace();
		}
	}
}
