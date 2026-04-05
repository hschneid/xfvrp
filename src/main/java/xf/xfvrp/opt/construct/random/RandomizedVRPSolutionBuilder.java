package xf.xfvrp.opt.construct.random;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.evaluation.EvaluationService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomizedVRPSolutionBuilder extends XFVRPOptBase {

    private final Random rand = new Random(1234);

    private final EvaluationService evaluationService = new EvaluationService();

    /*
     * (non-Javadoc)
     * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
     */
    @Override
    public Solution execute(Solution input) throws XFVRPException {
        // blocks must be planned on same route. Other presets are checked by evaluation service.
        List<List<Node>> validCustomers = getValidCustomersByBlock(input);

        return buildSolution(validCustomers, input.getModel());
    }

    private Solution buildSolution(List<List<Node>> validCustomers, XFVRPModel model) {
        // Bring customers into randomized order
        Collections.shuffle(validCustomers, rand);

        Solution sol = new Solution(model);
        NormalizeSolutionService.normalizeRoute(sol);

        // For every customer block ...
        for (List<Node> validNode : validCustomers) {

            var routeIdx = IntStream.range(0, sol.getRoutes().length)
                    .boxed()
                    .collect(Collectors.toList());
            Collections.shuffle(routeIdx, rand);

            // For every route
            for (Integer j : routeIdx) {
                // Check if customer can be added at the end of this route
                var newRoute = createRoute(validNode, sol.getRoutes()[j]);
                var isValid = check(sol, newRoute);
                if (isValid) {
                    sol.setRoute(j, newRoute);
                    break;
                }
            }
            NormalizeSolutionService.normalizeRoute(sol);
        }

        return sol;
    }

    private boolean check(Solution sol, Node[] newRoute) {
        var newSol = new Solution(sol.getModel());
        newSol.addRoute(newRoute);
        var result = evaluationService.check(newSol);

        return result.getPenalty() == 0;
    }

    private Node[] createRoute(List<Node> customer, Node[] route) {
        var newRoute = new Node[route.length + customer.size()];
        System.arraycopy(route, 0, newRoute, 0, route.length - 1);
        for (int i = 0; i < customer.size(); i++) {
            newRoute[route.length - 1 + i] = customer.get(i);
        }
        newRoute[newRoute.length - 1] = route[route.length - 1];

        return newRoute;
    }

    private List<List<Node>> getValidCustomersByBlock(Solution inputSolution) {
        Map<Integer, List<Node>> blockedNodes = Arrays.stream(inputSolution.getRoutes())
                .flatMap(Arrays::stream)
                .filter(node -> node.getPresetBlockIdx() != BlockNameConverter.DEFAULT_BLOCK_IDX)
                .filter(node -> node.getSiteType() == SiteType.CUSTOMER)
                .collect(Collectors.groupingBy(Node::getPresetBlockIdx));

        List<List<Node>> unblockedNodes = Arrays.stream(inputSolution.getRoutes())
                .flatMap(Arrays::stream)
                .filter(node -> node.getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX)
                .filter(node -> node.getSiteType() == SiteType.CUSTOMER)
                .map(node -> List.of(node))
                .collect(Collectors.toList());

        unblockedNodes.addAll(blockedNodes.values());

        return unblockedNodes;
    }
}
