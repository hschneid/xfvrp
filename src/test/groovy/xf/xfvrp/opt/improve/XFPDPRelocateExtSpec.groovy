package xf.xfvrp.opt.improve

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.fleximport.CustomerData
import xf.xfvrp.opt.improve.routebased.move.XFPDPMoveSearchUtil
import xf.xfvrp.opt.improve.routebased.move.XFPDPSingleMove

class XFPDPRelocateExtSpec extends Specification {

	def service = new XFPDPSingleMove()

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def "Search"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		
		def sol = Helper.set(model, [nd, n[1], n[2], nd, n[3], n[4], nd] as Node[])

		when:
		def impList = service.search(sol)

		then:
		impList.stream().filter({f -> f[0] == 1 && f[1] == 2 && f[2] == 4 && f[3] == 4}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 2 && f[2] == 6 && f[3] == 6}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 2 && f[2] == 4 && f[3] == 5}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 2 && f[2] == 4 && f[3] == 6}).count() == 1
		impList.stream().filter({f -> f[0] == 4 && f[1] == 5 && f[2] == 1 && f[3] == 1}).count() == 1
		impList.stream().filter({f -> f[0] == 4 && f[1] == 5 && f[2] == 2 && f[3] == 2}).count() == 1
		impList.stream().filter({f -> f[0] == 4 && f[1] == 5 && f[2] == 3 && f[3] == 3}).count() == 1
		impList.stream().filter({f -> f[0] == 4 && f[1] == 5 && f[2] == 2 && f[3] == 3}).count() == 1
	}
	
	def "Potential 1"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[3], n[4], n[1], n[2], nd] as Node[])
		def route = sol.getRoutes()[0]
		def queue = new PriorityQueue<float[]>({(o1, o2) -> Float.compare(o2[0], o1[0])})

		when:
		XFPDPMoveSearchUtil.search(sol, route, route, 0, 0, 3, 4, 1, 1, queue)
		
		then:
		queue.poll()[0] == -2
	}
	
	def "Potential 2"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[3], n[1], n[4], n[2], nd] as Node[])
		def route = sol.getRoutes()[0]
		def queue = new PriorityQueue<float[]>()

		when:
		XFPDPMoveSearchUtil.search(sol, route, route, 0, 0, 2, 4, 1, 3, queue)

		then:
		queue.poll()[0] == -2
	}
	
	def "Potential 3"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[3], n[1], n[4], n[2], nd] as Node[])
		def route = sol.getRoutes()[0]
		def queue = new PriorityQueue<float[]>()

		when:
		XFPDPMoveSearchUtil.search(sol, route, route, 0, 0, 1, 3, 4, 5, queue)

		then:
		queue.poll()[0] == -2
	}
	
	def "Potential 4"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[1], n[2], n[3], n[4], nd] as Node[])
		def route = sol.getRoutes()[0]
		def queue = new PriorityQueue<float[]>()

		when:
		XFPDPMoveSearchUtil.search(sol, route, route, 0, 0, 3, 4, 1, 2, queue)

		then:
		queue.poll()[0] == 4
	}
	
	def "Potential no move"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[1], n[2], n[3], n[4], nd] as Node[])
		def route = sol.getRoutes()[0]
		def queue = new PriorityQueue<float[]>()

		when:
		XFPDPMoveSearchUtil.search(sol, route, route, 0, 0, 1, 2, 3, 3, queue)

		then:
		queue.poll().size() == 0
	}
	
	def "Potential partial move"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[1], nd] as Node[])
		def route = sol.getRoutes()[0]
		def queue = new PriorityQueue<float[]>()

		when:
		XFPDPMoveSearchUtil.search(sol, route, route, 0, 0, 4, 1, 2, 2, queue)

		then:
		queue.poll().size() == 0
	}
	
	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 0,
				geoId: 1,
				demand: [2, 2],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.PICKUP)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 2,
				ylat: 0,
				geoId: 2,
				demand: [-2, -2],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 3,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.PICKUP)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 4,
				ylat: 0,
				geoId: 1,
				demand: [-1, -1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 5,
				externID: "5",
				xlong: 5,
				ylat: 0,
				geoId: 2,
				demand: [3, 3],
				timeWindow: [[0,99]],
				shipID: "C",
				loadType: LoadType.PICKUP)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 6,
				externID: "6",
				xlong: 6,
				ylat: 0,
				geoId: 3,
				demand: [-3, -3],
				timeWindow: [[0,99]],
				shipID: "C",
				loadType: LoadType.DELIVERY)
				.getNode()

		def customers =	[
				new CustomerData(externID: "1", shipID: "A"),
				new CustomerData(externID: "2", shipID: "A"),
				new CustomerData(externID: "3", shipID: "B"),
				new CustomerData(externID: "4", shipID: "B"),
				new CustomerData(externID: "5", shipID: "C"),
				new CustomerData(externID: "6", shipID: "C")
		] as List<CustomerData>

		nd.setIdx(0)
		n1.setIdx(1)
		n2.setIdx(2)
		n3.setIdx(3)
		n4.setIdx(4)
		n5.setIdx(5)
		n6.setIdx(6)

		def nodes = [nd, n1, n2, n3, n4, n5, n6] as Node[]
		new ShipmentConverter().convert(nodes, customers)

		return TestXFVRPModel.get(Arrays.asList(nodes), v)
	}

}
