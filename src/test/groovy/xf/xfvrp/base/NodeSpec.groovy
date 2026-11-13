package xf.xfvrp.base

import spock.lang.Specification

class NodeSpec extends Specification {

	def "Deep copy - preset lists and arrays are independent from original"() {
		given:
		def node = new Node()
		node.addPresetVehicle(1)
		node.addPresetDepot(2)
		node.setPresetRoutingBlackList([7, 8] as int[])
		node.setDemands([1.5f, 2.5f] as float[])

		when:
		def copy = node.copy()

		then:
		copy.getPresetBlockVehicleList() == [1] as Set
		copy.getPresetDepotList() == [2] as Set
		copy.getPresetRoutingBlackList() == [7, 8] as int[]
		copy.getDemand() == [1.5f, 2.5f] as float[]

		when:
		node.addPresetVehicle(3)
		node.addPresetDepot(4)
		node.setPresetRoutingBlackList([9] as int[])
		node.setDemands([9.9f, 0f] as float[])

		then:
		copy.getPresetBlockVehicleList() == [1] as Set
		copy.getPresetDepotList() == [2] as Set
		copy.getPresetRoutingBlackList() == [7, 8] as int[]
		copy.getDemand() == [1.5f, 2.5f] as float[]
	}
}
