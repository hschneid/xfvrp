package xf.xfvrp.opt.init.check.vrp

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.InvalidReason
import xf.xfvrp.base.Node
import xf.xfvrp.base.Quality
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.base.preset.BlockNameConverter
import xf.xfvrp.opt.improve.XFVRPRelocate
import xf.xfvrp.opt.init.solution.vrp.SolutionBuilderDataBag

class CheckServiceSpec extends Specification {

	def opt = Stub XFVRPRelocate
	def checkCustomerService = Stub CheckCustomerService

	def service = new CheckService();

	def setup() {
		service.optimizationMethod = opt
		opt.execute(_, _, null) >> {it[0]}
	}

	def "Get blocks"() {
		def nodes = getNodes()

		def v = new TestVehicle().getVehicle()

		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		when:
		def result = service.getBlocks(model)

		then:
		result.get(1).size() == 2
		result.get(1).contains(nodes[1])
		result.get(1).contains(nodes[2])
		result.get(2).size() == 1
		result.get(2).contains(nodes[3])
		result.get(BlockNameConverter.DEFAULT_BLOCK_IDX).size() == 2
		result.get(BlockNameConverter.DEFAULT_BLOCK_IDX).contains(nodes[0])
		result.get(BlockNameConverter.DEFAULT_BLOCK_IDX).contains(nodes[4])
	}

	def "Set nodes invalid"() {
		def n2 = new TestNode(externID: "1", siteType: SiteType.CUSTOMER, presetBlockIdx: 1).getNode();
		def n3 = new TestNode(externID: "2", siteType: SiteType.CUSTOMER, presetBlockIdx: 1).getNode();
		def n4 = new TestNode(externID: "3", siteType: SiteType.CUSTOMER, presetBlockIdx: 1).getNode();

		def nodesOfBlock = [n2, n3, n4]
		def invalidNodes = []

		n3.setInvalidReason(InvalidReason.CAPACITY)

		when:
		service.setNodesOfBlockInvalid(nodesOfBlock, invalidNodes, n3)
		then:
		invalidNodes.size() == 3
		invalidNodes.contains(n2)
		invalidNodes.contains(n3)
		invalidNodes.contains(n4)
		n2.getInvalidReason() == n3.getInvalidReason()
		n4.getInvalidReason() == n3.getInvalidReason()
	}

	def "Check block - Okay"() {
		def nodes = getNodes()

		def v = new TestVehicle().getVehicle()

		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		opt.check(_) >> new Quality(cost: 100, penalty: 0)

		when:
		def result = service.checkBlock(nodesOfBlock, model)

		then:
		result
	}

	def "Check block 2 - Okay"() {
		def nodes = getNodes()

		def v = new TestVehicle().getVehicle()

		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag(
				validNodes: Arrays.asList(nodes)
				)

		def invalidNodes = new ArrayList<Node>()
		opt.check(_) >> new Quality(cost: 100, penalty: 0)

		when:
		service.checkBlock(dataBag, invalidNodes, model, 1, nodesOfBlock)

		then:
		invalidNodes.size() == 0
		dataBag.validNodes.size() == 5
	}

	def "Check block 2 - Default block idx"() {
		def nodes = getNodes()

		def v = new TestVehicle().getVehicle()

		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag(
				validNodes: Arrays.asList(nodes)
				)

		def invalidNodes = new ArrayList<Node>()
		opt.check(_) >> new Quality(cost: 100, penalty: 0)

		when:
		service.checkBlock(dataBag, invalidNodes, model, BlockNameConverter.DEFAULT_BLOCK_IDX, nodesOfBlock)

		then:
		invalidNodes.size() == 0
		dataBag.validNodes.size() == 5
	}

	def "Check block 2 - Not okay"() {
		def nodes = getNodes()

		def v = new TestVehicle().getVehicle()

		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def validNodes = new ArrayList<Node>()
		validNodes.addAll(Arrays.asList(nodes))
		def dataBag = new SolutionBuilderDataBag(
				validNodes: validNodes
				)

		def invalidNodes = new ArrayList<Node>()
		opt.check(_) >> new Quality(cost: 100, penalty: 1)

		when:
		service.checkBlock(dataBag, invalidNodes, model, 1, nodesOfBlock)

		then:
		invalidNodes.size() == 2
		invalidNodes.contains(nodes[1])
		invalidNodes.contains(nodes[2])
		dataBag.validNodes.size() == 3
	}

	def "Check nodes of block - Okay for customers"() {
		def nodes = getNodes()
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag()

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, model, dataBag) >> true

		when:
		def result = service.checkNodesOfBlock(1, nodesOfBlock, dataBag, invalidNodes, model)
		
		then:
		result
		invalidNodes.size() == 0
		dataBag.validNodes.size() == 2
		dataBag.validNodes.contains(nodes[1])
		dataBag.validNodes.contains(nodes[2])
		dataBag.knownSequencePositions.size() == 2
		dataBag.knownSequencePositions.contains(1)
		dataBag.knownSequencePositions.contains(2)
	}

	Node[] getNodes() {
		return [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			new TestNode(externID: "1", siteType: SiteType.CUSTOMER, presetBlockIdx: 1, presetBlockPos: 1).getNode(),
			new TestNode(externID: "2", siteType: SiteType.CUSTOMER, presetBlockIdx: 1, presetBlockPos: 2).getNode(),
			new TestNode(externID: "3", siteType: SiteType.CUSTOMER, presetBlockIdx: 2).getNode(),
			new TestNode(externID: "4", siteType: SiteType.CUSTOMER, presetBlockIdx: BlockNameConverter.DEFAULT_BLOCK_IDX).getNode()
		] as Node[]
	}

}
