package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.quality.RouteQuality;
import xf.xfvrp.opt.Solution;

public class DepotCountEvaluationService {

    public static void check(Solution solution) {
        XFVRPModel model = solution.getModel();

        boolean isTainted = false;
        int[] taintedCounts = new int[model.getNbrOfDepots()];
        for (int i = 0; i < model.getNbrOfDepots(); i++) {
            taintedCounts[i] = Math.max(0, solution.getNbrRoutesOfDepot()[i] - model.getNodes()[i].getMaxNbrOfRoutes());
            isTainted = taintedCounts[i] > 0;
        }

        if(!isTainted)
            return;

        for (int i = solution.getRoutes().length - 1; i >= 0; i--) {
            if(solution.getRoutes()[i] != null &&
                    solution.getRoutes()[i].length > 2 &&
                    solution.getRoutes()[i][1].getSiteType() == SiteType.CUSTOMER &&
                    taintedCounts[solution.getRoutes()[i][0].getIdx()] > 0
            ) {
                RouteQuality routeQuality = new RouteQuality(solution.getRouteQualities()[i]);
                routeQuality.addCost(routeQuality.getCost() * 10);
                solution.setRouteQuality(i, routeQuality);

                taintedCounts[solution.getRoutes()[i][0].getIdx()]--;
            }
        }
    }
}
