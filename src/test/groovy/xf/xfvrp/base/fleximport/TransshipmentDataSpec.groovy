package xf.xfvrp.base.fleximport

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.preset.TrailerAccessibilityConverter

class TransshipmentDataSpec extends Specification {

	def "Transshipment data is imported as a node with site type TRANSSHIPMENT"() {
		def data = new TransshipmentData()

				data.setExternID("T1")
				data.setGeoId(15)
				data.setXlong(12.2f)
				data.setYlat(22.1f)
				data.setTimeWindow(200, 500)
				data.setFixedTransferTime(30)
				data.setParkingAllowed(false)
		when:
		def node = data.createTransshipment(155)

		then:
		node.getExternID() == "T1"
		node.getGeoId() == 15
		node.getGlobalIdx() == 155
		node.getSiteType() == SiteType.TRANSSHIPMENT
		node.getLoadType() == LoadType.UNDEF
		node.getServiceTime() == 30f
		Math.abs(node.getXlong() - 12.2) < 0.0001
		Math.abs(node.getYlat() - 22.1) < 0.0001
		node.getTimeWindow(0) == [200f, 500f] as float[]
	}

	def "Default transshipment has no restriction"() {
		def data = new TransshipmentData()

		when:
		data.setExternID("T1")
		def node = data.createTransshipment(0)

		then:
		node.getSiteType() == SiteType.TRANSSHIPMENT
		node.getServiceTime() == 0
		node.getTimeWindow(0)[0] == 0f
		node.getTimeWindow(0)[1] > 9999999
	}

	def "Lorry customer is blocked for trailer capable vehicles"() {
		def vehicles = [
				lorryWithTrailer("LORRY1", "TRAILER1", 0, 0),
				pureLorry("LORRY2", 1)
		] as Vehicle[]

		def importer = new FlexiImporter()
		importer.getCustomerData().setExternID("LC").setTrailerAccessible(false)
		importer.finishImport()
		def nodes = importer.getNodes(vehicles, null)

		when:
		def node = nodes.find { it.getExternID() == "LC" }

		then:
		// Allowlist semantics: LC is only served by the pure lorry LORRY2 (idx 1)
		node.getPresetBlockVehicleList() == [1] as Set
	}

	def "Lorry customer without pure lorry in fleet is unservable"() {
		def vehicles = [
				lorryWithTrailer("LORRY1", "TRAILER1", 0, 0)
		] as Vehicle[]

		def importer = new FlexiImporter()
		importer.getCustomerData().setExternID("LC").setTrailerAccessible(false)
		importer.finishImport()
		def nodes = importer.getNodes(vehicles, null)

		when:
		def node = nodes.find { it.getExternID() == "LC" }

		then:
		node.getPresetBlockVehicleList() == [TrailerAccessibilityConverter.UNSERVABLE_MARKER] as Set
	}

	def "Trailer customer is not blocked for any vehicle"() {
		def vehicles = [
				lorryWithTrailer("LORRY1", "TRAILER1", 0, 0),
				pureLorry("LORRY2", 1)
		] as Vehicle[]

		def importer = new FlexiImporter()
		importer.getCustomerData().setExternID("TC")
		importer.finishImport()
		def nodes = importer.getNodes(vehicles, null)

		when:
		def node = nodes.find { it.getExternID() == "TC" }

		then:
		node.getPresetBlockVehicleList().isEmpty()
	}

	def "Trailer without pulling lorry is invalid"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		importer.getVehicleData().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		importer.finishImport()

		when:
		importer.getVehicles()

		then:
		thrown(XFVRPException)
	}

	def "Lorry trailer reference to unknown vehicle is invalid"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getVehicleData().setName("LORRY1").setTrailerId("UNKNOWN").setCapacity([5] as float[])
		importer.finishImport()

		when:
		importer.getVehicles()

		then:
		thrown(XFVRPException)
	}

	def "Lorry trailer reference is resolved to trailer vehicle index"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		importer.getVehicleData().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		importer.getVehicleData().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[])
		importer.finishImport()

		when:
		def vehicles = importer.getVehicles()

		then:
		vehicles[1].isAutonomous()
		vehicles[1].getTrailerId() == 0
		vehicles[0].getTrailerId() == Vehicle.NO_TRAILER
		!vehicles[0].isAutonomous()
	}

	private Vehicle pureLorry(String name, int idx) {
		def data = new VehicleData()
		data.setName(name).setCapacity([10] as float[])
		return data.createVehicle(idx)
	}

	private Vehicle lorryWithTrailer(String name, String trailerName, int trailerIdx, int idx) {
		def data = new VehicleData()
		data.setName(name).setTrailerId(trailerName).setCapacity([10] as float[])
		def veh = data.createVehicle(idx)
		veh.setTrailerId(trailerIdx)
		return veh
	}
}
