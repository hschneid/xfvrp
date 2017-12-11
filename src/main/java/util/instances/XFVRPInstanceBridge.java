package util.instances;

import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.vrprep.model.instance.Instance;
import org.vrprep.model.instance.Instance.Fleet.VehicleProfile;
import org.vrprep.model.instance.Instance.Network.Nodes.Node;
import org.vrprep.model.instance.Instance.Requests.Request;
import org.vrprep.model.util.Instances;

import xf.xfvrp.XFVRP;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.metric.EucledianMetric;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.opt.init.precheck.PreCheckException;
import xf.xfvrp.report.Report;

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
		} catch (JAXBException | PreCheckException e) {
			e.printStackTrace();
		}
	}
}
