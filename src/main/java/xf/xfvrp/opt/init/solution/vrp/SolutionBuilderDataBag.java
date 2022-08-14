package xf.xfvrp.opt.init.solution.vrp;

import xf.xfvrp.base.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class SolutionBuilderDataBag {

    private final List<Node> validCustomers = new ArrayList<>();
    private final List<Node> validDepots = new ArrayList<>();
    private final List<Node> validReplenish = new ArrayList<>();
    private Set<Integer> knownSequencePositions = new HashSet<>();

    public List<Node> getValidCustomers() {
        return validCustomers;
    }

    public List<Node> getValidDepots() {
        return validDepots;
    }

    public List<Node> getValidReplenish() {
        return validReplenish;
    }

    public void resetKnownSequencePositions() {
        knownSequencePositions.clear();
    }

    public Set<Integer> getKnownSequencePositions() {
        return knownSequencePositions;
    }

    public void setKnownSequencePositions(Set<Integer> knownSequencePositions) {
        this.knownSequencePositions = knownSequencePositions;
    }

}
