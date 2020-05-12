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
 */

package ch.irb.nodes;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * @author Mathilde This class is a NodeGraph object specific to the GUI. It will share some information with the Node
 *         object from the IgTreeMaker such as the mutations with its parent, but it will have also some specific
 *         features used for the GUI such as the coordinates, the shape, the color etc.... A NodeGraph can have
 *         duplicated nodes (nodes with the same sequence), there will always be a "top" node among duplicated nodes
 *         which will have all the information related to the relationship to its parents etc.. The others will only be
 *         displayed in the paintComponent() method.
 */
public class NodeGraph extends NodeObject {
    static Logger logger = Logger.getLogger(NodeGraph.class);
    private ArrayList<NodeGraph> children = new ArrayList<NodeGraph>();
    private NodeGraph parent;
    private String shape = "circle";
    private int width;
    private int height;
    private Color color = Color.white;
    private boolean isCentered = false;
    private boolean underLined = false; // now underlined is dashed
    private int xPosition = 1; // Used to calculate the x Coordinates
    private boolean hasToBeInTheMiddle = false;
    private int xPositionForTheParent = 1;
    private float xCoord = 0;
    private float yCoord = 0;
    private boolean coordinatesProcessed = false;
    private boolean isALastNode = false;
    private Rectangle2D rectangle2d = new Rectangle2D.Float();
    private String immunizationInfoToDisplay = null;
    private Integer year = 0;
    private boolean isADuplicateNode = false;
    private boolean hasDuplicateNodes = false;
    private ArrayList<NodeGraph> duplicatedNodes = new ArrayList<NodeGraph>();
    private NodeGraph parentOfDuplicate;
    private int reads = 1; //used for the NGS data

    public NodeGraph() {
    }

    /**
     * @return the children
     */
    public ArrayList<NodeGraph> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(ArrayList<NodeGraph> children) {
        this.children = children;
    }

    /**
     * @return the shape
     */
    public String getShape() {
        return shape;
    }

    /**
     * @param shape the shape to set
     */
    public void setShape(String shape) {
        this.shape = shape;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    public void setCentered(boolean isCentered) {
        this.isCentered = isCentered;
    }

    public boolean isCentered() {
        return isCentered;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the xCoord
     */
    public float getXCoord() {
        return xCoord;
    }

    /**
     * @param xCoord the xCoord to set
     */
    private void setXCoord(float xCoord) {
        this.xCoord = xCoord;
    }

    /**
     * @return the yCoord
     */
    public float getYCoord() {
        return yCoord;
    }

    /**
     * @param yCoord the yCoord to set
     */
    private void setYCoord(float yCoord) {
        this.yCoord = yCoord;
    }

    /**
     * @return the isALastNode
     */
    public boolean isALastNode() {
        return isALastNode;
    }

    /**
     * @param isALastNode the isALastNode to set
     */
    public void setALastNode(boolean isALastNode) {
        this.isALastNode = isALastNode;
    }

    // set the shape and the color of the nodeGraph object, if B cell: circle, PC: rectangle
    // an one color foreach year
    public void processGraphicOptions() { // Set the color and shape
        if (isRoot) {
            color = new Color(161, 250, 80); // the GL will always be green
        }

        if (getCellInfo() != null) {
            if (getCellInfo().matches("// " + nodeId + " B")) {
                shape = "circle";
                immunizationInfoToDisplay = "B cell ";
            } else if (getCellInfo().matches("// " + nodeId + " PC")) {
                shape = "rectangle";
                immunizationInfoToDisplay = "Plasma cell ";
            }
        }
        if (getImmunizationInfo() != null) {
            immunizationInfoToDisplay += ", " + getImmunizationInfo();
            if (immunizationInfoToDisplay.matches(".*post.*")) {
                underLined = false;
            } else if (immunizationInfoToDisplay.matches(".*pre.*")) {
                underLined = true;
            }
        } else if (getNodeId().matches(".*BP.*")) {
            shape = "hexagon";
        }

    }

    // This position represents the position of a LAST node among all the other last nodes
    public void setXPosition(int xPosition) {
        // //logger.debug("For node " + getNodeId() + " we have the position " + xPosition);
        this.xPosition = xPosition;
    }

    public int getXPosition() {
        return xPosition;
    }

    /**
     * @return the hasToBeInTheMiddle
     */
    public boolean hasToBeInTheMiddle() {
        return hasToBeInTheMiddle;
    }

    public void setHasToBeInTheMiddle() {
        if (!parent.isRoot) {
            int numberChildrenWithThisParent = parent.getParent().getChildren().size();
            if (numberChildrenWithThisParent == 1) {
                ////logger.debug("This node " + getNodeId() + " has to be in the middle");
                hasToBeInTheMiddle = true;
            } else {
                parent.setHasToBeInTheMiddle();
            }
        } else {
            this.hasToBeInTheMiddle = true;
            //logger.debug("This node " + getNodeId() + " has to be in the middle");
        }
    }

    /**
     * @return the xPositionForTheParent
     */
    public int getxPositionForTheParent() {
        return xPositionForTheParent;
    }

    /**
     * @param xPositionForTheParent the xPositionForTheParent to set
     */
    // this is the position among the child, it starts by 1
    public void setxPositionForTheParent(int xPositionForTheParent) {
        this.xPositionForTheParent = xPositionForTheParent;
    }

    /**
     * @return the coordinatesProcessed
     */
    public boolean isCoordinatesProcessed() {
        return coordinatesProcessed;
    }

    public NodeGraph getParent() {
        return parent;
    }

    public void setParent(NodeGraph parent) {
        this.parent = parent;
    }

    /**
     * @param coordinatesProcessed the coordinatesProcessed to set
     */
    public void setCoordinatesProcessed(boolean coordinatesProcessed) {
        this.coordinatesProcessed = coordinatesProcessed;
    }

    /**
     * This method will calculate the X coordinate of the node
     *
     * @param lastNodesNumber
     * @param totalWidth
     * @param widthOfNodeShape
     */
    public void processXCoordinates(float lastNodesNumber, float totalWidth, float widthOfNodeShape) {
        // //logger.debug("Last nodes number is " + lastNodesNumber + ", and total width " + totalWidth);
        // First way to get the x coordinates, if this node belongs to the last nodes at the bottom of the tree
        if (isALastNode()) {
            float x = ((float) getXPosition() / (lastNodesNumber + 1)) * totalWidth - (widthOfNodeShape / 2 + 5);
            setXCoord(x);
            setCoordinatesProcessed(true);
            if (hasDuplicateNodes) {
                for (int i = 0; i < duplicatedNodes.size(); i++) {
                    NodeGraph node = duplicatedNodes.get(i);
                    node.setXCoord(x);
                    node.setCoordinatesProcessed(true);
                }
            }
        }
        // Second way to get the x coordinates, if the node is up the tree
        else {
            float childXMax = 0;
            float childXMini = totalWidth;
            for (int i = 0; i < children.size(); i++) {
                NodeGraph child = children.get(i);
                // //logger.debug("Processing child "+child.getNodeId()+" with x position "+child.getXPosition());
                if (child.getXCoord() > childXMax) { // position of the 'last' child of this node
                    childXMax = child.getXCoord();
                    // //logger.debug("CHILD x max "+childXMax);
                }
                if (child.getXCoord() < childXMini) { // position of the first child of the node
                    childXMini = child.getXCoord();
                    // //logger.debug("CHILD x mini "+childXMini);
                }
            }
            float x = ((childXMax - childXMini) / 2) + childXMini;
            setXCoord(x);
            setCoordinatesProcessed(true);
            if (hasDuplicateNodes) {
                for (int i = 0; i < duplicatedNodes.size(); i++) {
                    NodeGraph node = duplicatedNodes.get(i);
                    node.setXCoord(x);
                    node.setCoordinatesProcessed(true);
                }
            }
            // //logger.debug("For Node " + nodeId + " which is in the tree, we have x coordinate: " + x);
        }
    }

    /**
     * This method will calculate the Y coordinates of the node
     *
     * @param yDistanceWithParent
     * @param heightNodeShape
     */
    public void processYcoordinates(float yDistanceWithParent, int heightNodeShape) {
        // FIRST we set the height of the node
        // if the node has an EC50 value, it will be higher
        int height = heightNodeShape;
        if (getEC50() != null || (getComment1() != null && isShowComment1())) {
            height *= 1.8;
        }
        setHeight(height);
        // //logger.debug("For node "+getNodeId()+" height is "+getHeight());
        //logger.debug("For node GRAPH "+getNodeId());//+" we have parent "+getParent().getNodeId());
        float y = 40f;
        if (yDistanceWithParent == 0) // it is the UCA
        {
            setYCoord(y);
        } else {
            y = getParent().getYCoord() + getParent().getHeight() + yDistanceWithParent;
            if (getParent().hasDuplicateNodes()) {
                ArrayList<NodeGraph> dup = getParent().getDuplicatedNodes();
                for (NodeGraph du : dup) {
                    int h = du.getHeight();
                    y += (h + 2);
                }
            }
            setYCoord(y);
        }
        if (hasDuplicateNodes) {
            float yForDuplicated = y;
            for (int i = 0; i < duplicatedNodes.size(); i++) {
                NodeGraph node = duplicatedNodes.get(i);
                // We set the height of the duplicated nodes
                int dupHeight = heightNodeShape;
                if (node.getEC50() != null) {
                    dupHeight *= 2;
                }
                node.setHeight(dupHeight);

                if (i == 0) { // we take the height of the first node
                    yForDuplicated += (height + 2);
                } else {
                    yForDuplicated += (duplicatedNodes.get(i - 1).getHeight()) + 2;
                }
                node.setYCoord(yForDuplicated);
                // //logger.debug("For node " + node.getNodeId() + " we set y to " + yForDuplicated);
            }
        }
    }

    public void setRectangle2D(int widthOfShapeNode) {
        rectangle2d = new Rectangle2D.Float(getXCoord(), getYCoord(), widthOfShapeNode, height); // 70 30
        // setRectangle2D(x,y+32) for the duplicateNodes
        if (hasDuplicateNodes()) {
            float y = getYCoord();
            for (int i = 0; i < duplicatedNodes.size(); i++) {
                NodeGraph node = duplicatedNodes.get(i);
                if (i == 0) { // we take the height of the first node
                    y += (height + 2);
                } else {
                    y += (duplicatedNodes.get(i - 1).getHeight()) + 2;
                }
                node.setRectangle2D(getXCoord(), y, widthOfShapeNode);
            }
        }
    }

    private void setRectangle2D(float x, float y, float widthOfShapeNode) {
        rectangle2d = new Rectangle2D.Float(x, y, widthOfShapeNode, height);
    }

    public boolean containsPoint2D(Point2D point2d) {
        if (rectangle2d.contains(point2d)) {
            return true;
        } else {
            return false;
        }
    }

    public String getMutationTextToDisplay() {
        int nucMut = getNumberOfNucMutationsWithParent();
        int aaMut = getNumberOfAAMutationsWithParent();
        String mutations = String.valueOf(nucMut);
        if (isDNA) {
            mutations += " (" + aaMut + ")";
        }
        int insertionNumber = getInsertionNumber();
        int deletionNumber = getDeletionNumber();
        if (insertionNumber > 0) {
            nucMut -= insertionNumber;
            int insertionAA = insertionNumber / 3;
            aaMut -= insertionAA;
            mutations = nucMut + " +" + insertionNumber;
            if (isDNA) {
                mutations += " (" + aaMut + " +" + insertionAA + ")";
            }
        } else if (deletionNumber > 0) {
            nucMut -= deletionNumber;
            int deletionAA = deletionNumber / 3;
            aaMut -= deletionAA;
            mutations = nucMut + " +\u0394" + deletionNumber;
            if (isDNA) {
                mutations += " (" + aaMut + " +\u0394" + deletionAA + ")";
            }
        }
        return mutations;
    }

    /**
     * @return the immunizationInfoToDisplay
     */
    public String getImmunizationInfoToDisplay() {
        String immuToDisplay = null;
        if (immunizationInfoToDisplay != null) {
            immuToDisplay = immunizationInfoToDisplay.replaceAll("// " + getNodeId() + " ", "");
        }
        return immuToDisplay;
    }

    /**
     * @return the underLined
     */
    public boolean isUnderLined() {
        return underLined;
    }

    /**
     * @return the year
     */
    public Integer getYear() {
        return year;
    }

    /**
     * @param year the year to set
     */
    public void setYear(Integer year) {
        this.year = year;
    }

    public void setReads(int reads) {
        this.reads = reads;
    }

    public int getReads() {
        return reads;
    }

    /**
     * @return the isADuplicateNode
     */
    public boolean isADuplicateNode() {
        return isADuplicateNode;
    }

    /**
     * @param isADuplicateNode the isADuplicateNode to set
     */
    public void setADuplicateNode(boolean isADuplicateNode, NodeGraph parent) {
        this.isADuplicateNode = isADuplicateNode;
        this.parentOfDuplicate = parent;
    }

    /**
     * @return the hasDuplicateNodes
     */
    public boolean hasDuplicateNodes() {
        return hasDuplicateNodes;
    }

    /**
     * @param hasDuplicateNodes the hasDuplicateNodes to set
     */
    public void setHasDuplicateNodes(boolean hasDuplicateNodes) {
        this.hasDuplicateNodes = hasDuplicateNodes;
    }

    public void setDuplicatedNodes(String nodeIds, String rootNodeId, int i, HashMap<String, String> nodeIdToEC50,
                                   HashMap<String, String> nodeIdToComment1, HashMap<String, String> nodeIdToComment2) {
        // This method is used for Silvia trees
        setHasDuplicateNodes(true);
        String[] splitted = nodeIds.split(", ");
        for (String nodeId : splitted) {
            if (!nodeId.equals(getNodeId()) && !nodeId.equals(rootNodeId)) {
                ////logger.debug("Add dup " + nodeId);
                NodeGraph node = new NodeGraph();
                node.setDNA(isDNA());
                node.setNodeId(nodeId);
                node.setADuplicateNode(true, this);
                node.setSequence(getSequence());
                node.setProteinSequence();
                if (nodeIdToEC50.containsKey(nodeId)) {
                    node.setEC50(nodeIdToEC50.get(nodeId));
                }
                if (nodeIdToComment1.containsKey(nodeId)) {
                    node.setComment1(nodeIdToComment1.get(nodeId));
                }
                if (nodeIdToComment2.containsKey(nodeId)) {
                    node.setComment2(nodeIdToComment2.get(nodeId));
                }
                node.processGraphicOptions();
                duplicatedNodes.add(node);
            }
        }
    }

    public void setDuplicatedNodes(String cellInfo, String immuInfo, HashMap<String, String> nodeIdToEC50,
                                   HashMap<String, String> nodeIdToComment1, HashMap<String, String> nodeIdToComment2) {
        setHasDuplicateNodes(true);
        // logger.warn("We have duplicate nodes for " + getNodeId());
        String[] cellInfos = cellInfo.split("// ");
        int index = 0;
        HashMap<String, NodeGraph> idToNodeGraph = new HashMap<String, NodeGraph>();
        for (String info : cellInfos) {
            if (index > 0) {
                String[] splitted = info.split("\\s");
                String nodeId = splitted[0];
                NodeGraph node = new NodeGraph();
                node.setNodeId(nodeId);
                node.setDNA(isDNA());
                node.setADuplicateNode(true, this);
                node.setSequence(getSequence());
                node.setProteinSequence();
                node.setCellInfo("// " + info);
                idToNodeGraph.put(nodeId, node);
            }
            index++;
        }
        for (String nodeId : idToNodeGraph.keySet()) {
            if (nodeIdToEC50.containsKey(nodeId)) {
                NodeGraph nodeGraph = idToNodeGraph.get(nodeId);
                nodeGraph.setEC50(nodeIdToEC50.get(nodeId));
            }
            if (nodeIdToComment1.containsKey(nodeId)) {
                NodeGraph nodeGraph = idToNodeGraph.get(nodeId);
                nodeGraph.setComment1(nodeIdToComment1.get(nodeId));
            }
            if (nodeIdToComment2.containsKey(nodeId)) {
                NodeGraph nodeGraph = idToNodeGraph.get(nodeId);
                nodeGraph.setComment2(nodeIdToComment2.get(nodeId));
            }
        }
        index = 0;
        String[] immuInfos = immuInfo.split("// ");
        for (String info : immuInfos) {
            if (index > 0) {
                String[] splitted = info.split("\\s");
                String nodeId = splitted[0];
                NodeGraph node = idToNodeGraph.get(nodeId);
                node.setImmunizationInfo("// " + info);
                node.processGraphicOptions();
                duplicatedNodes.add(node);
            }
            index++;
        }
    }

    public ArrayList<NodeGraph> getDuplicatedNodes() {
        return duplicatedNodes;
    }

    public NodeGraph getDuplicatedNodeWithId(String id) {
        for (int i = 0; i < duplicatedNodes.size(); i++) {
            if (duplicatedNodes.get(i).getNodeId().equals(id)) {
                return duplicatedNodes.get(i);
            }
        }
        return null;
    }

    /**
     * @return the parentOfDuplicate
     */
    public NodeGraph getParentOfDuplicate() {
        return parentOfDuplicate;
    }

    public void setYearForDuplicatedNode(String id, Integer year) {
        for (int i = 0; i < duplicatedNodes.size(); i++) {
            NodeGraph dupNode = duplicatedNodes.get(i);
            if (dupNode.getNodeId().equals(id)) {
                dupNode.setYear(year);
            }
        }
    }

    public void printInfoAboutTheNode() {
        //logger.debug("Node Id: " + nodeId + " is root " + isRoot + " sequence " + sequence);
        //logger.debug("Protein sequence: " + proteinSequence);
        //logger.debug("His level is " + level);
        if (children.size() > 0) {
            //logger.debug("It has children: ");
            for (int i = 0; i < children.size(); i++) {
                //logger.debug("----> " + children.get(i).getNodeId());
            }
        } else {
            //logger.debug("It has no children!");
        }
        //logger.debug("Number of nucleotidic mutations from his parent is: " + numberOfNucMutationsWithParent);
        //logger.debug("Number of AA mutations from his parent is: " + numberOfAAMutationsWithParent);
        if (reverseInformation != null) {
            //logger.debug(reverseInformation);
        }
        if (doubleMutationInformation != null) {
            //logger.debug(doubleMutationInformation);
        }
        //logger.debug("\n\n");
    }
}
