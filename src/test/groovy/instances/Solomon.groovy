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

class Solomon extends Specification {

    // @Ignore
    def "do Solomon VRPTW tests" () {
        when:
        execute()

        then:
        assert true
    }

    private void execute() {
        def results = getResults(new File("./src/test/resources/solomon/RESULTS"))

        def files = Files
                .list(Path.of("./src/test/resources/solomon/"))
                .filter({f -> !f.fileName.toString().contains("RESULTS")})
                .sorted({c1, c2 -> (c1 <=> c2) })
                .collect(Collectors.toList())

        float[] deviation = new float[4]
        for (Path p : files) {
            def instanceName = p.fileName.toString().replace(".txt", "").toLowerCase()

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
        int[] vehicleData = split(lines.get(4))
        xfvrp.addVehicle()
                .setName("Vehicle")
                .setCapacity(vehicleData[1])

        int[] data = split(lines.get(9))
        xfvrp.addDepot()
                .setExternID(data[0]+"")
                .setXlong(data[1])
                .setYlat(data[2])
                .setTimeWindow(data[4], data[5])
                .setMaxNbrRoutes(nbrOfMaxRoutes)

        for (int i = 10; i < lines.size(); i++) {
            String customerData = lines.get(i)
            if(customerData.trim().length() == 0) {
                continue
            }

            data = split(customerData)
            xfvrp.addCustomer()
                    .setExternID(data[0]+"")
                    .setXlong(data[1])
                    .setYlat(data[2])
                    .setDemand(data[3])
                    .setTimeWindow(data[4], data[5])
                    .setServiceTime(data[6])
        }

        xfvrp.addOptType(XFVRPOptTypes.SAVINGS)
        //xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
        //xfvrp.addOptType(XFVRPOptTypes.PATH_RELOCATE)
        //xfvrp.addOptType(XFVRPOptTypes.PATH_EXCHANGE)

        xfvrp.getParameters().setNbrOfILSLoops(100)
        //xfvrp.addOptType(XFVRPOptTypes.ILS)

        xfvrp.setMetric(new EucledianMetric())

        return xfvrp
    }

    private int[] split(String line) {
        line = ";"+line
        line = line.replace(" ",";")

        String newLine = line
        do {
            line = newLine
            newLine = line.replace(";;", ";")
        } while(!newLine.equals(line))
        line = newLine

        def tokens = line.split(";")
        int[] data = new int[tokens.length - 1]
        for (int i = 1; i < tokens.length; i++) {
            data[i - 1] = Integer.parseInt(tokens[i])
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
