package xf.xfvrp.opt;

import xf.xfvrp.base.Node;

import java.util.Iterator;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class SolutionRoutesIterator implements Iterator<Node[]> {

    private final Node[][] routes;
    private final int length;
    private int currentIndex = -1;

    public SolutionRoutesIterator(Node[][] routes) {
        this.routes = routes;
        length = routes.length;
    }

    @Override
    public boolean hasNext() {
        int index = currentIndex + 1;
        while (index < length && routes[index] != null && routes[index].length == 0) {
            index++;
        }

        return !(index == length);
    }

    @Override
    public Node[] next() {
        currentIndex++;

        while (currentIndex < length && routes[currentIndex] != null && routes[currentIndex].length == 0) {
            currentIndex++;
        }

        if (currentIndex == length)
            return null;

        return routes[currentIndex];
    }
}
