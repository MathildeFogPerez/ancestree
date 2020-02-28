package ch.irb.IgGenealogicTreeMaker;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;


/**
 * @author Mathilde This class will store information about an ancestor such as its descendants, its mutations with its
 *         parent and with the root. Finally it will calculate the score related to it: score = numberMutations +
 *         numberOfDescendants + averageDistanceBetweenDescendants ; we add 1/x where x is the number of additional
 *         mutations for each node if x>3
 */
public class Ancestor {
	static Logger logger = Logger.getLogger(Ancestor.class);
	private int numberOfCommonMutations = 0;
	private int numberOfDescendants = 0;
	private ArrayList<Node> descendants = new ArrayList<Node>();
	private float averageDistanceFromRootForNonDirectDescendants = 0;
	private float averageDistanceBetweenDescendants = 0;
	private float score = 0;
	private String sequence;
	private TreeMap<Integer, String> mutationsFromRoot = new TreeMap<Integer, String>();
	private TreeMap<Integer, String> commonMutations = new TreeMap<Integer, String>();
	private boolean isPriority = false; // case where we want an ancestor for ALL the children

	public int getNumberOfCommonMutations() {
		return numberOfCommonMutations;
	}

	public void setNumberOfCommonMutations() {
		this.numberOfCommonMutations = commonMutations.size();
	}

	public int getNumberOfDescendants() {
		return numberOfDescendants;
	}

	public void setNumberOfDescendants() { 
		numberOfDescendants = descendants.size();
	}

	public ArrayList<Node> getDescendants() {
		return descendants;
	}

	public void setDescendants(ArrayList<Node> descendants) {
		this.descendants = descendants;
	}

	public void addDescendant(Node descendant) {
		this.descendants.add(descendant);
	}

	public float getAverageDistanceFromRootForNonDirectDescendants() {
		return averageDistanceFromRootForNonDirectDescendants;
	}

	public void setAverageDistanceFromRootForNonDirectDescendants(float averageDistanceFromRootForNonDirectDescendants) {
		this.averageDistanceFromRootForNonDirectDescendants = averageDistanceFromRootForNonDirectDescendants;
	}

	public float getAverageDistanceBetweenDescendants() {
		return averageDistanceBetweenDescendants;
	}

	public void setAverageDistanceBetweenDescendants(float averageDistanceBetweenDescendants) {
		this.averageDistanceBetweenDescendants = averageDistanceBetweenDescendants;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public void setMutationsFromRoot(TreeMap<Integer, String> parentMutationFromRoot) {
		TreeMap<Integer, String> newMutationsFromRoot = new TreeMap<Integer, String>();
		for (Entry<Integer, String> entry : parentMutationFromRoot.entrySet()) {
			newMutationsFromRoot.put(entry.getKey(), entry.getValue());
		}
		for (Entry<Integer, String> entry : commonMutations.entrySet()) {
			newMutationsFromRoot.put(entry.getKey(), entry.getValue());
		}
		this.mutationsFromRoot = newMutationsFromRoot;
	}

	public void addMutation(String mutation) {
		String[] mut = mutation.split(":");
		Integer position = Integer.parseInt(mut[0]);
		String muta = mut[1];
		commonMutations.put(position, muta);
	}

	// this method is used when we keep the best 5 double mutations and 2 reverse
	public void setCommonMutation(TreeMap<Integer, String> comMutations) {
		commonMutations.clear();
		commonMutations = comMutations;
	}

	public TreeMap<Integer, String> getCommonMutations() {
		return commonMutations;
	}

	public TreeMap<Integer, String> getMutationsFromRoot() {
		return mutationsFromRoot;
	}

	public void setPriority() {
		isPriority = true;
	}


	public void calculateScore() {
		int numberMutations = numberOfCommonMutations;

		// score = numberOfCommonMutations + numberOfDescendants + averageDistanceFromRootForDescendants +
		// averageDistanceBetweenDescendants;
		double numbMut = numberMutations;
		//if (numberMutations>10)
		//	numbMut=Math.pow(numberMutations, 2);
		score = (float) ((numbMut) + numberOfDescendants + averageDistanceBetweenDescendants
				+ averageDistanceFromRootForNonDirectDescendants);
		//logger.fatal("--------before SCORE IS "+score);
		//logger.fatal("--------before numbMu "+numbMut);
		//logger.fatal("--------before numberOfDescendants "+numberOfDescendants);
		//logger.fatal("--------before averageDistanceBetweenDescendants "+averageDistanceBetweenDescendants);
		//logger.fatal("--------before averageDistanceFromRootForNonDirectDescendants "+averageDistanceFromRootForNonDirectDescendants);
		// we add 1/x where x is the number of additional mutations for each node if x>3
		for (int i = 0; i < descendants.size(); i++) {
			Node desc = descendants.get(i);
			int numberOfMutForADescendant = desc.getMutationsFromRoot().size();
			float x = numberOfMutForADescendant - numberMutations;
			if (x > 3) {
				score += 1 / x;
			}
		}
		if (isPriority)
			score *= 10000;
		logger.debug("          SCORE is: " + score);
	}
	
	public void printDetails(){
		logger.warn("Number of common mutations " + numberOfCommonMutations + " and number of descendants "
				+ numberOfDescendants + " and average distance between descendants "
				+ averageDistanceBetweenDescendants + " and av dist from root between non direct desc is "
				+ averageDistanceFromRootForNonDirectDescendants);
	}
}
