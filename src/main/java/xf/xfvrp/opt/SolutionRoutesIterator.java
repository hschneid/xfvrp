package xf.xfvrp.opt;

import java.util.Iterator;

import xf.xfvrp.base.Node;

public class SolutionRoutesIterator implements Iterator<Node[]> {

	private final Node[][] routes;
	private int currentIndex = -1;
	private int length;
	
	public SolutionRoutesIterator(Node[][] routes) {
		this.routes = routes;
		length = routes.length;
	}
	
	@Override
	public boolean hasNext() {
		int index = currentIndex + 1;
		while(index < length && routes[index].length == 0) {
			index++;
		}
		
		return !(index == length);
	}

	@Override
	public Node[] next() {
		currentIndex++;
		
		while(currentIndex < length && routes[currentIndex].length == 0) {
			currentIndex++;
		}
		
		if(currentIndex == length)
			return null;
		
		return routes[currentIndex];
	}
}
