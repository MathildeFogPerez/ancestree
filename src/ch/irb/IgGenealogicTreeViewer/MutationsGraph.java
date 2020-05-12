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

package ch.irb.IgGenealogicTreeViewer;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import ch.irb.nodes.NodeGraph;

/**
 * @author Mathilde This class represent the mutations between a node and its parent It contains the nucleotidic
 *         mutations, the AA mutations, the number of mutations and the coordinates where it will be displayed in the
 *         tree in order it is clickable.
 */
public class MutationsGraph {
    static Logger logger = Logger.getLogger(MutationsGraph.class);
    private Rectangle2D rectangle2d = new Rectangle2D.Float();
    private Rectangle2D rectangleToWrite = new Rectangle2D.Float();
    private ArrayList<Point2D> linePoints = new ArrayList<Point2D>();
    private float xCoord = 0;
    private float yCoord = 0;
    private boolean isYellow = false; // if for one position the nucleotidic mutation is the same
    private boolean isPink = false; // if for one position the nucleotidic mutation is different
    private int boxed = 0; // the number of boxes will be similar to the mutations where there is the same AA change
    private String mutationsNumber;
    private String mutationsWithParent;
    private String aaMutationsWithParent;
    private String nodeId;
    private String nodeSequence;
    private String nodeProtSequence;
    private String aaMutationWithParent;
    private NodeGraph node;
    private Graphics2D g2d = null;

    public MutationsGraph(String mutationsNumber, String mutationsWithParent, float xcoord, float ycoord, float width,
                          float height, NodeGraph node, Graphics2D g2d) {
        this.mutationsNumber = mutationsNumber;
        this.mutationsWithParent = mutationsWithParent;
        this.node = node;
        this.g2d = g2d;
        setAaMutationsWithParent();
        nodeId = node.getNodeId();
        nodeSequence = node.getSequence();
        nodeProtSequence = node.getProteinSequence();
        // so far we dont use this rectangle to write, because I'm afraid it will be too consuming in term of
        // memory
        if (mutationsWithParent != null) {
            rectangleToWrite = new Rectangle2D.Float(xcoord, ycoord, width, height);
        }
    }

    /**
     * @return the nodeId
     */
    public String getNodeId() {
        return nodeId;
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
    public void setXCoord(float xCoord) {
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
    public void setYCoord(float yCoord) {
        this.yCoord = yCoord;
    }

    public boolean containsPoint2D(Point2D point2d) {
        if (rectangle2d.contains(point2d)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the mutationsWithParent
     */
    public String getMutationsWithParent() {
        return mutationsWithParent;
    }

    /**
     * @return the aaMutationsWithParent
     */
    public String getAaMutationsWithParent() {
        return aaMutationsWithParent;
    }


    public void setAaMutationsWithParent() {
        String nodeProtSeq = node.getProteinSequence();
        String parentProtSeq = node.getParent().getProteinSequence();
        String aaMutations = ",";
        for (int i = 0; i < nodeProtSeq.length(); i++) {
            String nodeAA = String.valueOf(nodeProtSeq.charAt(i));
            String parentAA = String.valueOf(parentProtSeq.charAt(i));
            if (!parentAA.equals(nodeAA)) {
                aaMutations += i + ":" + parentAA + "->" + nodeAA + ",";
            }
        }
        this.aaMutationsWithParent = aaMutations;
    }

    /**
     * @return the mutationsNumber
     */
    public String getMutationsNumber() {
        return mutationsNumber;
    }

    public Rectangle2D getStringLenght(int fontSize) {
        Font font = new Font("Arial", Font.PLAIN, fontSize);
        FontRenderContext frc = g2d.getFontMetrics().getFontRenderContext();
        Rectangle2D rect = font.getStringBounds(mutationsNumber, frc);
        return rect;
    }

    /**
     * @return the isYellow
     */
    public boolean isYellow() {
        return isYellow;
    }

    /**
     * @param isYellow the isYellow to set
     */
    public void setYellow(boolean isYellow) {
        this.isYellow = isYellow;
    }

    /**
     * @return the isPink
     */
    public boolean isPink() {
        return isPink;
    }

    /**
     * @param isPink the isPink to set
     */
    public void setPink(boolean isPink) {
        this.isPink = isPink;
    }

    /**
     * @return the boxed
     */
    public int getBoxed() {
        return boxed;
    }

    /**
     * @param boxed the boxed to set
     */
    public void setBoxed(int boxed) {
        this.boxed = boxed;
    }

    public void setAAchange(String aaMutationWithParent) {
        this.aaMutationWithParent = aaMutationWithParent;
    }

    public String getAAChange() {
        return aaMutationWithParent;
    }

    /**
     * @return the nodeSequence
     */
    public String getNodeSequence() {
        return nodeSequence;
    }

    /**
     * @return the nodeProtSequence
     */
    public String getNodeProtSequence() {
        return nodeProtSequence;
    }

    /**
     * @param nodeProtSequence the nodeProtSequence to set
     */
    public void setNodeProtSequence(String nodeProtSequence) {
        this.nodeProtSequence = nodeProtSequence;
    }

    public NodeGraph getNode() {
        return node;
    }

    /**
     * @return the rectangle2d
     */
    public Rectangle2D getRectangle2d() {
        return rectangle2d;
    }

    /**
     * @param rectangle2d the rectangle2d to set
     */
    public void setRectangle2d(Rectangle2D rectangle2d) {
        this.rectangle2d = rectangle2d;
    }

    /**
     * @return the rectangleToWrite
     */
    public Rectangle2D getAreaRectangleToWrite() {
        return rectangleToWrite;
    }

    /**
     * @param rectangleToWrite the rectangleToWrite to set
     */
    public void setRectangleToWrite(Rectangle2D rectangleToWrite) {
        this.rectangleToWrite = rectangleToWrite;
    }

    /**
     * @return the linePoints
     */
    public ArrayList<Point2D> getLinePoints() {
        return linePoints;
    }

    /**
     * @param linePoints the linePoints to set
     */
    public void setLinePoints(ArrayList<Point2D> linePoints) {
        this.linePoints = linePoints;
    }
}
