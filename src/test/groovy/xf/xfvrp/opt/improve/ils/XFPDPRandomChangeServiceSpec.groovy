package xf.xfvrp.opt.improve.ils

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.evaluation.EvaluationService
import xf.xfvrp.opt.improve.ils.XFPDPRandomChangeService.Choice
import xf.xfvrp.opt.improve.routebased.move.XFPDPMoveUtil

class XFPDPRandomChangeServiceSpec extends Specification {

	def random = Stub Random
	def realRandom = new Random(1234)
	def evaluationService = Stub EvaluationService
	def service = new XFPDPRandomChangeService()

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

	def parameter = new XFVRPParameter()

	def setup() {
		service.rand = random
	}

	def "Choose Src Pickup - Simple okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		random.nextInt(_) >> 0

		when:
		service.chooseSrcPickup(choice, sol)

		then:
		choice.srcRouteIdx == 0
		choice.srcPickPos == 1
	}

	def "Choose Src Pickup - On delivery"() {
		def model = initBase([0, 1, 1, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		random.nextInt(_) >>> [1, 0]

		when:
		service.chooseSrcPickup(choice, sol)

		then:
		choice.srcRouteIdx == 0
		choice.srcPickPos == 1
	}

	def "Choose Src Delivery - Okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[4], n[5], n[3], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1

		when:
		service.chooseSrcDelivery(choice, sol)

		then:
		choice.srcDeliPos == 4
	}

	def "Choose Src Delivery - Not Okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		n[3].setShipmentIdx(12)

		def sol = Helper.set(model, [nd, n[2], n[4], n[5], n[3], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1

		when:
		service.chooseSrcDelivery(choice, sol)

		then:
		thrown NoSuchElementException
	}


	def "Choose Dst Pickup - Simple okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1
		choice.srcDeliPos = 2
		random.nextInt(_) >>> [0, 3]

		when:
		service.chooseDstPickup(choice, sol)

		then:
		choice.dstRouteIdx == 0
		choice.dstPickPos == 4
	}

	def "Choose Dst Pickup - On src pickup"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1
		choice.srcDeliPos = 2
		random.nextInt(_) >>> [0, 0, 3]

		when:
		service.chooseDstPickup(choice, sol)

		then:
		choice.dstPickPos == 4
	}

	def "Choose Dst Pickup - On src delivery"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1
		choice.srcDeliPos = 2
		random.nextInt(_) >>> [0, 1, 3]

		when:
		service.chooseDstPickup(choice, sol)

		then:
		choice.dstPickPos == 4
	}

	def "Choose Dst Delivery - Simple okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1
		choice.srcDeliPos = 2
		choice.dstRouteIdx = 0
		choice.dstPickPos = 3
		random.nextInt(_) >> 3

		when:
		service.chooseDstDelivery(choice, sol)

		then:
		choice.dstDeliPos == 4
	}

	def "Choose Dst Delivery - Okay same pos"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1
		choice.srcDeliPos = 2
		choice.dstRouteIdx = 0
		choice.dstPickPos = 4
		random.nextInt(_) >> 3

		when:
		service.chooseDstDelivery(choice, sol)

		then:
		choice.dstDeliPos == 4
	}

	def "Choose Dst Delivery - Not Okay - no-op pos"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1
		choice.srcDeliPos = 2
		choice.dstRouteIdx = 0
		choice.dstPickPos = 3
		random.nextInt(_) >>> [2, 3]

		when:
		service.chooseDstDelivery(choice, sol)

		then:
		choice.dstDeliPos == 4
	}

	def "Choose Dst Delivery - Delivery before pickup"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 1
		choice.srcDeliPos = 2
		choice.dstRouteIdx = 0
		choice.dstPickPos = 3
		random.nextInt(_) >>> [1, 3]

		when:
		service.chooseDstDelivery(choice, sol)

		then:
		choice.dstDeliPos == 4
	}

	def "Check move - Okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[4], n[3], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 2
		choice.srcDeliPos = 4
		choice.dstRouteIdx = 0
		choice.dstPickPos = 5
		choice.dstDeliPos = 5

		when:
		def result = service.checkMove(choice, sol)
		def gt = sol.routes[0]

		then:
		result
		eq(gt[0], nd)
		eq(gt[1], n[2])
		eq(gt[2], n[3])
		eq(gt[3], n[4])
		eq(gt[4], n[5])
		eq(gt[5], nd)
	}

	def "Check move - Not feasible solution"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		def sol = Helper.set(model, [nd, n[2], n[4], n[3], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPickPos = 2
		choice.srcDeliPos = 4
		choice.dstRouteIdx = 0
		choice.dstPickPos = 5
		choice.dstDeliPos = 5

		evaluationService.check(sol) >> new Quality(cost: 100, penalty: 1)
		service.evaluationService = evaluationService

		when:
		def result = service.checkMove(choice, sol)
		def gt = sol.routes[0]

		then:
		!result
		eq(gt[0], nd)
		eq(gt[1], n[2])
		eq(gt[2], n[4])
		eq(gt[3], n[3])
		eq(gt[4], n[5])
		eq(gt[5], nd)
	}

	def "Random changes"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)

		service.rand = realRandom

		when:
		def result = true
		1.upto(100, {
			def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], n[6], n[7], nd] as Node[])
			try {
				result &= doRandomChange(sol, n)
			} catch(NoSuchElementException e) {e.printStackTrace()}
		})

		then:
		result
	}

	def "Execute - Okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], n[6], n[7], nd] as Node[])
		// 0, 4,5,2,3,6,7
		random.nextInt(_) >>> [2, 0, 0, 4, 0, 6]
		service.NBR_OF_VARIATIONS = 2

		when:
		def result = service.change(sol)
		def gt = result.getGiantRoute()

		then:
		eq(gt[0], nd)
		eq(gt[1], n[6])
		eq(gt[2], n[4])
		eq(gt[3], n[5])
		eq(gt[4], n[2])
		eq(gt[5], n[3])
		eq(gt[6], n[7])
		eq(gt[7], nd)
	}

	boolean doRandomChange(Solution sol, Node[] n) {
		def choice = new Choice()
		service.chooseSrcPickup(choice, sol)
		service.chooseSrcDelivery(choice, sol)
		service.chooseDstPickup(choice, sol)
		service.chooseDstDelivery(choice, sol)

		def oldRoutes = XFPDPMoveUtil.change(sol, choice.toArray())
		service.reverseChange(sol, choice.toArray(), oldRoutes)

		def gt = sol.routes[0]

		def result = (gt[0].externID == nd.externID &&
				gt[1] == n[2] &&
				gt[2] == n[3] &&
				gt[3] == n[4] &&
				gt[4] == n[5] &&
				gt[5] == n[6] &&
				gt[6] == n[7] &&
				gt[7].externID == nd.externID)

		if(!result)
			println "X " + choice.toArray()

		return result
	}

	XFVRPModel initBase(int[] presetBlocks, int[] presetPos) {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: -1,
				ylat: 0,
				geoId: 1,
				demand: [1, 1],
				shipmentIdx: 1,
				presetBlockIdx: presetBlocks[0],
				presetBlockPos: presetPos[0],
				timeWindow: [[0,99]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: -1,
				ylat: -1,
				geoId: 2,
				demand: [-1, -1],
				shipmentIdx: 1,
				presetBlockIdx: presetBlocks[1],
				presetBlockPos: presetPos[1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 4,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				shipmentIdx: 5,
				presetBlockIdx: presetBlocks[2],
				presetBlockPos: presetPos[2],
				timeWindow: [[0,99]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 4,
				ylat: 1,
				geoId: 3,
				demand: [-1, -1],
				shipmentIdx: 5,
				presetBlockIdx: presetBlocks[3],
				presetBlockPos: presetPos[3],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 5,
				externID: "5",
				xlong: 5,
				ylat: 0,
				geoId: 5,
				demand: [1, 1],
				shipmentIdx: 15,
				presetBlockIdx: presetBlocks[2],
				presetBlockPos: presetPos[2],
				timeWindow: [[0,99]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 6,
				externID: "6",
				xlong: 6,
				ylat: 1,
				geoId: 6,
				demand: [-1, -1],
				shipmentIdx: 15,
				presetBlockIdx: presetBlocks[3],
				presetBlockPos: presetPos[3],
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

		parameter.setWithPDP(true)

		return TestXFVRPModel.get(nodes, v, parameter)
	}

	void eq(Node a, Node b) {
		assert a.externID == b.externID
	}
}
