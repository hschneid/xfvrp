package xf.xfvrp.opt.init.check.vrp

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.base.preset.BlockNameConverter
import xf.xfvrp.opt.improve.XFVRPRelocate
import xf.xfvrp.opt.init.solution.vrp.SolutionBuilderDataBag

class CheckServiceSpec extends Specification {

	def opt = Stub XFVRPRelocate
	def checkCustomerService = Stub CheckCustomerService

	def service = new CheckService(
			checkCustomerService: checkCustomerService,
			optimizationMethod: opt
	);

	def setup() {
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
		//result.get(BlockNameConverter.DEFAULT_BLOCK_IDX).size() == 2
		//result.get(BlockNameConverter.DEFAULT_BLOCK_IDX).contains(nodes[0])
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
				validCustomers: Arrays.asList(nodes)
				)

		def invalidNodes = new ArrayList<Node>()
		opt.check(_) >> new Quality(cost: 100, penalty: 0)

		when:
		service.checkBlock(dataBag, invalidNodes, model, 1, nodesOfBlock)

		then:
		invalidNodes.size() == 0
		dataBag.validCustomers.size() == 5
	}

	def "Check block 2 - Default block idx"() {
		def nodes = getNodes()

		def v = new TestVehicle().getVehicle()

		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag(
				validCustomers: Arrays.asList(nodes)
				)

		def invalidNodes = new ArrayList<Node>()
		opt.check(_) >> new Quality(cost: 100, penalty: 0)

		when:
		service.checkBlock(dataBag, invalidNodes, model, BlockNameConverter.DEFAULT_BLOCK_IDX, nodesOfBlock)

		then:
		invalidNodes.size() == 0
		dataBag.validCustomers.size() == 5
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
				validCustomers: validNodes
				)

		def invalidNodes = new ArrayList<Node>()
		opt.check(_) >> new Quality(cost: 100, penalty: 1)

		when:
		service.checkBlock(dataBag, invalidNodes, model, 1, nodesOfBlock)

		then:
		invalidNodes.size() == 2
		invalidNodes.contains(nodes[1])
		invalidNodes.contains(nodes[2])
		dataBag.validCustomers.size() == 3
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

		checkCustomerService.checkCustomer(_, model, dataBag) >>> [true]

		when:
		def result = service.checkNodesOfBlock(1, nodesOfBlock, dataBag, invalidNodes, model)
		
		then:
		result
		invalidNodes.size() == 0
		dataBag.validCustomers.size() == 2
		dataBag.validCustomers.contains(nodes[1])
		dataBag.validCustomers.contains(nodes[2])
		dataBag.knownSequencePositions.size() == 2
		dataBag.knownSequencePositions.contains(1)
		dataBag.knownSequencePositions.contains(2)
	}
	
	def "Check nodes of block - Not okay for customers"() {
		def nodes = getNodes()
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag()

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, _, _) >>> [true, false]

		when:
		def result = service.checkNodesOfBlock(1, nodesOfBlock, dataBag, invalidNodes, model)
		
		then:
		!result
		invalidNodes.size() == 2
		invalidNodes.contains(nodes[1])
		invalidNodes.contains(nodes[2])
		dataBag.validCustomers.size() == 0
	}
	
	def "Check nodes of block - Not okay for a customer of default block"() {
		def nodes = getNodes()
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag()

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, _, _) >>> [true, false]

		when:
		def result = service.checkNodesOfBlock(BlockNameConverter.DEFAULT_BLOCK_IDX, nodesOfBlock, dataBag, invalidNodes, model)
		
		then:
		result
		invalidNodes.size() == 1
		invalidNodes.contains(nodes[2])
		dataBag.validCustomers.size() == 1
		dataBag.validCustomers.contains(nodes[1])
	}
	
	def "Check nodes of block - Okay for depots"() {
		def nodes = getNodes()
		nodes[1].siteType = SiteType.DEPOT
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[0], nodes[1]]

		def dataBag = new SolutionBuilderDataBag()

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, model, dataBag) >> true

		when:
		def result = service.checkNodesOfBlock(1, nodesOfBlock, dataBag, invalidNodes, model)
		
		then:
		result
		invalidNodes.size() == 0
		dataBag.validDepots.size() == 2
		dataBag.validDepots.contains(nodes[0])
		dataBag.validDepots.contains(nodes[1])
		dataBag.knownSequencePositions.size() == 0
	}
	
	def "Check nodes of block - Okay for replenishs"() {
		def nodes = getNodes()
		nodes[0].siteType = SiteType.REPLENISH
		nodes[1].siteType = SiteType.REPLENISH
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[0], nodes[1]]

		def dataBag = new SolutionBuilderDataBag()

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, model, dataBag) >> true

		when:
		def result = service.checkNodesOfBlock(1, nodesOfBlock, dataBag, invalidNodes, model)
		
		then:
		result
		invalidNodes.size() == 0
		dataBag.getValidReplenish().size() == 2
		dataBag.getValidReplenish().contains(nodes[0])
		dataBag.getValidReplenish().contains(nodes[1])
		dataBag.knownSequencePositions.size() == 0
	}
	
	def "Check blocks - Okay"() {
		def nodes = getNodes()
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag()

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, model, dataBag) >>> [true]
		
		def blocks = [
			(0) : [nodes[0], nodes[4]],
			(1) : [nodes[1], nodes[2]],
			(2) : [nodes[3]]
			]

		when:
		service.checkBlocks(blocks, dataBag, invalidNodes, model)
		
		then:
		invalidNodes.size() == 0
		dataBag.validDepots.size() == 1
		dataBag.validCustomers.size() == 4
	}
	
	def "Check blocks - One not okay"() {
		def nodes = getNodes()
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def nodesOfBlock = [nodes[1], nodes[2]]

		def dataBag = new SolutionBuilderDataBag()

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, model, dataBag) >>> [true, true, true, false]
		
		def blocks = [
			(0) : [nodes[0], nodes[4]],
			(1) : [nodes[1], nodes[2]],
			(2) : [nodes[3]]
			]

		when:
		service.checkBlocks(blocks, dataBag, invalidNodes, model)
		
		then:
		invalidNodes.size() == 1
		invalidNodes.contains(nodes[3])
		dataBag.validDepots.size() == 1
		dataBag.validCustomers.size() == 3
	}
	
	def "Check - Okay"() {
		def nodes = getNodes()
		def v = new TestVehicle().getVehicle()
		def p = new XFVRPParameter();

		def iMetric = new AcceleratedMetricTransformator().transform(new EucledianMetric(), nodes, v);
		def model = new XFVRPModel(nodes, iMetric, iMetric, v, p)

		def invalidNodes = new ArrayList<Node>()

		checkCustomerService.checkCustomer(_, _, _) >>> [true]

		when:
		def result = service.check(model, invalidNodes)
		
		then:
		invalidNodes.size() == 0
		result.validCustomers.size() == 4
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
