/*
   Copyright 2020 - Mathilde Foglierini Perez

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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import ch.irb.IgGenealogicTreeViewer.AncesTreeConverter.InputParser;
import org.apache.log4j.Logger;

import ch.irb.IgGenealogicTreeViewer.AncesTreeConverter.InputParser;
import ch.irb.nodes.Node;
import ch.irb.nodes.NodeGraph;

import static java.lang.Integer.parseInt;

/**
 * @author Mathilde This class is used to read the XML file and to create
 * NodeGraph objects according to this input file. The only difference
 * with the IgTreeMaker is that a NodeGraph has only ONE id and can have
 * duplicated nodes. These duplicated nodes will be only displayed in
 * the GUI, only the "top" node will have all the features of a
 * GraphNode (the mutations with its parent etc..)
 */
public class IgTreeReader {
    static Logger logger = Logger.getLogger(IgTreeReader.class);
    private String xmlFilePath;
    private String projectName;
    @SuppressWarnings("unused")
    private String previousBPNames;
    private boolean isDNA;
    private boolean hasImmuInfo = false;
    private boolean hasReadsInId = false;
    private Node rootNode;
    @SuppressWarnings("unused")
    private NodeGraph rootNodeGraph;
    private ArrayList<NodeGraph> allNodeGraphs = new ArrayList<>();
    private HashMap<String, NodeGraph> fromIdToNodeGraph = new HashMap<>();
    private TreeMap<Integer, ArrayList<NodeGraph>> fromLevelToNodes = new TreeMap<>();
    private ArrayList<Integer> years = new ArrayList<>();

    public IgTreeReader(String xmlFilePath, boolean isDnamlTree) throws JAXBException, Exception {
        this.xmlFilePath = xmlFilePath;
        if (isDnamlTree) {
            readAndProcessTreeDnaml();
        } else {
            readAndProcessTreeAncesTree();
        }
    }

    public void readAndProcessTreeDnaml() throws Exception {
        //Before 2016: IgTreeMaker.class before april 2020 DnamlOutputParser.class
        JAXBContext context = JAXBContext.newInstance(InputParser.class);
        Unmarshaller um = context.createUnmarshaller();
        // logger.debug("XML file path is: " + xmlFilePath);
        FileReader fileReader = new FileReader(xmlFilePath);
        //Before 2016: IgTreeMaker, before april 2020 DnamlOutputParser
        InputParser dnamlParser = (InputParser) um.unmarshal(fileReader);
        this.projectName = dnamlParser.getProjectName();
        this.rootNode = dnamlParser.getRootNode();
        this.previousBPNames = dnamlParser.getPreviousBPNames();
        setDNA(dnamlParser.isDNA());
        setHasReadsInId(dnamlParser.hasReadsInId());
        createANodeGraph(rootNode, true);
        setChildrenForGraphNode(rootNode);
        setNodesbyLevel(); // store in a tree map the nodes by their level
        setChildrenPosition(); // set the x position for the nodes
    }

    /*
     * Not used anymore (from 2016), but to keep in case
     */
    public void readAndProcessTreeAncesTree() throws Exception {
        JAXBContext context = JAXBContext.newInstance(InputParser.class);
        Unmarshaller um = context.createUnmarshaller();
        // logger.debug("XML file path is: " + xmlFilePath);
        FileReader fileReader = new FileReader(xmlFilePath);
        InputParser igTreeMaker = (InputParser) um.unmarshal(fileReader);
        this.projectName = igTreeMaker.getProjectName();
        this.rootNode = igTreeMaker.getRootNode();
        this.previousBPNames = igTreeMaker.getPreviousBPNames();
        setDNA(igTreeMaker.isDNA());
        createANodeGraph(rootNode, true);
        setChildrenForGraphNode(rootNode);
        setNodesbyLevel(); // store in a tree map the nodes by their level
        setChildrenPosition(); // set the x position for the nodes
    }

    public void createANodeGraph(Node node, boolean isRoot) throws Exception {
        //BUG fixed the 07.05.20 in the case of big tree (>500 igs) different BPs can have the same sequence with an Ig
        // BUT be at different place in the tree
        if (fromIdToNodeGraph.containsKey(node.getNodeId())) {
            throw new Exception("Something wrong with this tree: some BP/Ig have the same sequences in different nodes " +
                    "in the tree but they can not be collapsed.");
        }
        NodeGraph nodeGraph = new NodeGraph();
        nodeGraph.setRoot(isRoot);
        nodeGraph.setDNA(isDNA());
        //logger.info("Create a nodegraph for " + node.getNodeId() + " isDNA " + isDNA());
        if (node.getNodeId().matches("BP.*")) {
            nodeGraph.setBP(true);
        }
        // here we set only ONE id!!!
        String[] nodeIds = node.getNodeId().split(",");
        boolean hasDuplicatedNodes = false;
        String cellInfoForDuplicatedNodes = "";
        String immuInfoForDuplicatedNodes = "";
        if (nodeIds.length > 1) {
            nodeGraph.setNodeId(nodeIds[0]);
            hasDuplicatedNodes = true;
            // logger.debug("For node "+nodeIds[0]+" we have duplicate");
        } else { // we have one NodeGraph we can add the number of reads if
            // there are some
            if (hasReadsInId) {
                if (node.getNodeId().matches(".*_\\d+")) {
                    int reads = parseInt(node.getNodeId().split("_")[1]);
                    //logger.debug("SET READS "+reads+" for "+node.getNodeId());
                    nodeGraph.setReads(reads);
                } else if (!nodeGraph.isRoot() && !nodeGraph.isBP()) {
                    JOptionPane.showMessageDialog(new JFrame(), "The number of reads was not found in the Ig ID " + node.getNodeId());
                    this.hasReadsInId = false;
                }
            }
            nodeGraph.setNodeId(node.getNodeId());
        }
        nodeGraph.setSequence(node.getSequence());
        // logger.debug("........setProteinSequence for " + node.getNodeId());
        nodeGraph.setProteinSequence();
        nodeGraph.setNumberOfNucMutationsWithParent(node.getNumberOfNucMutationsWithParent());
        nodeGraph.setMutationsWithParent(node.getMutationsWithParent());
        nodeGraph.setNumberOfAAMutationsWithParent(node.getNumberOfAAMutationsWithParent());
        nodeGraph.setLevel(node.getLevel());
        if (node.getInsertionNumber() > 0) {
            nodeGraph.setHasInsertion(true);
            nodeGraph.setInsertionNumber(node.getInsertionNumber());
        }
        if (node.getDeletionNumber() > 0) {
            nodeGraph.setHasDeletion(true);
            nodeGraph.setDeletionNumber(node.getDeletionNumber());
        }
        if (node.getDoubleMutationInformation() != null) {
            nodeGraph.setDoubleMutationInformation(node.getDoubleMutationInformation());
        }
        if (node.getReverseInformation() != null) {
            nodeGraph.setReverseInformation(node.getReverseInformation());
        }

        // In the case the user loaded IMGT format and CDR FR regions
        if (node.getCdr_fr_regions() != null) {
            nodeGraph.setCdr_fr_regions(node.getCdr_fr_regions());
        }
        if (node.getImgtFormatSequence() != null) {
            nodeGraph.setImgtFormatSequence(node.getImgtFormatSequence());
        }

        if (node.getCellInfo() != null) {
            if (!hasDuplicatedNodes) {
                nodeGraph.setCellInfo(node.getCellInfo());
            } else {
                String[] cellInfos = node.getCellInfo().split("// ");
                int index = 0;
                for (String info : cellInfos) {
                    if (index > 0) { // because the first index is null
                        String[] splitted = info.split("\\s");
                        String nodeId = splitted[0];
                        if (nodeId.equals(nodeGraph.getNodeId())) {
                            nodeGraph.setCellInfo("// " + info);
                        } else {
                            cellInfoForDuplicatedNodes += "// " + info;
                        }
                    }
                    index++;
                }
            }
        }

        // EC50
        HashMap<String, String> nodeIdToEC50 = new HashMap<String, String>();
        if (node.getEC50() != null) {
            if (!hasDuplicatedNodes) {
                String[] spli = node.getEC50().split(":");
                String ec = spli[1].replace("//", "");
                nodeGraph.setEC50(ec);
            } else {
                String[] spli = node.getEC50().split("//");
                for (String s : spli) {
                    // logger.debug("processing string "+s);
                    String[] sp = s.split(":");
                    if (sp[0].equals(nodeGraph.getNodeId())) {
                        nodeGraph.setEC50(sp[1]);
                    } else {
                        nodeIdToEC50.put(sp[0], sp[1]);
                    }
                }
            }
        }

        // here we have to get the comments if there are some
        HashMap<String, String> nodeIdToComment1 = new HashMap<String, String>();
        if (node.getComment1() != null) {
            if (!hasDuplicatedNodes) {
                String[] spli = node.getComment1().split(":");
                String ec = spli[1].replace("//", "");
                nodeGraph.setComment1(ec);
            } else {
                String[] spli = node.getComment1().split("//");
                for (String s : spli) {
                    // logger.debug("processing string "+s);
                    String[] sp = s.split(":");
                    if (sp[0].equals(nodeGraph.getNodeId())) {
                        nodeGraph.setComment1(sp[1]);
                    } else {
                        nodeIdToComment1.put(sp[0], sp[1]);
                    }
                }
            }
        }

        HashMap<String, String> nodeIdToComment2 = new HashMap<String, String>();
        if (node.getComment2() != null) {
            if (!hasDuplicatedNodes) {
                String[] spli = node.getComment2().split(":");
                String ec = spli[1].replace("//", "");
                nodeGraph.setComment2(ec);
            } else {
                String[] spli = node.getComment2().split("//");
                for (String s : spli) {
                    // logger.fatal("processing string "+s);
                    String[] sp = s.split(":");
                    if (sp[0].equals(nodeGraph.getNodeId())) {
                        nodeGraph.setComment2(sp[1]);
                    } else {
                        nodeIdToComment2.put(sp[0], sp[1]);
                    }
                }
            }
        }

        if (node.getImmunizationInfo() != null) {
            // this set of Igs has immu info
            setHasImmuInfo(true);
            if (!hasDuplicatedNodes) {
                nodeGraph.setImmunizationInfo(node.getImmunizationInfo());
            } else {
                String[] immuInfos = node.getImmunizationInfo().split("// ");
                int index = 0;
                for (String info : immuInfos) {
                    if (index > 0) {// because the first index is null
                        String[] splitted = info.split("\\s");
                        String nodeId = splitted[0];
                        if (nodeId.equals(nodeGraph.getNodeId())) {
                            nodeGraph.setImmunizationInfo("// " + info);
                        } else {
                            immuInfoForDuplicatedNodes += "// " + info;
                        }
                    }
                    index++;
                }
                nodeGraph.setDuplicatedNodes(cellInfoForDuplicatedNodes, immuInfoForDuplicatedNodes, nodeIdToEC50,
                        nodeIdToComment1, nodeIdToComment2);
            }
            setYears(nodeGraph, node.getImmunizationInfo());
        } else if (hasDuplicatedNodes) { // for Silvia because she doesnt have
            // the immunization info
            nodeGraph.setDuplicatedNodes(node.getNodeId(), rootNode.getNodeId(), 1, nodeIdToEC50, nodeIdToComment1,
                    nodeIdToComment2);
        }
        nodeGraph.processGraphicOptions();
        fromIdToNodeGraph.put(nodeGraph.getNodeId(), nodeGraph);
        allNodeGraphs.add(nodeGraph);
        if (hasDuplicatedNodes) {
            ArrayList<NodeGraph> dupNodes = nodeGraph.getDuplicatedNodes();
            for (int i = 0; i < dupNodes.size(); i++) {
                allNodeGraphs.add(dupNodes.get(i));
            }
        }
        if (node.getChildrenForXmlFile() != null) {
            ArrayList<Node> children = node.getChildrenForXmlFile();
            for (int i = 0; i < children.size(); i++) {
                Node nod = children.get(i);
                createANodeGraph(nod, false);
            }
        }
    }

    public void setChildrenForGraphNode(Node node) { // we also set the parent
        if (node.getChildrenForXmlFile() != null) {
            String nodeId = node.getNodeId();
            //logger.debug("Processing GRAPH NODE nodeId " + nodeId);
            String[] nodeIds = nodeId.split(",");
            if (nodeIds.length > 1) {
                nodeId = nodeIds[0];
            }
            NodeGraph nodeGraph = fromIdToNodeGraph.get(nodeId);
            ArrayList<Node> children = node.getChildrenForXmlFile();
            ArrayList<NodeGraph> childrenGraph = new ArrayList<NodeGraph>();
            for (int i = 0; i < children.size(); i++) {
                Node child = children.get(i);
                String childId = child.getNodeId();
                String[] ids = childId.split(",");
                if (ids.length > 1) {
                    childId = ids[0];
                }
                NodeGraph childGraph = fromIdToNodeGraph.get(childId);
                //logger.debug("childId is " + childId);
                // logger.debug("childGraph is " + childGraph.toString());
                childrenGraph.add(childGraph);
                childGraph.setParent(nodeGraph);
                setChildrenForGraphNode(child);
            }
            nodeGraph.setChildren(childrenGraph);
        }
    }

    public void setNodesbyLevel() {
        for (int i = 0; i < allNodeGraphs.size(); i++) {
            NodeGraph node = allNodeGraphs.get(i);
            if (!node.isADuplicateNode()) {
                Integer level = node.getLevel();
                ArrayList<NodeGraph> nodesAtThisLevel = new ArrayList<NodeGraph>();
                if (fromLevelToNodes.containsKey(level)) {
                    nodesAtThisLevel = fromLevelToNodes.get(level);
                }
                nodesAtThisLevel.add(node);
                fromLevelToNodes.put(level, nodesAtThisLevel);
                // logger.debug("level "+level);
            }
        }
    }

    public void setChildrenPosition() {
        for (int i = 0; i < allNodeGraphs.size(); i++) {
            NodeGraph node = allNodeGraphs.get(i);
            if (!node.isADuplicateNode()) {
                ArrayList<NodeGraph> children = new ArrayList<NodeGraph>();
                for (int j = 0; j < children.size(); j++) {
                    NodeGraph child = children.get(j);
                    child.setXPosition(j + 1);
                }
            }
        }
    }

    private void setYears(NodeGraph nodeGraph, String immunizationInfo) {
        String[] immuInfos = immunizationInfo.split("// ");
        // logger.debug("After split, lenght is " + immuInfos.length);
        int index = 0;
        for (String immuInfo : immuInfos) {
            // logger.debug("immuInfo:" + immuInfo);
            if (index > 0) {// because the first index is null
                String[] splitt = immuInfo.split("\\s");
                String nodeId = splitt[0];
                String immu = splitt[1];
                String[] splitted = immu.split("\\s");
                for (int i = 0; i < splitted.length; i++) {
                    @SuppressWarnings("resource")
                    Scanner s = new Scanner(splitted[i]);
                    if (splitted[i].matches("post(\\d+)")) {
                        s.useDelimiter("post");
                        if (s.hasNextInt()) {
                            String postImmunizationYear = String.valueOf(s.nextInt());
                            String year = "20";
                            if (postImmunizationYear.length() == 1) {
                                year += "0";
                            }
                            year += postImmunizationYear;
                            if (nodeId.equals(nodeGraph.getNodeId())) {
                                nodeGraph.setYear(Integer.valueOf(year));
                            } else {
                                nodeGraph.setYearForDuplicatedNode(nodeId, Integer.valueOf(year));
                            }
                            years.add(Integer.valueOf(year));
                        }
                    } else if (splitted[i].matches(".*pre(\\d+)")) {
                        s.useDelimiter("pre");
                        if (s.hasNextInt()) {
                            String preImmunizationYear = String.valueOf(s.nextInt());
                            String year = "20";
                            if (preImmunizationYear.length() == 1) {
                                year += "0";
                            }
                            year += preImmunizationYear;
                            if (nodeId.equals(nodeGraph.getNodeId())) {
                                nodeGraph.setYear(Integer.valueOf(year));
                            } else {
                                nodeGraph.setYearForDuplicatedNode(nodeId, Integer.valueOf(year));
                            }
                            years.add(Integer.valueOf(year));
                        }
                    }
                }
            }
            index++;
        }
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
    }

    /**
     * @return the allNodeGraphs
     */
    public ArrayList<NodeGraph> getAllNodeGraphs() {
        return allNodeGraphs;
    }

    /**
     * @return the years
     */
    public ArrayList<Integer> getYears() {
        return years;
    }

    /**
     * @return the xmlFilePath
     */
    public String getXmlFilePath() {
        return xmlFilePath;
    }

    /**
     * @return the isDNA
     */
    public boolean isDNA() {
        return isDNA;
    }

    /**
     * @param isDNA the isDNA to set
     */
    public void setDNA(boolean isDNA) {
        this.isDNA = isDNA;
    }

    public void setHasReadsInId(boolean hasReadsInId) {
        this.hasReadsInId = hasReadsInId;
    }

    public boolean hasReadsInId() {
        return hasReadsInId;
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
}
