package util.instances

import spock.lang.Specification

class XFVRPInstanceBridgeSpec extends Specification {

	def service = new XFVRPInstanceBridge()
	
	def "Read input"() {
		
		def file = new File("./src/test/resources/CMT05.xml")
		println file.exists()
		
		when:
		
		def vrp = service.build("./src/test/resources/CMT05.xml")
		vrp.getData().getImporter().finishImport()
		
		then:
		vrp != null
		vrp.getData().importer.depotList.size() == 1
		vrp.getData().importer.customerList.size() == 199
		vrp.getData().importer.vehicleList.size() == 1
	}
	
}
