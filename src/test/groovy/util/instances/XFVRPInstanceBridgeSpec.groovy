package util.instances

import spock.lang.Specification

class XFVRPInstanceBridgeSpec extends Specification {

	def service = new XFVRPInstanceBridge()
	
	def "Read input"() {
		
		def file = new File("./src/test/resources/CMT05.xml")
		println file.exists()
		
		when:
		
		def vrp = service.build("./src/test/resources/CMT05.xml")
		vrp.importer.finishImport()
		
		then:
		vrp != null
		vrp.importer.depotList.size() == 1
		vrp.importer.customerList.size() == 199
		vrp.importer.vehicleList.size() == 1
	}
	
}
