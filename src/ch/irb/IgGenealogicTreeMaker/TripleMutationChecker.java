package ch.irb.IgGenealogicTreeMaker;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;


/**
 * @author Mathilde 
 * This class is used to avoid triple Mutation We dont want to have triple mutation in 2 different
 * cases: 
 * 1) when we set the possible parental relationship between 2 nodes. If this relationship generates a
 *    triple mutation in the child we get rid of her. 
 * 2) When we create a new ancestor common for some children.
 *    We check all the common mutation, if one of them lead to a triple mutation in the descendance (until the
 *    bottom of the tree), we dont store it 
 */

public class TripleMutationChecker {
	static Logger logger = Logger.getLogger(TripleMutationChecker.class);
	@SuppressWarnings("unused")
	private Node rootNode;

	public TripleMutationChecker(Node rootNode) {
		this.rootNode = rootNode;
	}

	/**
	 * Checks if is a triple mutation considered in a relationship child/parent
	 * 
	 * @param node
	 *            the node
	 * @param parent
	 *            the parent
	 * @param doubleMut
	 *            the double mut
	 * @return true, if is a triple mutation considered
	 */

	public boolean isATripleMutationConsidered(Node node, Node parent, TreeMap<Integer, String> doubleMut,
			TreeMap<Integer, String> revMut, TreeMap<Integer,String> mutationsFromRoot) {
		Node initialNode = node;
		Node initialParent = parent;
		TreeMap<Integer, String> allSpecialMutations = new TreeMap<Integer, String>();
		if (revMut != null) {
			allSpecialMutations.putAll(revMut);
		}
		if (doubleMut != null) {
			allSpecialMutations.putAll(doubleMut);
		}
		for (Map.Entry<Integer, String> entry : mutationsFromRoot.entrySet()){
			Integer position = entry.getKey();
			if (!allSpecialMutations.containsKey(position)){
				allSpecialMutations.put(position,entry.getValue());
			}
		}

		for (Map.Entry<Integer, String> entry : allSpecialMutations.entrySet()) {
			int numberOfMutation = 0;
			Integer position = entry.getKey();
			char initialNucleotide = node.getNucleotideAtPosition(position);
			char nucleotide = initialNucleotide;
			// logger.warn("In node " + initialNode.getNodeId() + " process position " + position.toString()
			// + " with nucleotide " + nucleotide);
			ArrayList<Node> processedNodes = new ArrayList<Node>(); // this is use in the case of a loop between nodes, when none is linked to the GL
			processedNodes.add(node);
			processedNodes.add(parent);
			while (!node.isRoot()) {
				//logger.warn("Parent is "+parent.getNodeId());
				numberOfMutation += isTheSameMutation(position, nucleotide, parent);
				//logger.warn("Numb of mut is "+numberOfMutation );
				node = parent;
				//logger.warn("Node is " + node.getNodeId());
				parent = node.getParent();
				if (processedNodes.contains(parent)){
					logger.warn("Problem with "+initialNode.getNodeId()+" we have a loop!!");
					break;
				}
				else
					processedNodes.add(parent);
				nucleotide = node.getNucleotideAtPosition(position);
			}
			node = initialNode;
			parent = initialParent;
			// logger.warn("For node " + initialNode.getNodeId() + " at position " + position + " we have " +
			// numberOfMutation
			// + " mutations from the root");
			if (numberOfMutation > 2) { // we have a triple mutation, we dont want to consider this parental
										// relationship
				//logger.warn("!!!!!!! Triple mutation, In node " + initialNode.getNodeId() + " at position " + position
				// + " we have " + numberOfMutation + " mutations from the root");
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if this mutation will be a triple one in the sequence of this ancestor 
	 * 
	 * @param parent
	 *            the parent
	 * @param position
	 *            the position
	 * @param mutation
	 *            the mutation
	 * @param children
	 *            the children
	 * @return true, if is this mutation will be a triple one
	 */

	public boolean isThisMutationWillBeATripleOne(Node parent, Integer position, String mutation,
			ArrayList<Node> children) {
		Node node = new Node();
		int numberOfMutation = 0;
		char nucleotide = mutation.split("->")[1].charAt(0);
		char initialNucleotide = nucleotide;

		// if (position.toString().equals("281"))
		// logger.warn("At position 281, mutation is " + mutation + " with nucleotide " + initialNucleotide);
		while (!node.isRoot()) {
			// if (position.toString().equals("84"))
			// logger.warn("Up the tree we have " + isTheSameMutation(position, nucleotide, parent) + " with node "
			// + parent.getNodeId());
			numberOfMutation += isTheSameMutation(position, nucleotide, parent);
			node = parent;
			parent = node.getParent();
			nucleotide = node.getNucleotideAtPosition(position);
		}

		// if (position.toString().equals("281")) {
		// logger.warn("Up the tree we have, number of mutations " + numberOfMutation);
		// }

		// we check also with the children
		for (int i = 0; i < children.size(); i++) {
			Node child = children.get(i);
			// if (position.toString().equals("281"))
			// logger.warn("Down the tree we have " + isTheSameMutation(position, initialNucleotide, child)
			// + " with child " + child.getNodeId());
			int mutWithChildren = isTheSameMutation(position, initialNucleotide, child) + numberOfMutation;
			char nucleotideToconsider = child.getNucleotideAtPosition(position);
			if (mutWithChildren > 2) { // we have a triple mutation, we dont want to consider this parental
				// relationship
				//logger.warn("!!!!!!! Triple mutation, In node " + firstNodeId + " at position " + position
				//		+ " we have " + mutWithChildren + " mutations from the root with its new children!!");
				return true;
			} else if (child.getChildren().size() > 0) { // it has children, check if there is not triple mutation there
				if (isThisMutationWillBeATripleOneInTheDescendants(child, position, nucleotideToconsider,
						child.getChildren(), mutWithChildren))
					return true;
			}
		}
		return false;

	}

	public boolean isThisMutationWillBeATripleOneInTheDescendants(Node node, Integer position, char nucleotide,
			ArrayList<Node> children, int mutationNumberUpTheTree) {
		// if (position.toString().equals("281"))
		// logger.warn("Looking into the children..., up the tree we have " + mutationNumberUpTheTree);
		// we count down the tree
		for (int i = 0; i < children.size(); i++) {
			Node child = children.get(i);
			int mutWithChildren = isTheSameMutation(position, nucleotide, child) + mutationNumberUpTheTree;
			// if (position.toString().equals("281"))
			// logger.warn("Down the tree we have " + isTheSameMutation(position, nucleotide, child) + " for child "
			// + child.getNodeId() + ", total number is " + mutWithChildren);
			char nucleotideToconsider = child.getNucleotideAtPosition(position);
			if (mutWithChildren > 2) { // we have a triple mutation, we dont want to consider this parental
				// relationship
				//logger.warn("!!!!!!! Triple mutation, In node " + node.getNodeId() + " at position " + position
				//		+ " we have " + mutWithChildren + " mutations from the root with its new children");
				return true;
			} else if (child.getChildren().size() > 0) { // it has children, check if there is not triple mutation there
				if (isThisMutationWillBeATripleOneInTheDescendants(child, position, nucleotideToconsider,
						child.getChildren(), mutWithChildren))
					return true;
			}
		}
		return false;
	}

	/**
	 * Checks if is the same mutation.
	 * 
	 * @param position
	 *            the position
	 * @param nucleotide
	 *            the nucleotide
	 * @param node
	 *            the node
	 * @return the int
	 */

	private int isTheSameMutation(Integer position, char nuc, Node node) { // return 0 if it is the same, 1 if it
																			// is different
		String nucToCompare = String.valueOf(nuc);
		String otherNuc = String.valueOf(node.getNucleotideAtPosition(position));
		if (!nucToCompare.equals(otherNuc))
			return 1;
		else
			return 0;
	}

}
