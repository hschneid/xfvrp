package xf.xfvrp.opt.improve.routebased.swap

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes
import xf.xfvrp.report.StringWriter

class XFVRPBorderSegmentExchangeTest extends Specification {

    def "optimize with border segment exchange"() {
        def vrp = build()
        when:
        vrp.executeRoutePlanning()
        def rep = vrp.getReport()
        println StringWriter.write(rep)

        then:
        rep != null
    }

    XFVRP build() {
        XFVRP xfvrp = new XFVRP()

        // xfvrp.setStatusMonitor(new DefaultStatusMonitor())

        xfvrp.addVehicle()
                .setName("Vehicle")
                .setCapacity(5)

        xfvrp.addDepot()
                .setExternID("DEP")
                .setXlong(-3)
                .setYlat(3)
                .setTimeWindow(0,99)
        xfvrp.addDepot()
                .setExternID("DEP2")
                .setXlong(3)
                .setYlat(3)
                .setTimeWindow(0,99)

        xfvrp.addCustomer().setExternID('1').setXlong(-2).setYlat(4).setDemand([1,1]as float[]).setServiceTime(5)
        xfvrp.addCustomer().setExternID('2').setXlong(-1).setYlat(3).setDemand([1,1]as float[]).setServiceTime(5)
        xfvrp.addCustomer().setExternID('3').setXlong(-2).setYlat(2).setDemand([1,1]as float[]).setServiceTime(5)
        xfvrp.addCustomer().setExternID('4').setXlong(2).setYlat(4).setDemand([1,1]as float[]).setServiceTime(5)
        xfvrp.addCustomer().setExternID('5').setXlong(1).setYlat(3).setDemand([1,1]as float[]).setServiceTime(5)
        xfvrp.addCustomer().setExternID('6').setXlong(2).setYlat(2).setDemand([1,1]as float[]).setServiceTime(5)

        xfvrp.addOptType(XFVRPOptTypes.BORDER_PATH_EXCHANGE)

        xfvrp.setMetric(new EucledianMetric())

        return xfvrp
    }
}
