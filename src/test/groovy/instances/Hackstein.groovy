package instances

import cern.colt.list.FloatArrayList
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Ignore
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.monitor.DefaultStatusMonitor
import xf.xfvrp.opt.XFVRPOptType
import xf.xfvrp.report.RouteReport

import java.util.concurrent.atomic.AtomicInteger

class Hackstein extends Specification {

    @Ignore
    def "test"() {
        def xfvrp = build(new File("./src/test/resources/hackstein/with_vehicle_restrictions.json"))
        when:
        xfvrp.executeRoutePlanning()
        def rep = xfvrp.getReport()

        then:
        for (RouteReport routeRep : rep.getRoutes()) {
            double s = routeRep.getEvents().stream()
            .filter(f -> f.getLoadType() == LoadType.DELIVERY)
            .mapToDouble(m -> (double)m.getAmounts()[0])
            .sum()
            //assert s <= 33.0
            println s
        }

        def maxValue = rep.getRoutes().stream().mapToDouble(r -> r.getEvents()
                .stream().mapToDouble(s -> (double)s.amounts[0]).sum()).max()
        println maxValue

        assert true
    }

    private XFVRP build(File file) {
        XFVRP xfvrp = new XFVRP()
        xfvrp.setStatusMonitor(new DefaultStatusMonitor())

        Map<?, ?> map = new ObjectMapper().readValue(file, Map.class)

        Collection<Map<String, ?>> customers = map.get("Customers")
        Collection<Map<String, ?>> depots = map.get("Depots")
        Map<Integer, Collection<Map<?,?>>> vehicles = map.get("Vehicles")

        vehicles.forEach((depot, depotVehicles) -> {
            depotVehicles.forEach(vehicle -> {
                Collection<Double> dblCap = vehicle.get("capacity")
                FloatArrayList fltCap = new FloatArrayList()
                dblCap.forEach(d -> fltCap.add((float)d))
                fltCap.trimToSize()
                xfvrp.getData().addVehicle()
                        .setName(vehicle.get("name"))
                        .setCapacity(fltCap.elements())
                        .setMaxRouteDuration(600)
            })
        })

        depots.forEach(depot -> {
            xfvrp.getData().addDepot()
                    .setExternID("DEP")
                    .setYlat((float)depot.get("lat"))
                    .setXlong((float)depot.get("lng"))
        })

        AtomicInteger counter = new AtomicInteger()
        customers.forEach(customer -> {
            Collection<Double> dblDemand = customer.get("amount")
            Collection<String> vehiclesAllowed = customer.get("vehicles")
            FloatArrayList fltDemand = new FloatArrayList()
            dblDemand.forEach(d -> fltDemand.add((float)d))
            fltDemand.trimToSize()
            var cust = xfvrp.getData().addCustomer()
                    .setExternID(counter.getAndIncrement()+"")
                    .setXlong((float)customer.get("lng"))
                    .setYlat((float)customer.get("lat"))
                    .setDemand(fltDemand.elements())
                    .setServiceTime((float)customer.get("serviceTime"))
                    .setLoadType(LoadType.DELIVERY)
            if (vehiclesAllowed != null && vehiclesAllowed.size() > 0)
                cust.setPresetBlockVehicleList(new HashSet<String>(vehiclesAllowed))
        })
        println "Added " + counter + " demands."

        xfvrp.addOptType(XFVRPOptType.SAVINGS)
        xfvrp.addOptType(XFVRPOptType.RELOCATE)
        //xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
        //xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

        xfvrp.getData().setMetric(new EucledianMetric())

        return xfvrp
    }
}