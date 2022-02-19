package xf.xfvrp.opt.improve.routebased.move

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class XFVRPSingleMove2Spec extends Specification {

	def service = new XFVRPSingleMove()

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()
	
	def nd2 = new TestNode(
		externID: "DEP2",
		globalIdx: 4,
		xlong: 0,
		ylat: -2,
		siteType: SiteType.DEPOT,
		demand: [0, 0],
		timeWindow: [[0,99],[2,99]]
		).getNode()

	def impList = new PriorityQueue<>(
			(o1, o2) -> Float.compare(o2[6], o1[6])
	)

	def "Search multi depot - Find improve"() {
		def model = initScenMultiDepot()
		def n = model.getNodes()
		service.setModel(model)
		
		def sol = Helper.set(model, [nd, n[2], nd, n[3], nd2, n[4], nd2] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(model, sol.getRoutes(), impList, 1, false)

		then:
		impList.size() == 4
		impList.stream().filter({f -> f.toList().subList(1,7) == [0,1,1,1,0,0]}).count() == 1
		impList.stream().filter({f -> f.toList().subList(1,7) == [1,0,1,1,0,0]}).count() == 1
		impList.stream().filter({f -> f.toList().subList(1,7) == [0,1,1,2,0,0]}).count() == 1
		impList.stream().filter({f -> f.toList().subList(1,7) == [1,0,1,2,0,0]}).count() == 1
		Math.abs(impList.stream().find({f -> f.toList().subList(1,7) == [1,0,1,1,0,0]})[0] - 1.414f) < 0.001f
		Math.abs(impList.stream().find({f -> f.toList().subList(1,7) == [0,1,1,1,0,0]})[0] - 1.414f) < 0.001f
		Math.abs(impList.stream().find({f -> f.toList().subList(1,7) == [1,0,1,2,0,0]})[0] - 1.414f) < 0.001f
		Math.abs(impList.stream().find({f -> f.toList().subList(1,7) == [0,1,1,2,0,0]})[0] - 1.414f) < 0.001f
	}
	
	def "Search multi depot - Find No improve"() {
		def model = initScenMultiDepot()
		def n = model.getNodes()
		service.setModel(model)
		
		def sol = Helper.set(model, [nd, n[2], nd2, n[4], nd2] as Node[])
		impList.clear()

		when:
		XFVRPMoveSearchUtil.search(model, sol.getRoutes(), impList, 1, false)

		then:
		impList.size() == 0
	}

	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: -1,
				ylat: 0,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: -1,
				ylat: 0.5f,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		nd2.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)

		def nodes = [nd, nd2, n1, n2, n3]

		return TestXFVRPModel.get(nodes, v)
	}
	
	XFVRPModel initScenMultiDepot() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 0,
				ylat: 1,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 1,
				ylat: 1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 0,
				ylat: -3,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		nd2.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)

		def nodes = [nd, nd2, n1, n2, n3]

		return TestXFVRPModel.get(nodes, v)
	}
}
