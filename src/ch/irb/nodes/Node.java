/*
 * Copyright 2020 - Mathilde Foglierini Perez

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   This class stores info for each node, main of the variables and methods were used
   with IgTreeMaker class (not used anymore since 2016, but we keep it in case)
 */

package ch.irb.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;

import ch.irb.IgGenealogicTreeMaker.Ig;


/**
 * The Class Node.
 *
 * @author Mathilde This class inherits from the NodeObject. It will have
 *         features and variables specific for the algo wich creates the tree
 */

public class Node extends NodeObject {

    /**
	 * The logger.
     */
    static Logger logger = Logger.getLogger(Node.class);

    /**
     * The related igs.
     */
    @XmlTransient
    private ArrayList<Ig> relatedIgs = new ArrayList<Ig>();

    /**
     * The parent is set.
     */
    @XmlTransient
    private boolean parentIsSet = false;

    /**
     * The has possible parents.
     */
    @XmlTransient
    private boolean hasPossibleParents = false;

    /**
     * The number of is a possible parent.
     */
    @XmlTransient
    private int numberOfIsAPossibleParent = 0;

    /**
     * The possible parents.
     */
    @XmlTransient
    private ArrayList<Node> possibleParents = new ArrayList<Node>();

    /**
     * The parent.
     */
    @XmlTransient
    public Node parent;

    /**
     * The parent id.
     */
    @XmlElement(name = "parent")
    public String parentId;

    /**
     * The children.
     */
    @XmlTransient
    public ArrayList<Node> children = new ArrayList<Node>();

    /**
     * The mutations from root.
     */
    @XmlTransient
    private TreeMap<Integer, String> mutationsFromRoot = new TreeMap<Integer, String>();

    /**
     * The common mutations for a BP ONLY.
     */
    @XmlTransient
    private TreeMap<Integer, String> mutationsInCommon = new TreeMap<Integer, String>();

    /**
     * The possible reverse.
     */
    @XmlTransient
    private TreeMap<Integer, String> possibleReverse = new TreeMap<Integer, String>();

    /**
     * The reverse mutations from root with a parent.
     */
    @XmlTransient
    private HashMap<Node, TreeMap<Integer, String>> reverseMutationsFromRootWithAParent = new HashMap<>();

    /**
     * The double mutations with a parent.
     */
    @XmlTransient
    private HashMap<Node, TreeMap<Integer, String>> doubleMutationsWithAParent = new HashMap<>();

    /**
     * The reverse number.
     */
    @XmlTransient
    private int reverseNumber = 0;

    /**
     * The double number.
     */
    @XmlTransient
    private int doubleNumber = 0;

    /**
     * Instantiates a new node.
     */
    public Node() {
        super();
    }

    /**
     * Gets the parent.
     *
     * @return the parent
     */
    @XmlTransient
    public Node getParent() {
        return parent;
    }

    /**
     * Sets the parent.
     *
     * @param parent the new parent
     */
    public void setParent(Node parent) {
        this.parent = parent;
        parentId = parent.getNodeId();
    }

    /**
     * Gets the related igs.
     *
     * @return the related igs
     */
    @XmlTransient
    public ArrayList<Ig> getRelatedIgs() {
        return relatedIgs;
    }

    /**
     * Gets the number of igs.
     *
     * @return the number of igs
     */
    @XmlTransient
    public int getNumberOfIgs() {
        return 1;
    }

    /**
     * Sets the related igs.
     *
     * @param relatedIgs the new related igs
     */
    public void setRelatedIgs(ArrayList<Ig> relatedIgs) {
        this.relatedIgs = relatedIgs;
    }

    /**
     * Adds the possible parent.
     *
     * @param parent the parent
     */
    public void addPossibleParent(Node parent) {
        possibleParents.add(parent);
        hasPossibleParents = true;
    }

    /**
     * Parent set.
     */
    public void parentSet() {
        this.parentIsSet = true;
    }

    /**
     * Checks if is parent set.
     *
     * @return true, if is parent set
     */
    public boolean isParentSet() {
        return parentIsSet;
    }

    /**
     * Checks for possible parents.
     *
     * @return true, if successful
     */
    public boolean hasPossibleParents() {
        return hasPossibleParents;
    }

    /**
     * Gets the children.
     *
     * @return the children
     */
    @XmlTransient
    public ArrayList<Node> getChildren() {
        return children;
    }

    /**
     * Gets the children for xml file.
     *
     * @return the children for xml file
     */
    @XmlElementWrapper(name = "children", required = false)
    @XmlElement(name = "node", required = true)
    public ArrayList<Node> getChildrenForXmlFile() {
		if (children.size() == 0) {
			return null;
		}
        return children;
    }

    /**
     * Sets the children for xml file.
     *
     * @param children the new children for xml file
     */
    public void setChildrenForXmlFile(ArrayList<Node> children) {
        this.children = children;
    }

    /**
     * Sets the children.
     *
     * @param children the new children
     */
    public void setChildren(ArrayList<Node> children) {
        this.children = children;
    }

    /**
     * Adds the child.
     *
     * @param child the child
     */
    public void addChild(Node child) {
        this.children.add(child);
    }

    /**
     * Removes the child.
     *
     * @param child the child
     */
    public void removeChild(Node child) {
        // logger.warn("Remove kid "+child.getNodeId()+" for parent
        // "+getNodeId());
        ArrayList<Node> kids = children;
        Node childToRemove = null;
        for (Node kid : kids) {
            if (kid.getNodeId().equals(child.getNodeId())) {
                childToRemove = kid;
            }
        }
        if (childToRemove == null) {
            logger.fatal("Problem here!! We can not remove " + child.getNodeId() + " with parent " + this.getNodeId());
            System.exit(1);
        }
        this.children.remove(childToRemove);
    }

    /**
     * Removes the all children.
     */
    public void removeAllChildren() {
        this.children = new ArrayList<>();
    }

    /**
     * Gets the mutations from root.
     *
     * @return the mutations from root
     */
    @XmlTransient
    public TreeMap<Integer, String> getMutationsFromRoot() {
        return mutationsFromRoot;
    }

    /**
     * @return the mutationsInCommon
     */
    @XmlTransient
    public TreeMap<Integer, String> getMutationsInCommon() {
        return mutationsInCommon;
    }

    /**
     * @param mutationsInCommon the mutationsInCommon to set
     */
    public void setMutationsInCommon(TreeMap<Integer, String> mutationsInCommon) {
        this.mutationsInCommon = mutationsInCommon;
    }

    // This method is used when we create the ancestor, and check that the
    // mutations will not be triple one..etc..

    /**
     * Gets the mutations from root at position.
     *
     * @param position the position
     * @return the mutations from root at position
     */
    public String getMutationsFromRootAtPosition(Integer position) {
        String mutation = mutationsFromRoot.get(position);
        // logger.debug("Mutation is " + mutation + " for node " + getNodeId() +
        // " at position " + position.toString());
        if (mutation == null && parentIsSet) {
            // logger.debug("Mutation is null for node "+getNodeId()+" at
            // position "+position.toString());
            if (reverseMutationsFromRootWithAParent.containsKey(parent)) {
                // logger.debug("get reverse for this parent...");
                if (reverseMutationsFromRootWithAParent.get(parent).containsKey(position)) {
                    mutation = (String) reverseMutationsFromRootWithAParent.get(parent).get(position);
                }
            }
        }
        if (mutation == null) {
            char nuc = getSequence().charAt(position.intValue());
            mutation = nuc + "->" + nuc;
        }
        return mutation;
    }

    /**
     * Gets the nucleotide at position.
     *
     * @param position the position
     * @return the nucleotide at position
     */
    public char getNucleotideAtPosition(Integer position) {
        return getSequence().charAt(position.intValue());
    }

    /**
     * Sets the mutations from root.
     *
     * @param mutationsFromRoot the mutations from root
     */
    public void setMutationsFromRoot(TreeMap<Integer, String> mutationsFromRoot) {
        this.mutationsFromRoot = mutationsFromRoot;
    }

    /**
     * Gets the reverse mutations from root.
     *
     * @param parent the parent
     * @return the reverse mutations from root
     */
    public TreeMap<Integer, String> getReverseMutationsFromRoot(Node parent) {
        return reverseMutationsFromRootWithAParent.get(parent);
    }

    /**
     * Sets the reverse mutations from root.
     *
     * @param parent                   the parent
     * @param reverseMutationsFromRoot the reverse mutations from root
     */
    public void setReverseMutationsFromRoot(Node parent, TreeMap<Integer, String> reverseMutationsFromRoot) {
        reverseMutationsFromRootWithAParent.put(parent, reverseMutationsFromRoot);
        for (Map.Entry<Integer, String> entry : reverseMutationsFromRoot.entrySet()) {
            logger.debug("For node " + getNodeId() + " with parent " + parent.getNodeId() + " we have reverse "
                    + entry.getKey().toString() + ": " + entry.getValue());
        }
    }

    /**
     * Adds the a reverse mutation from root.
     *
     * @param position    the position
     * @param revMutation the rev mutation
     */
    public void addAReverseMutationFromRoot(Integer position, String revMutation) {
        TreeMap<Integer, String> revMutFromRootWithAParent = new TreeMap<Integer, String>();
        if (reverseMutationsFromRootWithAParent.containsKey(parent)) {
            revMutFromRootWithAParent = reverseMutationsFromRootWithAParent.get(parent);
        }
        revMutFromRootWithAParent.put(position, revMutation);
        reverseMutationsFromRootWithAParent.put((Node) parent, revMutFromRootWithAParent);
    }

    /**
     * Removes the reverse mutation from root.
     *
     * @param position the position
     */
    public void removeReverseMutationFromRoot(Integer position) {
        TreeMap<Integer, String> revMutFromRootWithAParent = reverseMutationsFromRootWithAParent.get(parent);
        if (revMutFromRootWithAParent != null) {
            if (revMutFromRootWithAParent.containsKey(position)) {
                revMutFromRootWithAParent.remove(position);
                reverseMutationsFromRootWithAParent.put((Node) parent, revMutFromRootWithAParent);
            }
        }
    }

    /**
     * Gets the putative reverse mutation.
     *
     * @param position the position
     * @return the putative reverse mutation
     */
    public String getPutativeReverseMutation(Integer position) {
        TreeMap<Integer, String> revMutFromRootWithAParent = reverseMutationsFromRootWithAParent.get(parent);
        return revMutFromRootWithAParent.get(position);
    }

    /**
     * Sets the definitively the reverse mutations from root.
     *
     * @param position    the position
     * @param revMutation the rev mutation
     */
    public void setDefinitivelyTheReverseMutationsFromRoot(Integer position, String revMutation) {
        String reverseInformation = getReverseInformation();
		if (reverseInformation == null) {
			reverseInformation = "";
		}
        logger.debug("IN NODE " + nodeId + ", we store definitively the reverse mutation at position " + position + ": "
                + revMutation);
        mutationsFromRoot.put(position, revMutation);
        reverseInformation += " // Reverse at position " + position + ": " + revMutation;
        this.hasReversion = true;
        reverseNumber += 1;
        setReverseInformation(reverseInformation);
    }

    /**
     * Sets the definitively the reverse mutations from root.
     *
     * @param parent the new definitively the reverse mutations from root
     */
    public void setDefinitivelyTheReverseMutationsFromRoot(Node parent) {
        TreeMap<Integer, String> newMutationsFromRoot = new TreeMap<Integer, String>();
        for (Map.Entry<Integer, String> entry : mutationsFromRoot.entrySet()) {
            newMutationsFromRoot.put(entry.getKey(), entry.getValue());
        }
        TreeMap<Integer, String> reverseMutationsFromRoot = reverseMutationsFromRootWithAParent.get(parent);
        String reverseInformation = getReverseInformation();
		if (reverseInformation == null) {
			reverseInformation = "";
		}
        for (Map.Entry<Integer, String> entry : reverseMutationsFromRoot.entrySet()) {
            logger.debug("IN NODE " + nodeId + ", we store definitively the reverse mutation at position "
                    + entry.getKey().toString() + ": " + entry.getValue());
            newMutationsFromRoot.put(entry.getKey(), entry.getValue());
            reverseInformation += " // Reverse at position " + entry.getKey().toString() + ": " + entry.getValue();
            reverseNumber += 1;
            this.hasReversion = true;
        }
        setReverseInformation(reverseInformation);
        this.setMutationsFromRoot(newMutationsFromRoot);
    }

    /**
     * Sets the double mutations for a parent.
     *
     * @param parent         the parent
     * @param doubleMutation the double mutation
     */
    public void setDoubleMutationsForAParent(Node parent, TreeMap<Integer, String> doubleMutation) {
        doubleMutationsWithAParent.put(parent, doubleMutation);
    }

    /**
     * Checks for reversion.
     *
     * @return true, if successful
     */
    public boolean hasReversion() {
        return hasReversion;
    }

    /**
     * Gets the double mutations for a parent.
     *
     * @param parent the parent
     * @return the double mutations for a parent
     */
    public TreeMap<Integer, String> getDoubleMutationsForAParent(Node parent) {
        return doubleMutationsWithAParent.get(parent);
    }

    /**
     * Sets the a double mutation.
     *
     * @param position       the position
     * @param doubleMutation the double mutation
     */
    public void setADoubleMutation(Integer position, String doubleMutation) {
        // this should happen only once
        if (!mutationsFromRoot.get(position).equals(doubleMutation)) {
            // doubleMutations.put(position, doubleMutation);
            String doubleMutationInformation = getDoubleMutationInformation();
			if (doubleMutationInformation == null) {
				doubleMutationInformation = "";
			}
            logger.debug("IN NODE " + nodeId + ", we store definitively the double mutation at position "
                    + position.toString() + ": " + doubleMutation);
            mutationsFromRoot.put(position, doubleMutation);
            doubleMutationInformation += " // Double Mutation at position " + position.toString() + ": "
                    + doubleMutation;
            doubleNumber += 1;
            setDoubleMutationInformation(doubleMutationInformation);
        }
    }

    /**
     * Removes the double mutation set single mutation.
     *
     * @param position       the position
     * @param singleMutation the single mutation
     */
    public void removeDoubleMutationSetSingleMutation(Integer position, String singleMutation) {
        mutationsFromRoot.put(position, singleMutation);
        String doubleMutationInformation = getDoubleMutationInformation();
        if (doubleMutationInformation != null) {
            if (doubleMutationInformation.matches(".* // Double Mutation at position " + position + ": \\w->\\w.*")) {
                String[] splitted = doubleMutationInformation
                        .split(" // Double Mutation at position " + position + ": \\w->\\w");
                doubleMutationInformation = "";
                if (splitted.length > 0) {
					if (splitted[0] != null) {
						doubleMutationInformation = splitted[0];
					}
					if (splitted.length > 1) {
						if (splitted[1] != null) {
							doubleMutationInformation += splitted[1];
						}
					}
                }
                doubleNumber -= 1;
                setDoubleMutationInformation(doubleMutationInformation);
            }
        }
    }

    /**
     * Sets the double mutations.
     *
     * @param doubleMutations the double mutations
     */
    public void setDoubleMutations(TreeMap<Integer, String> doubleMutations) {
        String doubleMutationInformation = getDoubleMutationInformation();
		if (doubleMutationInformation == null) {
			doubleMutationInformation = "";
		}
        for (Map.Entry<Integer, String> entry : doubleMutations.entrySet()) {
            logger.debug("IN NODE " + nodeId + ", we store the double mutation at position " + entry.getKey().toString()
                    + ": " + entry.getValue());
            mutationsFromRoot.put(entry.getKey(), entry.getValue());
            doubleMutationInformation += " // Double Mutation at position " + entry.getKey().toString() + ": "
                    + entry.getValue();
            doubleNumber += 1;
        }
        setDoubleMutationInformation(doubleMutationInformation);
    }

    /**
     * Gets the possible reverse.
     *
     * @return the possible reverse
     */
    @XmlTransient
    public TreeMap<Integer, String> getPossibleReverse() {
        return possibleReverse;
    }

    /**
     * Sets the possible reverse.
     *
     * @param possibleReverse the possible reverse
     */
    public void setPossibleReverse(TreeMap<Integer, String> possibleReverse) {
        this.possibleReverse = possibleReverse;
    }

    /**
     * Gets the possible parents.
     *
     * @return the possible parents
     */
    public ArrayList<Node> getPossibleParents() {
        return possibleParents;
    }

    /**
     * Removes the possible parent.
     *
     * @param parent the parent
     */
    public void removePossibleParent(Node parent) {
        possibleParents.remove(parent);
    }

    /**
     * Gets the number of is a possible parent.
     *
     * @return the number of is a possible parent
     */
    public int getNumberOfIsAPossibleParent() {
        return numberOfIsAPossibleParent;
    }

    /**
     * Gets the reverse number.
     *
     * @return the reverseNumber
     */
    public int getReverseNumber() {
        return reverseNumber;
    }

    /**
     * Gets the double number.
     *
     * @return the doubleNumber
     */
    public int getDoubleNumber() {
        return doubleNumber;
    }

    /**
     * Sets the a possible parent.
     */
    public void setAPossibleParent() {
        this.numberOfIsAPossibleParent += 1;
    }

    /**
     * Sets the information for tree. This method is VERY important, because it
     * will generate all the latest information needed for the XML file.
     */
    public void setInformationForTree() {
        //logger.debug("Setting info for node " + nodeId + " is root " + isRoot());
        String cellInformation = "";
        String immunizationInformation = "";
        for (int i = 0; i < relatedIgs.size(); i++) {
            String igId = relatedIgs.get(i).getName();
			if (relatedIgs.get(i).getCellInfo() != null) {
				cellInformation += "// " + igId + " " + relatedIgs.get(i).getCellInfo();
			}
			if (relatedIgs.get(i).getImmunizationInfo() != null) {
				immunizationInformation += "// " + igId + " " + relatedIgs.get(i).getImmunizationInfo();
			}
        }

		if (cellInformation.length() > 0) {
			setCellInfo(cellInformation);
		}
		if (immunizationInformation.length() > 0) {
			setImmunizationInfo(immunizationInformation);
		}

        if (parent != null) {
            String mutationsWithParent = ",";
            String parentSequence = parent.getSequence();
            //logger.debug("For Node "+getNodeId()+" we have the parent sequence of parent "+parent.getNodeId());
            //logger.debug(parentSequence);
            char[] parentSeq = parentSequence.toCharArray();
            char[] seq = sequence.toCharArray();
            int numberOfMutations = 0;
            for (int z = 0; z < parentSequence.length(); z++) {
                String nuc1 = Character.toString(parentSeq[z]);
                String nuc2 = Character.toString(seq[z]);
                if (!nuc1.equals(nuc2)) {
                    numberOfMutations++;
                    String position = Integer.toString(z);
                    mutationsWithParent += position + ":" + nuc1 + "->" + nuc2 + ",";
                }
            }
            // logger.warn("Mut with parent "+mutationsWithParent);
            if (mutationsWithParent.length() > 1) {
                setMutationsWithParent(mutationsWithParent);
            }
            setNumberOfNucMutationsWithParent(numberOfMutations);
            // we translate compare the protein sequences
            String parentProtSequence = parent.getProteinSequence();
            setNumberOfAAMutationsWithParent(compareTwoProteinSequences(proteinSequence, parentProtSequence));
        }
    }

    /**
     * Prints the info about the node.
     */
    public void printInfoAboutTheNode() {
        logger.debug("\n\nNode Id: " + nodeId + " is root " + isRoot + " sequence " + sequence);
        logger.debug("Protein sequence: " + proteinSequence);
        for (int i = 0; i < relatedIgs.size(); i++) {
            logger.debug("Related ig is : " + relatedIgs.get(i).getName());
        }
        logger.debug("His level is " + level);
		if (parent != null)// in the case of the root node it doesnt have parent
		{
			logger.debug("His parent is " + parent.getNodeId());
		}
        if (hasPossibleParents) {
            logger.debug("It has possible parents: ");
            for (int i = 0; i < possibleParents.size(); i++) {
                logger.debug("  " + possibleParents.get(i).getNodeId());
            }
        }
        if (children.size() > 0) {
            logger.debug("It has children: ");
            for (int i = 0; i < children.size(); i++) {
                logger.debug("----> " + children.get(i).getNodeId());
            }
        } else {
            logger.debug("It has no children!");
        }
        logger.debug("Number of mutation from root for this node is: " + this.mutationsFromRoot.size());
        logger.debug("Number of nucleotidic mutations from his parent is: " + numberOfNucMutationsWithParent);
        logger.debug("Number of AA mutations from his parent is: " + numberOfAAMutationsWithParent);
		if (reverseInformation != null) {
			logger.debug(reverseInformation);
		}
		if (doubleMutationInformation != null) {
			logger.debug(doubleMutationInformation);
		}

    }
}
