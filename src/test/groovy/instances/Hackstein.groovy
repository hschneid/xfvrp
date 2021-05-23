package instances

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Ignore
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.monitor.DefaultStatusMonitor
import xf.xfvrp.opt.XFVRPOptType
import xf.xfvrp.report.RouteReport

class Hackstein extends Specification {

    def "test"() {
        def xfvrp = build(new File("./src/test/resources/hackstein/testinstance_01.json"))
        when:
        xfvrp.executeRoutePlanning()
        def rep = xfvrp.getReport()

        then:
        for (RouteReport routeRep : rep.getRoutes()) {
            double s = routeRep.getEvents().stream()
            .filter(f -> f.getLoadType() == LoadType.DELIVERY)
            .mapToDouble(m -> (double)m.getAmounts()[0])
            .sum()

            assert s < 33.0

            println s
        }

        assert true
    }

    private XFVRP build(File file) {
        XFVRP xfvrp = new XFVRP()

        xfvrp.setStatusMonitor(new DefaultStatusMonitor())

        JsonNode node = new ObjectMapper().readTree(file);

        String orders = node.get("Orders")

        float[] amts = splitValues(orders.substring(0, orders.indexOf("SRCLat")))
        float[] lats = splitValues(orders.substring(orders.indexOf("DSTLat"), orders.indexOf("DSTLng")))
        float[] lngs = splitValues(orders.substring(orders.indexOf("DSTLng"), orders.length()))

        xfvrp.addVehicle()
                .setName("Vehicle")
                .setCapacity(33)

        xfvrp.addDepot()
                .setExternID("DEP")
                .setXlong(48.79204)
                .setYlat(2.38525)

        for (int i = 0; i < amts.length; i++) {
            xfvrp.addCustomer()
                    .setExternID(i+"")
                    .setXlong(lats[i])
                    .setYlat(lngs[i])
                    .setDemand(amts[i])
                    .setLoadType(LoadType.DELIVERY)
        }

        xfvrp.addOptType(XFVRPOptType.SAVINGS)
        xfvrp.addOptType(XFVRPOptType.RELOCATE)
        //xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
        //xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

        xfvrp.setMetric(new EucledianMetric())

        return xfvrp
    }

    private float[] splitValues(String values) {
        String[] tokens = values.split(":")
        float[] v = new float[tokens.length - 2]
        for (i in 2..<tokens.length) {
            v[i-2] = Float.parseFloat(
                    tokens[i].split(",")[0]
                            .replace("}","")
                            .replace("\"", "")
            )
        }

        return v
    }
}
