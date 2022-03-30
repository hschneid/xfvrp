package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.Solution;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFPDPMoveUtil {

    public static final int IS_OVERHANG = -1;
    public static final int NO_OVERHANG = 0;

    public static Node[][] change(Solution solution, float[] val) throws XFVRPException {
        int srcRouteIdx = (int) val[1];
        int dstRouteIdx = (int) val[2];
        int srcPickPos = (int) val[3];
        int srcDeliPos = (int) val[4];
        int dstPickPos = (int) val[5];
        int dstDeliPos = (int) val[6];

        return move(
                solution,
                srcRouteIdx,
                dstRouteIdx,
                srcPickPos,
                srcDeliPos,
                dstPickPos,
                dstDeliPos
        );
    }

    private static Node[][] move(Solution solution, int srcRouteIdx, int dstRouteIdx, int srcPickPos, int srcDeliPos, int dstPickPos, int dstDeliPos) {
        if(srcDeliPos < srcPickPos ||dstDeliPos < dstPickPos)
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT,
                    String.format("Delivery is before pickup (%d %d %d %d)", srcPickPos, srcDeliPos, dstPickPos, dstDeliPos)
            );
        if(srcPickPos == 0 || srcDeliPos == 0 || dstPickPos == 0 || dstDeliPos == 0) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT,
                    String.format("Cannot have src or dst on first node of route (%d %d %d %d)", srcPickPos, srcDeliPos, dstPickPos, dstDeliPos)
            );
        }

        Node[] srcRoute = solution.getRoutes()[srcRouteIdx];
        Node[] dstRoute = solution.getRoutes()[dstRouteIdx];

        if(srcRouteIdx != dstRouteIdx) {
            solution.setRoute(srcRouteIdx, remove(srcRoute, srcPickPos, srcDeliPos));
            solution.setRoute(dstRouteIdx, add(dstRoute, srcRoute[srcPickPos], srcRoute[srcDeliPos], dstPickPos, dstDeliPos));

            return new Node[][] {
                    srcRoute,
                    dstRoute
            };
        } else {
            solution.setRoute(srcRouteIdx, moveIntraRoute(srcRoute, srcRoute[srcPickPos], srcRoute[srcDeliPos], dstPickPos, dstDeliPos));

            return new Node[][] {
                    srcRoute,
            };
        }
    }

    /**
     * Removes 2 nodes at posA and posB
     */
    private static Node[] remove(Node[] orig, int posA, int posB) {
        Node[] arr = new Node[orig.length - 2];
        System.arraycopy(orig,0, arr, 0, posA);
        if(posB - posA > 1)
            System.arraycopy(orig,posA + 1, arr, posA, posB - posA - 1);
        System.arraycopy(orig,posB + 1, arr, posB - 1, orig.length - posB - 1);

        return arr;
    }

    private static Node[] add(Node[] orig, Node pickup, Node delivery, int pickupPos, int deliveryPos) {
        Node[] arr = new Node[orig.length + 2];

        System.arraycopy(orig,0, arr, 0, pickupPos);
        arr[pickupPos] = pickup;
        if(deliveryPos - pickupPos > 0)
            System.arraycopy(orig, pickupPos, arr, pickupPos + 1, deliveryPos - pickupPos);
        arr[deliveryPos + 1] = delivery;
        System.arraycopy(orig, deliveryPos, arr, deliveryPos + 2, orig.length - deliveryPos);

        return arr;
    }

    private static Node[] moveIntraRoute(Node[] orig, Node pickup, Node delivery, int pickupPos, int deliveryPos) {
        Node[] arr = new Node[orig.length];

        int pos = 0;
        for (int i = 0; i < orig.length; i++) {
            if(i == pickupPos) {
                arr[pos++] = pickup;
            }
            if(i == deliveryPos) {
                arr[pos++] = delivery;
            }

            if(orig[i] == pickup || orig[i] == delivery) {
                continue;
            }

            arr[pos++] = orig[i];
        }

        return arr;
    }
}
