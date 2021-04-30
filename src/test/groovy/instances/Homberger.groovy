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

class Homberger extends Specification {

    def sizes = ["200", "400"]

    @Ignore
    def "do Homberger VRPTW tests" () {
        when:
        for (String size : sizes) {
            execute(size)
        }

        then:
        assert true
    }

    @Ignore
    def "do Homberger VRPTW test for certain instance" () {
        when:
        def report = executeInstance(Path.of("./src/test/resources/homberger/200/R2_2_1.TXT"))

        println String.format("%.2f", report.getSummary().getDistance()) + " " +
                String.format("%.0f", report.getSummary().getNbrOfUsedVehicles())
        then:
        assert true
    }

    private void execute(String instanceSize) {
        def results = getResults(new File("./src/test/resources/homberger/"+instanceSize+"/RESULTS"))

        def files = Files
                .list(Path.of("./src/test/resources/homberger/"+instanceSize+"/"))
                .filter({f -> !f.fileName.toString().contains("RESULTS")})
                .sorted({c1, c2 -> (c1 <=> c2) })
                .collect(Collectors.toList())

        float[] deviation = new float[4]
        for (Path p : files) {
            def instanceName = p.fileName.toString().replace(".TXT", "").toLowerCase()
            def bestResult = results.get(instanceName)

            long time = System.currentTimeMillis()
            def report = executeInstance(p)
            time = System.currentTimeMillis() - time

            deviation[0] += time
            deviation[1] += ((report.getSummary().getDistance() / bestResult[0] - 1) * 100)
            deviation[2] += ((report.getSummary().getNbrOfUsedVehicles() / bestResult[1] - 1) * 100)
            deviation[3]++

            println instanceSize + " " + instanceName + " " +
                    String.format("%.2f", report.getSummary().getDistance()) + " " +
                    String.format("%.0f", report.getSummary().getNbrOfUsedVehicles()) + " " +
                    String.format("%.2f", ((report.getSummary().getDistance() / bestResult[0] - 1) * 100)) + " " +
                    String.format("%.2f", ((report.getSummary().getNbrOfUsedVehicles() / bestResult[1] - 1) * 100)) + " " +
                    String.format("%.2f", time / 1000)
        }
        println instanceSize + " " +
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
        String vehicleData = lines.get(4)
        int[] data = split(vehicleData)
        xfvrp.addVehicle()
                .setName("Vehicle")
                .setCapacity(data[1])
                .setCount(data[0])
                //.setFixCost(1000)

        String depotData = lines.get(9)
        data = split(depotData)
        xfvrp.addDepot()
                .setExternID(data[0]+"")
                .setXlong(data[1])
                .setYlat(data[2])
                .setTimeWindow(data[4], data[5])

        for (int i = 10; i < lines.size(); i++) {
            String customerData = lines.get(i)
            if(customerData.trim().length() == 0) {
                continue;
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

        xfvrp.addOptType(XFVRPOptType.FIRST_BEST)
        xfvrp.setNbrOfLoopsForILS(10)
        xfvrp.addOptType(XFVRPOptType.RELOCATE)
        xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
        xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

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
