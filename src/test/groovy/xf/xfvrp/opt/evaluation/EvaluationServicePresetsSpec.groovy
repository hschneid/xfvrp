package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class EvaluationServicePresetsSpec extends Specification {

	def service = new EvaluationService();

	def nd = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
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
		def model = initScenWithPos(v, [2, 3, 2] as int[], [1, 0, 2] as int[])
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
		return initScenAbstract(v, presetBlocks, [0, 0, 0] as int[], [0, 0, 0] as int[])
	}
	
	XFVRPModel initScenWithRanks(Vehicle v, int[] presetBlocks, int[] presetRanks) {
		return initScenAbstract(v, presetBlocks, presetRanks, [0, 0, 0] as int[])
	}
	
	XFVRPModel initScenWithPos(Vehicle v, int[] presetBlocks, int[] presetPos) {
		return initScenAbstract(v, presetBlocks, [0, 0, 0] as int[], presetPos)
	}

	XFVRPModel initScenAbstract(Vehicle v, int[] presetBlocks, int[] presetRanks, int[] presetPos) {
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
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0);
		n1.setIdx(1);
		n2.setIdx(2);
		n3.setIdx(3);

		def nodes = [nd, n1, n2, n3] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
