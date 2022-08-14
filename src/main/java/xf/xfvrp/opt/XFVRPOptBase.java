package xf.xfvrp.opt;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPBase;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.evaluation.EvaluationService;

import java.util.Random;


/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * This class contains utility methods for all extending classes.
 * <p>
 * There are methods for changing a solution (giant tour)
 * like move, swap or relocate and evaluating a solution by
 * simulating the route plan node by node (Lineare evaluation time).
 * <p>
 * All classes inheriting this class have access to an instance of XFVRPModel.
 *
 * @author hschneid
 */
public abstract class XFVRPOptBase extends XFVRPBase<XFVRPModel> {

    public boolean isSplittable = false;
    protected Random rand = new Random(1234);
    protected EvaluationService evaluationService = new EvaluationService();

    /**
     * Inverts the node sequence in the range
     * from start to end, both inclusive.
     */
    protected void swap(Node[] nodes, int start, int end) {
        int offset = 0;
        while (end - offset > start + offset) {
            Node tmp = nodes[end - offset];
            nodes[end - offset] = nodes[start + offset];
            nodes[start + offset] = tmp;
            offset++;
        }
    }

    /**
     * Processes a check evaluation.
     */
    public Quality check(Solution solution) throws XFVRPException {
        return evaluationService.check(solution);
    }

    /**
     * Processes a check evaluation for two routes.
     */
    public Quality check(Solution solution, int routeIdxA, int routeIdxB) throws XFVRPException {
        return evaluationService.check(solution, routeIdxA, routeIdxB);
    }

    public Random getRandom() {
        return rand;
    }

    /**
     * Returns the distance between two given XFNodes
     */
    protected float getDistanceForOptimization(Node n1, Node n2) {
        return model.getDistanceForOptimization(n1, n2);
    }
}
