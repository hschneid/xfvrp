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

class XFPDPRelocateBaseSpec extends Specification {

	def service = new XFPDPSingleMove()

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def "Shipment positions"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[1], n[2], n[3], n[4], n[5], n[6], nd] as Node[])

		when:
		def result = XFPDPMoveSearchUtil.getShipmentPositions(sol.routes, sol.getModel())
		
		then:
		result[0][0] == 1
		result[0][1] == 2
		result[1][0] == 3
		result[1][1] == 4
		result[2][0] == 5
		result[2][1] == 6
	}
	
	def "Change"() {
		def model = initScen()
		def n = model.getNodes()

		def sol = Helper.set(model, [nd, n[3], n[1], n[4], n[5], n[6], n[2], nd] as Node[])
		
		when:
		service.change(sol, [-1, 0, 0, 2, 6, 1, 1] as float[])
		def nn = Helper.get(sol)

		then:
		nn[0].externID == "DEP"
		nn[1].externID == "1"
		nn[2].externID == "2"
		nn[3].externID == "3"
		nn[4].externID == "4"
		nn[5].externID == "5"
		nn[6].externID == "6"
		nn[7].externID == "DEP"
	}

	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: -2,
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
				xlong: -2,
				ylat: 1f,
				geoId: 2,
				demand: [-2, -2],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: -1,
				ylat: 2,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.PICKUP)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 0,
				ylat: 2,
				geoId: 1,
				demand: [-1, -1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 5,
				externID: "5",
				xlong: 1,
				ylat: 1,
				geoId: 2,
				demand: [3, 3],
				timeWindow: [[0,99]],
				shipID: "C",
				loadType: LoadType.PICKUP)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 6,
				externID: "6",
				xlong: 1,
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
