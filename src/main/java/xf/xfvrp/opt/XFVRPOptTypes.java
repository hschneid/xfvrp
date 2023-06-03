package xf.xfvrp.opt;

import xf.xfvrp.opt.construct.insert.XFPDPFirstBestInsert;
import xf.xfvrp.opt.construct.insert.XFVRPFirstBestInsert;
import xf.xfvrp.opt.construct.random.RandomizedVRPSolutionBuilder;
import xf.xfvrp.opt.construct.savings.XFVRPConst;
import xf.xfvrp.opt.construct.savings.XFVRPSavings;
import xf.xfvrp.opt.improve.XFVRPNoOpt;
import xf.xfvrp.opt.improve.ils.XFPDPILS;
import xf.xfvrp.opt.improve.ils.XFVRPILS;
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

    public static XFVRPOptType NONE = new XFVRPOptType(XFVRPNoOpt.class);

    // Construction heuristics
    public static XFVRPOptType SAVINGS = new XFVRPOptType(XFVRPSavings.class);
    public static XFVRPOptType CONST = new XFVRPOptType(XFVRPConst.class);
    public static XFVRPOptType FIRST_BEST = new XFVRPOptType(XFVRPFirstBestInsert.class);
    public static XFVRPOptType RANDOM = new XFVRPOptType(RandomizedVRPSolutionBuilder.class);

    // Improvement heuristics - Neighborhood search
    public static XFVRPOptType RELOCATE = new XFVRPOptType(XFVRPSingleMove.class);
    public static XFVRPOptType PATH_RELOCATE = new XFVRPOptType(XFVRPSegmentMove.class);
    public static XFVRPOptType SWAP = new XFVRPOptType(XFVRPSingleSwap.class);
    public static XFVRPOptType SWAPSEGMENT = new XFVRPOptType(XFVRPSegmentSwap.class);
    public static XFVRPOptType PATH_EXCHANGE = new XFVRPOptType(XFVRPSegmentExchange.class);
    public static XFVRPOptType BORDER_PATH_EXCHANGE = new XFVRPOptType(XFVRPBorderSegmentExchange.class);

    // Iterated local search
    public static XFVRPOptType ILS = new XFVRPOptType(XFVRPILS.class);

    // Pickup & Delivery - Improvement heuristics - Neighborhood search
    public static XFVRPOptType PDP_CHEAPEST_INSERT = new XFVRPOptType(XFPDPFirstBestInsert.class);
    public static XFVRPOptType PDP_RELOCATE2 = new XFVRPOptType(XFPDPSingleMove.class);
    public static XFVRPOptType PDP_ILS = new XFVRPOptType(XFPDPILS.class);
}
