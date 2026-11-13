package xf.xfvrp.base.preset

import spock.lang.Specification
import util.instances.TestNode
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.fleximport.CustomerData

class BlockNameConverterSpec extends Specification {

	def service = new BlockNameConverter()
	
	def "Set block index mixed"() {
		def customers = [
				new CustomerData(externID: "AA", presetBlockName: "BlockA"),
				new CustomerData(externID: "AB", presetBlockName: "BlockA"),
				new CustomerData(externID: "BA", presetBlockName: "BlockB"),
				new CustomerData(externID: "BB", presetBlockName: "BlockB"),
				new CustomerData(externID: "C", presetBlockName: ""),
				new CustomerData(externID: "D", presetBlockName: null)
			] as List<CustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1),
			customers.get(1).createCustomer(2),
			customers.get(2).createCustomer(3),
			customers.get(3).createCustomer(4),
			customers.get(4).createCustomer(5),
			customers.get(5).createCustomer(6)
			] as Node[]
				
		when:
		service.convert(nodes, customers)

		then:
		nodes[0].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[1].getPresetBlockIdx() == 1
		nodes[2].getPresetBlockIdx() == 1
		nodes[3].getPresetBlockIdx() == 2
		nodes[4].getPresetBlockIdx() == 2
		nodes[5].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[6].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
	}
	
	def "Set block index only blocks"() {
		def customers = [
			new CustomerData(externID: "AA", presetBlockName: "BlockA"),
			new CustomerData(externID: "AB", presetBlockName: "BlockA"),
			new CustomerData(externID: "BA", presetBlockName: "BlockB"),
			new CustomerData(externID: "BB", presetBlockName: "BlockB"),
			new CustomerData(externID: "CA", presetBlockName: "BlockC"),
			new CustomerData(externID: "CB", presetBlockName: "BlockC")
			] as List<CustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1),
			customers.get(1).createCustomer(2),
			customers.get(2).createCustomer(3),
			customers.get(3).createCustomer(4),
			customers.get(4).createCustomer(5),
			customers.get(5).createCustomer(6)
			] as Node[]
				
		when:
		service.convert(nodes, customers)

		then:
		nodes[0].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[1].getPresetBlockIdx() == 1
		nodes[2].getPresetBlockIdx() == 1
		nodes[3].getPresetBlockIdx() == 2
		nodes[4].getPresetBlockIdx() == 2
		nodes[5].getPresetBlockIdx() == 3
		nodes[6].getPresetBlockIdx() == 3
	}
	
	def "Set block index only undef"() {
		def customers = [
			new CustomerData(externID: "AA", presetBlockName: ""),
			new CustomerData(externID: "AB", presetBlockName: " "),
			new CustomerData(externID: "BA", presetBlockName: "  "),
			new CustomerData(externID: "BB", presetBlockName: null),
			new CustomerData(externID: "CA", presetBlockName: ""),
			new CustomerData(externID: "CB", presetBlockName: null)
			] as List<CustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1),
			customers.get(1).createCustomer(2),
			customers.get(2).createCustomer(3),
			customers.get(3).createCustomer(4),
			customers.get(4).createCustomer(5),
			customers.get(5).createCustomer(6)
			] as Node[]
				
		when:
		service.convert(nodes, customers)

		then:
		nodes[0].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[1].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[2].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[3].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[4].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[5].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
		nodes[6].getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX
	}
	
}
