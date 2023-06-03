package util.instances

import xf.xfvrp.base.*
import xf.xfvrp.base.compartment.CompartmentInitializer
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.InternalMetric
import xf.xfvrp.base.metric.Metric
import xf.xfvrp.base.monitor.StatusManager
import xf.xfvrp.opt.init.ModelBuilder

import java.util.stream.Collectors

class TestXFVRPModel {

    static XFVRPModel get(List<Node> nodes, Vehicle vehicle) {
        return get(
                nodes,
                vehicle,
                new XFVRPParameter()
        )
    }

    static XFVRPModel get(List<Node> nodes, Vehicle vehicle, XFVRPParameter parameter) {
        Node[] nodeArr = nodes.toArray(new Node[0])

        def metric = new EucledianMetric()

        return get(
                nodeArr,
                metric,
                vehicle,
                parameter
        )
    }


    static XFVRPModel get(Node[] nodeArr, Metric metric, Vehicle vehicle, XFVRPParameter parameter) {
        def nodes = nodeArr.toList().stream()
                .filter(f -> f.getSiteType() == SiteType.DEPOT)
                .sorted(Comparator.comparing({Node f -> f.externID}))
                .collect(Collectors.toList())
        nodes.addAll(nodeArr.toList().stream()
                .filter(f -> f.getSiteType() == SiteType.REPLENISH)
                .sorted(Comparator.comparing({Node f -> f.externID}))
                .collect(Collectors.toList()))
        nodes.addAll(nodeArr.toList().stream()
                .filter(f -> f.getSiteType() == SiteType.CUSTOMER)
                .sorted(Comparator.comparing({Node f -> f.externID}))
                .collect(Collectors.toList()))

        nodeArr = nodes.toArray(new Node[0])

        List<CompartmentType> types = new ArrayList<>()
        CompartmentInitializer.check(nodeArr, types, new Vehicle[]{vehicle})

        return new ModelBuilder().build(
                nodeArr,
                types.toArray(new CompartmentType[0]),
                vehicle,
                metric,
                parameter,
                new StatusManager()
        )
    }

    static XFVRPModel get(Node[] nodeArr, CompartmentType[] compartmentTypes, InternalMetric metric, InternalMetric optMetric, Vehicle vehicle, XFVRPParameter parameter) {
        return new XFVRPModel(
                nodeArr,
                compartmentTypes,
                metric,
                optMetric,
                vehicle,
                parameter
        )
    }
}
