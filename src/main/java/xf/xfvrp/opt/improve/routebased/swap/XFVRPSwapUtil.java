package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPSwapUtil {

    public static final int NO_INVERT = 0;
    public static final int A_INVERT = 1;
    public static final int B_INVERT = 2;
    public static final int BOTH_INVERT = 3;

    /**
     * Changes a solution according to given parameter
     */
    public static void change(Solution solution, float[] val) throws XFVRPException {
        int aRouteIndex = (int) val[1];
        int bRouteIndex = (int) val[2];
        int aPos = (int) val[3];
        int bPos = (int) val[4];
        int aSegmentLength = (int) val[5];
        int bSegmentLength = (int) val[6];
        int invertType = (int) val[7];

        invert(solution, aRouteIndex, bRouteIndex, aPos, bPos, aSegmentLength, bSegmentLength, invertType);
        exchange(solution, aRouteIndex, bRouteIndex, aPos, bPos, aSegmentLength, bSegmentLength);
    }

    public static void reverseChange(Solution solution, float[] val) throws XFVRPException {
        int aRouteIndex = (int) val[1];
        int bRouteIndex = (int) val[2];
        int aPos = (int) val[3];
        int bPos = (int) val[4];
        int aSegmentLength = (int) val[5];
        int bSegmentLength = (int) val[6];
        int invertType = (int) val[7];

        // Swap segment parameter, that A is always before B
        if(aRouteIndex == bRouteIndex && bPos < aPos) {
            int tmp = aPos; aPos = bPos; bPos = tmp;
            tmp = aSegmentLength; aSegmentLength = bSegmentLength; bSegmentLength = tmp;

            if(invertType == A_INVERT) invertType = B_INVERT;
            else if(invertType == B_INVERT) invertType = A_INVERT;
        }

        if(aRouteIndex != bRouteIndex) {
            // Just change segment lengths
            exchange(solution, aRouteIndex, bRouteIndex, aPos, bPos, bSegmentLength, aSegmentLength);
        } else {
            int newBPos = bPos - (aSegmentLength - bSegmentLength);
            exchange(solution, aRouteIndex, bRouteIndex, aPos, newBPos, bSegmentLength, aSegmentLength);
        }
        invert(solution, aRouteIndex, bRouteIndex, aPos, bPos, aSegmentLength, bSegmentLength, invertType);
    }

    /**
     * Exchanges two segments of the giant tour. First segment
     * starts at position a and includes la many nodes. Second segments
     * starts at position b and includes lb many nodes. The two
     * segments must not overlap each other.
     */
    private static void exchange(
            Solution solution,
            int aRouteIndex,
            int bRouteIndex,
            int aPos,
            int bPos,
            int aSegmentLength,
            int bSegmentLength
    ) throws XFVRPException {
        if(aRouteIndex == bRouteIndex &&
                (((aPos < bPos) && (aPos + aSegmentLength) >= bPos) ||
                ((bPos < aPos) && (bPos + bSegmentLength) >= aPos))
        ) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT, "Segments are overlapping");
        }

        Node[][] routes = solution.getRoutes();

        // Segments must not touch a depot
        if(aPos == 0 || bPos == 0) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT, "Segments contain the leading depot");
        }
        if(aPos + aSegmentLength == routes[aRouteIndex].length - 1 ||
                bPos + bSegmentLength == routes[bRouteIndex].length - 1) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT, "Segments contain the trailing depot");
        }

        // Segments have same size, easy because lengths are not changing
        if(aSegmentLength == bSegmentLength) {
            swapSegmentsEqualLength(solution, aRouteIndex, bRouteIndex, aPos, bPos, aSegmentLength);
        } else {
            // Swap segment parameter, that A is always before B
            if(aRouteIndex == bRouteIndex && bPos < aPos) {
                int tmp = aPos; aPos = bPos; bPos = tmp;
                tmp = aSegmentLength; aSegmentLength = bSegmentLength; bSegmentLength = tmp;
            }

            // Fetch the segments
            Node[] aSegment = new Node[aSegmentLength + 1];
            System.arraycopy(routes[aRouteIndex], aPos , aSegment, 0, aSegment.length);
            Node[] bSegment = new Node[bSegmentLength + 1];
            System.arraycopy(routes[bRouteIndex], bPos , bSegment, 0, bSegment.length);

            if(aRouteIndex != bRouteIndex) {
                routes[aRouteIndex] = replace(routes[aRouteIndex], aPos, aPos + aSegmentLength, bSegment);
                routes[bRouteIndex] = replace(routes[bRouteIndex], bPos, bPos + bSegmentLength, aSegment);
            } else {
                Node[] intermediates = new Node[bPos - (aPos + aSegmentLength + 1)];
                System.arraycopy(routes[aRouteIndex], aPos + aSegmentLength + 1, intermediates, 0, intermediates.length);

                System.arraycopy(bSegment, 0, routes[aRouteIndex], aPos, bSegment.length);
                System.arraycopy(intermediates, 0, routes[aRouteIndex], aPos + bSegment.length, intermediates.length);
                System.arraycopy(aSegment, 0, routes[aRouteIndex], aPos + bSegment.length + intermediates.length, aSegment.length);
            }
        }
    }

    /**
     * Removes the segment between start and end and inserts newSegment
     */
    private static Node[] replace(Node[] route, int start, int end, Node[] newSegment) {
        Node[] newRoute = new Node[route.length + (newSegment.length - (end - start + 1))];
        System.arraycopy(route, 0, newRoute, 0, start);
        System.arraycopy(newSegment, 0, newRoute, start, newSegment.length);
        System.arraycopy(route, end + 1, newRoute, start + newSegment.length, route.length - end - 1);

        return newRoute;
    }

    private static void swapSegmentsEqualLength(Solution solution, int aRouteIndex, int bRouteIndex, int aPos, int bPos, int aSegmentLength) {
        Node[] aRoute = solution.getRoutes()[aRouteIndex];
        Node[] bRoute = solution.getRoutes()[bRouteIndex];
        for (int i = 0; i <= aSegmentLength; i++) {
            Node tmp = aRoute[aPos + i];
            aRoute[aPos + i] = bRoute[bPos + i];
            bRoute[bPos + i] = tmp;
        }
    }

    private static void invert(Solution solution,
                               int aRouteIndex,
                               int bRouteIndex,
                               int aPos,
                               int bPos,
                               int aSegmentLength,
                               int bSegmentLength,
                               int invertTye) {
        switch (invertTye) {
            case A_INVERT: {
                XFVRPMoveUtil.swap(solution, aRouteIndex, aPos, aPos + aSegmentLength);
                break;
            }
            case B_INVERT: {
                XFVRPMoveUtil.swap(solution, bRouteIndex, bPos, bPos + bSegmentLength);
                break;
            }
            case BOTH_INVERT: {
                XFVRPMoveUtil.swap(solution, aRouteIndex, aPos, aPos + aSegmentLength);
                XFVRPMoveUtil.swap(solution, bRouteIndex, bPos, bPos + bSegmentLength);
                break;
            }
            default:
                // NO_INVERT
                break;
        }
    }
}
