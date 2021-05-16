package xf.xfvrp.opt;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.construct.XFVRPConst;
import xf.xfvrp.opt.construct.XFVRPSavings;
import xf.xfvrp.opt.construct.insert.XFPDPFirstBestInsert;
import xf.xfvrp.opt.construct.insert.XFVRPFirstBestInsert;
import xf.xfvrp.opt.improve.XFPDPRelocate;
import xf.xfvrp.opt.improve.giantroute.XFVRP2Opt;
import xf.xfvrp.opt.improve.giantroute.XFVRP2OptIntra;
import xf.xfvrp.opt.improve.giantroute.XFVRP3Opt;
import xf.xfvrp.opt.improve.giantroute.XFVRP3PointMove;
import xf.xfvrp.opt.improve.ils.XFPDPILS;
import xf.xfvrp.opt.improve.ils.XFVRPILS;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSegmentMove;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSegmentExchange;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSegmentSwap;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSingleSwap;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This enumeration holds all for the user accessible optimization
 * methods.
 * 
 * @author hschneid
 *
 */
public enum XFVRPOptType {

	// Construction
	SAVINGS(XFVRPSavings.class),
	CONST(XFVRPConst.class),
	FIRST_BEST(XFVRPFirstBestInsert.class),

	// Giant route based
	OPT2(XFVRP2Opt.class),
	OPT2_INTRA(XFVRP2OptIntra.class),
	OPT3(XFVRP3Opt.class),
	OPT3_POINTMOVE(XFVRP3PointMove.class),

	// Route based
	RELOCATE(XFVRPSingleMove.class),
	PATH_RELOCATE(XFVRPSegmentMove.class),
	SWAP(XFVRPSingleSwap.class),
	SWAPSEGMENT(XFVRPSegmentSwap.class),
	PATH_EXCHANGE(XFVRPSegmentExchange.class),

	// Iterated local search
	ILS(XFVRPILS.class),

	// Pickup & Delivery
	PDP_CHEAPEST_INSERT(XFPDPFirstBestInsert.class),
	PDP_RELOCATE(XFPDPRelocate.class),
	PDP_ILS(XFPDPILS.class);
	
	private Class<? extends XFVRPOptBase> clazz; 
	
	/**
	 * 
	 * @param clazz
	 */
	private XFVRPOptType(Class<? extends XFVRPOptBase> clazz) {
		this.clazz = clazz;
	}
	
	/**
	 * Creates an instance of the chosen opt type class in clazz.
	 * 
	 * @return An object instance
	 */
	public XFVRPOptBase createInstance() throws XFVRPException {
		try {
			return (XFVRPOptBase) Class.forName(clazz.getName()).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "no copy of optimization procedure possible", e);
		}
	}
}
