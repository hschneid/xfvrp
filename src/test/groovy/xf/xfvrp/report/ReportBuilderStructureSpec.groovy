package xf.xfvrp.report

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.report.build.ReportBuilder

class ReportBuilderStructureSpec extends Specification {

	def service = new ReportBuilder()

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

	def "Feasability - Starts not with DEPOT"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()


		def sol = Helper.setNoNorm(model, [n[2], nd, n[3], n[4], nd] as Node[])

		when:
		service.getReport(sol)

		then:
		thrown XFVRPException
	}

	def "Feasability - Ends not with DEPOT"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()


		def sol = Helper.set(model, [nd, n[2], n[3], n[4]] as Node[])

		when:
		service.getReport(sol)

		then:
		thrown XFVRPException
	}

	def "Feasability - NullPointer in Route"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		def sol = Helper.setNoNorm(model, [nd, n[2], n[3], n[4]] as Node[])
		sol.getRoutes()[0][2] = null

		when:
		service.getReport(sol)

		then:
		thrown XFVRPException
	}

	def "Feasability - No customer"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)

		def sol = Helper.set(model, [nd, nd] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getSummary().getNbrOfUsedVehicles() == 0
	}

	def "Feasability - No nodes"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()


		def sol = Helper.set(model, [] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result.getRoutes().isEmpty()
	}

	def "Feasability - Null route"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		def sol = Helper.setNoNorm(model, nd)
		sol.getRoutes()[0] = null

		when:
		service.getReport(sol)

		then:
		thrown(XFVRPException)
	}

	def "Ignore empty routes"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, nd, nr, nr, n[2], n[3], n[4], nr, nd, nd] as Node[])

		when:
		def result = service.getReport(sol)

		then:
		result != null
		result.getSummary().getNbrOfUsedVehicles() == 1
		result.getRoutes().size() == 1
		result.getRoutes().get(0).getEvents().size() == 5
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

		def nodes = [nd, nr, n1, n2, n3]

		return TestXFVRPModel.get(nodes, v)
	}
}
