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
package ch.irb.IgGenealogicTreeViewer.AncesTreeConverter;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import ch.irb.IgGenealogicTreeMaker.Ig;
import ch.irb.IgGenealogicTreeMaker.NodesSorting;
import ch.irb.IgGenealogicTreeViewer.IgTreeViewerFrame;
import ch.irb.ManageFastaFiles.FastaFormatException;
import ch.irb.imgt.ProcessIMGTData;
import ch.irb.nodes.Node;

@XmlRootElement(namespace = "ch.irb.IgGenealogicTreeMaker")
public class DnamlOutputParser {

    static Logger logger = Logger.getLogger(DnamlOutputParser.class);
	static String fs = System.getProperty("file.separator");
    static String ls = System.getProperty("line.separator");

    /**
     * The project name.
     */
    @XmlAttribute(name = "projectName")
    private String projectName = "test_dnaml";

    /**
     * The root node.
     */
    @XmlElement(name = "GermLine")
    public Node rootNode = null;

    @XmlElement(name = "previousBPNames")
    public String previousBPNames = "";

    @XmlElement(name = "isDNA")
    public boolean isDNA = true;

    @XmlElement(name = "hasReadsInId")
    private boolean hasReadsInId = false;

    /**
     * The ig tree_xml.
     */
    @XmlTransient
    private String igTree_xml = "";

    private ArrayList<String> bpIsIg = new ArrayList<>();
    private HashMap<String, String> idToSequence = new HashMap<>();
    private HashMap<Node, ArrayList<Ig>> nodeToIgsList = new HashMap<>();
    private HashMap<String, Node> sequenceToNode = new HashMap<>();
    private HashMap<String, ArrayList<String>> sequenceToIds = new HashMap<>();
    private ArrayList<Node> nodes = new ArrayList<>();
    private File dnamlOutputFile;
    private boolean isImgtFormatLoaded = false;
    private IgTreeViewerFrame igTreeViewerFrame;

    public static void main(String[] args) {
        try {
            @SuppressWarnings("unused")
            DnamlOutputParser parser = new DnamlOutputParser(new File("clone1_outFile.txt"), null,
                    new IgTreeViewerFrame(), false);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (HeadlessException e) {
            e.printStackTrace();
        } catch (FastaFormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Instantiates a new ig tree maker.
     */
    public DnamlOutputParser() {
    }

    public DnamlOutputParser(File dnamlOutputFile, File IMGTFile, IgTreeViewerFrame igTreeViewerFrame,
                             boolean hasReadsInId) throws IOException, JAXBException, SAXException, FastaFormatException {
        this.igTreeViewerFrame = igTreeViewerFrame;
        this.dnamlOutputFile = dnamlOutputFile;
        this.hasReadsInId = hasReadsInId;
        // logger.debug("Parse file " + dnamlOutputFile.getName());
        // We create a project by taking the name of the dnaml output file
        projectName = dnamlOutputFile.getName().replaceAll("\\..*", "");
        // create the directory if it doesnt exist
        File dir = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        else{ //we add a number
            int i =2;
            while (dir.exists()){
                if (projectName.matches(".*_\\d+")) {
                    int indexUnderscore = projectName.lastIndexOf("_");
                    projectName=projectName.substring(0,indexUnderscore);
                }
                projectName +="_"+i;
                dir = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName);
                i++;
            }
            dir.mkdir();
        }
        String error = checkIfValidInput();
        if (error != null) {
            // here the related JFrame has to be passed in arguments!!!
            JOptionPane.showMessageDialog(igTreeViewerFrame, error, "Invalid dnaml output file",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // we store the IMGT info
        if (IMGTFile != null) {
            isImgtFormatLoaded = true;
        }
        storeSequences();
        rebuildTree();
        // In the case of insertion/deletion, if there is information about the
        // CDR/FR regions we change them
        // and we set these regions for the BPs sequences as well
        if (isImgtFormatLoaded) {
            try {
                @SuppressWarnings("unused")
                ProcessIMGTData process = new ProcessIMGTData(IMGTFile, nodes);
            } catch (Exception e) {
                // get error from IMGT file parser
                JOptionPane.showMessageDialog(igTreeViewerFrame, e.getMessage(), "Wrong IMGT data sequence",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (isImgtFormatLoaded) {
            // we sort the Ig like it will appear into the alignment
            NodesSorting nodesSorting = new NodesSorting(nodes);
            ArrayList<Node> sortedNodes = nodesSorting.getOrderedNodes();
            createFileForBaselineWithCDR3(sortedNodes);
            createFileForBaselineWithoutCDR3(sortedNodes);
        }

        HashMap<String, String> fastaIdToSeq = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isRoot() && !node.isBP()) {
                fastaIdToSeq.put(node.getNodeId(), node.getSequence());
            }
        }
        createXMLFile();
        //not necessary for the release
        //createMatrixTreeFile();
        launchGUI2();
    }

    /*
     * First we check that the dnaml output file is valid: the tree needs to be
     * rooted (with UCA) and the intermediate nodes (BPs) have to be
     * reconstructed in order to get all sequences
     */
    private String checkIfValidInput() throws IOException {
        boolean isARootedTree = false;
        boolean hasTheReconstructedSequences = false;
        String error = null;
        @SuppressWarnings("resource")
        BufferedReader fileReader = new BufferedReader(new FileReader(dnamlOutputFile.getPath()));
        String line;
        while ((line = fileReader.readLine()) != null) {
            // logger.debug(line);
            if (line.contains("(although rooted by outgroup)")) {
                isARootedTree = true;
            }
            if (line.contains("Probable sequences at interior nodes:")) {
                hasTheReconstructedSequences = true;
            }
        }
        if (!isARootedTree) {
            error = "The dnaml output file is not valid, the tree is not rooted by outgroup (missing UCA)!";
        } else if (!hasTheReconstructedSequences) {
            error = "The dnaml output file is not valid, the reconstructed sequences (BPs) are not present in the file.";
        }
        return error;
    }

    /*
     * Then we get all sequences, compare them if they are some identical
     */
    private void storeSequences() throws IOException {
        @SuppressWarnings("resource")
        BufferedReader fileReader = new BufferedReader(new FileReader(dnamlOutputFile.getPath()));
        String line;
        boolean getReconstructedSequences = false;
        while ((line = fileReader.readLine()) != null) {
            if (line.contains("Reconstructed sequence")) {
                getReconstructedSequences = true;
            } else if (getReconstructedSequences && line.trim().length() > 1) { // we
                // get
                // the
                // sequences
                String[] cells = line.trim().split("\\s{2,}");
                String id = cells[0];
                if (id.matches("\\d+")) {
                    id = "BP" + id;
                }
                String sequence = cells[1].replaceAll("\\s", "").toUpperCase();
                String seq = "";
                if (idToSequence.containsKey(id)) {
                    seq = idToSequence.get(id);
                }
                seq += sequence;
                idToSequence.put(id, seq);
            }
        }
        // Store the ids per sequence
        for (String id : idToSequence.keySet()) {
            String seq = idToSequence.get(id);
            ArrayList<String> ids = new ArrayList<>();
            if (sequenceToIds.containsKey(seq)) {
                ids = sequenceToIds.get(seq);
            }
            ids.add(id);
            sequenceToIds.put(seq, ids);
        }

        // check if inside the ids, there is a BP that we will replace by an
        // existing IG in the tree
        for (String seq : sequenceToIds.keySet()) {
            ArrayList<String> ids = sequenceToIds.get(seq);
            for (String id : ids) {
                if (id.matches("BP\\d+") && ids.size() > 1) { // it's a number =
                    // BP
                    //logger.debug("This BP: " + id + " will be replaced by an Ig , ids are " + ids);
                    bpIsIg.add(id);
                }
            }

        }
    }

    /*
     * Finally we reconstruct the tree using the 'table' in the output file, no
     * need to have the newick format
     */
    private void rebuildTree() throws IOException {
        @SuppressWarnings("resource")
        BufferedReader fileReader = new BufferedReader(new FileReader(dnamlOutputFile.getPath()));
        String line;
        boolean getTable = false;
        int index = 0;
        while ((line = fileReader.readLine()) != null) {
            if (line.contains("-------        ---            ------      ------- ---------- ------")) {
                getTable = true;
            } else if (getTable && line.length() > 1) {
                if (line.contains("significantly positive")) {
                    getTable = false;
                } else {
                    String[] cells = line.trim().split("\\s{2,}");
                    if (cells[0].matches("\\d+")) {
                        cells[0] = "BP" + cells[0];
                    }
                    if (cells[1].matches("\\d+")) {
                        cells[1] = "BP" + cells[1];
                    }
                    String parentId = cells[0];
                    String childId = cells[1];
                    boolean isRoot = false;
                    if (index == 0) {// the first line is an exception because
                        // it is a rooted tree!
                        parentId = cells[1];
                        childId = cells[0];
                        isRoot = true;
                        logger.debug("-----------------------SET ROOT----------" + parentId);
                    }
                    Node parentNode = processNode(parentId, isRoot);
                    if (isRoot) {
                        rootNode = parentNode;
                        //logger.debug("This node is the root " + rootNode.getNodeId());
                    }
                    Node childNode = processNode(childId, false);
                    String parentSeq = idToSequence.get(parentId);
                    String childSeq = idToSequence.get(childId);
                    if (!parentSeq.equals(childSeq)) {
                        // the 2 sequences are not identical, we can set the parental
                        // relationship between them
                        // if this child doesnt exist yes (case of duplicate
                        // sequences) BUG fixed the 20.03.15
                        boolean isAlreadyHere = false;
                        for (Node k : parentNode.getChildren()) {
                            if (k.getNodeId().equals(childNode.getNodeId())) {
                                //logger.debug("child already stored: " + k.getNodeId());
                                isAlreadyHere = true;
                            }
                        }
                        if (!isAlreadyHere) {
                            parentNode.addChild(childNode);
                            childNode.setParent(parentNode);
                            // logger.debug("Parent " + parentNode.getNodeId() +
                            // " and child " + childNode.getNodeId());
                        }
                    }
                    // logger.debug("Parent " + parentId + " and child " +
                    // childId);
                    index++;
                }
            }
        }
        // we set the levels of the nodes
        setLevelForChildren(rootNode);
        // we add the list of Igs for each node
        for (Node node : nodes) {
            if (nodeToIgsList.containsKey(node)) {
                ArrayList<Ig> igsList = nodeToIgsList.get(node);
                node.setRelatedIgs(igsList);
            }
        }

        // Then we check if there are some R or Y in the sequence

        ArrayList<Node> nodesToCheck = new ArrayList<>();
        nodesToCheck.addAll(nodes);
        for (Node node : nodesToCheck) {
            String sequence = idToSequence.get(node.getNodeId());
            boolean setSeq = true;
            // TODO to comment for the release but to keep to use in-house
            // bug fixed the 28.01.16.
            //OFF
            boolean seqToCheck = false;
			if (sequence.contains("R") || sequence.contains("Y") || sequence.contains("S") || sequence.contains("M")
					|| sequence.contains("K") || sequence.contains("W")) {
				sequence = processSpecialNuc(node);
                System.out.println("Changing UPAC nuc for node "+node.getNodeId());
				seqToCheck = true;
			}

            // Finally we check that this modified node sequence is not an existing one
            if (seqToCheck) {
                for (Node otherNode : nodesToCheck) {
                    if (!otherNode.getNodeId().equals(node.getNodeId())) {
                        String seq = idToSequence.get(otherNode.getNodeId());
                        if (seq.equals(sequence)) {
                            logger.warn("!!!!!! By replacing a R/Y nuc we got the same sequence for " + node.getNodeId()
                                    + " and " + otherNode.getNodeId());
                            // we remove this BP if its parent or its child has
                            // the same sequence
                            if (node.getNodeId().equals(otherNode.getParent().getNodeId())) {
                                // BP is  the parent
                                ArrayList<Node> kids = node.getChildren();
                                for (Node kid : kids) {
                                    if (!kid.getNodeId().equals(otherNode.getNodeId())) {
                                        otherNode.addChild(kid);
                                        kid.setParent(otherNode);
                                    }
                                }
                                Node par = node.getParent();
                                par.removeChild(node);
                                par.addChild(otherNode);
                                nodes.remove(node);
                                logger.warn(
                                        "... we remove " + node.getNodeId() + " since his child has the same sequence");
                                setSeq = false;
                            } else if (otherNode.getNodeId().equals(node.getParent().getNodeId())) {
                                // BP is the child
                                ArrayList<Node> kids = node.getChildren();
                                for (Node kid : kids) {
                                    otherNode.addChild(kid);
                                    kid.setParent(otherNode);
                                }
                                otherNode.removeChild(node);
                                nodes.remove(node);
                                logger.warn("... we remove " + node.getNodeId()
                                        + " since his parent has the same sequence");
                                setSeq = false;
                            }
                        }
                    }
                }
            }

            if (setSeq) {
                // we set the sequence we set the sequence for good (to avoid
                // translation problem when we have a R/Y)
                node.setSequence(sequence);
                // we set the information for good
                node.setInformationForTree();
            }

        }

        // we change the id in the case we have identical sequences
        for (Node node : nodes) {
            ArrayList<Ig> igsList = node.getRelatedIgs();
            if (igsList.size() > 1) {
                String newId = "";
                for (Ig ig : igsList) {
                    newId += ", " + ig.getFastaId();
                }
                // to delete the first ','
                //logger.debug("changing id " + node.getNodeId() + " with " + newId.substring(2));
                node.setNodeId(newId.substring(2));
            }
        }
    }

    @SuppressWarnings("unused")
    private int getNodeLevel(Node node, int level) {
        // 0 is the GL level
        Node parent = node.getParent();
        if (parent.equals(rootNode)) {
            return level;
        } else {
            return getNodeLevel(parent, level += 1);
        }
    }

    /*
     * This method is used to calculated the number of A/G or T/C at a given
     * position for all the children of a node
     */
    private int[] getNucNumber(ArrayList<Node> children, int position, char tempNuc) {
        int nuc1Number = 0;
        int nuc2Number = 0;
        char nuc1 = 'A';
        char nuc2 = 'G';
        if (tempNuc == 'Y') {
            nuc1 = 'T';
            nuc2 = 'C';
        } else if (tempNuc == 'S') {
            nuc1 = 'G';
            nuc2 = 'C';
        } else if (tempNuc == 'M') {
            nuc1 = 'A';
            nuc2 = 'C';
        } else if (tempNuc == 'K') {
            nuc1 = 'G';
            nuc2 = 'T';
        } else if (tempNuc == 'W') {
            nuc1 = 'A';
            nuc2 = 'T';
        }
        for (Node child : children) {
            String sequence = idToSequence.get(child.getNodeId());
            char childNuc = sequence.charAt(position);
            if (childNuc == nuc1) {
                nuc1Number++;
            } else if (childNuc == nuc2) {
                nuc2Number++;
            }
            ArrayList<Node> childrenOfChild = child.getChildren();
            if (childrenOfChild.size() > 0) {
                int[] childNucNumber = getNucNumber(childrenOfChild, position, tempNuc);
                nuc1Number += childNucNumber[0];
                nuc2Number += childNucNumber[1];
            }
        }
        int[] nucNumber = {nuc1Number, nuc2Number};
        return nucNumber;
    }

    private Node processNode(String id, boolean isRoot) {
        // logger.info("Process node: " + id);
        String sequence = idToSequence.get(id);
        Node node = null;
        // This is the case where a BP has the same sequence than an Ig
        if (bpIsIg.contains(id)) {
            if (sequenceToNode.containsKey(sequence)) {
                node = sequenceToNode.get(sequence);
            } else {
                node = new Node();
                node.setDNA(true);
                // node.setSequence(sequence);
                // To set the nodeId we take the first Ig on the list which is
                // not a BP
                ArrayList<String> ids = sequenceToIds.get(sequence);
                String nodeId = null;
                for (String relatedId : ids) {
                    if (!relatedId.matches("BP\\d+")) { // "BP\\d+"
                        nodeId = relatedId;
                        break;
                    }
                }
                // in the case where a BP is equal to another BP but not a Ig
                if (nodeId == null) {
                    nodeId = ids.get(0); // we set with the first id
                }
                node.setNodeId(nodeId);
                sequenceToNode.put(sequence, node);
                // we create an Ig and link it to the node
                // logger.debug("Set nodeId: " + nodeId + " with seq " +
                // sequence);
                Ig ig = new Ig(nodeId, sequence);
                ig.setAlignedSequence(sequence);
                ArrayList<Ig> igsList = new ArrayList<>();
                if (nodeToIgsList.containsKey(node)) {
                    igsList = nodeToIgsList.get(node);
                }
                igsList.add(ig);
                nodeToIgsList.put(node, igsList);

            }
        } else {// This node is not a BP identical to an Ig
            if (sequenceToNode.containsKey(sequence)) { // we already have a
                // node with this
                // sequence
                node = sequenceToNode.get(sequence);
                // logger.debug("For " + id + " we already have " +
                // node.getNodeId() + " with the same sequence");
                // we create an Ig and link it to the node if we dont have it
                // yet!
                Ig ig = new Ig(id, sequence);
                ig.setAlignedSequence(sequence);
                ArrayList<Ig> igsList = new ArrayList<>();
                if (nodeToIgsList.containsKey(node)) {
                    igsList = nodeToIgsList.get(node);
                }
                boolean alreadyStored = false;
                for (Ig i : igsList) {
                    if (i.getFastaId().equals(id)) {
                        alreadyStored = true;
                    }
                }
                if (!alreadyStored) {
                    igsList.add(ig);
                }
                nodeToIgsList.put(node, igsList);
            } else { // the root will always go here first
                // logger.debug("Create NODE: " + id);
                node = new Node();
                node.setSequence(sequence);
                node.setNodeId(id);
                node.setDNA(true);
                node.setRoot(isRoot);
                sequenceToNode.put(sequence, node);

                // we create an Ig and link it to the node if it's not a BP!
                if (!id.matches("BP\\d+")) {
                    Ig ig = new Ig(id, sequence);
                    ArrayList<Ig> igsList = new ArrayList<>();
                    if (nodeToIgsList.containsKey(node)) {
                        igsList = nodeToIgsList.get(node);
                    }
                    igsList.add(ig);
                    nodeToIgsList.put(node, igsList);
                }
            }

        }
        // logger.debug("processed node: "+node.getNodeId());
        if (!nodes.contains(node)) {
            // logger.debug("...we add it to nodes array "+node.getNodeId());
            nodes.add(node);
        }
        return node;
    }

    private void createFileForBaselineWithCDR3(ArrayList<Node> sortedNodes) {
        // Here we create an output file to get the positive/negative selection
        try {
            String clip_out = System.getProperty("user.dir") + fs + "output" + fs + projectName + fs + projectName
                    + "_BASELINe_input.fasta";
            FileWriter fstream = new FileWriter(clip_out);
            BufferedWriter out = new BufferedWriter(fstream);

            // first we write the UCA
            String rootSequence = rootNode.getImgtFormatSequence();
            String rootId = rootNode.getNodeId();
            out.write(">>" + rootId + System.getProperty("line.separator") + rootSequence
                    + System.getProperty("line.separator"));
            // for (Integer dist : dists) {
            // logger.warn("DIST "+dist);
            // ArrayList<Node> nodesWithThisDist =
            // sortedNodesByDistance.get(dist);
            // for (Node node : nodesWithThisDist) {
            for (Node node : sortedNodes) {
                if (!node.isRoot()) {
                    String sequence = node.getImgtFormatSequence();
                    if (sequence == null) { // BP case!
						if (!node.hasDeletion() && !rootNode.hasDeletion()) {
							sequence = getImgtFormattedSequence(rootSequence, node.getSequence());
						}
                    }
					if (sequence != null) {
						out.write(">" + node.getNodeId() + System.getProperty("line.separator") + sequence
								+ System.getProperty("line.separator"));
					}
                }
            }
            // }
            // Close the output stream
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createFileForBaselineWithoutCDR3(ArrayList<Node> sortedNodes) {
        // Here we create an output file to get the positive/negative selection
        try {
            String clip_out = System.getProperty("user.dir") + fs + "output" + fs + projectName + fs + projectName
                    + "_BASELINe_without_CDR3_input.fasta";
            FileWriter fstream = new FileWriter(clip_out);
            BufferedWriter out = new BufferedWriter(fstream);
            // first we write the UCA
            String rootSequence = rootNode.getImgtFormatSequenceWithoutCDR3();
            String rootId = rootNode.getNodeId();
            out.write(">>" + rootId + System.getProperty("line.separator") + rootSequence
                    + System.getProperty("line.separator"));
            for (Node node : sortedNodes) {
                if (!node.isRoot()) {
                    String sequence = node.getImgtFormatSequenceWithoutCDR3();
                    if (sequence == null) { // BP case!
						if (!node.hasDeletion() && !rootNode.hasDeletion()) {
							sequence = getImgtFormattedSequence(rootSequence, node.getSequence());
						}
                    }
					if (sequence != null) {
						out.write(">" + node.getNodeId() + System.getProperty("line.separator") + sequence
								+ System.getProperty("line.separator"));
					}
                }
            }
            // Close the output stream
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getImgtFormattedSequence(String rootSequenceImgtFormatted, String sequence) {
        String imgtSequence = "";
        int index = 0;
        for (int i = 0; i < rootSequenceImgtFormatted.length(); i++) {
            String nuc = String.valueOf(rootSequenceImgtFormatted.charAt(i));
			if (!nuc.equals(".")) {
				imgtSequence += String.valueOf(sequence.charAt(index));
				index++;
			} else {
				imgtSequence += ".";
			}
        }
        return imgtSequence;
    }

    public void setIgTree_xml(String xmlName) {
        this.igTree_xml = xmlName;
    }

    @XmlTransient
    public String getIgTree_xml() {
        return igTree_xml;
    }

    /**
     * Creates the xml file.
     *
     * @throws JAXBException the jAXB exception
     * @throws IOException   Signals that an I/O exception has occurred.
     * @throws SAXException  the sAX exception
     */
    private void createXMLFile() throws JAXBException, IOException, SAXException {

        setIgTree_xml(System.getProperty("user.dir") + fs + "output" + fs + projectName + fs + projectName + ".xml");

        // create JAXB context and instantiate marshaller
        JAXBContext context = JAXBContext.newInstance(InputParser.class); //
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        Writer w = null;
        try {
            w = new FileWriter(getIgTree_xml());
            m.marshal(this, w);
        } finally {
            try {
                w.close();
            } catch (Exception e) {
            }
        }
    }

    private void createMatrixTreeFile()throws IOException{
        File matrixFile = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName + fs + projectName + "_matrixTree.tsv");
        BufferedWriter out = new BufferedWriter(new FileWriter(matrixFile));
        ArrayList<Node> orderedNodes = new ArrayList<>();
        writeNodeAndChildrenIds(out,rootNode,orderedNodes);
        out.write(ls);
        for (Node node: orderedNodes){
            out.write(node.getNodeId());
            ArrayList<Node> children = node.getChildren();
            //determine the relation
            for (Node node2: orderedNodes){
                int edge=0;
                for (Node kid: children){
                    if (kid.getNodeId().equals(node2.getNodeId())){
                        edge = kid.getNumberOfNucMutationsWithParent();
                        break;
                    }
                }
                out.write("\t"+edge);
            }
            out.write(ls);
        }
        out.close();
    }

    private void writeNodeAndChildrenIds(BufferedWriter out, Node node,ArrayList<Node> orderedNodes) throws IOException {
        orderedNodes.add(node);
        if (!node.isRoot()){
            out.write("\t");
        }
        out.write(node.getNodeId());
        for (Node kid :node.getChildren()){
            writeNodeAndChildrenIds(out,kid,orderedNodes);
        }
    }


    private void launchGUI2() {
        // here we update the IgTreeViewerFrame
        igTreeViewerFrame.updateIgTreeViewerFrame(igTree_xml);
    }

    public String getProjectName() {
        return projectName;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public String getPreviousBPNames() {
        return null;
    }

    public boolean isDNA() {
        return isDNA;
    }

    public boolean hasReadsInId() {
        return hasReadsInId;
    }

    private void setLevelForChildren(Node node) {
        ArrayList<Node> children = node.getChildren();
        for (int c = 0; c < children.size(); c++) {
            Node child = children.get(c);
            child.setLevel(child.getParent().getLevel() + 1);
            if (child.getChildren().size() > 0) {
                setLevelForChildren(child);
            }
        }
    }

    /*
     * R A or G Y C or T S G or C W A or T K G or T M A or C
     */
    @SuppressWarnings("unused")
    private String processSpecialNuc(Node node) {
        // get the position
        int position = 0;
        String sequence = idToSequence.get(node.getNodeId());
        String newSequence = null;
        ArrayList<Node> children = node.getChildren();
        for (char nuc : sequence.toCharArray()) {
            String replacedNuc = null;
            if (nuc == 'R') { // it is a A or G
                int[] nucNumber = getNucNumber(children, position, nuc);
                int Anumber = nucNumber[0];
                int Gnumber = nucNumber[1];
                // logger.debug("!!! For node " + node.getNodeId() + ", at
                // position: " + position + " we got a " + nuc);
                // logger.debug("In the children, we have number of A " +
                // Anumber + " and number of G " + Gnumber);
                if (Anumber > Gnumber) {
                    replacedNuc = "A";
                } else {
                    replacedNuc = "G";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                // logger.debug("--> previous sequence was: " + sequence + " and
                // new one is " + newSequence);
                sequence = newSequence;
            } else if (nuc == 'Y') { // it is a T or C
                int[] nucNumber = getNucNumber(children, position, nuc);
                int Tnumber = nucNumber[0];
                int Cnumber = nucNumber[1];
                // logger.debug("!!! For node " + node.getNodeId() + ", at
                // position: " + position + " we got a " + nuc);
                // logger.debug("In the children, we have number of T " +
                // Tnumber + " and number of C " + Cnumber);
                if (Tnumber > Cnumber) {
                    replacedNuc = "T";
                } else {
                    replacedNuc = "C";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                // logger.debug("--> previous sequence was: " + sequence + " and
                // new one is " + newSequence);
                sequence = newSequence;
            } else if (nuc == 'S') { // it is a G or C
                int[] nucNumber = getNucNumber(children, position, nuc);
                int Gnumber = nucNumber[0];
                int Cnumber = nucNumber[1];
                if (Gnumber > Cnumber) {
                    replacedNuc = "G";
                } else {
                    replacedNuc = "C";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                sequence = newSequence;
            } else if (nuc == 'M') { // it is a A or C
                int[] nucNumber = getNucNumber(children, position, nuc);
                int Anumber = nucNumber[0];
                int Cnumber = nucNumber[1];
                if (Anumber > Cnumber) {
                    replacedNuc = "A";
                } else {
                    replacedNuc = "C";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                sequence = newSequence;
            } else if (nuc == 'K') { // it is a G or T
                int[] nucNumber = getNucNumber(children, position, nuc);
                int Gnumber = nucNumber[0];
                int Tnumber = nucNumber[1];
                if (Gnumber > Tnumber) {
                    replacedNuc = "G";
                } else {
                    replacedNuc = "T";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                sequence = newSequence;
            } else if (nuc == 'W') { // it is a A or T
                int[] nucNumber = getNucNumber(children, position, nuc);
                int Anumber = nucNumber[0];
                int Tnumber = nucNumber[1];
                if (Anumber > Tnumber) {
                    replacedNuc = "A";
                } else {
                    replacedNuc = "T";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                sequence = newSequence;
            }
            // logger.debug("--> previous sequence was: "+sequence);

            position++;
        }
        return newSequence;
    }

}
