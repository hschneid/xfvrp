package xf.xfvrp.opt.improve.ils

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.monitor.StatusManager
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.XFVRPOptBase
import xf.xfvrp.opt.evaluation.EvaluationService
import xf.xfvrp.opt.improve.giantroute.XFVRP2Opt
import xf.xfvrp.opt.improve.routebased.move.XFVRPSegmentMove
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove

class XFVRPILSSpec extends Specification {

	def opt1 = Stub XFVRP2Opt
	def opt2 = Stub XFVRPSingleMove
	def opt3 = Stub XFVRPSegmentMove
	def evaluationService = Stub EvaluationService
	def statusManager = Stub StatusManager
	def service = new XFVRPILS(evaluationService: evaluationService)

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

	def "Choose"() {
		service.optPropArr = [0.1, 0.2, 0.3, 0.4]

		when:
		def res = service.choose([false, false, false, false] as boolean[])
		then:
		res >= 0
		res < service.optPropArr.length
	}

	def "Choose Only one"() {
		service.optPropArr = [0.1, 0.2, 0.3, 0.4]

		when:
		def res = service.choose([true, true, false, true] as boolean[])
		then:
		res == 2
	}

	def "Check termination - Loops"() {
		def model = initScen()
		service.setModel(model)

		service.model.getParameter().setNbrOfILSLoops(15)

		when:
		def result = service.checkTerminationCriteria(16)
		then:
		!result
	}

	def "Check termination - Time"() {
		def model = initScen()
		service.setModel(model)

		service.model.getParameter().setNbrOfILSLoops(15)
		service.model.getParameter().setMaxRunningTimeInSec(1)
		def statusManager = new StatusManager(startTime: System.currentTimeMillis())
		service.statusManager = statusManager

		Thread.sleep(1500)

		when:
		def result = service.checkTerminationCriteria(1)

		then:
		!result
	}

	def "Check termination - Okay"() {
		def model = initScen()
		service.setModel(model)

		service.model.getParameter().setNbrOfILSLoops(15)
		service.model.getParameter().setMaxRunningTimeInSec(10000)
		def statusManager = new StatusManager(startTime: System.currentTimeMillis())
		service.statusManager = statusManager

		when:
		def result = service.checkTerminationCriteria(1)

		then:
		result
	}

	def "Local search"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		service.optArr = [opt1, opt2, opt3] as XFVRPOptBase[]
		service.optPropArr = [0.1, 0.3, 0.6]

		def sol = Helper.set(model, [nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def quality = new Quality(cost: 111, penalty: 0)
		evaluationService.check(_) >> quality
		opt1.improve(_ ,_) >> sol
		opt2.improve(_ ,_) >> sol
		opt3.improve(_ ,_) >> sol

		when:
		def result = service.localSearch(sol)

		then:
		result != null
	}


	def "Execute"() {
		def model = initScen()
		model.getParameter().setNbrOfILSLoops(5)
		def n = model.getNodes()
		service.setModel(model)
		service.optArr = [opt1, opt2]
		service.optPropArr = [0.5, 0.5]
		def quality = new Quality(cost: 100, penalty: 0)
		def betterQuality = new Quality(cost: 80, penalty: 0)
		evaluationService.check(_ as Solution) >>> [quality, betterQuality]

		def sol = Helper.set(model, [nd, n[2], n[5], n[4], nd, nd, n[3], nd] as Node[])

		opt1.execute(_ ,_ , _) >> sol
		opt2.execute(_ ,_ , _) >> sol

		when:
		def result = service.execute(sol, model, statusManager)

		then:
		result != null
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
				ylat: -1,
				geoId: 2,
				demand: [1, 1],
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
}
