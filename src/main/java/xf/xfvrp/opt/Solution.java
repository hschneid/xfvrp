package xf.xfvrp.opt;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.quality.RouteQuality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * The solution is the central object for the representation of one single point in search space.
 * <p>
 * It contains the set of chosen routes, the quality of each route and the sum of all qualities.
 **/
public class Solution implements Iterable<Node[]> {

    private final XFVRPModel model;
    private final List<RouteQuality> invalidatedRoutesQualities = new ArrayList<>();
    private Node[][] routes = new Node[1][0];
    private RouteQuality[] routeQualities = new RouteQuality[]{new RouteQuality(0, null)};
    private Quality totalQuality = new Quality(null);
    private int[] nbrRoutesOfDepot = new int[0];
    // Route -> is more then allowed number of routes for this depot
    private boolean[] isOverhang = new boolean[1];

    public Solution(XFVRPModel model) {
        this.model = model;

        if (model != null) {
            nbrRoutesOfDepot = new int[model.getNbrOfDepots()];
        }
    }

    public XFVRPModel getModel() {
        return model;
    }

    public Node[][] getRoutes() {
        return routes;
    }

    public RouteQuality[] getRouteQualities() {
        return routeQualities;
    }

    public Quality getQuality() {
        return totalQuality;
    }

    public int[] getNbrRoutesOfDepot() {
        return nbrRoutesOfDepot;
    }

    public boolean[] getOverhangRoutes() {
        return isOverhang;
    }

    public void deleteRoute(int routeIndex) {
        // Reduce number of routes per depot
        nbrRoutesOfDepot[routes[routeIndex][0].getIdx()]--;

        routes[routeIndex] = new Node[0];

        totalQuality.sub(routeQualities[routeIndex]);
        routeQualities[routeIndex] = new RouteQuality(0, null);
        isOverhang[routeIndex] = false;
    }

    public void addRoute(Node[] newRoute) {
        // Update count of routes per depot, if route has customers
        if (newRoute[0].getSiteType() == SiteType.DEPOT)
            nbrRoutesOfDepot[newRoute[0].getIdx()]++;

        // Replace empty slot with new route, if available
        for (int i = 0; i < routes.length; i++) {
            if (routes[i] == null || routes[i].length == 0) {
                routes[i] = newRoute;
                routeQualities[i] = new RouteQuality(i, null);
                return;
            }
        }

        // Otherwise, enlarge routes and place new route at the end
        routes = Arrays.copyOf(routes, routes.length + 1);
        routes[routes.length - 1] = newRoute;

        routeQualities = Arrays.copyOf(routeQualities, routeQualities.length + 1);
        routeQualities[routeQualities.length - 1] = new RouteQuality(routeQualities.length - 1, null);

        isOverhang = Arrays.copyOf(isOverhang, routes.length);
    }

    public void addRoutes(Node[][] newRoutes) {
        for (int i = newRoutes.length - 1; i >= 0; i--) {
            if (newRoutes[i] != null)
                addRoute(newRoutes[i]);
        }
    }

    public void setRoute(int routeIndex, Node[] route) {
        routes[routeIndex] = route;
    }

    /**
     * Updates the quality of a certain route.
     * <p>
     * This means, the sub the current quality and add the new one.
     */
    public void setRouteQuality(int routeIndex, Quality quality) {
        totalQuality.sub(routeQualities[routeIndex]);

        routeQualities[routeIndex] = (RouteQuality) quality;
        totalQuality.add(quality);
    }

    /**
     * This invalidates the quality of a certain route, so that
     * it can be overwritten.
     */
    public void invalidateRouteQuality(int routeIdx) {
        invalidatedRoutesQualities.add(new RouteQuality(
                routeIdx,
                routeQualities[routeIdx]
        ));
    }

    public Solution copy() {
        Solution solution = new Solution(this.model);

        Node[][] copyRoutes = new Node[routes.length][];
        for (int i = 0; i < routes.length; i++)
            copyRoutes[i] = Arrays.copyOf(routes[i], routes[i].length);
        solution.routes = copyRoutes;

        solution.routeQualities = new RouteQuality[routeQualities.length];
        for (int i = 0; i < routeQualities.length; i++)
            solution.routeQualities[i] = new RouteQuality(i, routeQualities[i]);
        solution.totalQuality = new Quality(totalQuality);

        solution.nbrRoutesOfDepot = Arrays.copyOf(nbrRoutesOfDepot, nbrRoutesOfDepot.length);
        solution.isOverhang = Arrays.copyOf(isOverhang, isOverhang.length);

        return solution;
    }

    @Override
    public Iterator<Node[]> iterator() {
        return new SolutionRoutesIterator(routes);
    }

    /**
     * Tells the solution, that the search is over and invalidated qualities can be purged.
     */
    public void fixateQualities() {
        invalidatedRoutesQualities.clear();
    }

    /**
     * Means, that the routes changed without reevaluation. The route qualities
     * must be reset to the old value.
     */
    public void resetQualities() {
        for (RouteQuality invalidatedQuality : invalidatedRoutesQualities) {
            setRouteQuality(invalidatedQuality.getRouteIdx(), invalidatedQuality);
        }
        invalidatedRoutesQualities.clear();
    }

    /**
     * This will purge all routes over the number of routes to retain.
     * <p>
     * This function is called to shrink the number of routes, which makes
     * sense for the optimization. So for-loops will not test so often empty
     * routes.
     */
    public void retainRoutes(int nbrOfRoutesToRetain) {
        Node[][] newRoutes = new Node[nbrOfRoutesToRetain][];
        RouteQuality[] newRouteQualities = new RouteQuality[nbrOfRoutesToRetain];
        System.arraycopy(routes, 0, newRoutes, 0, nbrOfRoutesToRetain);
        System.arraycopy(routeQualities, 0, newRouteQualities, 0, nbrOfRoutesToRetain);

        this.routes = newRoutes;
        this.routeQualities = newRouteQualities;
        this.isOverhang = new boolean[newRoutes.length];

        updateNbrOfRoutesPerDepot();
    }

    public boolean isValid() {
        return routes.length > 0 &&
                routes[0].length > 0 &&
                // Has customers
                Arrays.stream(routes)
                        .flatMap(Arrays::stream)
                        .anyMatch(n -> n.getSiteType() == SiteType.CUSTOMER);
    }

    private void updateNbrOfRoutesPerDepot() {
        Arrays.fill(nbrRoutesOfDepot, 0);

        for (int i = routes.length - 1; i >= 0; i--) {
            if (routes[i][0].getSiteType() == SiteType.DEPOT) {
                nbrRoutesOfDepot[routes[i][0].getIdx()]++;
            }
        }
    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int i = 0; i < routes.length; i++) {
            for (int j = 0; j < routes[i].length; j++) {
                sb.append(routes[i][j].getExternID() + ",");
            }
            sb.append("|");
        }

        return sb.toString();
    }
}
