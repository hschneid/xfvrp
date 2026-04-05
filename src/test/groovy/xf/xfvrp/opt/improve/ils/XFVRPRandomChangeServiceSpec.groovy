package xf.xfvrp.opt.improve.ils

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.opt.improve.ils.XFVRPRandomChangeService.Choice

class XFVRPRandomChangeServiceSpec extends Specification {

	def random = Stub Random
	def service = new XFVRPRandomChangeService()

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

	def setup() {
		service.rand = random
	}

	def "Choose Src - Simple okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		random.nextInt(_) >>> [0, 1]

		when:
		service.chooseSrc(choice, sol)

		then:
		choice.srcRouteIdx == 0
		choice.srcPos == 2
		choice.segmentLength == 0
	}

	def "Choose Src - With blocks"() {
		def model = initBase([0, 1, 1, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		random.nextInt(_) >>> [0, 2]

		when:
		service.chooseSrc(choice, sol)

		then:
		choice.srcRouteIdx == 0
		choice.srcPos == 2
		choice.segmentLength == 0
	}

	def "Choose Src - With Pos"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 1, 2, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		random.nextInt(_) >>> [0, 1]

		when:
		service.chooseSrc(choice, sol)

		then:
		choice.srcRouteIdx == 0
		choice.srcPos == 2
		choice.segmentLength == 1
	}

	def "Choose Src - With Blocks and Pos"() {
		def model = initBase([1, 1, 1, 0] as int[], [1, 2, 3, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		random.nextInt(_) >>> [0, 2]

		when:
		service.chooseSrc(choice, sol)

		then:
		choice.srcRouteIdx == 0
		choice.srcPos == 1
		choice.segmentLength == 2
	}

	def "Choose Dst - Simple okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPos = 1
		choice.segmentLength = 0
		random.nextInt(_) >>> [0,3]

		when:
		service.chooseDst(choice, sol)

		then:
		choice.dstRouteIdx == 0
		choice.dstPos == 4
	}

	def "Choose Dst - In src path"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPos = 1
		choice.segmentLength = 2
		random.nextInt(_) >>> [0, 2, 4]

		when:
		service.chooseDst(choice, sol)

		then:
		choice.dstRouteIdx == 0
		choice.dstPos == 5
	}

	def "Choose Dst - With blocks"() {
		def model = initBase([0, 0, 1, 1] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPos = 1
		choice.segmentLength = 0
		random.nextInt(_) >>> [0,3]

		when:
		service.chooseDst(choice, sol)

		then:
		choice.dstRouteIdx == 0
		choice.dstPos == 3
	}

	def "Check move - Okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], nd, n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPos = 1
		choice.segmentLength = 0
		choice.dstRouteIdx = 1
		choice.dstPos = 1

		when:
		def result = service.checkMove(choice, sol)
		def gt = Helper.get(sol)

		then:
		result
		eq(gt[0], nd)
		eq(gt[1], n[3])
		eq(gt[2], n[4])
		eq(gt[3], nd)
		eq(gt[4], nd)
		eq(gt[5], n[2])
		eq(gt[6], n[5])
		eq(gt[7], nd)
	}

	def "Check move - Not Okay 1"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], nd, n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 1
		choice.srcPos = 1
		choice.segmentLength = 0
		choice.dstRouteIdx = 0
		choice.dstPos = 1

		when:
		def result = service.checkMove(choice, sol)
		def gt = Helper.get(sol)

		then:
		!result
		eq(gt[0], nd)
		eq(gt[1], n[2])
		eq(gt[2], n[3])
		eq(gt[3], n[4])
		eq(gt[4], nd)
		eq(gt[5], nd)
		eq(gt[6], n[5])
		eq(gt[7], nd)
	}

	def "Check move - Not Okay 2"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], nd, n[3], n[4], n[5], nd] as Node[])

		def choice = new Choice()
		choice.srcRouteIdx = 0
		choice.srcPos = 1
		choice.segmentLength = 0
		choice.dstRouteIdx = 1
		choice.dstPos = 1

		when:
		def result = service.checkMove(choice, sol)
		def gt = Helper.get(sol)

		then:
		!result
		eq(gt[0], nd)
		eq(gt[1], n[2])
		eq(gt[2], nd)
		eq(gt[3], nd)
		eq(gt[4], n[3])
		eq(gt[5], n[4])
		eq(gt[6], n[5])
		eq(gt[7], nd)
	}

	def "Execute - Okay"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], nd, n[5], nd] as Node[])

		random.nextInt(_) >>> [0,0,1,1,1,0,0,1]
		service.NBR_OF_VARIATIONS = 2

		when:
		service.change(sol)
		def gt = Helper.get(sol)

		then:
		eq(gt[0], nd)
		eq(gt[1], n[3])
		eq(gt[2], n[5])
		eq(gt[3], n[4])
		eq(gt[4], nd)
		eq(gt[5], nd)
		eq(gt[6], n[2])
		eq(gt[7], nd)
	}

	def "Execute - Reach termination criteria"() {
		def model = initBase([0, 0, 0, 0] as int[], [0, 0, 0, 0] as int[])
		def n = model.getNodes()
		service.setModel(model)


		def sol = Helper.set(model, [nd, n[2], n[3], n[4], nd, n[5], nd] as Node[])

		random.nextInt(_) >>> [0,0,0,2,1,0,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1]
		service.NBR_OF_VARIATIONS = 2
		service.NBR_ACCEPTED_INVALIDS = 10

		when:
		service.change(sol)
		def gt = Helper.get(sol)

		then:
		eq(gt[0], nd)
		eq(gt[1], n[3])
		eq(gt[2], n[2])
		eq(gt[3], n[4])
		eq(gt[4], nd)
		eq(gt[5], nd)
		eq(gt[6], n[5])
		eq(gt[7], nd)
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
				presetBlockIdx: presetBlocks[0],
				presetBlockPos: presetPos[0],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: -1,
				ylat: -1,
				geoId: 2,
				demand: [1, 1],
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
				presetBlockIdx: presetBlocks[2],
				presetBlockPos: presetPos[2],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 4,
				ylat: 1,
				geoId: 3,
				demand: [1, 1],
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

		def nodes = [nd, nd2, n1, n2, n3, n4]

		return TestXFVRPModel.get(nodes, v)
	}

	void eq(Node a, Node b) {
		assert a.externID == b.externID
	}
}
