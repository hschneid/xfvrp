package xf.xfvrp.base.preset

import spock.lang.Specification
import util.instances.TestNode
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.fleximport.InternalCustomerData

class BlockPositionConverterSpec extends Specification {

	def service = new BlockPositionConverter()
	
	def "Set blocked positions"() {
		def customers = [
			new InternalCustomerData(externID: "AA", presetBlockName: "A", presetBlockPos: 66),
			new InternalCustomerData(externID: "AB", presetBlockName: "A", presetBlockPos: 7),
			new InternalCustomerData(externID: "BA", presetBlockName: "B", presetBlockPos: -1),
			new InternalCustomerData(externID: "BB", presetBlockName: "B", presetBlockPos: 57),
			new InternalCustomerData(externID: "EA", presetBlockName: "", presetBlockPos: 1),
			new InternalCustomerData(externID: "EB", presetBlockName: "", presetBlockPos: 6),
			new InternalCustomerData(externID: "EC", presetBlockName: "", presetBlockPos: 89),
			new InternalCustomerData(externID: "CC", presetBlockName: "A", presetBlockPos: 1),
			new InternalCustomerData(externID: "DA", presetBlockName: "C", presetBlockPos: 1),
			new InternalCustomerData(externID: "DB", presetBlockName: "C", presetBlockPos: 0)

			] as List<InternalCustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP1", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1),
			customers.get(1).createCustomer(2),
			customers.get(2).createCustomer(3),
			customers.get(3).createCustomer(4),
			customers.get(4).createCustomer(5),
			customers.get(5).createCustomer(6),
			customers.get(6).createCustomer(7),
			customers.get(7).createCustomer(8),
			customers.get(8).createCustomer(9),
			customers.get(9).createCustomer(10)
			] as Node[]
				
		when:
		service.convert(nodes, customers)

		then:
		nodes[0].getPresetBlockPos() == BlockPositionConverter.UNDEF_POSITION
		nodes[1].getPresetBlockPos() == 3
		nodes[2].getPresetBlockPos() == 2
		nodes[3].getPresetBlockPos() == BlockPositionConverter.UNDEF_POSITION
		nodes[4].getPresetBlockPos() == BlockPositionConverter.UNDEF_POSITION
		nodes[5].getPresetBlockPos() == BlockPositionConverter.UNDEF_POSITION
		nodes[6].getPresetBlockPos() == BlockPositionConverter.UNDEF_POSITION
		nodes[7].getPresetBlockPos() == BlockPositionConverter.UNDEF_POSITION
		nodes[8].getPresetBlockPos() == 1
		nodes[9].getPresetBlockPos() == 2
		nodes[10].getPresetBlockPos() == 1
	}
}
