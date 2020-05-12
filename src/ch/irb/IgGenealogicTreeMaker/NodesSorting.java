/*Copyright 2020 - Mathilde Foglierini Perez

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   
 * This class is used to sort the clones in the same order than in the alignment software.
 * It will be easier to analyze the alignment output with the BASELine output
 */

package ch.irb.IgGenealogicTreeMaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ch.irb.nodes.Node;

public class NodesSorting {
    static Logger logger = Logger.getLogger(NodesSorting.class);
    private ArrayList<Node> allNodes = new ArrayList<>();
    private ArrayList<Node> isNodeACluster = new ArrayList<>();
    private ArrayList<Node> orderedNodes = new ArrayList<>();
    private Node rootNode;

    public NodesSorting(ArrayList<Node> allNodes) {
        this.allNodes = allNodes;
        setNodesOrder();
    }

    /*
     * This method set the order of the Ig from the top to the bottom
     */
    private void setNodesOrder() {
        for (Node node : allNodes) {
            if (node.isRoot()) {
                rootNode = node;
            }
        }
        // we set the BP or Ig cluster; when all the children are Igs
        for (Node node : allNodes) {
            ArrayList<Node> children = node.getChildren();
            int isChildIg = 0;
            for (Node child : children) {
                if (!child.isBP()) {
                    isChildIg += 1;
                }
            }
            if (isChildIg == children.size()) {
                isNodeACluster.add(node);
            }
        }

        // we start from the UCA
        orderedNodes.add(rootNode);
        setChildrenOrder(rootNode);

    }

    private void setChildrenOrder(Node node) {
        //System.out.println("Set Children for "+node.getNodeId());
        ArrayList<Node> children = node.getChildren();
        TreeMap<Integer, ArrayList<Node>> igChildren = new TreeMap<Integer, ArrayList<Node>>();
        TreeMap<Integer, ArrayList<Node>> bpChildren = new TreeMap<Integer, ArrayList<Node>>();
        for (Node child : children) {
            int mutNumWithParent = child.getNumberOfNucMutationsWithParent();
            //logger.debug("for node "+child.getNodeId()+" the number of mutations with the parent is "+mutNumWithParent);
            Integer key = mutNumWithParent;
            ArrayList<Node> nodes = new ArrayList<Node>();
            if (child.isBP()) {
                if (bpChildren.containsKey(key)) {
                    nodes = bpChildren.get(key);
                }
                nodes.add(child);
                bpChildren.put(key, nodes);
            } else {
                if (igChildren.containsKey(key)) {
                    nodes = igChildren.get(key);
                }
                nodes.add(child);
                igChildren.put(key, nodes);
            }
        }

        // First we put the IG
        Integer[] mutations = new Integer[igChildren.size()];
        igChildren.keySet().toArray(mutations);
        Arrays.sort(mutations); // we sort the mutations numbers
        for (Integer mutation : mutations) {
            ArrayList<Node> nodes = igChildren.get(mutation);
            for (Node child : nodes) {
                if (!orderedNodes.contains(child)) {
                    orderedNodes.add(child);
                }
                setOrderForCluster(child);
            }
        }

        // Then the BP
        mutations = new Integer[bpChildren.size()];
        bpChildren.keySet().toArray(mutations);
        Arrays.sort(mutations); // we sort the mutations numbers
        for (Integer mutation : mutations) {
            ArrayList<Node> nodes = bpChildren.get(mutation);
            for (Node child : nodes) {
                if (!orderedNodes.contains(child)) {
                    orderedNodes.add(child);
                }
                setOrderForCluster(child);
            }
        }

        // Finally we run the method foreach child
        for (Node child : children) {
            setChildrenOrder(child);
        }
    }

    private void setOrderForCluster(Node node) {
        if (isNodeACluster.contains(node)) {
            ArrayList<Node> children = node.getChildren();
            for (Node child : children) {
                if (!orderedNodes.contains(child)) {
                    orderedNodes.add(child);
                }
                setOrderForCluster(child);
            }
        }
    }

    /**
     * @return the orderedNodes
     */
    public ArrayList<Node> getOrderedNodes() {
        return orderedNodes;
    }

}
