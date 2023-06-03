package xf.xfvrp.opt.improve.routebased.move

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel

class XFVRPSegmentMove2Spec extends Specification {

	def service = new XFVRPSegmentMove()

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

	PriorityQueue<float[]> impList = new PriorityQueue<>(
			(o1, o2) -> Float.compare(o2[6], o1[6])
	)

	def "Search single depot - No invert - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], nd, n[4], n[5], nd] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(sol, impList, 3, true)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f.toList().subList(1,7) == [0,1,1,2,1,0]}).count() == 1
		impList.stream().filter({f -> f.toList().subList(1,7) == [1,0,1,2,1,0]}).count() == 1
		Math.abs(impList.stream().find({f -> f.toList().subList(1,7) == [1,0,1,2,1,0]})[0] - 4) < 0.001f
		Math.abs(impList.stream().find({f -> f.toList().subList(1,7) == [0,1,1,2,1,0]})[0] - 4.828) < 0.001f
	}

	def "Search single depot - Long scenario - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], nd, n[4], n[5], n[6], n[7], nd] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(sol, impList, 3, true)

		then:
		impList.size() > 0
	}

	def "Search single depot - Find No improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[4], n[2], n[3], n[5], nd] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(sol, impList, 3, true)

		then:
		impList.size() == 0
	}

	def "Search single depot - Deactivated invert - Not the right improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], nd, n[5], n[4], nd] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(sol, impList, 3, false)

		then:
		impList.size() > 0
		// Invertation flag is never 1
		impList.stream().filter({f -> f[6] == 1}).count() == 0
	}

	def "Search multi depot - With invert - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd2, n[2], n[3], nd, n[5], n[4], nd] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(sol, impList, 3, true)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[6] == 1}).count() > 0
	}

	def "Search - Deactivated invert - Not the right improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd2, n[2], n[3], nd, n[5], n[4], nd] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(sol, impList, 3, false)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[6] == 1}).count() == 0
	}

	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

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

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6]

		return TestXFVRPModel.get(nodes, v)
	}
}
