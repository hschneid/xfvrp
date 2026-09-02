package xf.xfvrp.opt.improve.routebased;

import java.util.*;

/**
 * Copyright (c) 2012-2026 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * Memory-efficient cache for neighborhood search that exploits invariance
 * between iterations.
 * <p>
 * Stores all improving moves per route pair. When a move changes
 * only 2 routes, all cached moves between unchanged routes remain valid.
 * <p>
 * Memory: O(M) where M is the total number of improving moves across all pairs.
 *
 * @author hschneid
 */
public class SearchCache {

    private final Map<Long, List<float[]>> movesPerPair = new HashMap<>();
    private final Set<Integer> changedRoutes = new HashSet<>();
    private boolean initialized = false;

    /**
     * Resets the cache completely. Called at the start of an optimization run.
     */
    public void reset() {
        movesPerPair.clear();
        changedRoutes.clear();
        initialized = false;
    }

    /**
     * Checks if a route pair needs re-evaluation.
     */
    public boolean needsEvaluation(int routeA, int routeB) {
        if (!initialized) return true;
        return changedRoutes.contains(routeA) || changedRoutes.contains(routeB);
    }

    /**
     * Stores all improving moves for a route pair.
     */
    public void setMoves(int routeA, int routeB, List<float[]> moves) {
        long key = pairKey(routeA, routeB);
        if (moves != null && !moves.isEmpty()) {
            movesPerPair.put(key, moves);
        } else {
            movesPerPair.remove(key);
        }
    }

    /**
     * Gets the cached moves for a route pair, or null if none cached.
     */
    public List<float[]> getMoves(int routeA, int routeB) {
        return movesPerPair.get(pairKey(routeA, routeB));
    }


    /**
     * Called after an improving move has been applied. Sets the changed routes
     * and removes all cached entries involving those routes.
     */
    public void invalidateRoutes(int routeA, int routeB) {
        changedRoutes.clear();
        changedRoutes.add(routeA);
        if (routeA != routeB) {
            changedRoutes.add(routeB);
        }
        removeEntriesForChangedRoutes();
        initialized = true;
    }

    /**
     * Marks an additional route as changed (e.g., due to overhang or normalization).
     */
    public void addChangedRoute(int route) {
        if (changedRoutes.add(route)) {
            movesPerPair.entrySet().removeIf(entry -> {
                int r1 = decodeRouteA(entry.getKey());
                int r2 = decodeRouteB(entry.getKey());
                return r1 == route || r2 == route;
            });
        }
    }

    public int size() {
        return movesPerPair.size();
    }

    public Set<Integer> getChangedRoutes() {
        return Collections.unmodifiableSet(changedRoutes);
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void removeEntriesForChangedRoutes() {
        movesPerPair.entrySet().removeIf(entry -> {
            int r1 = decodeRouteA(entry.getKey());
            int r2 = decodeRouteB(entry.getKey());
            return changedRoutes.contains(r1) || changedRoutes.contains(r2);
        });
    }

    static long pairKey(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return ((long) min << 24) | (max & 0xFFFFFFL);
    }

    private static int decodeRouteA(long key) {
        return (int) ((key >> 24) & 0xFFFFFF);
    }

    private static int decodeRouteB(long key) {
        return (int) (key & 0xFFFFFF);
    }
}
