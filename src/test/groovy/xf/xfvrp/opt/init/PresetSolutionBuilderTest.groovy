package xf.xfvrp.opt.init

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.opt.XFVRPOptTypes

import java.util.stream.Collectors

class PresetSolutionBuilderTest extends Specification {

    def "Consider presets and unblocked customers"() {
        def vrp = getVRP()
        vrp.getParameters().setPredefinedSolutionString("{(B,H,C),(D2,G,D2),(,),(X,Y),(G)}")

        when:
        vrp.executeRoutePlanning()
        def res = vrp.getReport()
        then:
        res.routes.count{rep ->
            rep.getEvents().stream().map({e -> e.ID}).collect(Collectors.joining(","))
            .contains("B,H,C")
        } == 1
        res.routes.count{rep ->
            rep.getEvents().stream().map({e -> e.ID}).collect(Collectors.joining(","))
                    .contains("D2,G,D2")
        } == 1
        res.routes.count {rep -> rep.events.size() == 3 && rep.events.get(1).ID == 'A'} == 1
    }

    def "Broken preset pattern"() {
        def vrp = getVRP()
        vrp.getParameters().setPredefinedSolutionString("{(B,H,C),(,(D2,G,D2)}")

        when:
        vrp.executeRoutePlanning()

        then:
        thrown(XFVRPException)
    }


    static XFVRP getVRP() {
        def vrp = new XFVRP()
        vrp.addVehicle().setName('V1').setCapacity([10] as float[])
        // Add depots
        vrp.addDepot().setExternID('D1').setXlong(0).setYlat(0)
        vrp.addDepot().setExternID('D2').setXlong(-10).setYlat(-10)
        // Add customers
        vrp.addCustomer().setExternID('A').setXlong(10).setYlat(10).setDemand(1)
        vrp.addCustomer().setExternID('B').setXlong(11).setYlat(11).setDemand(1)
        vrp.addCustomer().setExternID('C').setXlong(10).setYlat(11).setDemand(1)
        vrp.addCustomer().setExternID('D').setXlong(11).setYlat(10).setDemand(1)
        vrp.addCustomer().setExternID('E').setXlong(9).setYlat(10).setDemand(1)
        vrp.addCustomer().setExternID('F').setXlong(10).setYlat(9).setDemand(1)
        vrp.addCustomer().setExternID('G').setXlong(11).setYlat(9).setDemand(1)
        vrp.addCustomer().setExternID('H').setXlong(9).setYlat(11).setDemand(1)
        vrp.addCustomer().setExternID('I').setXlong(10.5).setYlat(10.5).setDemand(1)

        vrp.setMetric(Metrics.EUCLEDIAN.get())
        vrp.addOptType(XFVRPOptTypes.NONE)
        return vrp
    }
}
