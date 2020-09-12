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
 *
 *  This is the biggest class (panel) of AncesTree GUI, it will draw a tree (made from a dnaml output file)
 *  and allows the user to interact with it.
 */

package ch.irb.IgGenealogicTreeViewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import ch.irb.currentDirectory.GetSetCurrentDirectory;
import ch.irb.nodes.NodeGraph;

/**
 * @author Mathilde This is the main panel of Ancestree GUI. This panel will
 *         display the tree: the nodes, the lines which link them and the number
 *         of mutations between them. The user can get the sequences (nuc and
 *         AA) for each node, and it can have information about the mutations.
 */

@SuppressWarnings("serial")
public class IgTreePanel extends JPanel {
    private IgTreePanel igTreePanel = this;
    static Logger logger = Logger.getLogger(IgTreePanel.class);
    static PValueCodeColor pValueCodeColor = new PValueCodeColor();
    static Color yellowColor = new Color(255, 255, 102);
    static Color pinkColor = new Color(255, 204, 204);
    static Color intenseRed = new Color(255, 0, 0);
    static Color lightRed = new Color(244, 188, 189);
    static Color intenseGreen = new Color(0, 153, 0);
    static Color lightGreen = new Color(183, 227, 199);
    static Color lightGrey = new Color(176, 176, 176);
    static Color darkGrey = new Color(53, 53, 53);
    private boolean everyThingIsGrey = false;
    private int widthOfNodeShape = 116;
    private int heightOfNodeShape = 50;
    private int fontSizeNode = 11;//20
    private int fontSizeMutationsNumber = 20;
    private boolean smaller = false;
    private File xmlFile;
    private ArrayList<NodeGraph> allNodeGraphs = new ArrayList<NodeGraph>();
    private String rootNodeSequence;
    private NodeGraph rootNode;
    private SetImgtInfo setImgtInfo = null;
    private TreeMap<String, MutationsGraph> allMutations = new TreeMap<String, MutationsGraph>();
    private boolean fadingON = true;
    private CLIPdata clipData;
    private ClipParser clipParser;
    private ArrayList<String> igInOrder;
    private Rectangle2D clipRectangle = new Rectangle2D.Float();
    private boolean isDNA = true;
    private boolean isNewDataSaved = false;
    private boolean isImgtFormatLoaded = false;
    private TreeMap<Integer, ArrayList<NodeGraph>> fromLevelToNodes = new TreeMap<Integer, ArrayList<NodeGraph>>();
    private int lastLevel;
    private int minLevelForLastNode = 0;
    private float totalWidth = 300;
    private float totalHeight = 500;
    private int lastNodesNumber = 0;
    private float miniNumberOfMutations = 0;
    private ColorByYear colorByYear;
    private GetSetCurrentDirectory getSetCurrentDir;
    final static float dash1[] = {5.0f};
    final static BasicStroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5.0f, dash1,
            0.0f);
    final static BasicStroke plain = new BasicStroke();
    private boolean hasImmuInfo = false;
    private double scale = 1;
    private double zoomInc = 0.05;
    private NodeGraph centerNode = null;
    private boolean hasReadsInId = false;
    private ArrayList<Color> ec50Colors = new ArrayList<Color>(Arrays.asList(new Color(204,0,0),
            new Color(0,76,153),
            new Color(204,102,0),
            new Color(0,102,0),
            new Color(76,0,153)));

    public IgTreePanel(IgTreeReader igTreeReader, GetSetCurrentDirectory getSetCurrentDir) {
        //logger.debug("IgTreePanel...");
        this.setBackground(Color.white);
        this.getSetCurrentDir = getSetCurrentDir;
        // Here we get the path of the project the user loaded
        setXmlFile(new File(igTreeReader.getXmlFilePath()));
        isDNA = igTreeReader.isDNA();
        setHasImmuInfo(igTreeReader.hasImmuInfo());

        this.addMouseListener(new MyMouseListener(this));
        setAllNodeGraphs(igTreeReader.getAllNodeGraphs());
        setFromLevelToNodes(igTreeReader.getFromLevelToNodes());
        // Process the YEARS COLORS
        ArrayList<Integer> yearsArray = igTreeReader.getYears();
        int[] yearss = new int[yearsArray.size()];
        for (int i = 0; i < yearsArray.size(); i++) {
            yearss[i] = yearsArray.get(i).intValue();
        }
        this.hasReadsInId = igTreeReader.hasReadsInId();
        //we set the colors for the nodes
        setColors(yearss);
        setLastNodes();// store the information for the last nodes
        // we have the number of the last nodes so we have to set the width
        totalWidth = (lastNodesNumber * (widthOfNodeShape + 25)) + 40;
        //System.out.println("LastNodesnumber = "+lastNodesNumber+" widthNodeShape = "+ widthOfNodeShape+" total width = "+totalWidth);
        totalHeight = this.getHeight();
        setXPositions();
    }


    /**
     * This method is used to set the last nodes (used for the general display
     * of the tree and to calculate the width) At this point we also do the
     * following: - set the color for the nodes and their duplicated nodes - set
     * the IMGT info if the user loaded them
     */
    private void setLastNodes() {
        for (int i = 0; i < allNodeGraphs.size(); i++) {
            NodeGraph node = allNodeGraphs.get(i);
            if (!node.isADuplicateNode()) {
                if (node.isRoot()) { // node is the root
                    // Here we also set the CDR FR region from IMGT in the case
                    // the user loaded them
                    if (node.getCdr_fr_regions() != null) {
                        isImgtFormatLoaded = true;
                        setImgtInfo = new SetImgtInfo(node);
                    }
                }
                ArrayList<NodeGraph> children = node.getChildren();
                if (children.size() == 0) { // we store this node like a last
                    // one
                    node.setALastNode(true);
                    lastNodesNumber++;
                    // if this node has the minimum level we want it displayed
                    // in the middle of the tree, in order we
                    // dont have crossed lines
                    NodeGraph parent = node.getParent();
                    if (node.getLevel() == minLevelForLastNode && parent.getChildren().size() >= 2) {
                        node.setHasToBeInTheMiddle();
                    }
                }
            }
        }
    }

    /**
     * Here we want to set a position (starting at 1) for the order of the
     * children we do this in order we dont have crossed lines
     */
    private void setXPositions() {
        int xPosition = 1;
        for (int i = 0; i < allNodeGraphs.size(); i++) {
            NodeGraph node = allNodeGraphs.get(i);
            if (node.isRoot()) {
                xPosition = setXPositionForChildren(node, xPosition);
            }
        }
    }

    private int setXPositionForChildren(NodeGraph node, int xPosition) {
        int childrenNumber = node.getChildren().size();
        ArrayList<NodeGraph> nodesToBeFirst = new ArrayList<NodeGraph>();
        TreeMap<String, NodeGraph> fromMutNumberToNode = new TreeMap<String, NodeGraph>();
        for (NodeGraph child : node.getChildren()) {
            if (child.hasToBeInTheMiddle()) {
                nodesToBeFirst.add(child);
                // here take the mutations with the parent to put to the left
                // the bigger number
                int mutationWithParent = child.getNumberOfNucMutationsWithParent();
                fromMutNumberToNode.put(mutationWithParent + "," + child.getNodeId(), child);
            }
        }
        if (nodesToBeFirst.size() == 0) {
            for (int c = 0; c < childrenNumber; c++) {
                int xPositionForTheParent = c + 1;
                NodeGraph child = node.getChildren().get(c);
                child.setxPositionForTheParent(xPositionForTheParent);
                if (child.isALastNode()) {
                    child.setXPosition(xPosition);
                    xPosition++;
                } else {
                    xPosition = setXPositionForChildren(child, xPosition);
                }
            }
        } else { // case where we have a node that has to be in the middle
            ArrayList<NodeGraph> nodesInOrder = new ArrayList<NodeGraph>();
            int middle = (int) Math.ceil((double) childrenNumber / 2);
            // First we put the nodes on the left
            int index = 1;
            for (NodeGraph child : node.getChildren()) {
                if (index == middle) {
                    break;
                }
                if (!child.hasToBeInTheMiddle()) {
                    int xPositionForTheParent = index;
                    child.setxPositionForTheParent(xPositionForTheParent);
                    nodesInOrder.add(child);
                    index++;
                }
            }

            // Then the one that have to be in the middle
            String[] muts = new String[fromMutNumberToNode.size()];
            fromMutNumberToNode.keySet().toArray(muts);
            Arrays.sort(muts); // we sort the position
            int ch = index;
            for (String mut : muts) {
                NodeGraph child = fromMutNumberToNode.get(mut);
                int xPositionForTheParent = ch + 1;
                child.setxPositionForTheParent(xPositionForTheParent);
                nodesInOrder.add(child);
                ch++;
            }

            // then the rest of the children
            int start = nodesToBeFirst.size() + 1;
            for (int c = 0; c < childrenNumber; c++) {
                int xPositionForTheParent = start + c + 1;
                NodeGraph child = node.getChildren().get(c);
                if (!nodesInOrder.contains(child)) {
                    child.setxPositionForTheParent(xPositionForTheParent);
                    nodesInOrder.add(child);
                }
            }

            for (int c = 0; c < nodesInOrder.size(); c++) {
                NodeGraph child = nodesInOrder.get(c);
                // If it's a last node, we set the position among all the last
                // nodes
                if (child.isALastNode()) {
                    child.setXPosition(xPosition);
                    xPosition++;
                } else {
                    xPosition = setXPositionForChildren(child, xPosition);
                }
            }
        }
        return xPosition;
    }

    /**
     * This method will calculate the coordinates for all the nodes of the tree.
     * First we do the X coordinates, because they are only fixed by the number
     * of last nodes Then we do the Y coordinates that depend of the number of
     * mutations with each parent.
     */
    private void calculateCoordinates() {
        // First we do X coordinates
        // first calculate coordinates of the last nodes
        // we set to 100 pixels the minimum Y distance between a parent and its
        // child,
        // we set this distance to the minimum number of mutations
        for (int i = 0; i < allNodeGraphs.size(); i++) {
            NodeGraph node = allNodeGraphs.get(i);
            if (node.isALastNode()) {
                node.processXCoordinates(lastNodesNumber, totalWidth, widthOfNodeShape);
            }
            // finally we set to 1 the mini number of mutations, in order we can
            // compare different trees
            miniNumberOfMutations = 1;
        }

        // then to the rest of the tree, level by level starting from the bottom
        // of the tree (-1)
        for (int z = 1; z <= lastLevel; z++) {
            Integer level = lastLevel - z;
            ArrayList<NodeGraph> nodesAtThisLevel = fromLevelToNodes.get(level);
            for (int h = 0; h < nodesAtThisLevel.size(); h++) {
                NodeGraph node = nodesAtThisLevel.get(h);
                if (!node.isALastNode()) {
                    node.processXCoordinates(lastNodesNumber, totalWidth, widthOfNodeShape);
                }
            }
        }

        // We process y coordinates from the top of the tree to the bottom
        int newHeight = this.getHeight();
        for (int z = 0; z <= lastLevel; z++) {
            Integer level = z;
            ArrayList<NodeGraph> nodesAtThisLevel = fromLevelToNodes.get(level);
            for (int h = 0; h < nodesAtThisLevel.size(); h++) {
                NodeGraph node = nodesAtThisLevel.get(h);
                // 100 pixels is the mini Y dist between a parent and its child
                float mutationsNumber = node.getNumberOfNucMutationsWithParent();
                // logger.debug("mut number is "+mutationsNumber);
                float yDistanceParentChild = 0;
                if (mutationsNumber != 0) {
                    yDistanceParentChild = (float) ((float) ((1 / Math.log(5)) * 20 * mutationsNumber)
                            // before (float) ((float) (1 / Math.log(6)) * (20 * mutationsNumber)
                            / miniNumberOfMutations);
                    if (yDistanceParentChild < 100) {// before 30
                            yDistanceParentChild = 100; // to avoid crossing
                            // lines in the big tree// before= 30
                    }
                }
                node.processYcoordinates(yDistanceParentChild, heightOfNodeShape);
                node.setRectangle2D(widthOfNodeShape);
                // We have to see if we have to resize the JPanel in term of
                // height
                int yToTest = (int) (node.getYCoord() + node.getHeight());
                if (node.hasDuplicateNodes()) {
                    ArrayList<NodeGraph> dup = node.getDuplicatedNodes();
                    for (NodeGraph du : dup) {
                        yToTest += (du.getHeight() + 2);
                    }
                }
                if (yToTest >= newHeight) {
                    newHeight = yToTest + 20;
                }
                totalHeight = newHeight;
                // logger.debug("TOT HEIGHT is "+totalHeight);
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // logger.debug("repaint IgTREE");
        // logger.debug("\n\n");
        Graphics2D gd2 = (Graphics2D) g;
        int w = (int) this.getWidth();// real width of canvas
        int h = (int) this.getHeight(); // real height of canvas

        gd2.scale(scale, scale);

        calculateCoordinates();
        gd2.clearRect(0, 0, w, h);
        gd2.setColor(Color.white);
        gd2.fillRect(0, 0, w, h);
        draw(gd2);
        gd2.dispose();

        // Here we also create the EXCEL file wich contains all the information
        // about mutations, when the user loaded
        // the IMGT format
        // we put it OFF so far
        // if (createExcellFileWithMutations == null && isImgtFormatLoaded)
        // createExcellFileWithMutations = new
        // CreateExcellFileWithMutations(allMutations, xmlFile,
        // setImgtInfo.getPositionNucToCdrFrRegions(), isDNA);
    }

    /**
     * This is the zoom function
     */
    public void setScale(int direction, Point pt) {
        scale += direction * zoomInc;
        if (scale >= 1.05) {
            scale = 1;
        } else if (scale <= 0.001) {
            scale = 0.05;
        }
        removeAll();
        revalidate();
        repaint();
        setVisible(pt, direction);
    }

    /**
     * This method set the visible rectangle when the user zoom in/out
     */
    private void setVisible(Point pt, int direction) {
        // logger.info("SCALE IS "+scale);
        Rectangle visibleRectangle = igTreePanel.getVisibleRect();
        double width = visibleRectangle.getWidth() * scale;
        double height = visibleRectangle.getHeight() * scale;
        double halfWidth = (width / 2) * scale;
        double halfHeight = (height / 2) * scale;
        double xNode = visibleRectangle.getX() + pt.getX();
        double yNode = visibleRectangle.getY() + pt.getY();
        if (centerNode != null) {
            xNode = centerNode.getXCoord() * scale;
            yNode = centerNode.getYCoord() * scale;
        }
        double x = xNode - (halfWidth);
        double y = yNode - (halfHeight);
        visibleRectangle.x = (int) x;
        visibleRectangle.y = (int) y;
        igTreePanel.scrollRectToVisible(visibleRectangle);
    }

    /**
     * when the user search a node, to display it in the middle of the UI
     */
    public boolean setSearchedNode(String nodeName) {
        if (centerNode != null) {
            centerNode.setCentered(false);
            centerNode = null;
        }
        boolean found = false;
        for (NodeGraph node : allNodeGraphs) {
            if (node.getNodeId().equalsIgnoreCase(nodeName.trim())) {
                centerNode = node;
                found = true;
                break;
            }
        }
        if (found) {
            // the rectangle must have in center the selected node
            Rectangle visibleRectangle = igTreePanel.getVisibleRect();
            double width = visibleRectangle.getWidth() * scale;
            double height = visibleRectangle.getHeight() * scale;
            double xNode = centerNode.getXCoord() * scale;
            double yNode = centerNode.getYCoord() * scale;
            double x = xNode - (width / 2);
            double y = yNode - (height / 2);
            Rectangle rectangle = new Rectangle((int) x, (int) y, (int) width, (int) height);
            igTreePanel.scrollRectToVisible(rectangle);
            centerNode.setCentered(true);
            removeAll();
            revalidate();
            repaint();
        }
        return found;
    }

    /**
     * Reset the display
     */
    public void resetSearchNode() {
        if (centerNode != null) {
            centerNode.setCentered(false);
            centerNode = null;
        }
        everyThingIsGrey = false;
        scale = 1;
        removeAll();
        revalidate();
        repaint();
        Rectangle visibleRectangle = igTreePanel.getVisibleRect();
        Rectangle rectangle = new Rectangle(0, 0, (int) visibleRectangle.getWidth(),
                (int) visibleRectangle.getHeight());
        igTreePanel.scrollRectToVisible(rectangle);
    }

    /**
     * This method comes from the JPanelToExport interface, it is used in the
     * paintComponent(Graphics g) method and also to export this JPanel into EPS
     * format.
     */
    public void draw(Graphics2D gd2) {
        // we draw the nodes
        for (int z = 0; z <= lastLevel; z++) {
            Integer level = z;
            ArrayList<NodeGraph> nodesAtThisLevel = fromLevelToNodes.get(level);
            for (int h = 0; h < nodesAtThisLevel.size(); h++) {
                NodeGraph node = nodesAtThisLevel.get(h);
                drawNodeShapes(node, gd2);
                if (node.hasDuplicateNodes()) {
                    ArrayList<NodeGraph> dupNodes = node.getDuplicatedNodes();
                    for (int d = 0; d < dupNodes.size(); d++) {
                        NodeGraph dupNode = dupNodes.get(d);
                        drawNodeShapes(dupNode, gd2);
                    }
                }
            }
        }

        // Now we have displayed all the nodes and the lines between them, we
        // draw the mutation numbers between the
        // nodes.
        for (MutationsGraph mutationsGraph : allMutations.values()) {
            // we find the best position to display
            Rectangle2D rect = getBestPositionForMutations(mutationsGraph);
            mutationsGraph.setRectangle2d(rect);
            // we draw the string
            drawMutationNumber(mutationsGraph, gd2);
        }

        // If there are the BASELINe data we display them
        if (clipData != null) {
            int x = (int) totalWidth - 260;
            if (totalWidth<400){
                x= 220;
            }
            int y = 20;
            Font normalFont = new Font("Arial", Font.PLAIN, 20);
            Font smallFont = new Font("Arial", Font.ITALIC, 11);
            gd2.setColor(new Color(183, 183, 183));
            gd2.setFont(normalFont);
            gd2.fillRect(x, y, 240, 45);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 240, 45);
            gd2.drawString("P value", x + 90, y + 22); // "Selection Value
            // (\u03A3)"
            gd2.setFont(smallFont);
            gd2.drawString("Click here to get score for all sequences", x + 25, y + 37);
            int xForRect = x + 25;
            int yForRect = y + 22;
            // This rectangle is used to define the "clickable" region to launch
            // the Frame where all
            clipRectangle = new Rectangle2D.Float(xForRect, yForRect, 210, 20);
            y += 45;
            gd2.setFont(normalFont);
            gd2.setColor(new Color(226, 226, 226));
            gd2.fillRect(x, y, 120, 40);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 120, 40);
            gd2.drawString("CDR", x + 39, y + 29);
            x += 120;
            gd2.setColor(new Color(226, 226, 226));
            gd2.fillRect(x, y, 120, 40);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 120, 40);
            gd2.drawString("FWR", x + 37, y + 29);
            x -= 120;
            y += 40;

            String valueCDR = clipData.getpValCDR(); // getSigmaCDR();
            String valueFR = clipData.getpValFR();// getSigmaFR();
            Color color = pValueCodeColor.getColor(clipData.getpValCDR());
            gd2.setColor(color);
            gd2.fillRect(x, y, 120, 40);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 120, 40);
            Font font = gd2.getFont();
            FontRenderContext frc = gd2.getFontMetrics().getFontRenderContext();
            Rectangle2D rect = font.getStringBounds(valueCDR, frc);
            int xMiddle = (int) (x + 60 - (rect.getWidth() / 2));
            int yMiddle = (int) (y + 53 - rect.getHeight());
            gd2.drawString(valueCDR, xMiddle, yMiddle);
            x += 120;
            color = pValueCodeColor.getColor(clipData.getpValFR());
            gd2.setColor(color);
            gd2.fillRect(x, y, 120, 40);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 120, 40);
            rect = font.getStringBounds(valueFR, frc);
            xMiddle = (int) (x + 60 - (rect.getWidth() / 2));
            yMiddle = (int) (y + 53 - rect.getHeight());
            gd2.drawString(valueFR, xMiddle, yMiddle);
        }

    }

    /**
     * @return the allNodeGraphs
     */
    public ArrayList<NodeGraph> getAllNodeGraphs() {
        return allNodeGraphs;
    }

    /**
     * @param allNodeGraphs the allNodeGraphs to set
     */
    public void setAllNodeGraphs(ArrayList<NodeGraph> allNodeGraphs) {
        this.allNodeGraphs = allNodeGraphs;
        // Here we check if there are more than 15 nodes we decrease the size of
        // the shapes etc...
        if (allNodeGraphs.size() >= 20) {
            smaller = true;
            heightOfNodeShape = 40;
            widthOfNodeShape = 116; // 90
            //fontSizeNode = 19; // 20
            fontSizeMutationsNumber = 18;
        }
        for (NodeGraph node : allNodeGraphs) {
            if (node.isRoot()) {
                setRootNodeSequence(node.getSequence());
                rootNode = node;
            }
        }
    }

    /**
     * @return the fromLevelToNodes
     */
    public TreeMap<Integer, ArrayList<NodeGraph>> getFromLevelToNodes() {
        return fromLevelToNodes;
    }

    /**
     * @param fromLevelToNodes the fromLevelToNodes to set
     */
    public void setFromLevelToNodes(TreeMap<Integer, ArrayList<NodeGraph>> fromLevelToNodes) {
        this.fromLevelToNodes = fromLevelToNodes;
        Integer[] levels = new Integer[fromLevelToNodes.size()];
        fromLevelToNodes.keySet().toArray(levels);
        Arrays.sort(levels); // we sort the position
        int maxLevel = 0;
        for (Integer level : levels) {
            if (level.intValue() > maxLevel) {
                maxLevel = level.intValue();
            }
        }
        lastLevel = maxLevel;

        int minLevelForLastNode = lastLevel;
        for (int i = 0; i < allNodeGraphs.size(); i++) {
            NodeGraph node = allNodeGraphs.get(i);
            if (!node.isADuplicateNode()) {
                if (node.getChildren().size() == 0) { // is a last node
                    int level = node.getLevel();
                    if (level < minLevelForLastNode) {
                        minLevelForLastNode = level;
                    }
                }
            }
        }
        this.minLevelForLastNode = minLevelForLastNode;
        //logger.debug("Mini level for last node  is " + minLevelForLastNode);
    }

    public ColorByYear getColorByYear() {
        return colorByYear;
    }

    /**
     * @return the fadingON
     */
    public boolean isFadingON() {
        return fadingON;
    }

    /**
     * @param fadingON the fadingON to set
     */
    public void setFadingON(boolean fadingON) {
        this.fadingON = fadingON;
    }

    /**
     * @return the clipData
     */
    public CLIPdata getClipData() {
        return clipData;
    }

    /**
     * @param clipData the clipData to set
     */
    public void setClipData(CLIPdata clipData, ClipParser clipParser, ArrayList<String> igInOrder) {
        this.clipData = clipData;
        this.clipParser = clipParser;
        this.igInOrder = igInOrder;
        repaint();
    }

    /**
     * This method is used to get the color of the pValue coming from the CLIP
     * output
     *
     * @param pValue
     * @return Color
     */
    @SuppressWarnings("unused")
    private Color getpValueColor(String pValue) {
        if (pValue.matches(".*NA.*")) {
            return Color.white;
        }
        boolean isNegatif = false;
        if (pValue.matches("-.*")) {
            isNegatif = true;
        }
        BigDecimal pVal = new BigDecimal(pValue);
        BigDecimal absVal = pVal.abs();
        if ((absVal.compareTo(new BigDecimal(0.05)) == -1) || (absVal.compareTo(new BigDecimal(0.05)) == -0)) {
            if (isNegatif) {
                return intenseGreen;
            } else {
                return intenseRed;
            }
        } else {
            if (isNegatif) {
                return lightGreen;
            } else {
                return lightRed;
            }
        }

    }

    private void drawNodeShapes(NodeGraph node, Graphics2D g) {

        int x = (int) node.getXCoord();
        int y = (int) node.getYCoord();
        String nodeId = node.getNodeId();
        // logger.debug("Processing node for GUI: "+node.getNodeId());
        Color color = node.getColor();
        if (isEveryThingIsGrey() && isFadingON()) {
            color = Color.white;
        }
        g.setColor(color);
        int height = node.getHeight();
        if (node.getShape().equals("rectangle")) {
            g.fillRect(x, y, widthOfNodeShape, height); // previous 70 30
            color = Color.black;
            if (isEveryThingIsGrey() && isFadingON()) {
                color = lightGrey;
            }
            g.setColor(color);
            g.drawRect(x, y, widthOfNodeShape, height);
            if (node.isCentered()) { //this is the "searched" node
                g.setColor(Color.RED);
                g.drawRect(x - 1, y - 1, widthOfNodeShape + 2, height + 2);
            }
        } else if (node.getShape().equals("circle")) {
            g.fillOval(x, y, widthOfNodeShape, height);
            color = Color.black;
            if (isEveryThingIsGrey() && isFadingON()) {
                color = lightGrey;
            }
            g.setColor(color);
            if (node.isUnderLined()) {
                g.setStroke(dashed);
            }
            g.drawOval(x, y, widthOfNodeShape, height);
            if (node.isCentered()) {//this is the "searched" node
                g.setColor(Color.RED);
                g.drawOval(x - 1, y - 1, widthOfNodeShape + 2, height + 2);
            }
        } else if (node.getShape().equals("hexagon")) {
            Polygon polygon = getHexagon(x, y, widthOfNodeShape, height);
            g.fillPolygon(polygon);
            color = Color.black;
            if (isEveryThingIsGrey() && isFadingON()) {
                color = lightGrey;
            }
            g.setColor(color);
            g.drawPolygon(polygon);
            if (node.isCentered()) {//this is the "searched" node
                g.setColor(Color.RED);
                Polygon polygon2 = getHexagon(x - 1, y - 1, widthOfNodeShape + 2, height + 2);
                g.drawPolygon(polygon2);
            }
        }
        g.setStroke(plain);
        color = Color.black;
        if (isEveryThingIsGrey() && isFadingON()) {
            color = lightGrey;
        }
        g.setColor(color);

        printInsideTheNode(g, node, y, x);

        if (!node.isRoot() && !node.isADuplicateNode()) {
            // draw the proportional line
            int parentX = (int) node.getParent().getXCoord() + (widthOfNodeShape / 2);
            int parentY = (int) node.getParent().getYCoord() + node.getParent().getHeight();
            if (node.getParent().hasDuplicateNodes()) {
                ArrayList<NodeGraph> dup = node.getParent().getDuplicatedNodes();
                for (NodeGraph du : dup) {
                    int h = du.getHeight();
                    parentY += (h + 2);
                }
            }
            g.setColor(color);
            g.drawLine(parentX, parentY, x + (widthOfNodeShape / 2), y); // +35
            // we store the pixels belonging to this line
            ArrayList<Point2D> linePoints = getLinePoints(parentX, parentY, x + (widthOfNodeShape / 2), y);
            String mutationsNumber = node.getMutationTextToDisplay();
            MutationsGraph mutationsGraph;
            // by default we do like the line goes on the right
            float xcoord = node.getParent().getXCoord();
            float ycoord = (float) parentY - node.getParent().getHeight();
            float w = node.getXCoord() + widthOfNodeShape - xcoord;
            float h = node.getYCoord() - ycoord;

            // if line goes on the left
            if (node.getXCoord() < node.getParent().getXCoord()) {
                xcoord = node.getXCoord();
                w = node.getParent().getXCoord() + widthOfNodeShape - xcoord;
            }

            if (!allMutations.containsKey(nodeId)) {
                mutationsGraph = new MutationsGraph(mutationsNumber, node.getMutationsWithParent(), xcoord, ycoord, w,
                        h, node, g);
            } else {
                mutationsGraph = allMutations.get(nodeId);
                // will be used later to "distribute" the display, so far we
                // dont use it (memory consuming)
                mutationsGraph.setRectangleToWrite(new Rectangle2D.Float(xcoord, ycoord, w, h));
            }
            // we store the points of the line between the 2 nodes in the
            // MutationsGraph object
            mutationsGraph.setLinePoints(linePoints);
            allMutations.put(nodeId, mutationsGraph);
        }
    }

    private void printInsideTheNode(Graphics2D g, NodeGraph node, int y, int x) {
        String EC50 = node.getEC50();
        String comment = node.getComment1();
        String nodeIdToPrint = node.getNodeId();
        // here we made a method to write exactly in the middle
        Font fonte = new Font("Arial", Font.PLAIN, fontSizeNode);
        int div =2;
        if (nodeIdToPrint.matches("BP\\d+")||nodeIdToPrint.equals("UCA")){
            fonte = new Font("Arial", Font.PLAIN, 19);
            div=1;
        }
        g.setFont(fonte);
        //System.out.println("FONT "+fonte.getSize());
        FontRenderContext frc = g.getFontMetrics().getFontRenderContext();
        Rectangle2D rect = fonte.getStringBounds(nodeIdToPrint, frc);
        int xString = (int) (x + (widthOfNodeShape / 2) - (rect.getWidth() / 2));
        float mult = 1.15f;// 1.15
        if (smaller) {
            mult = 1.25f;// 1.25
        }
        if (EC50 == null && (comment == null || (comment != null && !node.isShowComment1()))) {
            int yString = (int) (y + (mult * heightOfNodeShape) - (rect.getHeight()*div));
            g.drawString(nodeIdToPrint, xString, yString);
        }
        // Print the EC 50 and comment
        else {
            mult = 1.5f;// 1.5
            if (smaller) {
                mult = 1.4f; //1.4
            }
            // If there is EC50 and comment we write the node Id a bit upper
            float toSubs = 0;
            if (EC50 != null && comment != null && node.isShowComment1()) {
                toSubs = mult * 3;
            }
            // we write the NodeId
            int yString = (int) (y + (mult * heightOfNodeShape) - rect.getHeight() - toSubs);
            g.drawString(nodeIdToPrint, xString, yString);
            int toSubstracte =0;// 6;
            int toSubsForSmaller = 0;//12;
            int toSubsForComment = 0;//10;
            if (smaller) {
                toSubstracte = 0;//4;
                toSubsForSmaller = 0;//8;
                toSubsForComment = 0;//7;
            }
            int fontArg = Font.PLAIN;
            Font fon = new Font("Calibri", fontArg, (fontSizeNode - toSubstracte));
            Font smallerFont = new Font("Calibri", Font.PLAIN, (fontSizeNode - (toSubsForSmaller))); // used
            // to
            // write
            // ng/ml

            // print EC50
            if (EC50 != null) {
                //TODO here allow to write several EC50, with one color different for each
                rect = fon.getStringBounds(EC50, frc);
                Rectangle2D rectUnity = smallerFont.getStringBounds(" ng/ml", frc);
                xString = (int) (x + (widthOfNodeShape / 2) - ((rect.getWidth() + rectUnity.getWidth()) / 2));
                mult = 2.1f;
                float toAdd = 0;
                if (comment != null && node.isShowComment1()) {
                    toAdd = mult * 3;
                }
                yString = (int) (y + (mult * (node.getHeight() / 2)) - rect.getHeight() + toAdd);
                g.setFont(fon);
                Color colForEc50 = getColorForEC50(EC50);
                g.setColor(colForEc50);
                // If there is EC50 and comment we write the EC50 more down
                if (!EC50.contains(",")) {
                    g.drawString(EC50, xString, yString);
                    Rectangle2D r = fon.getStringBounds(EC50, frc);
                    g.setFont(smallerFont);
                    g.drawString(" ng/ml", (int) (xString + r.getWidth()), yString);
                }
                else{ //the user entered several EC50, one for each Ag
                    String[] ec50s = EC50.split(",");
                    int index=0;
                    int ec50XString = xString;
                    for (String ec50: ec50s){
                        Color color = getColorForSeveralEC50(index);
                        String toPrint = ec50.trim()+" ";
                        g.setColor(color);
                        g.drawString(toPrint, ec50XString, yString);
                        Rectangle2D r = fon.getStringBounds(toPrint, frc);
                        ec50XString += r.getWidth();
                        if (index == ec50s.length-1){
                            g.setFont(smallerFont);
                            g.setColor(Color.BLACK);
                            g.drawString(" ng/ml", ec50XString, yString);
                        }
                        index++;
                    }
                }
            }

            // print comment1
            if (comment != null && node.isShowComment1()) {
                mult = 2.1f;
                Font commentFont = new Font("Calibri", Font.PLAIN, (fontSizeNode - toSubsForComment));
                Rectangle2D commentRect = commentFont.getStringBounds(comment, frc);
                float toAdd = 4;
                if (EC50 != null) {
                    toAdd = (float) (mult * 9);
                    if (smaller) {
                        toAdd = (float) (mult * 7);
                    }
                }
                xString = (int) (x + (widthOfNodeShape / 2) - (commentRect.getWidth() / 2));
                yString = (int) (y + (mult * (node.getHeight() / 2)) - commentRect.getHeight() - toAdd);
                g.setFont(commentFont);
                g.setColor(darkGrey);
                g.drawString(comment, xString, yString);
            }
        }

    }

    private Polygon getHexagon(int X, int Y, int w, int h) { // where w is the
        // width of the
        // shape and h
        // its height
        int x1 = X + (w / 9);
        int x2 = (int) (X + (float) (8 * w / 9));
        int x3 = X + w;
        int x4 = (int) (X + (float) (8 * w / 9));
        int x5 = X + (w / 9);
        int x6 = X;
        int[] x = {x1, x2, x3, x4, x5, x6};
        int[] y = {Y, Y, Y + h / 2, Y + h, Y + h, Y + h / 2};
        Polygon polygon = new Polygon();
        for (int i = 0; i < 6; i++) {
            polygon.addPoint(x[i], y[i]);
        }
        return polygon;
    }

    /**
     * This method is used to calculate the best position to display the number
     * of mutations between 2 nodes
     *
     * @param mutationsGraph
     * @return Rectangle2D which gets the X and X position of the mutation
     * numbers to be written
     */
    private Rectangle2D getBestPositionForMutations(MutationsGraph mutationsGraph) {
        Rectangle2D finalRectangle = new Rectangle2D.Float();
        float x = 0;
        float y = 0;
        Rectangle2D rectForString = mutationsGraph.getStringLenght(fontSizeMutationsNumber);
        float stringLenght = (float) rectForString.getWidth();
        ArrayList<Point2D> linePoints = mutationsGraph.getLinePoints();
        // First we try the most common position that would be the middle and on
        // the left if
        // the parent is on the right, and on the right if the parent is on the
        // left
        Point2D firstPoint = linePoints.get(0);
        Point2D lastPoint2d = linePoints.get(linePoints.size() - 1);
        boolean writeLeft = true;
        if (firstPoint.getX() < lastPoint2d.getX()) {
            writeLeft = false;
        }
        // we take the middle
        Point2D middlePoint = linePoints.get(linePoints.size() / 2);
        x = (float) middlePoint.getX();
        y = (float) middlePoint.getY();
        float yParent = (float) mutationsGraph.getAreaRectangleToWrite().getY();
        float ydist = (float) (lastPoint2d.getY() - firstPoint.getY());
        float xdist = (float) (firstPoint.getX() - lastPoint2d.getX());
        if (xdist < 0) {
            xdist = (float) (lastPoint2d.getX() - firstPoint.getX());
        }
        // here if dist between nodes is == 30 (mini dist), we set the y to ~ (y
        // parent + height node)
        // logger.warn("yParent is "+yParent+" and yNode is "+yNode);
        // logger.warn("yDist for node "+mutationsGraph.getNodeId()+" is
        // "+ydist+" and x dist is: "+xdist+", div is "+ydist/xdist);
        if (ydist == ((mutationsGraph.getNode().getParent().getHeight() + 30) - mutationsGraph.getNode().getHeight())) { // TO
            // DELETE
            // heightOfNodeShape
            y = yParent + mutationsGraph.getNode().getParent().getHeight();
        } else if (xdist == 0) {
            y -= (float) (0.5 * rectForString.getHeight());
        } else {
            y -= (float) (0.8 * rectForString.getHeight());
        }
        if (writeLeft) {// we have to shift the position taking in account the
            // lenght of the string
            x -= (stringLenght + 5);
            if (xdist == 0) {
                x -= 13;
                if (smaller) {
                    x += 12;
                    y += 2;
                }
            } else if (ydist / xdist < 0.5f) {
                x -= 18;
                y -= 10;
                if (smaller) {
                    y += 3;
                    x += 8;
                }
            } else {
                x -= 5;
                y += 3;
            }
        } else {// we write right
            xdist = (float) (lastPoint2d.getX() - firstPoint.getX());
            if (ydist / xdist < 0.5f) {
                y -= 10;
                if (smaller) {
                    y += 3;
                }
            } else {
                y += 3;
            }
            x += 5;
        }
        finalRectangle = new Rectangle2D.Float(x, y, stringLenght + 8, (float) rectForString.getHeight() + 2);
        return finalRectangle;
    }

    /**
     * this method will draw the String representing the mutation numbers
     * between 2 nodes, on the JPanel Graphics object
     *
     * @param mutationsGraph
     * @param g
     */
    private void drawMutationNumber(MutationsGraph mutationsGraph, Graphics2D g) {

        int x = (int) mutationsGraph.getRectangle2d().getX();
        int y = (int) mutationsGraph.getRectangle2d().getY();
        int w = (int) mutationsGraph.getRectangle2d().getWidth();
        int h = (int) mutationsGraph.getRectangle2d().getHeight();
        String mutationsNumber = mutationsGraph.getMutationsNumber();

        if (mutationsGraph.isYellow()) {
            g.setColor(yellowColor);
            g.fillRect(x, y, w, h);
        } else if (mutationsGraph.isPink()) {
            g.setColor(pinkColor);
            g.fillRect(x, y, w, h);
        }
        if (mutationsGraph.getBoxed() > 0) {
            int boxesNumber = mutationsGraph.getBoxed();
            int xRect = x;// /x - 5;
            int yRect = y;// y - 21;
            int widthRect = w;// 80;
            int heightRect = h;// 28;
            for (int i = 0; i < boxesNumber; i++) {
                g.setColor(new Color(84, 84, 84));
                g.drawRect(xRect, yRect, widthRect, heightRect);
                xRect -= 2;
                yRect -= 2;
                widthRect += 4;
                heightRect += 4;
            }
            g.setColor(new Color(84, 84, 84));
            g.setFont(new Font("Arial", Font.PLAIN, 18)); // dialog
            g.drawString(mutationsGraph.getAAChange(), x - 2, y - 5);
        }
        Color color = Color.black;
        if (isEveryThingIsGrey() && isFadingON()) {
            color = lightGrey;
        }
        g.setColor(color);
        Font fonte2 = new Font("Arial", Font.PLAIN, fontSizeMutationsNumber);
        g.setFont(fonte2);
        g.drawString(mutationsNumber, x + 3, y + (6 * h / 8));
    }

	/*
     * @Override public Dimension getMinimumSize() { return new Dimension((int)
	 * totalWidth, (int) totalHeight); }
	 */

    /**
     * This method is used to store all the points (pixels) of a line
     *
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @return
     */
    private ArrayList<Point2D> getLinePoints(int x, int y, int x2, int y2) {
        ArrayList<Point2D> points = new ArrayList<Point2D>();
        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0) {
            dx1 = -1;
        } else if (w > 0) {
            dx1 = 1;
        }
        if (h < 0) {
            dy1 = -1;
        } else if (h > 0) {
            dy1 = 1;
        }
        if (w < 0) {
            dx2 = -1;
        } else if (w > 0) {
            dx2 = 1;
        }
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) {
                dy2 = -1;
            } else if (h > 0) {
                dy2 = 1;
            }
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {
            Point2D pt = new Point2D.Float(x, y);
            points.add(pt);
            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x += dx1;
                y += dy1;
            } else {
                x += dx2;
                y += dy2;
            }
        }
        return points;
    }

    /**
     * @return the isImgtFormatLoaded
     */
    public boolean isImgtFormatLoaded() {
        return isImgtFormatLoaded;
    }

    /**
     * @return the everyThingIsGrey
     */
    public boolean isEveryThingIsGrey() {
        return everyThingIsGrey;
    }

    /**
     * @param everyThingIsGrey the everyThingIsGrey to set
     */
    public void setEveryThingIsGrey(boolean everyThingIsGrey) {
        this.everyThingIsGrey = everyThingIsGrey;
    }

    /**
     * This method returns a color depending on the ECc50 value: red, green or
     * blue
     *
     * @param val
     * @return color
     */
    private Color getColorForEC50(String val) {
        //This is a case of a single value for EC50, if there are several values, we give one color for each
        Color color = Color.black;
        if (!val.contains(",")) {
            String value = val.replace("~", "");
            BigDecimal numb = new BigDecimal(value);
            if (numb.compareTo(new BigDecimal(20)) <= 0) {
                color = new Color(198, 0, 0);// red
            } else if (numb.compareTo(new BigDecimal(200)) <= 0) {
                color = new Color(0, 138, 0); // green
            } else if (numb.compareTo(new BigDecimal(200)) > 0) {
                color = new Color(0, 0, 64);// blue
            }
        }
        return color;
    }

    private Color getColorForSeveralEC50(int index){
        Color color = Color.BLACK;
        if (ec50Colors.get(index)!=null){
            color= ec50Colors.get(index);
        }
        return color;
    }

    /**
     * Not used so far
     *
     * @param val
     * @return
     */
    @SuppressWarnings("unused")
    private boolean getBoldForEC50(String val) {
        String value = val.replace("~", "");
        BigDecimal numb = new BigDecimal(value);
        if (numb.compareTo(new BigDecimal(200)) <= 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @author Mathilde This class will be the listener to this JPanel.
     *         Basically, each time the user released the button of the mouse,
     *         it will take the position (Point2D) and check if there is an
     *         object drawn there (a node, a mutation etc...) and will open a
     *         pop up if necessary.
     */
    class MyMouseListener implements MouseListener {
        private final JComponent component;

        public MyMouseListener(JComponent component) {
            this.component = component;
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        @SuppressWarnings("unused")
        public void mouseReleased(MouseEvent e) {
            // we get the position of the mouse
            int x = (int) (e.getX() / scale);
            int y = (int) (e.getY() / scale);
            Point2D point2d = new Point2D.Float(x, y);
            for (int i = 0; i < allNodeGraphs.size(); i++) {
                NodeGraph node = allNodeGraphs.get(i);
                if (node.containsPoint2D(point2d)) {
                    // logger.debug("We click node "+node.getNodeId());
                    NodeFrame nodeFrame = new NodeFrame(node, component, igTreePanel);
                    return;
                }
            }
            for (Entry<String, MutationsGraph> entry : allMutations.entrySet()) {
                MutationsGraph mutationGraph = entry.getValue();
                if (mutationGraph.containsPoint2D(point2d)) {
                    NodeGraph node = mutationGraph.getNode();
                    MutationsFrame mutationsFrame = new MutationsFrame(igTreePanel,
                            mutationGraph.getMutationsWithParent(), allMutations, mutationGraph.getNode(), rootNode,
                            setImgtInfo, isDNA, getSetCurrentDir);
                    return;
                }
            }

            // Last we check that the user click on the "get p-values for all
            // sequences", fot the BASELINe data
            if (clipRectangle.contains(point2d)) {
                // here we open a window to display all the results of the
                // focused test
                ClipDataFrame clipDataFrame = new ClipDataFrame(component, clipParser, igInOrder, getSetCurrentDir);
            }
        }
    }

    public Dimension getPreferredSize() {
        int w = (int) (scale * totalWidth), h = (int) (scale * totalHeight);
        Dimension dim = new Dimension(w, h);
        return dim;
    }

    /**
     * @return the isNewDataSaved
     */
    public boolean isNewDataSaved() {
        return isNewDataSaved;
    }

    /**
     * @param isNewDataSaved the isNewDataSaved to set
     */
    public void setNewDataSaved(boolean isNewDataSaved) {
        this.isNewDataSaved = isNewDataSaved;
    }

    /**
     * @return the xmlFile
     */
    public File getXmlFile() {
        return xmlFile;
    }

    /**
     * @param xmlFile the xmlFile to set
     */
    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    /**
     * @return the rootNodeSequence
     */
    public String getRootNodeSequence() {
        return rootNodeSequence;
    }

    /**
     * @param rootNodeSequence the rootNodeSequence to set
     */
    public void setRootNodeSequence(String rootNodeSequence) {
        this.rootNodeSequence = rootNodeSequence;
    }

    /**
     * @return the hasImmuInfo
     */
    public boolean hasImmuInfo() {
        return hasImmuInfo;
    }

    /**
     * @param hasImmuInfo the hasImmuInfo to set
     */
    public void setHasImmuInfo(boolean hasImmuInfo) {
        this.hasImmuInfo = hasImmuInfo;
    }

    public boolean hasReadsInId() {
        return hasReadsInId;
    }

    /**
     * This method set the color for all the nodes, it can be in purple by
     * default, or by reads freq for NGS or by year (Leon data)
     */
    private void setColors(int[] years) {
        Color rootColor = new Color(161, 250, 80);
        if (hasReadsInId) { // color by reads number
            double readsMx = 0;
            double totReads = 0;
            for (NodeGraph node : allNodeGraphs) {
                totReads += node.getReads();
                if (node.getReads() > readsMx) {
                    readsMx = node.getReads();
                }
            }
            //logger.debug("Read max is " + readsMx);
            for (NodeGraph node : allNodeGraphs) {
                if (node.isRoot()) {
                    node.setColor(rootColor);
                } else if (node.isBP()) {
                    node.setColor(Color.white);
                } else {
                    double p = (node.getReads() * 100) / totReads;
                    // logger.debug("For "+node.getNodeId()+" p is "+p);
                    // double red = p < 50 ? 255 : Math.round(256 - (p - 50) *
                    // 5.12);
                    // double green = p > 50 ? 255 : Math.round((p) * 5.12);
                    Color color = null;
                    if (p > 0 && p <= 1) {
                        color = new Color(255, 204, 204);
                    } else if (p > 1 && p <= 5) {
                        color = new Color(255, 153, 153);
                    } else if (p > 5 && p <= 10) {
                        color = new Color(255, 102, 102);
                    } else if (p > 10 && p <= 20) {
                        color = new Color(255, 51, 51);
                    } else if (p > 20 && p <= 100) {
                        color = new Color(255, 0, 0);
                    }
                    node.setColor(color);
                }
            }
        } else if (years.length > 0) { // color by year
            colorByYear = new ColorByYear(years);
            for (NodeGraph node : allNodeGraphs) {
                if (node.isRoot()) {
                    node.setColor(rootColor);
                } else if (node.isBP()) {
                    node.setColor(Color.white);
                } else {
                    node.setColor(colorByYear.getColorForYear(node.getYear()));
                }
            }
        } else { // default color
            Color defaultColor = new Color(Integer.parseInt("CDB5CD", 16));// mauve
            for (NodeGraph node : allNodeGraphs) {
                if (node.isRoot()) {
                    node.setColor(rootColor);
                } else if (node.isBP()) {
                    node.setColor(Color.white);
                } else {
                    node.setColor(defaultColor);
                }
            }
        }

        // we set the color of the duplicated one if there are
        for (NodeGraph node : allNodeGraphs) {
            if (node.hasDuplicateNodes()) {
                ArrayList<NodeGraph> dupNodes = node.getDuplicatedNodes();
                for (int j = 0; j < dupNodes.size(); j++) {
                    NodeGraph dupNode = dupNodes.get(j);
                    dupNode.setColor(node.getColor());
                }
            }
        }
    }
}
