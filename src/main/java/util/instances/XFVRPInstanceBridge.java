package util.instances;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import util.instances.Instance.Fleet.VehicleProfile;
import util.instances.Instance.Network.Nodes.Node;
import util.instances.Instance.Requests.Request;
import xf.xfvrp.XFVRP;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.metric.EucledianMetric;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.opt.init.precheck.PreCheckException;
import xf.xfvrp.report.Report;

public class XFVRPInstanceBridge {

	public XFVRP build(String pathName) throws FileNotFoundException, JAXBException {
		Instance instance = InstanceBuilder.read(new FileInputStream(pathName));

		XFVRP vrp = new XFVRP();

		Map<String, Request> requests = instance.requests.request.stream().collect(Collectors.toMap(n -> n.getNode().toString(), v -> v));

		for (Node node : instance.network.nodes.node) {
			int nodeType = node.getType().intValue();
			
			if(nodeType == 0) {
				addDepot(node, vrp);
			} else if(nodeType == 1) {
				addCustomer(node, requests, vrp);
			}
		}

		for (VehicleProfile vehicle : instance.fleet.vehicleProfile) {
			vrp.addVehicle()
			.setName(vehicle.getType().toString())
			.setCapacity(new float[]{vehicle.getCapacity().floatValue()});
		}

		return vrp;
	}

	private void addCustomer(Node node, Map<String, Request> requests, XFVRP vrp) {
		String nodeId = node.getId().toString();

		vrp.addCustomer()
		.setExternID(nodeId)
		.setXlong(node.getCx().floatValue())
		.setYlat(node.getCy().floatValue())
		.setLoadType(LoadType.DELIVERY)
		.setDemand(new float[]{requests.get(nodeId).getQuantity().floatValue()});
	}

	private void addDepot(Node node, XFVRP vrp) {
		vrp.addDepot()
		.setExternID(node.getId().toString())
		.setXlong(node.getCx().floatValue())
		.setYlat(node.getCy().floatValue());
	}
	
	private void opt(XFVRP vrp) throws PreCheckException {
		/*vrp.addOptType(XFVRPOptType.CONST);
		vrp.addOptType(XFVRPOptType.RELOCATE);
		vrp.addOptType(XFVRPOptType.SWAP);
		vrp.addOptType(XFVRPOptType.PATH_RELOCATE);*/
		vrp.addOptType(XFVRPOptType.ILS);
		vrp.setNbrOfLoopsForILS(2000);
		
		vrp.setMetric(new EucledianMetric());
		vrp.executeRoutePlanning();
		
		Report report = vrp.getReport();
		System.out.println("Dist " + report.getSummary().getDistance());
		System.out.println("Nbr " + report.getSummary().getNbrOfUsedVehicles());
	}

	public static void main(String[] args) {
		try {
			XFVRPInstanceBridge i = new XFVRPInstanceBridge();
			XFVRP vrp = i.build("./src/test/resources/CMT05.xml");
			i.opt(vrp);
		} catch (FileNotFoundException | JAXBException | PreCheckException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
