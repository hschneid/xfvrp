package xf.xfvrp.opt.construct;

import xf.xfvrp.base.Node;

/**
 * Copyright (c) 2026 Lars Hackstein
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * A derived task of the VRP with Trailers and Transshipments (VRPTT). Each
 * customer request is decomposed into one or more ways of service (spec §2.1):
 * <ul>
 * <li>{@link Mode#DIRECT}: The customer is visited directly by a lorry
 * (optionally with its coupled trailer).</li>
 * <li>{@link Mode#VIA_TDP}: The load for the customer is transshipped at a
 * transshipment location (TL). The customer is served after the TL visit.</li>
 * </ul>
 *
 * @author hschneid
 */
public class VRPTTTask {

	/**
	 * Way of service of a customer.
	 */
	public enum Mode {
		/**
		 * Customer is visited directly.
		 */
		DIRECT,
		/**
		 * Customer is served via a transshipment location.
		 */
		VIA_TDP
	}

	private final Mode mode;
	private final Node customer;
	private final Node transshipment;

	/**
	 * @param mode          Way of service of the customer
	 * @param customer      The customer to serve
	 * @param transshipment The transshipment location (only for mode VIA_TDP)
	 */
	VRPTTTask(Mode mode, Node customer, Node transshipment) {
		this.mode = mode;
		this.customer = customer;
		this.transshipment = transshipment;
	}

	public Mode getMode() {
		return mode;
	}

	public Node getCustomer() {
		return customer;
	}

	public Node getTransshipment() {
		return transshipment;
	}

	@Override
	public String toString() {
		return mode + (transshipment != null ? "(" + customer + " via " + transshipment + ")" : "(" + customer + ")");
	}
}
