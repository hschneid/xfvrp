package xf.xfvrp.opt.improve.routebased.move

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.evaluation.EvaluationService

class XFVRPSegmentMoveIntSpec extends Specification {

	def service = new XFVRPSegmentMove()
	def evalService = new EvaluationService()

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def nd2 = new TestNode(
	externID: "DEP2",
	globalIdx: 5,
	xlong: 3,
	ylat: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol

	def "Find improvement for single depot"() {
		def model = initSDScen()
		def n = model.getNodes()

		sol = Helper.set(model, [nd, n[2], n[1], nd, n[3], n[4], nd] as Node[])

		def currentQuality = evalService.check(sol)
		
		when:
		def newQuality = service.improve(sol, currentQuality)
		sol = NormalizeSolutionService.normalizeRoute(sol)
		def checkedQuality = evalService.check(sol)
		def routes = Helper.get(sol)
		
		then:
		newQuality != null
		checkedQuality != null
		Math.abs(newQuality.getFitness() - checkedQuality.getFitness()) < 0.001
		newQuality.getPenalty() == 0
		Math.abs(newQuality.getCost() - 9.656) < 0.001
		routes[0].externID == 'DEP'
		routes[1].externID == 'DEP'
		routes[2].externID == 'DEP'
		routes[3] == n[3]
		routes[4] == n[1]
		routes[5] == n[2]
		routes[6] == n[4]
		routes[7].externID == 'DEP'
	}
	
	def "Find improvement for multi depot"() {
		def model = initMDScen()
		def n = model.getNodes()

		sol = Helper.set(model, [nd, n[3], n[2], nd, n[4], n[5], nd] as Node[])

		def currentQuality = evalService.check(sol)

		when:
		def newQuality = service.improve(sol, currentQuality)
		sol = NormalizeSolutionService.normalizeRoute(sol)
		def checkedQuality = evalService.check(sol)
		def routes = Helper.get(sol)
		
		then:
		newQuality != null
		checkedQuality != null
		Math.abs(newQuality.getFitness() - checkedQuality.getFitness()) < 0.001
		newQuality.getPenalty() == 0
		Math.abs(newQuality.getCost() - 9.656) < 0.001
		routes[0].externID == 'DEP'
		routes[1].externID == 'DEP'
		routes[2].externID == 'DEP'
		routes[3] == n[4]
		routes[4] == n[2]
		routes[5] == n[3]
		routes[6] == n[5]
		routes[7].externID == 'DEP'
		routes[8].externID == 'DEP2'
		routes[9].externID == 'DEP2'
	}
	
	def "Find no improvement"() {
		def model = initMDScen()
		def n = model.getNodes()

		sol = Helper.set(model, [nd, n[4], n[2], n[3], n[5], nd, nd] as Node[])

		def currentQuality = evalService.check(sol)

		when:
		def newQuality = service.improve(sol, currentQuality)
		def newGiantRoute = Helper.get(sol)
		
		then:
		newQuality == null
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[4]
		newGiantRoute[2] == n[2]
		newGiantRoute[3] == n[3]
		newGiantRoute[4] == n[5]
		newGiantRoute[5] == nd
		newGiantRoute[6] == nd
	}

	XFVRPModel initMDScen() {
		def v = new TestVehicle(name: "V1", capacity: [4, 4]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: -2,
				ylat: 2,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 2,
				ylat: 2,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: -1,
				ylat: 1,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 1,
				ylat: 1,
				geoId: 4,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 6,
				externID: "5",
				xlong: 2,
				ylat: 1,
				geoId: 5,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 7,
				externID: "6",
				xlong: 3,
				ylat: 1,
				geoId: 6,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		nd2.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)
		n4.setIdx(5)
		n5.setIdx(6)
		n6.setIdx(7)

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6] as Node[]

		return TestXFVRPModel.get(Arrays.asList(nodes), v)
	}
	
	XFVRPModel initSDScen() {
		def v = new TestVehicle(name: "V1", capacity: [4, 4]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: -2,
				ylat: 2,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 2,
				ylat: 2,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: -1,
				ylat: 1,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 1,
				ylat: 1,
				geoId: 4,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 6,
				externID: "5",
				xlong: 2,
				ylat: 1,
				geoId: 5,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 7,
				externID: "6",
				xlong: 3,
				ylat: 1,
				geoId: 6,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		n1.setIdx(1)
		n2.setIdx(2)
		n3.setIdx(3)
		n4.setIdx(4)
		n5.setIdx(5)
		n6.setIdx(6)

		def nodes = [nd, n1, n2, n3, n4, n5, n6] as Node[]

		return TestXFVRPModel.get(Arrays.asList(nodes), v)
	}
}
