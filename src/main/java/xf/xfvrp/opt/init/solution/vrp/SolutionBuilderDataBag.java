package xf.xfvrp.opt.init.solution.vrp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xf.xfvrp.base.Node;

public class SolutionBuilderDataBag {

	private List<Node> validNodes = new ArrayList<>();
	private List<Node> validDepots = new ArrayList<>();
	private List<Node> validReplenish = new ArrayList<>();
	private Set<Integer> knownSequencePositions = new HashSet<>();
	
	public List<Node> getValidNodes() {
		return validNodes;
	}
	public void setValidNodes(List<Node> validNodes) {
		this.validNodes = validNodes;
	}
	public List<Node> getValidDepots() {
		return validDepots;
	}
	public void setValidDepots(List<Node> validDepots) {
		this.validDepots = validDepots;
	}
	public List<Node> getValidReplenish() {
		return validReplenish;
	}
	public void setValidReplenish(List<Node> validReplenish) {
		this.validReplenish = validReplenish;
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
