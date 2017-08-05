package xf.xfvrp.opt.init;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

public class PresetSolutionBuilder {

	private static Logger LOG = LoggerFactory.getLogger(PresetSolutionBuilder.class);
	
	/**
	 * 
	 * @param nodes
	 * @param model
	 * @return
	 */
	public Solution build(List<Node> nodes, XFVRPModel model) {
		String predefinedSolutionString = model.getParameter().getPredefinedSolutionString();

		if(!checkPredefinedSolutionString(predefinedSolutionString))
			throw new IllegalStateException("The predefined solution string "+predefinedSolutionString+" is not valid.");

		PresetSolutionBuilderDataBag dataBag = prepare(nodes, model);

		// Separate the solution string into the blocks
		List<Node> giantRoute = new ArrayList<>();
		for (String block : split(predefinedSolutionString)) {
			readBlock(block, giantRoute, dataBag);
		}

		// Put the unassigned customers with single routes in the giant route
		addUnassignedNodes(dataBag, giantRoute);

		Solution solution = new Solution();
		solution.setGiantRoute(giantRoute.toArray(new Node[0]));
		return solution;
	}

	private PresetSolutionBuilderDataBag prepare(List<Node> nodes, XFVRPModel model) {
		PresetSolutionBuilderDataBag dataBag = new PresetSolutionBuilderDataBag();
		
		dataBag.setModel(model);
		dataBag.setNodes(nodes);
		
		// Generate a map for each extern id to a node index
		IntStream.range(0, nodes.size()).forEach(i -> dataBag.addNodeId(nodes.get(i), i));

		dataBag.setAvailableCustomers(new HashSet<>(nodes.subList(model.getNbrOfDepots() + model.getNbrOfReplenish(), nodes.size())));
		return dataBag;
	}

	private void addUnassignedNodes(PresetSolutionBuilderDataBag dataBag, List<Node> giantRoute) {
		for (Node customer : dataBag.getAvailableCustomers()) {
			giantRoute.add(dataBag.getNextDepot());
			giantRoute.add(customer);
		}
		giantRoute.add(dataBag.getNextDepot());
	}

	private String[] split(String predefinedSolutionString) {
		predefinedSolutionString = predefinedSolutionString.substring(1, predefinedSolutionString.length() - 1);
		String[] predefinedBlocks = predefinedSolutionString.split("\\),\\(");
		predefinedBlocks[0] = predefinedBlocks[0].substring(1);
		predefinedBlocks[predefinedBlocks.length - 1] = predefinedBlocks[predefinedBlocks.length - 1].substring(0, predefinedBlocks[predefinedBlocks.length - 1].length() - 1);
		return predefinedBlocks;
	}

	private void readBlock(String block, List<Node> giantRoute, PresetSolutionBuilderDataBag dataBag) {
		String[] entries = block.split(",");

		if(entries.length == 0)
			return;
		
		// Every block has to start with a any depot
		giantRoute.add(dataBag.getNextDepot());

		// A block can hold customers and depots
		for (int i = 0; i < entries.length; i++) {
			if(dataBag.containsNode(entries[i])) {
				addEntry(giantRoute, dataBag, entries, i);
			} else {
				LOG.warn(" Init warning - Node "+entries[i]+" is no valid customer (unknown).");
			}
		}

		// Every block has to end with a depot
		giantRoute.add(dataBag.getNextDepot());
	}

	private void addEntry(List<Node> giantRoute, PresetSolutionBuilderDataBag dataBag, String[] entries, int i) {
		Node n = dataBag.getNode(entries[i]);
		
		if(n.getSiteType() == SiteType.DEPOT) {
			giantRoute.add(dataBag.getNextDepot(n));
		} else if (dataBag.getAvailableCustomers().contains(n)) {
			giantRoute.add(n);
			dataBag.getAvailableCustomers().remove(n);
		} else {
			LOG.warn(" Init warning - Node "+entries[i]+" is already in the solution.");
		}
	}

	/**
	 * 
	 * @param predefinedSolutionString
	 * @return
	 */
	private boolean checkPredefinedSolutionString(String predefinedSolutionString) {
		return predefinedSolutionString.matches("\\{(\\([^\\(\\)]+\\),)*(\\([^\\(\\)]+\\))+\\}");
	}
}
