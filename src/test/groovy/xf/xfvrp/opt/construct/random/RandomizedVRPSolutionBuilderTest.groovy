package xf.xfvrp.opt.construct.random

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.opt.XFVRPOptTypes
import xf.xfvrp.report.Report

import java.util.stream.Collectors
import java.util.stream.IntStream

class RandomizedVRPSolutionBuilderTest extends Specification {

    def positions = new int[9]

    def setup() {
        Arrays.fill(positions, -1)
    }

    def "build random solution without presets"() {
        def vrp = buildXFVRP(new String[9], new int[9], positions, new String[9], new String[9][0])

        when:
        vrp.executeRoutePlanning()
        def rep1 = vrp.getReport()

        vrp.executeRoutePlanning()
        def rep2 = vrp.getReport()

        then:
        rep1.routes.count {r -> r.vehicle.name == 'INVALID'} == 0
        rep1.routes.count(r -> r.events[0].ID == 'D1') > 0
        rep1.routes.count(r -> r.events[0].ID == 'D2') > 0
        rep2.routes.count {r -> r.vehicle.name == 'INVALID'} == 0
        rep2.routes.count(r -> r.events[0].ID == 'D1') > 0
        rep2.routes.count(r -> r.events[0].ID == 'D2') > 0
        rep1.summary.getCost() - rep2.summary.getCost() > 1
        allNodesPlanned(rep1)
        allNodesPlanned(rep2)
    }

    def "build random solution with preset block names"() {
        def blockNames = [null, 'B1', null, 'B2', null, null, 'B1', 'B2', null] as String[]
        def vrp = buildXFVRP(blockNames, new int[9], positions, new String[9], new String[9][0])

        when:
        vrp.executeRoutePlanning()
        def rep = vrp.getReport()

        then:
        // B, G
        rep.routes.find {route -> route.events.count({e ->
            e.ID == 'B' || e.ID == 'G'
        }) == 2}
        // D, H
        rep.routes.find {route -> route.events.count({e ->
            e.ID == 'D' || e.ID == 'H'
        }) == 2}
        allNodesPlanned(rep)
    }

    def "build random solution with preset block names and rank"() {
        def blockNames = [null, 'B1', null, 'B1', null, null, 'B1', 'B1', null] as String[]
        def blockRanks = [0, 6, 0, 3, 0, 0, 9, 1, 0] as int[]
        def vrp = buildXFVRP(blockNames, blockRanks, positions, new String[9], new String[9][0])

        when:
        vrp.executeRoutePlanning()
        def rep = vrp.getReport()

        then:
        // H, D, B, G
        rep.routes.find {route -> IntStream
                .range(0, route.events.size())
                .mapToObj({i -> new String[] {i+"", route.events[i].ID}})
                .filter({s ->
                    s[1] == 'B' ||
                            s[1] == 'D' ||
                            s[1] == 'G' ||
                            s[1] == 'H'
                })
                .sorted(Comparator.comparing({s -> Integer.valueOf(s[0])}))
                .map({s -> s[1]})
                .collect(Collectors.joining(',')) == 'H,D,B,G'
        }
        allNodesPlanned(rep)
    }

    def "build random solution with preset block names and postitions"() {
        def blockNames = [null, 'B1', null, 'B1', null, null, 'B1', 'B1', null] as String[]
        def blockPositions = [0, 3, 0, 1, 0, 0, 2, 4, 0] as int[]
        def vrp = buildXFVRP(blockNames, new int[9], blockPositions, new String[9], new String[9][0])

        when:
        vrp.executeRoutePlanning()
        def rep = vrp.getReport()

        then:
        rep.routes.find {route -> IntStream
                .range(0, route.events.size())
                .mapToObj({i -> new String[] {i+"", route.events[i].ID}})
                .filter({s ->
                    s[1] == 'B' ||
                            s[1] == 'D' ||
                            s[1] == 'G' ||
                            s[1] == 'H'
                })
                .sorted(Comparator.comparing({s -> Integer.valueOf(s[0])}))
                .map({s -> s[1]})
                .collect(Collectors.joining(',')) == 'D,G,B,H'
        }
        allNodesPlanned(rep)
    }

    def "build random solution with preset depots"() {
        def blockDepots = [null, 'D1', null, 'D2', null, null, 'D1', 'D2', null] as String[]
        def vrp = buildXFVRP(new String[9], new int[9], positions, blockDepots, new String[9][0])

        when:
        vrp.executeRoutePlanning()
        def rep = vrp.getReport()

        then:
        rep.routes.find {route ->
            route.events[0].ID == 'D1' && route.events.any({e ->e.ID == 'B'})
        } != null
        rep.routes.find {route ->
            route.events[0].ID == 'D1' && route.events.any({e ->e.ID == 'G'})
        } != null
        rep.routes.find {route ->
            route.events[0].ID == 'D2' && route.events.any({e ->e.ID == 'D'})
        } != null
        rep.routes.find {route ->
            route.events[0].ID == 'D2' && route.events.any({e ->e.ID == 'H'})
        } != null
        allNodesPlanned(rep)
    }

    def "build random solution with preset blacklist"() {
        def blacklist = [null, ['A', 'C'], null, ['A','E'], null, null, ['H','B'], null, null] as String[][]
        def vrp = buildXFVRP(new String[9], new int[9], positions, new String[9], blacklist)

        when:
        vrp.executeRoutePlanning()
        def rep = vrp.getReport()

        then:
        !rep.routes.any {route ->
            route.events.any({e ->e.ID == 'B'}) && route.events.any({e ->e.ID == 'A' || e.ID == 'C'})
        }
        !rep.routes.any {route ->
            route.events.any({e ->e.ID == 'D'}) && route.events.any({e ->e.ID == 'A' || e.ID == 'E'})
        }
        !rep.routes.any {route ->
            route.events.any({e ->e.ID == 'G'}) && route.events.any({e ->e.ID == 'B' || e.ID == 'H'})
        }
        allNodesPlanned(rep)
    }

    XFVRP buildXFVRP(String[] blockNames, int[] blockRanks, int[] blockPos, String[] blockDepots, String[][] blacklist) {
        XFVRP xfvrp = new XFVRP()
        xfvrp.addVehicle().setName('V1').setCapacity([4] as float[]).setFixCost(11).setVarCost(4)

        // Add depots
        xfvrp.addDepot().setExternID('D1').setXlong(0).setYlat(0)
        xfvrp.addDepot().setExternID('D2').setXlong(-10).setYlat(-10)
        // Add customers
        xfvrp.addCustomer().setExternID('A').setXlong(10).setYlat(10).setDemand(1).setPresetBlockName(blockNames[0]).setPresetBlockRank(blockRanks[0]).setPresetBlockPos(blockPos[0]).setPresetDepotList(depotSet(blockDepots[0])).setPresetRoutingBlackList(blackSet(blacklist[0]))
        xfvrp.addCustomer().setExternID('B').setXlong(11).setYlat(11).setDemand(1).setPresetBlockName(blockNames[1]).setPresetBlockRank(blockRanks[1]).setPresetBlockPos(blockPos[1]).setPresetDepotList(depotSet(blockDepots[1])).setPresetRoutingBlackList(blackSet(blacklist[1]))
        xfvrp.addCustomer().setExternID('C').setXlong(10).setYlat(11).setDemand(1).setPresetBlockName(blockNames[2]).setPresetBlockRank(blockRanks[2]).setPresetBlockPos(blockPos[2]).setPresetDepotList(depotSet(blockDepots[2])).setPresetRoutingBlackList(blackSet(blacklist[2]))
        xfvrp.addCustomer().setExternID('D').setXlong(11).setYlat(10).setDemand(1).setPresetBlockName(blockNames[3]).setPresetBlockRank(blockRanks[3]).setPresetBlockPos(blockPos[3]).setPresetDepotList(depotSet(blockDepots[3])).setPresetRoutingBlackList(blackSet(blacklist[3]))
        xfvrp.addCustomer().setExternID('E').setXlong(9).setYlat(10).setDemand(1).setPresetBlockName(blockNames[4]).setPresetBlockRank(blockRanks[4]).setPresetBlockPos(blockPos[4]).setPresetDepotList(depotSet(blockDepots[4])).setPresetRoutingBlackList(blackSet(blacklist[4]))
        xfvrp.addCustomer().setExternID('F').setXlong(10).setYlat(9).setDemand(1).setPresetBlockName(blockNames[5]).setPresetBlockRank(blockRanks[5]).setPresetBlockPos(blockPos[5]).setPresetDepotList(depotSet(blockDepots[5])).setPresetRoutingBlackList(blackSet(blacklist[5]))
        xfvrp.addCustomer().setExternID('G').setXlong(11).setYlat(9).setDemand(1).setPresetBlockName(blockNames[6]).setPresetBlockRank(blockRanks[6]).setPresetBlockPos(blockPos[6]).setPresetDepotList(depotSet(blockDepots[6])).setPresetRoutingBlackList(blackSet(blacklist[6]))
        xfvrp.addCustomer().setExternID('H').setXlong(9).setYlat(11).setDemand(1).setPresetBlockName(blockNames[7]).setPresetBlockRank(blockRanks[7]).setPresetBlockPos(blockPos[7]).setPresetDepotList(depotSet(blockDepots[7])).setPresetRoutingBlackList(blackSet(blacklist[7]))
        xfvrp.addCustomer().setExternID('I').setXlong(10.5).setYlat(10.5).setDemand(1).setPresetBlockName(blockNames[8]).setPresetBlockRank(blockRanks[8]).setPresetBlockPos(blockPos[8]).setPresetDepotList(depotSet(blockDepots[8])).setPresetRoutingBlackList(blackSet(blacklist[8]))

        xfvrp.setMetric(Metrics.EUCLEDIAN.get())
        xfvrp.addOptType(XFVRPOptTypes.RANDOM)

        return xfvrp
    }

    Set<String> depotSet(String dep) {
        if(dep == null) return null
        return Set.of(dep)
    }

    Set<String> blackSet(String[] list) {
        if(list == null || list.length == 0) return null
        return Arrays.stream(list).collect(Collectors.toSet())
    }

    boolean allNodesPlanned(Report s) {
        return 9 == s.routes
                .stream()
                .flatMap(r -> r.events.stream())
                .filter({f -> f.siteType == SiteType.CUSTOMER})
                .map({n -> n.ID})
                .distinct()
                .count()
    }

}
