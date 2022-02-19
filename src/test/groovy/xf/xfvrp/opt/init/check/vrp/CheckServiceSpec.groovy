package xf.xfvrp.opt.init.check.vrp

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.base.preset.BlockNameConverter
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove
import xf.xfvrp.opt.init.solution.vrp.SolutionBuilderDataBag

class CheckServiceSpec extends Specification {

	def opt = Stub XFVRPSingleMove
	def checkCustomerService = Stub CheckCustomerService

	def service = new CheckService(
			checkCustomerService: checkCustomerService,
			optimizationMethod: opt
	)

	def setup() {
		opt.execute(_, _, null) >> {it[0]}
	}
	
	def "Check nodes of block - Okay for depots"() {
		def nodes = getNodes()
		nodes[1].siteType = SiteType.DEPOT
		def model = build(nodes)

		def nodesOfBlock = [nodes[0], nodes[1]]

		def dataBag = new SolutionBuilderDataBag()

		checkCustomerService.checkCustomer(_ as Node, model, dataBag) >> true

		when:
		def result = service.checkNodesOfBlock(1, nodesOfBlock, dataBag, model)
		
		then:
		result
		dataBag.validDepots.size() == 2
		dataBag.validDepots.contains(nodes[0])
		dataBag.validDepots.contains(nodes[1])
		dataBag.knownSequencePositions.size() == 0
	}
	
	def "Check nodes of block - Okay for replenishs"() {
		def nodes = getNodes()
		nodes[0].siteType = SiteType.REPLENISH
		nodes[1].siteType = SiteType.REPLENISH
		def model = build(nodes)

		def nodesOfBlock = [nodes[0], nodes[1]]

		def dataBag = new SolutionBuilderDataBag()

		checkCustomerService.checkCustomer(_, model, dataBag) >> true

		when:
		def result = service.checkNodesOfBlock(1, nodesOfBlock, dataBag, model)
		
		then:
		result
		dataBag.getValidReplenish().size() == 2
		dataBag.getValidReplenish().contains(nodes[0])
		dataBag.getValidReplenish().contains(nodes[1])
		dataBag.knownSequencePositions.size() == 0
	}
	
	def "Check blocks - Okay"() {
		def nodes = getNodes()
		def model = build(nodes)

		checkCustomerService.checkCustomer(_, model, _ as SolutionBuilderDataBag) >>> [true]
		
		def blocks = [
			(0) : [nodes[0], nodes[4]],
			(1) : [nodes[1], nodes[2]],
			(2) : [nodes[3]]
			]

		when:
		def dataBag = service.checkBlocks(blocks, model)
		
		then:
		dataBag.validDepots.size() == 1
		dataBag.validCustomers.size() == 4
	}
	
	def "Check blocks - One not okay"() {
		def nodes = getNodes()
		def model = build(nodes)

		checkCustomerService.checkCustomer(_, model, _ as SolutionBuilderDataBag) >>> [true, true, true, false]
		
		def blocks = [
			(0) : [nodes[0], nodes[4]],
			(1) : [nodes[1], nodes[2]],
			(2) : [nodes[3]]
			]

		when:
		def dataBag = service.checkBlocks(blocks, model)
		
		then:
		dataBag.validDepots.size() == 1
		dataBag.validCustomers.size() == 3
		dataBag.validCustomers.count {n -> n == nodes[3]} == 0
	}

	XFVRPModel build(Node[] nodes) {
		def v = new TestVehicle().getVehicle()

		return TestXFVRPModel.get(Arrays.asList(nodes), v)
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
