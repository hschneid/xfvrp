package instances

import spock.lang.Ignore
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptType
import xf.xfvrp.report.Report

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

class MDVRPTW_Cordeau extends Specification {

    @Ignore
    def "do Cordeau MDVRPTW tests" () {
        when:
        execute()

        then:
        assert true
    }

    private void execute() {
        def results = getResults(new File("./src/test/resources/MDVRPTW-cordeau/RESULTS"))

        def files = Files
                .list(Path.of("./src/test/resources/MDVRPTW-cordeau/"))
                .filter({f -> !f.fileName.toString().contains("RESULTS")})
        //.filter({p -> p.getFileName().toString().contains("pr07")})
                .sorted({c1, c2 -> (c1 <=> c2) })
                .collect(Collectors.toList())

        float[] deviation = new float[4]
        for (Path p : files) {
            def instanceName = p.fileName.toString().replace(".txt", "").toLowerCase()
            def bestResult = results.get(instanceName)

            long time = System.currentTimeMillis()
            def report = executeInstance(p)
            time = System.currentTimeMillis() - time

            deviation[0] += time
            deviation[1] += ((report.getSummary().getDistance() / bestResult[0] - 1) * 100)
            deviation[2] += ((report.getSummary().getNbrOfUsedVehicles() / bestResult[1] - 1) * 100)
            deviation[3]++

            println instanceName + " " +
                    String.format("%.2f", report.getSummary().getDistance()) + " " +
                    String.format("%.0f", report.getSummary().getNbrOfUsedVehicles()) + " " +
                    String.format("%.2f", ((report.getSummary().getDistance() / bestResult[0] - 1) * 100)) + " " +
                    String.format("%.2f", ((report.getSummary().getNbrOfUsedVehicles() / bestResult[1] - 1) * 100)) + " " +
                    String.format("%.2f", time / 1000)
        }
        println "Final: " +
                String.format("%.0f", deviation[3]) + " " +
                String.format("%.2f", deviation[0] / deviation[3]) + " " +
                String.format("%.2f", deviation[1] / deviation[3]) + " " +
                String.format("%.2f", deviation[2] / deviation[3])
    }

    private Report executeInstance(Path p) {
        def xfvrp = build(p.toFile())

        xfvrp.executeRoutePlanning()
        return xfvrp.getReport()
    }

    private XFVRP build(File file) {
        XFVRP xfvrp = new XFVRP()

        // xfvrp.setStatusMonitor(new DefaultStatusMonitor())

        List<String> lines = Files.readAllLines(file.toPath());
        float[] headerData = split(lines.get(0))
        float[] depotData = split(lines.get(1))
        int nbrOfDepots = (int)headerData[3]
        int nbrOfCustomers = (int)headerData[2]
        xfvrp.addVehicle()
                .setName("Vehicle")
                .setCapacity(depotData[1])

        // Customers
        int offset = 1 + nbrOfDepots
        for (int i = 0; i < nbrOfCustomers; i++) {
            float[] data = split(lines.get(i + offset))
            xfvrp.addCustomer()
                    .setExternID(((int)data[0])+"")
                    .setXlong(data[1])
                    .setYlat(data[2])
                    .setServiceTime(data[3])
                    .setDemand(data[4])
                    .setTimeWindow(data[7 + nbrOfDepots], data[8 + nbrOfDepots])
        }

        // Depots (at end of file)
        for (i in 0..<nbrOfDepots) {
            float[] data = split(lines.get(offset + nbrOfCustomers + i))
            xfvrp.addDepot()
                    .setExternID(((int)data[0])+"")
                    .setXlong(data[1])
                    .setYlat(data[2])
                    .setTimeWindow(data[7], data[8])
        }

        xfvrp.addOptType(XFVRPOptType.FIRST_BEST)
        xfvrp.setNbrOfLoopsForILS(10)
        xfvrp.addOptType(XFVRPOptType.RELOCATE)
        xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
        xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

        xfvrp.setMetric(new EucledianMetric())

        return xfvrp
    }

    private static float[] split(String line) {
        line = line.trim()
        line = line.replace(" ",";")
        line = line.replace("\t",";")

        String newLine = line
        do {
            line = newLine
            newLine = line.replace(";;", ";")
        } while(!newLine.equals(line))
        line = newLine

        def tokens = line.split(";")
        float[] data = new float[tokens.length]
        for (int i = 0; i < tokens.length; i++) {
            data[i] = Float.parseFloat(tokens[i])
        }

        return data
    }

    private Map<String, float[]> getResults(File file) {
        List<String> lines = Files.readAllLines(file.toPath())

        return lines.stream()
                .map({line -> line.split("\t")})
                .collect(
                        Collectors.toMap( {k -> k[0]}, {v -> [
                                Float.parseFloat(v[1]),Float.parseFloat(v[2])
                        ] as float[]}, {v1,v2 -> v1})
                )
    }
}
