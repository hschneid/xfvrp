package xf.xfvrp.opt.evaluation


import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.XFVRP
import xf.xfvrp.base.*
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.XFVRPOptType

class EvaluationServiceCapacityIntSpec extends Specification {

	def "Check capacities with different compartment types"() {
		XFVRP vrp = new XFVRP()
		vrp.setMetric(Metrics.EUCLEDIAN.get())
		vrp.addOptType(XFVRPOptType.RELOCATE)
		vrp.addVehicle().setName('V').setCapacity([10, 5, 6] as float)
		vrp.addCompartment(CompartmentType.DELIVERY)
		vrp.addCompartment(CompartmentType.PICKUP)
		vrp.addCompartment(CompartmentType.MIXED)

		vrp.addDepot().setExternID('nD')
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 1
		result.routes[0].vehicle.name == 'V1'
	}

}
