package util.instances

import xf.xfvrp.base.Node
import xf.xfvrp.base.NormalizeSolutionService
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.opt.Solution

import java.util.stream.Collectors

class Helper {

    static Solution set(Node... nodes) {
        def modelNodes = new ArrayList<Node>()

        modelNodes.addAll(Arrays.asList(nodes)
                .stream()
                .filter({f -> f.siteType == SiteType.DEPOT})
                .sorted(Comparator.comparing({Node n -> n.externID}))
                .distinct()
                .collect(Collectors.toList())
        )
        modelNodes.addAll(Arrays.asList(nodes)
                .stream()
                .filter({f -> f.siteType == SiteType.REPLENISH})
                .sorted(Comparator.comparing({Node n -> n.externID}))
                .distinct()
                .collect(Collectors.toList())
        )
        modelNodes.addAll(Arrays.asList(nodes)
                .stream()
                .filter({f -> f.siteType == SiteType.CUSTOMER})
                .sorted(Comparator.comparing({Node n -> n.externID}))
                .collect(Collectors.toList())
        )

        def model = TestXFVRPModel.get(modelNodes, new TestVehicle(name: "V1").getVehicle())

        return set(model, nodes)
    }

    static Solution set(XFVRPModel model, Node... nodes) {
        def sol = setNoNorm(model, nodes)
        NormalizeSolutionService.normalizeRoute(sol)

        return sol
    }

    static Solution setNoNorm(XFVRPModel model, Node... nodes) {
        def sol = new Solution(model)
        sol.setGiantRoute(nodes as Node[])

        return sol
    }
}
