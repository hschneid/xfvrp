package xf.xfvrp.opt.improve.routebased;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil;

import java.util.*;

/**
 * Copyright (c) 2012-2026 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * This class provides the basis local search structure
 * of iteratively calling an improve-method and memorizing
 * the best solution.
 * <p>
 * The downhill search stops if no further improvement
 * can be found.
 * <p>
 * A {@link SearchCache} is used to exploit invariance between iterations:
 * When only 2 routes change in a move, all improving moves between unaffected
 * route pairs remain valid and do not need to be re-evaluated. The cache
 * stores all improving moves per route pair.
 *
 * @author hschneid
 */
public abstract class XFVRPOptImpBase extends XFVRPOptBase {

    protected SearchCache searchCache = new SearchCache();

    /**
     * Constructor for all improvement heuristics
     */
    public XFVRPOptImpBase() {
        isSplittable = true;
    }

    /**
     * Searches for all improving moves between two routes (undirected pair).
     * For move operators, this includes both directions (a→b and b→a).
     * For swap operators, this includes all swap configurations.
     * For mixed operators, this includes both moves and swaps.
     *
     * @param solution the current solution
     * @param queue    the queue to add all improving moves to
     * @param routeIdxA first route index
     * @param routeIdxB second route index (may equal routeIdxA for intra-route search)
     */
    protected abstract void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB);

    protected abstract Node[][] change(Solution solution, float[] changeParameter) throws XFVRPException;

    /**
     * Evaluates all dirty route pairs and stores their moves in the cache.
     * Clean pairs are skipped - their cached moves remain valid.
     */
    protected void evaluateDirtyPairs(Solution solution) {
        Node[][] routes = solution.getRoutes();
        int nbrOfRoutes = routes.length;
        for (int a = 0; a < nbrOfRoutes; a++) {
            for (int b = a; b < nbrOfRoutes; b++) {
                if (searchCache.needsEvaluation(a, b)) {
                    ArrayDeque<float[]> pairQueue = new ArrayDeque<>();
                    searchRoutePair(solution, pairQueue, a, b);
                    searchCache.setMoves(a, b, new ArrayList<>(pairQueue));
                }
            }
        }
    }

    /**
     * Collects cached moves for a directed route pair (src → dst).
     * Only moves where move[1]==src and move[2]==dst are added.
     */
    protected void collectDirectedMoves(int src, int dst, Queue<float[]> queue) {
        int a = Math.min(src, dst);
        int b = Math.max(src, dst);
        List<float[]> cached = searchCache.getMoves(a, b);
        if (cached != null) {
            for (float[] move : cached) {
                if ((int) move[1] == src && (int) move[2] == dst) {
                    queue.add(move);
                }
            }
        }
    }

    /**
     * Collects all cached moves for a route pair.
     */
    protected void collectPairMoves(int routeIdxA, int routeIdxB, Queue<float[]> queue) {
        List<float[]> cached = searchCache.getMoves(routeIdxA, routeIdxB);
        if (cached != null) {
            queue.addAll(cached);
        }
    }

    /**
     * Collects cached moves for a directed route pair, filtered by array length.
     * Used by mixed operators (SegmentExchange) to separate moves (length 8) from swaps (length 9).
     */
    protected void collectDirectedMoves(int src, int dst, Queue<float[]> queue, int moveArrayLength) {
        int a = Math.min(src, dst);
        int b = Math.max(src, dst);
        List<float[]> cached = searchCache.getMoves(a, b);
        if (cached != null) {
            for (float[] move : cached) {
                if (move.length == moveArrayLength && (int) move[1] == src && (int) move[2] == dst) {
                    queue.add(move);
                }
            }
        }
    }

    /**
     * Collects cached moves for a route pair, filtered by array length.
     */
    protected void collectPairMoves(int routeIdxA, int routeIdxB, Queue<float[]> queue, int moveArrayLength) {
        List<float[]> cached = searchCache.getMoves(routeIdxA, routeIdxB);
        if (cached != null) {
            for (float[] move : cached) {
                if (move.length == moveArrayLength) {
                    queue.add(move);
                }
            }
        }
    }

    /**
     * Searches all route pairs and returns all improving moves.
     * Uses the SearchCache to skip re-evaluation of unchanged route pairs.
     * <p>
     * Subclasses must implement the collection from cache in the iteration
     * order matching their original SearchUtil.search() for consistent
     * tie-breaking behavior.
     */
    protected abstract Queue<float[]> search(Solution solution);

    @Override
    public Solution execute(Solution solution) throws XFVRPException {
        Quality bestResult = check(solution);

        searchCache.reset();

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) / 1000.0 < model.getParameter().getMaxRunningTimeInSec()) {
            Quality result = improve(solution, bestResult);
            if (result == null)
                break;

            bestResult = result;

            // Capture route IDs before normalization
            int[] oldRouteIds = solution.getRouteIds().clone();
            int oldNbrOfRoutes = oldRouteIds.length;
            boolean[] oldOverhang = Arrays.copyOf(
                    solution.getOverhangRoutes(),
                    solution.getOverhangRoutes().length
            );

            NormalizeSolutionService.normalizeRoute(solution);

            updateCacheAfterNormalization(solution, oldRouteIds, oldOverhang, oldNbrOfRoutes);
        }

        NormalizeSolutionService.normalizeRouteWithCleanup(solution);

        return solution;
    }

    /**
     * Searches for the best feasible improving move and applies it.
     * <p>
     * Uses the SearchCache (via search()) to skip re-evaluation of unchanged
     * route pairs. Only pairs where at least one route was changed in the
     * previous iteration are re-evaluated. For all other pairs, the cached
     * moves are reused.
     */
    protected Quality improve(final Solution solution, Quality bestResult) throws XFVRPException {
        check(solution);

        Queue<float[]> improvingSteps = search(solution);

        // Find first valid improving change
        while (!improvingSteps.isEmpty()) {
            float[] val = improvingSteps.remove();

            Node[][] oldRoutes = change(solution, val);

            Quality result = check(solution, (int) val[1], (int) val[2]);
            if (isImprovement(result, bestResult, (int) val[7])) {
                solution.fixateQualities();

                searchCache.invalidateRoutes((int) val[1], (int) val[2]);

                return result;
            }

            reverseChange(solution, val, oldRoutes);
        }

        return null;
    }

    private void updateCacheAfterNormalization(Solution solution, int[] oldRouteIds, boolean[] oldOverhang, int oldNbrOfRoutes) {
        boolean[] newOverhang = solution.getOverhangRoutes();
        int newNbrOfRoutes = solution.getRoutes().length;
        int[] newRouteIds = solution.getRouteIds();

        // Invalidate any route whose ID changed (route was replaced/modified by normalization)
        int minLen = Math.min(oldNbrOfRoutes, newNbrOfRoutes);
        for (int i = 0; i < minLen; i++) {
            if (newRouteIds[i] != oldRouteIds[i]) {
                searchCache.addChangedRoute(i);
            }
        }

        // Invalidate routes whose overhang status changed
        int minOhLen = Math.min(oldOverhang.length, newOverhang.length);
        for (int i = 0; i < minOhLen; i++) {
            if (oldOverhang[i] != newOverhang[i]) {
                searchCache.addChangedRoute(i);
            }
        }

        // Invalidate newly added routes
        for (int i = oldNbrOfRoutes; i < newNbrOfRoutes; i++) {
            searchCache.addChangedRoute(i);
        }
    }

    private boolean isImprovement(Quality currentResult, Quality bestResult, int overhangFlag) {
        return currentResult.getPenalty() == 0 &&
                (currentResult.getFitness() < bestResult.getFitness() ||
                        overhangFlag == XFVRPMoveUtil.IS_OVERHANG);
    }

    private void reverseChange(Solution solution, float[] val, Node[][] oldRoutes) {
        solution.setRoute((int) val[1], oldRoutes[0]);
        if (oldRoutes.length > 1)
            solution.setRoute((int) val[2], oldRoutes[1]);
        solution.resetQualities();
    }


}
