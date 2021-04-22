package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.Solution;

public class XFVRPMoveUtil {

    public static final int NO_INVERT = 0;
    public static final int INVERT = 1;

    public static void change(Solution solution, float[] val) throws XFVRPException {
        int srcRouteIdx = (int) val[1];
        int dstRouteIdx = (int) val[2];
        int srcPos = (int) val[3];
        int dstPos = (int) val[4];
        int segmentLength = (int) val[5];
        int isInverted = (int) val[6];

        if(isInverted == INVERT) swap(solution, srcRouteIdx, srcPos, srcPos + segmentLength);
        move(solution, srcRouteIdx, dstRouteIdx, srcPos, srcPos + segmentLength, dstPos);
    }

    public static void reverseChange(Solution solution, float[] val) throws XFVRPException {
        int srcRouteIdx = (int) val[1];
        int dstRouteIdx = (int) val[2];
        int srcPos = (int) val[3];
        int dstPos = (int) val[4];
        int segmentLength = (int) val[5];
        int isInverted = (int) val[6];

        if(srcRouteIdx == dstRouteIdx && dstPos > srcPos)
            move(solution, dstRouteIdx, srcRouteIdx, dstPos - segmentLength - 1, dstPos - 1, srcPos);
        else if(srcRouteIdx == dstRouteIdx && dstPos < srcPos)
            move(solution, dstRouteIdx, srcRouteIdx, dstPos, dstPos + segmentLength, srcPos + segmentLength + 1);
        else
            move(solution, dstRouteIdx, srcRouteIdx, dstPos, dstPos + segmentLength, srcPos);
        if(isInverted == INVERT) swap(solution, srcRouteIdx, srcPos, srcPos + segmentLength);
    }

    /**
     * Moves the nodes in the range from srcStart and srcEnd, both inclusive,
     * before the position dstPos from one route to another route.
     */
    private static void move(Solution solution, int srcRouteIdx, int dstRouteIdx, int srcStart, int srcEnd, int dstPos) throws XFVRPException {
        if(srcEnd < srcStart)
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT,
                    String.format("Range is defined in wrong way (end is bigger than start) start=%d, end=%d", srcStart, srcEnd)
            );
        if(srcStart == 0 || dstPos == 0) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT,
                    String.format("Cannot have src or dst on first node of route start=%d, end=%d, dst=%d", srcStart, srcEnd, dstPos)
            );
        }

        Node[] srcRoute = solution.getRoutes()[srcRouteIdx];
        Node[] dstRoute = solution.getRoutes()[dstRouteIdx];

        Node[] nodes = new Node[srcEnd - srcStart + 1];
        System.arraycopy(srcRoute, srcStart, nodes, 0, nodes.length);

        if(srcRouteIdx != dstRouteIdx) {
            solution.setRoute(srcRouteIdx, remove(srcRoute, srcStart, srcEnd));
            solution.setRoute(dstRouteIdx, addBefore(dstRoute, nodes, dstPos));
        } else {
            if(srcStart < dstPos) {
                System.arraycopy(srcRoute, srcEnd + 1, srcRoute, srcStart, dstPos - srcEnd);
                System.arraycopy(nodes, 0, srcRoute, dstPos - ((srcEnd - srcStart) + 1), nodes.length);
            } else {
                System.arraycopy(srcRoute, dstPos, srcRoute, dstPos + (srcEnd - srcStart) + 1, srcStart - dstPos);
                System.arraycopy(nodes, 0, srcRoute, dstPos, nodes.length);
            }
        }
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

    /**
     * Removes nodes from a given route and returns the
     * cleaned route
     */
    private static Node[] remove(Node[] orig, int srcPosIncl, int dstPosIncl) {
        Node[] arr = new Node[orig.length - ((dstPosIncl - srcPosIncl) + 1)];
        System.arraycopy(orig,0, arr, 0, srcPosIncl);
        System.arraycopy(orig,dstPosIncl + 1, arr, srcPosIncl, orig.length - dstPosIncl - 1);

        return arr;
    }

    private static Node[] addBefore(Node[] orig, Node[] nodes, int pos) {
        Node[] arr = new Node[orig.length + nodes.length];

        System.arraycopy(orig,0, arr, 0, pos);
        System.arraycopy(nodes,0, arr, pos, nodes.length);
        System.arraycopy(orig, pos, arr, pos + nodes.length, orig.length - pos);

        return arr;
    }
}
