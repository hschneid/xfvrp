package instances

import spock.lang.Ignore
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes
import xf.xfvrp.report.Report

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.IntStream

class CMT extends Specification {

    @Ignore
    def "do CMT CVRP tests" () {
        when:
        execute()

        then:
        assert true
    }

    private void execute() {
        def results = getResults(new File("./src/test/resources/CMT/RESULTS"))

        def files = Files
                .list(Path.of("./src/test/resources/CMT/"))
                .filter({f -> !f.fileName.toString().contains("RESULTS")})
                .sorted({c1, c2 -> (c1 <=> c2) })
                .collect(Collectors.toList())

        float[] deviation = new float[4]
        for (Path p : files) {
            def instanceName = p.fileName.toString().replace(".vrp", "").toUpperCase()
            def bestResult = results.get(instanceName)

            long time = System.currentTimeMillis()
            def report = executeInstance(p, (int)bestResult[1])

            time = System.currentTimeMillis() - time

            deviation[0] += time
            deviation[1] += ((report.getSummary().getDistance() / bestResult[0] - 1) * 100)
            deviation[2] += ((report.getSummary().getNbrOfUsedVehicles() / bestResult[1] - 1) * 100)
            deviation[3]++

            println instanceName + " " +
                    String.format("%.2f", report.getSummary().getDistance()) + " " +
                    String.format("%d", report.getSummary().getNbrOfUsedVehicles()) + " " +
                    String.format("%.2f", ((report.getSummary().getDistance() / bestResult[0] - 1) * 100)) + " " +
                    String.format("%.2f", ((report.getSummary().getNbrOfUsedVehicles() / bestResult[1] - 1) * 100)) + " " +
                    String.format("%.2f", time / 1000)
        }
        println String.format("%.0f", deviation[3]) + " " +
                String.format("%.2f", deviation[0] / deviation[3]) + " " +
                String.format("%.2f", deviation[1] / deviation[3]) + " " +
                String.format("%.2f", deviation[2] / deviation[3])
    }

    private Report executeInstance(Path p, int nbrOfMaxRoutes) {
        def xfvrp = build(p.toFile(), nbrOfMaxRoutes)

        xfvrp.executeRoutePlanning()
        return xfvrp.getReport()
    }

    private XFVRP build(File file, int nbrOfMaxRoutes) {
        XFVRP xfvrp = new XFVRP()

        // xfvrp.setStatusMonitor(new DefaultStatusMonitor())

        List<String> lines = Files.readAllLines(file.toPath())
        def vehicleData = lines.get(5).split(":")
        def maxDurationIdx = IntStream.range(0, lines.size()).filter(idx -> lines.get(idx).contains("DISTANCE")).findAny().orElse(-1)
        def maxDuration = (maxDurationIdx > 0) ? Float.parseFloat(lines.get(maxDurationIdx).split(":")[1].trim()) : 9999999

        def serviceTimeIdx = IntStream.range(0, lines.size()).filter(idx -> lines.get(idx).contains("SERVICE_TIME")).findAny().orElse(-1)
        def serviceTime = (serviceTimeIdx > 0) ? Float.parseFloat(lines.get(serviceTimeIdx).split(":")[1].trim()) : 0

        xfvrp.addVehicle()
                .setName("Vehicle")
                .setCapacity(Integer.parseInt(vehicleData[1].trim()))
                .setMaxRouteDuration(maxDuration)

        int nbrOfNodes = Integer.parseInt(lines.get(3).split(":")[1].trim())
        int coordIdx = IntStream.range(0, lines.size()).filter(idx -> lines.get(idx).trim().equals("NODE_COORD_SECTION")).findAny().orElse(-1)
        int demandIdx = IntStream.range(0, lines.size()).filter(idx -> lines.get(idx).trim().equals("DEMAND_SECTION")).findAny().orElse(-1)

        def depot = lines.get(coordIdx + 1).trim().split(" ")
        xfvrp.addDepot()
                .setExternID("0")
                .setXlong(Float.parseFloat(depot[1].trim()))
                .setYlat(Float.parseFloat(depot[2].trim()))
                .setMaxNbrRoutes(nbrOfMaxRoutes)


        for (int i = 2; i <= nbrOfNodes; i++) {
            def coords = lines.get(coordIdx + i).trim().split(" ")
            def demands = lines.get(demandIdx + i).trim().split(" ")

            xfvrp.addCustomer()
                    .setExternID((i-1)+"")
                    .setXlong(Float.parseFloat(coords[1].trim()))
                    .setYlat(Float.parseFloat(coords[2].trim()))
                    .setDemand(Float.parseFloat(demands[1].trim()))
                    .setServiceTime(serviceTime)
        }

        xfvrp.addOptType(XFVRPOptTypes.SAVINGS)
        xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
        xfvrp.addOptType(XFVRPOptTypes.PATH_RELOCATE)
        xfvrp.addOptType(XFVRPOptTypes.PATH_EXCHANGE)

        xfvrp.getParameters().setNbrOfILSLoops(500)
        xfvrp.addOptType(XFVRPOptTypes.ILS)

        xfvrp.setMetric(new EucledianMetric())

        return xfvrp
    }

    private Map<String, float[]> getResults(File file) {
        return Files.readAllLines(file.toPath()).stream()
                .map({line -> line.split("\t")})
                .collect(
                        Collectors.toMap( {k -> k[0]}, {v -> [
                                Float.parseFloat(v[1]),Float.parseFloat(v[2])
                        ] as float[]}, {v1,v2 -> v1})
                )
    }
}
