package instances

import cern.colt.list.FloatArrayList
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.runners.JUnit4
import spock.lang.Ignore
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.MapMetric
import xf.xfvrp.base.monitor.DefaultStatusMonitor
import xf.xfvrp.opt.XFVRPOptTypes
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

    @Test
    def "test_presets_and_timewindows"() {
        def xfvrp = build(new File("./src/test/resources/hackstein/presets_and_timewindows.json"))
        when:
        xfvrp.executeRoutePlanning()
        def rep = xfvrp.getReport()

        then:
        var nbrStops = 0
        rep.getRoutes().forEach((r) -> {
            nbrStops += r.getSummary().getNbrOfEvents() - 2
            r.getSummary().getNbrOfStops()
        });
        println "Nbr Routes: " + rep.getRoutes().size() + " Nbr Stops: " + nbrStops

        rep.getRoutes().forEach((r) -> {
            double[] kpi = new double[2]
            r.getEvents().forEach(e -> {
                kpi[0] += e.travelTime
                kpi[1] += e.service
            })
            println "Route Duration: " + r.getSummary().getDuration() +
                    " Delay: " + r.getSummary().delay + " Waiting Time: " + r.getSummary().getWaitingTime() +
                    " #Stops: " + r.getSummary().getNbrOfStops() +
                    " TravelTime: " + kpi[0] + " Service Time: " + kpi[1]
        });
        println "Error statistics:"
        rep.getErrors().statistics.forEach((k,v) -> {
            println " - " + k + ": " + v
        })
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
            var depotData = xfvrp.getData().addDepot()
                    .setExternID("DEP")
                    .setYlat((float)depot.get("lat"))
                    .setXlong((float)depot.get("lng"))
            if (depot.containsKey("geoId")) depotData.setGeoId(depot.get("geoId"))
        })

        AtomicInteger counter = new AtomicInteger()
        customers.forEach(customer -> {
            Collection<Double> dblDemand = customer.get("amount")
            Collection<String> vehiclesAllowed = customer.get("vehicles")
            FloatArrayList fltDemand = new FloatArrayList()
            dblDemand.forEach(d -> fltDemand.add((float)d))
            fltDemand.trimToSize()
            var cust = xfvrp.getData().addCustomer()
                    .setExternID(customer.get("externID"))
                    .setXlong((float)customer.get("lng"))
                    .setYlat((float)customer.get("lat"))
                    .setDemand(fltDemand.elements())
                    .setServiceTime((float)customer.get("serviceTime"))
                    .setLoadType(LoadType.DELIVERY)

            if (customer.containsKey("geoId")) cust.setGeoId(customer.get("geoId"))
            if (customer.containsKey("twOpen")) cust.setOpen1((float)customer.get("twOpen"))
            if (customer.containsKey("twClose")) cust.setClose1((float)customer.get("twClose"))
            if (customer.containsKey("presetBlock")) cust.setPresetBlockName(customer.get("presetBlock"))
            //if (customer.containsKey("presetPos")) cust.setPresetBlockPos(customer.get("presetPos"))


            if (vehiclesAllowed != null && vehiclesAllowed.size() > 0)
                cust.setPresetBlockVehicleList(new HashSet<String>(vehiclesAllowed))
        })
        println "Added " + counter + " demands."

        // Distances
        if (map.containsKey("Distances")) {
            counter = new AtomicInteger()
            var metric = new MapMetric();
            Collection<float[]> distances = map.get("Distances")
            distances.forEach(d -> {
                metric.addTime((int)d[0],(int)d[1],(float)d[2])
                metric.addDist((int)d[0],(int)d[1],(float)d[2])
                counter.getAndIncrement();
            })
            println "Added " + counter + " distances."
            xfvrp.getData().setMetric(metric)
        } else {
            xfvrp.getData().setMetric(new EucledianMetric())
        }

        xfvrp.addOptType(XFVRPOptTypes.SAVINGS)
        xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
        //xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
        //xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

        return xfvrp
    }
}