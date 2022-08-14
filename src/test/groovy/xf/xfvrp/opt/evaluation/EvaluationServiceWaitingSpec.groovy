package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.XFVRP
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.base.monitor.DefaultStatusMonitor
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.XFVRPOptTypes

class EvaluationServiceWaitingSpec extends Specification {

	def "Shift times to minimal waiting time"() {
		def xfvrp = build()
		xfvrp.addCustomer().setExternID("C1").setXlong(0).setYlat(50).setDemand(1)
				.setOpen1(100).setClose1(200).setServiceTime(10)
		xfvrp.addCustomer().setExternID("C2").setXlong(0).setYlat(100).setDemand(1)
				.setOpen1(200).setClose1(300).setServiceTime(10)
		xfvrp.addCustomer().setExternID("C3").setXlong(0).setYlat(150).setDemand(1)
				.setOpen1(300).setClose1(400).setServiceTime(10)
		xfvrp.getParameters().setPredefinedSolutionString("{(C1,C2,C3)}")

		when:
		xfvrp.executeRoutePlanning()
		def result = xfvrp.getReport()
		then:
		result != null
		// 0: Abfahrt 50 um 100 dazu sein => 0 Wartezeit
		// 1: Abfahrt 150 um 200 dazu sein => 0 Wartezeit
	}

	XFVRP build() {
		def vrp = new XFVRP()
		vrp.addDepot().setExternID("DEP1").setXlong(0).setYlat(0)
				.setOpen1(0).setClose1(1000)

		vrp.addVehicle()
				.setName("VEH")
				.setCapacity([1000] as float[])
				.setMaxRouteDuration(400)

		vrp.setMetric(Metrics.EUCLEDIAN.get())
		vrp.addOptType(XFVRPOptTypes.RELOCATE)
		vrp.setStatusMonitor(new DefaultStatusMonitor())

		return vrp
	}


}
