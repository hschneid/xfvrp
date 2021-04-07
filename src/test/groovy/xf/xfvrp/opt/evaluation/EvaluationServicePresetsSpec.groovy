package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.base.monitor.StatusManager
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.init.ModelBuilder

class EvaluationServicePresetsSpec extends Specification {

	def statusManager = Stub StatusManager
	def service = new EvaluationService();

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
		siteType: SiteType.DEPOT,
		xlong: -1,
		ylat: -1,
		demand: [0, 0],
		timeWindow: [[0,99],[2,99]]
		).getNode()

	def sol;

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Block Preset - Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen(v, [2, 2, 3] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}

	def "Block Preset - Not Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen(v, [2, 2, 3] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[3], nd, n[2], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 6.242) < 0.001
	}
	
	def "Block Count Preset - Not Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen(v, [2, 2, 3] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 3.414) < 0.001
	}
	
	def "Block Rank Preset - Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithRanks(v, [2, 3, 2] as int[], [1, 1, 2] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Block Rank Preset - Not Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithRanks(v, [2, 3, 2] as int[], [2, 1, 1] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Block Pos Preset - Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithPos(v, [2, 2, 2] as int[], [1, 2, 3] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Block Pos Preset - Not Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithPos(v, [2, 3, 2] as int[], [2, 1, 3] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Block Pos Preset - Okay, DEFAULT BLOCK"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithPos(v, [0, 3, 0] as int[], [2, 1, 3] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Block Pos Preset - Not Okay 2"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithPos(v, [2, 2, 2] as int[], [1, 3, 2] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Depot Preset - Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithDepots(v, [0, 4, 0] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], nd2, n[3], nd, n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 9.656) < 0.001
	}
	
	def "Depot Preset - Not Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithDepots(v, [0, 0, 4] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], nd2, n[3], nd, n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 9.656) < 0.001
	}
	
	def "Vehicle Preset - Okay"() {
		def v = new TestVehicle(idx: 1, name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithVehicles(v, [1, 1, 1] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Vehicle Preset - Not Okay"() {
		def v = new TestVehicle(idx: 1, name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithVehicles(v, [1, 2, 1] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	def "Blacklist Preset - Okay"() {
		def v = new TestVehicle(idx: 1, name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithBlackNodes(v, [-1, 3, 2] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], nd, n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 5.414) < 0.001
	}
	
	def "Blacklist Preset - Not Okay"() {
		def v = new TestVehicle(idx: 1, name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScenWithBlackNodes(v, [-1, 3, 2] as int[])
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4) < 0.001
	}
	
	XFVRPModel initScen(Vehicle v, int[] presetBlocks) {
		return initScenAbstract(v, presetBlocks, [0, 0, 0] as int[], [0, 0, 0] as int[], [0, 0, 0] as int[], [-1, -1, -1] as int[])
	}
	
	XFVRPModel initScenWithRanks(Vehicle v, int[] presetBlocks, int[] presetRanks) {
		return initScenAbstract(v, presetBlocks, presetRanks, [0, 0, 0] as int[], [0, 0, 0] as int[], [-1, -1, -1] as int[])
	}
	
	XFVRPModel initScenWithPos(Vehicle v, int[] presetBlocks, int[] presetPos) {
		return initScenAbstract(v, presetBlocks, [0, 0, 0] as int[], presetPos, [0, 0, 0] as int[], [-1, -1, -1] as int[])
	}
	
	XFVRPModel initScenWithVehicles(Vehicle v, int[] presetVehicles) {
		return initScenAbstract(v, [0, 0, 0] as int[], [0, 0, 0] as int[], [0, 0, 0] as int[], presetVehicles, [-1, -1, -1] as int[])
	}
	
	XFVRPModel initScenWithBlackNodes(Vehicle v, int[] presetBlackNodes) {
		return initScenAbstract(v, [0, 0, 0] as int[], [0, 0, 0] as int[], [0, 0, 0] as int[], [0, 0, 0] as int[], presetBlackNodes)
	}
	
	XFVRPModel initScenWithDepots(Vehicle v, int[] presetDepots) {
		return initMultiDepotScenAbstract(v, [0, 0, 0] as int[], [0, 0, 0] as int[], [0, 0, 0] as int[], presetDepots)
	}

	XFVRPModel initScenAbstract(Vehicle v, int[] presetBlocks, int[] presetRanks, int[] presetPos, int[] presetVehicles, int[] presetBlackNodes) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 0,
				ylat: 1,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				presetBlockIdx: presetBlocks[0],
				presetBlockRank: presetRanks[0],
				presetBlockPos: presetPos[0],
				presetVehicleIdx: presetVehicles[0],
				presetBlackNodeIdx: presetBlackNodes[0],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 1,
				ylat: 1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				presetBlockIdx: presetBlocks[1],
				presetBlockRank: presetRanks[1],
				presetBlockPos: presetPos[1],
				presetVehicleIdx: presetVehicles[1],
				presetBlackNodeIdx: presetBlackNodes[1],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				presetBlockIdx: presetBlocks[2],
				presetBlockRank: presetRanks[2],
				presetBlockPos: presetPos[2],
				presetVehicleIdx: presetVehicles[2],
				presetBlackNodeIdx: presetBlackNodes[2],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0);
		n1.setIdx(1);
		n2.setIdx(2);
		n3.setIdx(3);

		def nodes = [nd, n1, n2, n3] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new ModelBuilder().build(nodes, v, metric, parameter, statusManager)
	}
	
	XFVRPModel initMultiDepotScenAbstract(Vehicle v, int[] presetBlocks, int[] presetRanks, int[] presetPos, int[] presetDepots) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 0,
				ylat: 1,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				presetBlockIdx: presetBlocks[0],
				presetBlockRank: presetRanks[0],
				presetBlockPos: presetPos[0],
				presetDepotGlobalIdx: presetDepots[0],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 1,
				ylat: 1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				presetBlockIdx: presetBlocks[1],
				presetBlockRank: presetRanks[1],
				presetBlockPos: presetPos[1],
				presetDepotGlobalIdx: presetDepots[1],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				presetBlockIdx: presetBlocks[2],
				presetBlockRank: presetRanks[2],
				presetBlockPos: presetPos[2],
				presetDepotGlobalIdx: presetDepots[2],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		nd2.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)

		def nodes = [nd, nd2, n1, n2, n3] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
