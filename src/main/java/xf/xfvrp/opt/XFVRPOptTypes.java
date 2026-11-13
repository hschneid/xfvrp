package xf.xfvrp.opt;

import xf.xfvrp.opt.construct.insert.XFPDPFirstBestInsert;
import xf.xfvrp.opt.construct.insert.XFVRPFirstBestInsert;
import xf.xfvrp.opt.construct.random.RandomizedVRPSolutionBuilder;
import xf.xfvrp.opt.construct.savings.XFVRPConst;
import xf.xfvrp.opt.construct.savings.XFVRPSavings;
import xf.xfvrp.opt.construct.VRPTTSolutionBuilder;
import xf.xfvrp.opt.improve.XFVRPNoOpt;
import xf.xfvrp.opt.improve.ils.XFPDPILS;
import xf.xfvrp.opt.improve.ils.XFVRPILS;
import xf.xfvrp.opt.improve.ils.VRPTTILS;
import xf.xfvrp.opt.improve.routebased.move.XFPDPSingleMove;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSegmentMove;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPBorderSegmentExchange;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSegmentExchange;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSegmentSwap;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSingleSwap;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * This list holds types for the user accessible optimization methods.
 *
 * @author hschneid
 */
public class XFVRPOptTypes {

    public static final XFVRPOptType NONE = new XFVRPOptType(XFVRPNoOpt.class);

    // Construction heuristics
    public static final XFVRPOptType SAVINGS = new XFVRPOptType(XFVRPSavings.class);
    public static final XFVRPOptType CONST = new XFVRPOptType(XFVRPConst.class);
    public static final XFVRPOptType FIRST_BEST = new XFVRPOptType(XFVRPFirstBestInsert.class);
    public static final XFVRPOptType RANDOM = new XFVRPOptType(RandomizedVRPSolutionBuilder.class);

    // Improvement heuristics - Neighborhood search
    public static final XFVRPOptType RELOCATE = new XFVRPOptType(XFVRPSingleMove.class);
    public static final XFVRPOptType PATH_RELOCATE = new XFVRPOptType(XFVRPSegmentMove.class);
    public static final XFVRPOptType SWAP = new XFVRPOptType(XFVRPSingleSwap.class);
    public static final XFVRPOptType SWAPSEGMENT = new XFVRPOptType(XFVRPSegmentSwap.class);
    public static final XFVRPOptType PATH_EXCHANGE = new XFVRPOptType(XFVRPSegmentExchange.class);
    public static final XFVRPOptType BORDER_PATH_EXCHANGE = new XFVRPOptType(XFVRPBorderSegmentExchange.class);

    // Iterated local search
    public static final XFVRPOptType ILS = new XFVRPOptType(XFVRPILS.class);

    // Pickup & Delivery - Improvement heuristics - Neighborhood search
    public static final XFVRPOptType PDP_CHEAPEST_INSERT = new XFVRPOptType(XFPDPFirstBestInsert.class);
    public static final XFVRPOptType PDP_RELOCATE2 = new XFVRPOptType(XFPDPSingleMove.class);
    public static final XFVRPOptType PDP_ILS = new XFVRPOptType(XFPDPILS.class);

    // Vehicle Routing Problem with Trailers and Transshipments
    public static final XFVRPOptType VRPTT = new XFVRPOptType(VRPTTSolutionBuilder.class);
    public static final XFVRPOptType VRPTT_ILS = new XFVRPOptType(VRPTTILS.class);
}
