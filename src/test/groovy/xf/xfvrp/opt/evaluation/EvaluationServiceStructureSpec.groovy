package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.Solution

class EvaluationServiceStructureSpec extends Specification {

	def service = new EvaluationService()

	def nd = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
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

	def "Feasability - Starts not with DEPOT"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([n[2], nd, n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		thrown XFVRPException
	}
	
	def "Feasability - Ends not with DEPOT"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4]] as Node[])
		
		when:
		def result = service.check(sol, model)

		then:
		thrown XFVRPException
	}

	def "Ignore empty routes"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], nd] as Node[])
		
		def sol2 = new Solution()
		sol2.setGiantRoute([nd, nd, nr, nr, n[2], n[3], n[4], nr, nd, nd] as Node[])

		when:
		def result = service.check(sol, model)
		def result2 = service.check(sol2, model)

		then:
		result != null
		result2 != null
		result.getPenalty() == 0
		result2.getPenalty() == 0
		Math.abs(result.getCost() - result2.getCost()) < 0.001

	}
	
	def "Eval with two depots at start"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([Util.createIdNode(nd, 0), Util.createIdNode(nd, 1), n[2], n[3], n[4], Util.createIdNode(nd, 2)] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
	}

	XFVRPModel initScen1(Vehicle v, LoadType loadType) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 1,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: loadType)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 0,
				ylat: 1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: loadType)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: loadType)
				.getNode()

		nd.setIdx(0)
		nr.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)

		def nodes = [nd, nr, n1, n2, n3] as Node[]

		return TestXFVRPModel.get(Arrays.asList(nodes), v)
	}
}
