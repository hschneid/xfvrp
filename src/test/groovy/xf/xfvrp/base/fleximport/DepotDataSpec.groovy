package xf.xfvrp.base.fleximport

import spock.lang.Specification
import xf.xfvrp.base.SiteType

class DepotDataSpec extends Specification {
	
	def "Check sets 1"() {
		def data = new DepotData()
				
		when:
		data.setExternID("A")
		data.setGeoId(15)
		data.setXlong(12.2)
		data.setYlat(22.1)
		data.setTimeWindow(200, 500)
		
		then:
		data.getExternID() == "A"
		data.getGeoId() == 15
		Math.abs(data.getXlong() - 12.2) < 0.0001
		Math.abs(data.getYlat() - 22.1) < 0.0001
		data.getTimeWindowList().get(0)[1] == 500

	}
	
	def "Create node"() {
		def data = new DepotData()
				
				data.setExternID("A")
				data.setGeoId(15)
				data.setXlong(12.2)
				data.setYlat(22.1)
				data.setTimeWindow(200, 500)
				data.setTimeWindow(400, 500)
		when:
		
		def node = data.createDepot(155)
		
		then:
		node.getExternID() == "A"
		node.getGeoId() == 15
		node.getGlobalIdx() == 155
		node.getSiteType() == SiteType.DEPOT
		node.getLoadType() == null
		Math.abs(node.getXlong() - 12.2) < 0.0001
		Math.abs(node.getYlat() - 22.1) < 0.0001
		node.timeWindowArr.length == 2
	}
	
	def "Check time window 1"() {
		def data = new DepotData()
		data.setOpen1(400)
		data.setClose1(405)
		data.setTimeWindow(200, 500)

		when:
		data.checkTimeWindows()
		then:
		data.timeWindowList.size() == 2
		data.timeWindowList.get(0).length == 2
		data.timeWindowList.get(1).length == 2
		data.timeWindowList.get(0)[0] == 200
		data.timeWindowList.get(0)[1] == 500
		data.timeWindowList.get(1)[0] == 400
		data.timeWindowList.get(1)[1] == 405
	}

	def "Check time window 2"() {
		def data = new DepotData()
		data.setTimeWindow(200, 500)
		data.setTimeWindow(400, 405)

		when:
		data.checkTimeWindows()
		then:
		data.timeWindowList.size() == 2
		data.timeWindowList.get(0).length == 2
		data.timeWindowList.get(1).length == 2
		data.timeWindowList.get(0)[0] == 200
		data.timeWindowList.get(0)[1] == 500
		data.timeWindowList.get(1)[0] == 400
		data.timeWindowList.get(1)[1] == 405
	}

	def "Check time window 3"() {
		def data = new DepotData()
		data.setOpen1(400)
		data.setClose1(405)
		data.setOpen2(200)
		data.setClose2(500)

		when:
		data.checkTimeWindows()
		then:
		data.timeWindowList.size() == 2
		data.timeWindowList.get(0).length == 2
		data.timeWindowList.get(1).length == 2
		data.timeWindowList.get(0)[0] == 200
		data.timeWindowList.get(0)[1] == 500
		data.timeWindowList.get(1)[0] == 400
		data.timeWindowList.get(1)[1] == 405
	}

	def "Check time window 4"() {
		def data = new DepotData()

		when:
		data.checkTimeWindows()
		
		then:
		data.timeWindowList.size() == 1
		data.timeWindowList.get(0)[0] == 0
		data.timeWindowList.get(0)[1] > 9999999
	}

}
