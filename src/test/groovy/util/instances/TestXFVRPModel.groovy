package util.instances

import xf.xfvrp.base.Node
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.compartment.CompartmentInitializer
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.InternalMetric
import xf.xfvrp.base.metric.Metric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.base.monitor.DefaultStatusMonitor
import xf.xfvrp.base.monitor.StatusManager
import xf.xfvrp.opt.init.ModelBuilder

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

    public static XFVRPModel get(Node[] nodeArr, CompartmentType[] compartmentTypes, InternalMetric metric, InternalMetric optMetric, Vehicle vehicle, XFVRPParameter parameter) {
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
