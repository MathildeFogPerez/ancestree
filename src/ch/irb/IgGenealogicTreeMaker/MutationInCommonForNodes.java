package ch.irb.IgGenealogicTreeMaker;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;


public class MutationInCommonForNodes {
	static Logger logger = Logger.getLogger(MutationInCommonForNodes.class);
	private Integer position;
	private ArrayList<Node> nodesWithThisMutation = new ArrayList<Node>();
	private ArrayList<Node> processedNodes = new ArrayList<Node>();
	private ArrayList<ArrayList<Node>> listOfListsOfNodes = new ArrayList<ArrayList<Node>>();

	public MutationInCommonForNodes(Integer position) {
		this.position = position;
	}

	public void addTwoNodes(Node node1, Node node2) {
		if (!processedNodes.contains(node1)) {
			processedNodes.add(node1);
			nodesWithThisMutation.add(node1);
		}
		if (!processedNodes.contains(node2)) {
			processedNodes.add(node2);
			nodesWithThisMutation.add(node2);
		}
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.add(node1);
		nodes.add(node2);
		if (!areThisListOfNodesAlreadyStored(nodes)) {
			int index = listOfListsOfNodes.size();
			// logger.debug("List of list of nodes add an array of size " +
			// nodes.size()+" at index "+index);
			listOfListsOfNodes.add(index, nodes);
		}
	}

	public void generateAllPossibleListsOfNode() {
		// ArrayList<String>
		//logger.fatal("-------->FOR MUT "+position.toString());
		for (int i = 0; i < nodesWithThisMutation.size(); i++) {
			Node node1 = nodesWithThisMutation.get(i);
			ArrayList<Node> arrayOne = new ArrayList<Node>();
			arrayOne.add(node1);
			//logger.fatal("We start with Node1 " + node1.getNodeId());
			secondLoop: for (int j = 0; j < nodesWithThisMutation.size(); j++) {
				Node node2 = nodesWithThisMutation.get(j);
				if (node1.getNodeId().equals(node2.getNodeId()))
					continue secondLoop;
				else {
					arrayOne.add(node2);
					//logger.fatal(" we add Node2 " + node2.getNodeId());
					if (arrayOne.size() > 1) {
						//logger.fatal("Size >1, we check the array");
						ArrayList<Node> nodes = new ArrayList<Node>();
						for (int z = 0; z < arrayOne.size(); z++) {
							nodes.add(arrayOne.get(z));
						}
						if (!areThisListOfNodesAlreadyStored(nodes)) {
							int index = listOfListsOfNodes.size();
						//	logger.fatal("List of list of nodes add an array of size " +
						//	 nodes.size()+" at index "+index);
							listOfListsOfNodes.add(index, nodes);
						}
					}
				}
			}
		}

	}

	@SuppressWarnings("rawtypes")
	private boolean areThisListOfNodesAlreadyStored(ArrayList<Node> nodes) {
		ArrayList<Node> nodesTocheck = nodes;
		//logger.debug("Processing an array for ");
		Iterator iterator = listOfListsOfNodes.iterator();
		listLoop: while (iterator.hasNext()) {
			@SuppressWarnings("unchecked")
			ArrayList<Node> array = (ArrayList<Node>) iterator.next();
			int numberOfExistingNodes = 0;
			if (array.size() != nodesTocheck.size()) {
				// logger.debug("Go to the next list loop!!");
				continue listLoop;
			}
			for (int z = 0; z < nodesTocheck.size(); z++) {
				Node nodeToCheck = nodesTocheck.get(z);
				// logger.debug("Node to check " + nodeToCheck.getNodeId());
				for (int j = 0; j < array.size(); j++) {
					Node existingNode = array.get(j);
					// logger.debug("Existing node " + existingNode.getNodeId());
					if (existingNode.equals(nodeToCheck)) {
						// logger.debug("Node to check is the same " + nodeToCheck.getNodeId() + " and existing "
						// + existingNode.getNodeId());
						numberOfExistingNodes++;
					}
					// logger.debug("Number of existing nodes is "+numberOfExistingNodes);
					if (numberOfExistingNodes == nodesTocheck.size()) {
					//	logger.debug("WE already have this array!");
						return true;
					}
				}
			}
		}
		return false;
	}

	public ArrayList<ArrayList<Node>> getListOfListOfNodes() {
		return listOfListsOfNodes;
	}

	public ArrayList<String> getMutationAtThisPosition(ArrayList<Node> listOfNodes) {

		ArrayList<String> mutations = new ArrayList<>();
		for (int j = 0; j < listOfNodes.size(); j++) {
			//logger.warn("loop");
			Node node = listOfNodes.get(j);
			//String mutation = node.getMutationsFromRoot().get(position);
			String mutation = node.getMutationsFromRootAtPosition(position);
			//TODO WRONG to delete this part!!
			/*if (mutation==null) { //case where a reversion was set and then removed because it didnt fit the rules
				//mutation = node.getPutativeReverseMutation(position);
				//if (mutation == null) { //case where a reversion was set and then removed because it didnt fit the rules
					return null;
				//}
			}*/
			String nuc[] = mutation.split("->");
			if (!nuc[0].equals(nuc[1])){
				mutations.add(mutation);
			}
			else{//TODO WRONG 
				return null;
			}
		}
		return mutations;
	}

	public void printInfo() {
		logger.debug("This MutationInCommonForNodes has " + listOfListsOfNodes.size() + " arrays of nodes.");
	}

}
