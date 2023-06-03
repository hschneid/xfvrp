package xf.xfvrp.opt.construct.insert;

import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Insertion heuristic First-Best
 * <p>
 * Scope: This heuristic shall reduce the number of routes during construction.
 * If you wish a reduction of route length choose for Savings.
 * <p>
 * All customers are marked as unplanned and are brought in
 * a randomized order. Then the customers are inserted sequentially,
 * where the cheapest insert position is search for the current customers to plan.
 * <p>
 * Additionally a reinsert is possible, so that bad decisions can be corrected.
 *
 * @author hschneid
 */
public class XFVRPFirstBestInsert extends XFVRPOptBase {

    /*
     * (non-Javadoc)
     * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
     */
    @Override
    public Solution execute(Solution input) throws XFVRPException {
        List<Node> customers = getCustomers(input.getModel());

        // Init with empty route (attention for multiple depots)
        Solution solution = initNewSolution(input.getModel());

        // Randomized ordering of customer insertion
        Collections.shuffle(customers, rand);

        // Insert all customers
        insertCustomers(solution, customers);

        // Reinsert all customers (loop-able)
        for (int i = 0; i < model.getParameter().getILSLoops(); i++)
            reinsertNodes(solution, customers);

        NormalizeSolutionService.normalizeRouteWithCleanup(solution);

        return solution;
    }

    /**
     * Inserts the unplanned customers into the solution
     * <p>
     * The customers are inserted sequentially in a randomized order. For each
     * customer the cheapest insert position is calculated with the current
     * giant route solution. Each insertion adjusts the current solution.
     */
    private void insertCustomers(Solution solution, List<Node> customers) throws XFVRPException {
        for (Node customer : customers) {
            boolean inserted = insertCustomer(solution, customer);
            if (!inserted) {
                // If no feasible insertion can be found, add empty routes
                NormalizeSolutionService.normalizeRoute(solution);
                insertCustomer(solution, customer);
            }
        }
    }

    /**
     * Each node will be removed from current solution and reinserted.
     * <p>
     * This make sense, because this heuristic is strongly dependent from
     * ordered sequence of customer to insert. So a reinsert of first inserted
     * nodes may give the possibility to find a better insert position than
     * the order insertion before
     */
    private void reinsertNodes(Solution solution, List<Node> customers) throws XFVRPException {
        for (Node customer : customers) {
            // Remove customer
            removeCustomer(solution, customer);
            // Reinsert customer
            boolean inserted = insertCustomer(solution, customer);
            if (!inserted) {
                // If no feasible insertion can be found, add empty routes
                NormalizeSolutionService.normalizeRoute(solution);
                insertCustomer(solution, customer);
            }
        }
    }

    private boolean insertCustomer(Solution solution, Node customer) {
        // Get all feasible insertion points on current routes
        List<float[]> insertPoints = evaluate(solution, customer);

        // Sort for lowest insertion costs (reverse orientation)
        insertPoints.sort((a, b) -> (int) ((b[0] - a[0]) * 1000f));

        // For all found feasible insertion points
        for (int i = insertPoints.size() - 1; i >= 0; i--) {
            float[] val = insertPoints.get(i);
            int routeIdx = (int) val[1];

            // Insert customer
            Node[] oldRoute = insertCustomer(solution, customer, routeIdx, (int) val[2]);

            // Evaluate new solution
            Quality qq = check(solution, routeIdx, routeIdx);
            if (qq.getPenalty() == 0) {
                solution.fixateQualities();
                return true;
            }

            // Reverse change
            solution.setRoute(
                    routeIdx,
                    oldRoute
            );
            solution.resetQualities();
        }

        return false;
    }

    /**
     * Evaluates all insertion points in a current solution for
     * a new customer, where this customer is not part of the current
     * solution.
     */
    private List<float[]> evaluate(Solution solution, Node customer) throws XFVRPException {
        List<float[]> insertPoints = new ArrayList<>();

        // For all insert positions
        for (int routeIdx = 0; routeIdx < solution.getRoutes().length; routeIdx++) {
            Node[] route = solution.getRoutes()[routeIdx];
            for (int pos = 1; pos < route.length; pos++) {
                insertPoints.add(new float[]{
                        getEffortOfInsertion(customer, route, pos, solution.getModel()),
                        routeIdx,
                        pos
                });
            }
        }

        return insertPoints;
    }

    /**
     * Calculates the additional distance to add a new customer to given route.
     */
    private float getEffortOfInsertion(Node newCustomer, Node[] route, int pos, XFVRPModel model) {
        return model.getDistanceForOptimization(route[pos - 1], newCustomer) +
                model.getDistanceForOptimization(newCustomer, route[pos])
                - model.getDistanceForOptimization(route[pos - 1], route[pos]);
    }

    /**
     * Inserts a customer node into a solution at a certain position.
     */
    private Node[] insertCustomer(Solution solution, Node customer, int routeIdx, int insertPos) {
        Node[] route = solution.getRoutes()[routeIdx];
        Node[] newRoute = new Node[route.length + 1];

        System.arraycopy(route, 0, newRoute, 0, insertPos);
        newRoute[insertPos] = customer;
        System.arraycopy(route, insertPos, newRoute, insertPos + 1, route.length - insertPos);

        solution.setRoute(routeIdx, newRoute);

        return route;
    }

    /**
     * Removes a certain customer node from giant route and returns the giant route without this customer node
     */
    private void removeCustomer(Solution solution, Node customer) throws XFVRPException {
        for (int routeIdx = solution.getRoutes().length - 1; routeIdx >= 0; routeIdx--) {
            for (int pos = solution.getRoutes()[routeIdx].length - 1; pos >= 0; pos--) {
                if (solution.getRoutes()[routeIdx][pos] == customer) {
                    Node[] route = solution.getRoutes()[routeIdx];
                    Node[] newRoute = new Node[route.length - 1];

                    System.arraycopy(route, 0, newRoute, 0, pos);
                    System.arraycopy(route, pos + 1, newRoute, pos, route.length - pos - 1);

                    solution.setRoute(routeIdx, newRoute);
                    return;
                }
            }
        }
    }

    /**
     * Initialize a new solution with single empty routes per depot.
     */
    private Solution initNewSolution(XFVRPModel model) {
        Node[] route = new Node[2];
        route[0] = Util.createIdNode(model.getNodes()[0], 0);
        route[1] = Util.createIdNode(model.getNodes()[0], 1);

        Solution solution = new Solution(model);
        solution.addRoute(route);
        return NormalizeSolutionService.normalizeRoute(solution);
    }

    /**
     * Retries all customers nodes from node array in model.
     */
    private List<Node> getCustomers(XFVRPModel model) {
        List<Node> customers = new ArrayList<>();
        for (int i = model.getNbrOfDepots() + model.getNbrOfReplenish(); i < model.getNbrOfNodes(); i++)
            customers.add(model.getNodes()[i]);
        return customers;
    }
}
