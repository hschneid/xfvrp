package xf.xfvrp.report

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.report.build.ReportBuilder

class ReportBuilderTimeWindowSpec extends Specification {

	def service = new ReportBuilder()

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
	timeWindow: [[2,8.5]]
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
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		result.getSummary().getDelay() == 0
		result.getSummary().getDelay(model.getVehicle()) == 0
		result.getRoutes().get(0).getSummary().getDelay() == 0
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 6) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDuration() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDuration() - 1) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(0).getTravelTime() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getTravelTime() - 1) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(0).getWaiting() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getWaiting() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getWaiting() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getWaiting() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getWaiting() - 0) < 0.001
	}

	def "Basic Time Windows - Not Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[3,3.9f]],[[5,6]]] as float[][][], 0f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 0.1) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 0.1) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 0.1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDelay() - 0.1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 6) < 0.001
	}

	def "With Service Time - Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 0) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 6.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 7.5) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDuration() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDuration() - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDuration() - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDuration() - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDuration() - 1) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(0).getTravelTime() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getTravelTime() - 1) < 0.001
	}

	def "With Service Time - Not Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 0.5) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 6.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDelay() - 0.5) < 0.001
	}

	def "With Loading at depot - Okay"() {
		depot = nd3
		parameter.setLoadingTimeAtDepot(true)
		def model = initScenBasic([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([cp(depot), n[1], n[2], n[3], cp(depot)] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 0) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getService() - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 6.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getService() - 0) < 0.001
	}

	def "With Loading at depot - Not Okay"() {
		depot = nd2
		parameter.setLoadingTimeAtDepot(true)
		def model = initScenBasic([[[4,5]],[[5,6]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([cp(depot), n[1], n[2], n[3], cp(depot)] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 1.5) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 3.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getService() - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 4.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 6.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 8) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDelay() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 9) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 9) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDelay() - 1) < 0.001
	}

	def "With UnLoading at depot - Okay"() {
		depot = nd4
		parameter.setUnloadingTimeAtDepot(true)
		def model = initScenBasicPickup([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([cp(depot), n[1], n[2], n[3], cp(depot)] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 0) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getService() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 6.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDelay() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 9) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDelay() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getService() - 1.5) < 0.001
	}

	def "With UnLoading at depot - Not Okay"() {
		depot = nd5
		parameter.setUnloadingTimeAtDepot(true)
		def model = initScenBasicPickup([[[3,4]],[[4,5.5]],[[6,7]]] as float[][][], 0.5f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([cp(depot), n[1], n[2], n[3], cp(depot)] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 0.5) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getService() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 6.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getService() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDelay() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 7.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 9) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDelay() - 0.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getService() - 1.5) < 0.001
	}

	def "With Max Waiting Time - Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[5,6]],[[6,7]]] as float[][][], 0f, new TestVehicle(name: "V1", capacity: [3, 3], maxWaitingTime: 1))
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getWaitingTime() - 1) < 0.001
		Math.abs(result.getSummary().getWaitingTime(model.getVehicle()) - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getWaitingTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getWaiting() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 7) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 7) < 0.001
	}

	def "With Max Driving Time - Okay"() {
		depot = nd2
		def model = initScenBasic([[[3,4]],[[4,5]],[[5,6]]] as float[][][], 0f,
				new TestVehicle(name: "V1", capacity: [3, 3], maxDrivingTimePerShift: 2f, waitingTimeBetweenShifts: 1f)
		)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDuration() - 6) < 0.001
		Math.abs(result.getSummary().getDuration(model.getVehicle()) - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDuration() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3) < 0.001

		// PAUSE
		result.getRoutes().get(0).getEvents().get(2).getSiteType() == SiteType.PAUSE
		result.getRoutes().get(0).getEvents().get(2).getID() == n[2].getExternID()
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDuration() - 1) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 5) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 6) < 0.001

		// PAUSE 2
		result.getRoutes().get(0).getEvents().get(5).getSiteType() == SiteType.PAUSE
		result.getRoutes().get(0).getEvents().get(5).getID() == depot.getExternID()
		Math.abs(result.getRoutes().get(0).getEvents().get(5).getArrival() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(5).getDeparture() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(5).getDuration() - 1) < 0.001

		Math.abs(result.getRoutes().get(0).getEvents().get(6).getArrival() - 8) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(6).getDeparture() - 8) < 0.001
		
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDuration() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(5).getDuration() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(6).getDuration() - 1) < 0.001
		
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getTravelTime() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getTravelTime() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getTravelTime() - 1) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(5).getTravelTime() - 0) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(6).getTravelTime() - 1) < 0.001
	}

	def "Multi Time Windows - Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[2,3], [5,6]],[[5,6]]] as float[][][], 0f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		result.getSummary().getDelay() == 0
		result.getSummary().getDelay(model.getVehicle()) == 0
		result.getRoutes().get(0).getSummary().getDelay() == 0
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 7) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 7) < 0.001
	}

	def "Multi Time Windows - Not Okay"() {
		depot = nd
		def model = initScenBasic([[[3,4]],[[1,2], [2,2.5]],[[5,6]]] as float[][][], 0f, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([depot, n[1], n[2], n[3], depot] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getRoutes().size() == 1
		Math.abs(result.getSummary().getDelay() - 1.5) < 0.001
		Math.abs(result.getSummary().getDelay(model.getVehicle()) - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getSummary().getDelay() - 1.5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getArrival() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(0).getDeparture() - 2) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getArrival() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(1).getDeparture() - 3) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getArrival() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(2).getDeparture() - 4) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getArrival() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(3).getDeparture() - 5) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getArrival() - 6) < 0.001
		Math.abs(result.getRoutes().get(0).getEvents().get(4).getDeparture() - 6) < 0.001
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

	def depotId = 0
	private Node cp(Node node) {
		return Util.createIdNode(node, depotId++)
	}
}
