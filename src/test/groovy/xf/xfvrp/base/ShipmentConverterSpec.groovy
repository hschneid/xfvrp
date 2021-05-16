package xf.xfvrp.base

import spock.lang.Specification
import util.instances.TestNode
import xf.xfvrp.base.fleximport.CustomerData

class ShipmentConverterSpec extends Specification {

	def service = new ShipmentConverter();

	def customers
	
	def "Convert shipment - successfull"() {
		def model = createModel()
		def n = model.getNodes();
				
		when:
		service.convert(n, customers);
		
		then:
		n[0].shipmentIdx == -1
		n[1].shipmentIdx == -1
		n[2].shipmentIdx == -1
		n[3].shipmentIdx == 0
		n[4].shipmentIdx == 0
		n[5].shipmentIdx == 1
		n[6].shipmentIdx == 1
		n[7].shipmentIdx == 2
		n[8].shipmentIdx == -1
	}
	
	private XFVRPModel createModel() {
		customers =	[
			new CustomerData(externID: "2", shipID: "A"),
			new CustomerData(externID: "3", shipID: "A"),
			new CustomerData(externID: "5", shipID: "B"),
			new CustomerData(externID: "6", shipID: "B"),
			new CustomerData(externID: "7", shipID: "C"),
			new CustomerData(externID: "8")
			] as List<CustomerData>
		
		def n1 = new TestNode(externID: "1", siteType: SiteType.DEPOT).getNode();
		def n2 = new TestNode(externID: "2", siteType: SiteType.CUSTOMER, shipID: "A").getNode();
		def n3 = new TestNode(externID: "3", siteType: SiteType.CUSTOMER, shipID: "A").getNode();
		def n4 = new TestNode(externID: "4", siteType: SiteType.DEPOT).getNode();
		def n5 = new TestNode(externID: "5", siteType: SiteType.CUSTOMER, shipID: "B").getNode();
		def n6 = new TestNode(externID: "6", siteType: SiteType.CUSTOMER, shipID: "B").getNode();
		def n7 = new TestNode(externID: "7", siteType: SiteType.CUSTOMER, shipID: "C").getNode();
		def n8 = new TestNode(externID: "8", siteType: SiteType.CUSTOMER).getNode();
		def n9 = new TestNode(externID: "9", siteType: SiteType.DEPOT).getNode();
		
		def model = new XFVRPModel([n1, n4, n9, n2, n3, n5, n6, n7, n8] as Node[], null, null, null, null);

		return model
	}
	
}
