package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.opt.XFVRPOptTypes
import xf.xfvrp.report.StringWriter

import java.util.stream.Collectors

class EvaluationServiceMaxRoutesDepotSpec extends Specification {

	def "Check capacities with different compartment types - all fit on 1 truck"() {
		XFVRP vrp = build()
		vrp.addVehicle().setName('V').setCapacity([3] as float[])

		vrp.addDepot().setExternID('nD')
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand(1)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand(1)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand(1)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand(1)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand(1)
		vrp.addCustomer().setExternID('n6').setXlong(-105).setYlat(-105).setDemand(1)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()
		println StringWriter.write(result);

		then:
		result.routes.size() == 2
		result.routes[0].vehicle.name == 'V'
		result.routes[1].vehicle.name == 'V'
	}

	XFVRP build() {
		XFVRP vrp = new XFVRP()
		vrp.setMetric(Metrics.EUCLEDIAN.get())
		vrp.addOptType(XFVRPOptTypes.RELOCATE)
		return vrp
	}
}
