package xf.xfvrp.base.preset

import spock.lang.Specification
import util.instances.TestVehicle
import xf.xfvrp.base.Vehicle

class VehiclePriorityInitialiserSpec extends Specification {

	def service = new VehiclePriorityInitialiser();
	
	def "Set vehicle priorities with mix"() {
		def vehicles = [
			new TestVehicle(idx: 1, priority: 5).getVehicle(),
			new TestVehicle(idx: 2, priority: 1).getVehicle(),
			new TestVehicle(idx: 3, priority: Vehicle.PRIORITY_UNDEF, capacity: [10]).getVehicle(),
			new TestVehicle(idx: 4, priority: Vehicle.PRIORITY_UNDEF, capacity: [20]).getVehicle(),
			] as Vehicle[]
				
		when:
		def result = service.execute(vehicles)

		then:
		result != null
		result.length == 4
		result[0].idx == 2
		result[1].idx == 1
		result[2].idx == 4
		result[3].idx == 3
	}
	
	def "Set vehicle priorities only undef"() {
		def vehicles = [
			new TestVehicle(idx: 1, priority: Vehicle.PRIORITY_UNDEF, capacity: [10]).getVehicle(),
			new TestVehicle(idx: 2, priority: Vehicle.PRIORITY_UNDEF, capacity: [20]).getVehicle(),
			new TestVehicle(idx: 3, priority: Vehicle.PRIORITY_UNDEF, capacity: [30]).getVehicle(),
			new TestVehicle(idx: 4, priority: Vehicle.PRIORITY_UNDEF, capacity: [40]).getVehicle(),
			] as Vehicle[]
				
		when:
		def result = service.execute(vehicles)

		then:
		result != null
		result.length == 4
		result[0].idx == 4
		result[1].idx == 3
		result[2].idx == 2
		result[3].idx == 1
	}
	
	def "Set vehicle priorities only prio"() {
		def vehicles = [
			new TestVehicle(idx: 1, priority: 5).getVehicle(),
			new TestVehicle(idx: 2, priority: 1).getVehicle(),
			new TestVehicle(idx: 3, priority: 2).getVehicle(),
			new TestVehicle(idx: 4, priority: 20).getVehicle(),
			] as Vehicle[]
				
		when:
		def result = service.execute(vehicles)

		then:
		result != null
		result.length == 4
		result[0].idx == 2
		result[1].idx == 3
		result[2].idx == 1
		result[3].idx == 4
	}
}
