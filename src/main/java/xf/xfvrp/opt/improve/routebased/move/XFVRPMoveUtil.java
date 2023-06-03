package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.Solution;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPMoveUtil {

    public static final int NO_INVERT = 0;
    public static final int INVERT = 1;
    public static final int IS_OVERHANG = -1;
    public static final int NO_OVERHANG = 0;

    /**
     * Moves the nodes in the range from srcStart and srcEnd, both inclusive,
     * before the position dstPos from one route to another route.
     */
    public static Node[][] change(Solution solution, float[] val) throws XFVRPException {
        int srcRouteIdx = (int) val[1];
        int dstRouteIdx = (int) val[2];
        int srcStart = (int) val[3];
        int dstPos = (int) val[4];
        int segmentLength = (int) val[5];
        int isInverted = (int) val[6];
        int srcEnd = srcStart + segmentLength;

        if (srcEnd < srcStart)
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT,
                    String.format("Range is defined in wrong way (end is bigger than start) start=%d, end=%d", srcStart, srcEnd)
            );
        if (srcStart == 0 || dstPos == 0) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT,
                    String.format("Cannot have src or dst on first node of route start=%d, end=%d, dst=%d", srcStart, srcEnd, dstPos)
            );
        }

        Node[] srcRoute = solution.getRoutes()[srcRouteIdx];
        Node[] dstRoute = solution.getRoutes()[dstRouteIdx];

        Node[] nodes = new Node[srcEnd - srcStart + 1];
        System.arraycopy(srcRoute, srcStart, nodes, 0, nodes.length);
        if (isInverted == INVERT) swap(nodes, 0, nodes.length - 1);

        if (srcRouteIdx != dstRouteIdx) {
            solution.setRoute(srcRouteIdx, remove(srcRoute, srcStart, srcEnd));
            solution.setRoute(dstRouteIdx, addBefore(dstRoute, nodes, dstPos));

            return new Node[][]{srcRoute, dstRoute};
        } else {
            solution.setRoute(srcRouteIdx, moveIntraRoute(srcStart, srcEnd, dstPos, srcRoute, nodes));

            return new Node[][]{srcRoute};
        }
    }

    private static Node[] moveIntraRoute(int srcStart, int srcEnd, int dstPos, Node[] srcRoute, Node[] nodes) {
        Node[] newRoute = new Node[srcRoute.length];

        if (srcStart < dstPos) {
            System.arraycopy(srcRoute, 0, newRoute, 0, srcStart);
            System.arraycopy(srcRoute, srcEnd + 1, newRoute, srcStart, dstPos - srcEnd);
            System.arraycopy(nodes, 0, newRoute, dstPos - ((srcEnd - srcStart) + 1), nodes.length);
            System.arraycopy(srcRoute, dstPos, newRoute, dstPos, srcRoute.length - dstPos);
        } else {
            System.arraycopy(srcRoute, 0, newRoute, 0, dstPos);
            System.arraycopy(srcRoute, dstPos, newRoute, dstPos + (srcEnd - srcStart) + 1, srcStart - dstPos);
            System.arraycopy(nodes, 0, newRoute, dstPos, nodes.length);
            System.arraycopy(srcRoute, srcEnd + 1, newRoute, srcEnd + 1, srcRoute.length - (srcEnd + 1));
        }
        return newRoute;
    }

    /**
     * Inverts the node sequence for a certain route
     * in the range from start to end (both inclusive)
     */
    public static void swap(Solution solution, int routeIdx, int start, int end) {
        Node[] route = solution.getRoutes()[routeIdx];

        int offset = 0;
        while (end - offset > start + offset) {
            Node tmp = route[end - offset];
            route[end - offset] = route[start + offset];
            route[start + offset] = tmp;
            offset++;
        }
    }

    public static void swap(Node[] route, int start, int end) {
        int offset = 0;
        while (end - offset > start + offset) {
            Node tmp = route[end - offset];
            route[end - offset] = route[start + offset];
            route[start + offset] = tmp;
            offset++;
        }
    }

    /**
     * Removes nodes from a given route and returns the
     * cleaned route
     */
    private static Node[] remove(Node[] orig, int srcPosIncl, int dstPosIncl) {
        Node[] arr = new Node[orig.length - ((dstPosIncl - srcPosIncl) + 1)];
        System.arraycopy(orig, 0, arr, 0, srcPosIncl);
        System.arraycopy(orig, dstPosIncl + 1, arr, srcPosIncl, orig.length - dstPosIncl - 1);

        return arr;
    }

    private static Node[] addBefore(Node[] orig, Node[] nodes, int pos) {
        Node[] arr = new Node[orig.length + nodes.length];

        System.arraycopy(orig, 0, arr, 0, pos);
        System.arraycopy(nodes, 0, arr, pos, nodes.length);
        System.arraycopy(orig, pos, arr, pos + nodes.length, orig.length - pos);

        return arr;
    }
}
