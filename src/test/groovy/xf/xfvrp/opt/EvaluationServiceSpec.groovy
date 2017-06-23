package xf.xfvrp.opt

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator

class EvaluationServiceSpec extends Specification {

	def service = new EvaluationService();

	def model
	
	def nd = new Node(
		//globalIdx: 
		0,
		//externID: 
		"DEP",
		//siteType: 
		SiteType.DEPOT,
		//xlong: 
		0,
		//ylat: 
		0,
		//geoId: 
		0,
		//demand: 
		[0, 0, 0] as float[],
		//timeWindow: 
		[[0, 2], [2, 6]] as float[][],
		//serviceTime: 
		0,
		//serviceTimeForSite: 
		0,
		//loadType: 
		LoadType.DELIVERY,
		//presetBlockPos: 
		0,
		//presetBlockRank: 
		0,
		//shipID: 
		"0",
		//nbrOfPackages: 
		1,
		//heightOfPackage: 
		1,
		//widthOfPackage: 
		1,
		//lengthOfPackage: 
		1,
		//weightOfPackage: 
		1,
		//loadBearingOfPackage: 
		1,
		//stackingGroupOfPackage: 
		1,
		//containerTypeOfPackage: 
		1)

	def n1 = new Node(
		//globalIdx: 
		1,
		//externID: 
		"1",
		//siteType: 
		SiteType.CUSTOMER,
		//xlong: 
		1,
		//ylat: 
		1,
		//geoId: 
		1,
		//demand: 
		[1, 1, 1] as float[],
		//timeWindow: 
		[[0, 2] as float[], [2, 6] as float[]] as float[][],
		//serviceTime: 
		0,
		//serviceTimeForSite: 
		0,
		//loadType: 
		LoadType.DELIVERY,
		//presetBlockPos: 
		0,
		//presetBlockRank: 
		0,
		//shipID: 
		"0",
		//nbrOfPackages: 
		1,
		//heightOfPackage: 
		1,
		//widthOfPackage: 
		1,
		//lengthOfPackage: 
		1,
		//weightOfPackage: 
		1,
		//loadBearingOfPackage: 
		1,
		//stackingGroupOfPackage: 
		1,
		//containerTypeOfPackage: 
		1)
	
	def v = new Vehicle(
		//int idx
		0,
		//String name
		"V1", 
		//int nbrOfAvailableVehicles
		99999, 
		//float[] capacity
		[10,10,10] as float[],
		//float maxRouteDuration
		1000, 
		//int maxStopCount
		1000, 
		//float maxWaitingTime
		1000,	
		//float fixCost
		0, 
		//float varCost
		1, 
		//int vehicleMetricId
		0,
		//float capacityOfVesselFirst
		1, 
		//float capacity2OfVesselFirst
		1, 
		//int heightOfVesselFirst
		1, 
		//int widthOfVesselFirst
		1, 
		//int lengthOfVesselFirst
		1,
		//float capacityOfVesselSecond
		1, 
		//float capacity2OfVesselSecond
		1,  
		//int heightOfVesselSecond
		1, 
		//int widthOfVesselSecond
		1, 
		//int lengthOfVesselSecond
		1,
		//float maxDrivingTimePerShift
		1000, 
		//float waitingTimeBetweenShifts
		1000, 
		//int priority
		1)
	
	def parameter = new XFVRPParameter()
	
	def metric = new EucledianMetric()

	def setup() {
		nd.setIdx(0);
		n1.setIdx(1);
		
		def nodes = [nd, n1] as Node[];
		
		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);
		
		model = new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}

	def "Simple check"() {
		def sol = new Solution();
		sol.setGiantRoute([nd, n1, nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 2.828) < 0.001
	}
}
