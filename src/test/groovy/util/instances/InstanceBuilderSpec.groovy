package util.instances

import java.nio.charset.StandardCharsets

import spock.lang.Specification

class InstanceBuilderSpec extends Specification {

	def service = new InstanceBuilder()
	
	def "Read input"() {
		
		def input = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
		<instance>
			<info>
				<dataset>UnitTest</dataset>
				<name>UT01</name>
			</info>
			<network>
				<nodes>
					<node id="1" type="1">
						<cx>22.0</cx>
						<cy>22.0</cy>
					</node>
					<node id="2" type="1">
						<cx>36.0</cx>
						<cy>26.0</cy>
					</node>
					<node id="200" type="0">
                		<cx>35.0</cx>
                		<cy>35.0</cy>
            		</node>
				</nodes>
				<euclidean/>
				<decimals>0</decimals>
			</network>
			<fleet>
				<vehicle_profile type="0">
					<departure_node>200</departure_node>
					<arrival_node>200</arrival_node>
					<capacity>200.0</capacity>
				</vehicle_profile>
			</fleet>
			<requests>
				<request id="0" node="1">
					<quantity>18.0</quantity>
				</request>
				<request id="1" node="2">
					<quantity>26.0</quantity>
				</request>
			</requests>
		</instance>'''
		
		def stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
		
		when:
		
		def result = service.read(stream)
		
		then:
		result != null
		result.getNetwork().getNodes().getNode().size() == 3
		result.getNetwork().getNodes().getNode().get(0).getId() == 1
		result.getNetwork().getNodes().getNode().get(0).getType() == 1
		result.getNetwork().getNodes().getNode().get(0).getCx() == 22
		result.getNetwork().getNodes().getNode().get(0).getCy() == 22
		result.getNetwork().getEuclidean() != null
		result.getNetwork().getDecimals() == 0
		result.getFleet().getVehicleProfile().size() == 1
		result.getFleet().getVehicleProfile().get(0).getType() == 0
		result.getFleet().getVehicleProfile().get(0).getDepartureNode().get(0) == 200
		result.getFleet().getVehicleProfile().get(0).getArrivalNode().get(0) == 200
		result.getFleet().getVehicleProfile().get(0).getCapacity() == 200
		result.getRequests().getRequest().size() == 2
		result.getRequests().getRequest().get(0).getId() == 0
		result.getRequests().getRequest().get(0).getNode() == 1
		result.getRequests().getRequest().get(0).getQuantity() == 18
	}
	
}
