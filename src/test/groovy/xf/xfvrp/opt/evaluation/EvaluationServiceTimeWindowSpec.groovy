package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class EvaluationServiceTimeWindowSpec extends Specification {

	def service = new EvaluationService()

	def depot

	def nd = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[2,7]]
	).getNode()

	def nd2 = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[2,8]]
	).getNode()

	def nd3 = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0.5f,8]]
	).getNode()

	def nd4 = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[2,9]]
	).getNode()

	def nd5 = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[2,8.9]]
	).getNode()

	def nr = new TestNode(
	externID: "REP",
	siteType: SiteType.REPLENISH,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Basic Time Windows - Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[4,5]],[[5,6]]] as float[][][], 0f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "Basic Time Windows - Not Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[3,3.9f]],[[5,6]]] as float[][][], 0f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "With Service Time - Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "With Service Time - Not Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "With Loading at depot - Okay"() {
		depot = nd3
		parameter.setLoadingTimeAtDepot(true)
		def model = initScenBasic([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "With Loading at depot - Not Okay"() {
		depot = nd2
		parameter.setLoadingTimeAtDepot(true)
		def model = initScenBasic([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "With UnLoading at depot - Okay"() {
		depot = nd4
		parameter.setUnloadingTimeAtDepot(true)
		def model = initScenBasicPickup([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "With UnLoading at depot - Not Okay"() {
		depot = nd5
		parameter.setUnloadingTimeAtDepot(true)
		def model = initScenBasicPickup([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "With Max Waiting Time - Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[5,6]],[[6,7]]] as float[][][], 0f, new TestVehicle(name: "V1", capacity: [3, 3], maxWaitingTime: 1))
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "With Max Waiting Time - Not Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[5.1,6]],[[6,7]]] as float[][][], 0f, new TestVehicle(name: "V1", capacity: [3, 3], maxWaitingTime: 1))
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "With Max Driving Time - Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[4,5]],[[5,6]]] as float[][][], 0f, new TestVehicle(name: "V1", capacity: [3, 3], maxDrivingTimePerShift: 2f, waitingTimeBetweenShifts: 1f))
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "With Max Driving Time - Not Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[4,4.9]],[[5,6]]] as float[][][], 0f, new TestVehicle(name: "V1", capacity: [3, 3], maxDrivingTimePerShift: 2f, waitingTimeBetweenShifts: 1f))
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Multi Time Windows - Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[2,3], [3,4]],[[5,6]]] as float[][][], 0f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Multi Time Windows - Not Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[2,3], [3,3.9]],[[5,6]]] as float[][][], 0f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	XFVRPModel initScenBasic (float[][][] timeWindows, float serviceTime, TestVehicle paraV) {
		return initScenBasicAbstract(timeWindows, serviceTime, LoadType.DELIVERY, paraV)
	}
	
	XFVRPModel initScenBasicPickup (float[][][] timeWindows, float serviceTime, TestVehicle paraV) {
		return initScenBasicAbstract(timeWindows, serviceTime, LoadType.PICKUP, paraV)
	}

	XFVRPModel initScenBasicAbstract (float[][][] timeWindows, float serviceTime, LoadType loadType, TestVehicle paraV) {
		if(paraV == null)
			paraV = new TestVehicle(name: "V1", capacity: [3, 3])

		def v = paraV.getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 0,
				ylat: 1,
				geoId: 1,
				demand: [1, 1],
				timeWindow: timeWindows[0],
				serviceTime: serviceTime,
				loadType: loadType)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 1,
				ylat: 1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: timeWindows[1],
				serviceTime: serviceTime,
				loadType: loadType)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: timeWindows[2],
				serviceTime: serviceTime,
				loadType: loadType)
				.getNode()

		depot.setIdx(0)
		n1.setIdx(1)
		n2.setIdx(2)
		n3.setIdx(3)

		def nodes = [depot, n1, n2, n3] as Node[]

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v)

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
