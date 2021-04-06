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

   *  This class will take in input 2 kind of trees: one made by dnaml from Phylip package and one made by IgPhyML
   * (immancantation workflow)
 */
package ch.irb.IgGenealogicTreeViewer.AncesTreeConverter;

import ch.irb.IgGenealogicTreeMaker.Ig;
import ch.irb.IgGenealogicTreeMaker.NodesSorting;
import ch.irb.IgGenealogicTreeViewer.IgTreeViewerFrame;
import ch.irb.IgGenealogicTreeViewer.airr.IgPhyMLParser;
import ch.irb.IgGenealogicTreeViewer.airr.TsvAirrParser;
import ch.irb.ManageFastaFiles.FastaFileParser;
import ch.irb.ManageFastaFiles.FastaFormatException;
import ch.irb.IgGenealogicTreeViewer.airr.ProcessAIRRdata;
import ch.irb.imgt.ProcessIMGTData;
import ch.irb.nodes.Node;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XmlRootElement(namespace = "ch.irb.IgGenealogicTreeMaker")
public class InputParser {

    static Logger logger = Logger.getLogger(InputParser.class);
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
    private File airrInputFile;
    private File igPhyMLfile;
    private File igPhyMLfastafile;
    private boolean isImgtFormatLoaded = false;
    private IgTreeViewerFrame igTreeViewerFrame;
    private boolean isDnamlInput = true;
    private HashMap<String, String> originalBPidtoBPid = new HashMap<>();
    private TsvAirrParser tsvAirrParser;
    private IgPhyMLParser igPhyMLParser;
    private HashSet<String> nodeNames = new HashSet<>();

    public static void main(String[] args) {
        try {
            @SuppressWarnings("unused")
            //For dnaml input
            /*InputParser parser = new InputParser(new File("clone1_outFile.txt"), null,
                    new IgTreeViewerFrame(), false);*/
                    //For IgPhyML
                    InputParser parser = new InputParser(new File("C:\\Users\\mperez\\LocalDocuments" +
                    "\\RAW_files_toSendToServer\\IgPhyML\\example_airr.tsv"), new File("C:\\Users\\mperez\\LocalDocuments" +
                    "\\RAW_files_toSendToServer\\IgPhyML\\example_igphyml-pass.tab"), "1", new IgTreeViewerFrame());

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Instantiates a new ig tree maker.
     */
    public InputParser() {
    }

    /*
    This constructor is used in the case of dnaml output file +/- IMGT gapped format fasta file
     */
    public InputParser(File dnamlOutputFile, File IMGTFile, IgTreeViewerFrame igTreeViewerFrame,
                       boolean hasReadsInId) throws Exception {
        this.isDnamlInput = true;
        this.igTreeViewerFrame = igTreeViewerFrame;
        this.dnamlOutputFile = dnamlOutputFile;
        this.hasReadsInId = hasReadsInId;
        // //logger.debug("Parse file " + dnamlOutputFile.getName());
        // We create a project by taking the name of the dnaml output file
        projectName = dnamlOutputFile.getName().replaceAll("\\..*", "");
        // create the directory if it doesnt exist
        File dir = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName);
        if (!dir.exists()) {
            dir.mkdir();
        } else { //we add a number
            int i = 2;
            while (dir.exists()) {
                if (projectName.matches(".*_\\d+")) {
                    int indexUnderscore = projectName.lastIndexOf("_");
                    projectName = projectName.substring(0, indexUnderscore);
                }
                projectName += "_" + i;
                dir = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName);
                i++;
            }
            dir.mkdir();
        }
        String error = checkIfValidInputDnaml();
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
        storeSequencesDnaml();
        rebuildTree(null);
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
   This constructor is used in the case of an airr format tab file, generated by changeo + IgPhyML output + cloneId to process
    */
    public InputParser(File airrInputFile, File igPhyMLfile, String cloneId, IgTreeViewerFrame igTreeViewerFrame) throws Exception {
        this.isDnamlInput = false;
        this.igTreeViewerFrame = igTreeViewerFrame;
        this.airrInputFile = airrInputFile;
        this.igPhyMLfile = igPhyMLfile;
        igPhyMLfastafile = new File(igPhyMLfile.getParent() + System.getProperty("file.separator") + igPhyMLfile.getName()
                .replace(".tab", "_hlp_asr.fasta"));
        //logger.debug("CLONE ID: "+cloneId);
        // //logger.debug("Parse file " + dnamlOutputFile.getName());
        // We create a project by taking the name of the airr file + the cloneId
        projectName = igPhyMLfile.getName().replace("_igphyml-pass.tab", "") + "_cloneId" + cloneId;
        // create the directory if it doesnt exist
        File dir = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName);
        if (!dir.exists()) {
            dir.mkdir();
        } else { //we add a number
            int i = 2;
            while (dir.exists()) {
                if (projectName.matches(".*_\\d+")) {
                    int indexUnderscore = projectName.lastIndexOf("_");
                    projectName = projectName.substring(0, indexUnderscore);
                }
                projectName += "_" + i;
                dir = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName);
                i++;
            }
            dir.mkdir();
        }
        //
        String error = checkIfValidAirrIgPhyMLInput(cloneId);
        if (error != null) {
            // here the related JFrame has to be passed in arguments!!!
            JOptionPane.showMessageDialog(igTreeViewerFrame, error, "Invalid IgPhyML file",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        storeSequencesAirr(cloneId);
        rebuildTree(cloneId);
        boolean imgtInfo = true;
        try {
            // we set the FR/CDR regions using the AIRR data
            ProcessAIRRdata processAIRRdata = new ProcessAIRRdata(tsvAirrParser, cloneId, nodes);
        } catch (Exception e) {
            // get warning from IMGT file parser
            JOptionPane.showMessageDialog(igTreeViewerFrame, e.getMessage(), "No FR/CDR regions information",
                    JOptionPane.WARNING_MESSAGE);
            imgtInfo = false;
        }
        // we sort the Ig like it will appear into the alignment
        if (imgtInfo) {
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
    private String checkIfValidInputDnaml() throws IOException {
        boolean isARootedTree = false;
        boolean hasTheReconstructedSequences = false;
        String error = null;
        @SuppressWarnings("resource")
        BufferedReader fileReader = new BufferedReader(new FileReader(dnamlOutputFile.getPath()));
        String line;
        while ((line = fileReader.readLine()) != null) {
            // //logger.debug(line);
            if (line.contains("(although rooted by outgroup)")) {
                isARootedTree = true;
            }
            if (line.contains("Probable sequences at interior nodes:")) {
                hasTheReconstructedSequences = true;
            }
        }
        fileReader.close();
        if (!isARootedTree) {
            error = "The dnaml output file is not valid, the tree is not rooted by outgroup (missing UCA)!";
        } else if (!hasTheReconstructedSequences) {
            error = "The dnaml output file is not valid, the reconstructed sequences (BPs) are not present in the file.";
        }
        return error;
    }


    /*
     * First we check that the airr file is valid, that we have all info in the airr file + IgPhyML output file
     */
    private String checkIfValidAirrIgPhyMLInput(String cloneId) throws IOException {
        String error = null;

        //and in the IgPhyML file
        String newickTree = getNewickTree(cloneId);
        if (newickTree == null) {
            error = "There is no clone id " + cloneId + " in the IgPhyML file!";
        }

        getNodeNames(newickTree);
        //check that the cloneId exists in the AIRR file
        tsvAirrParser = new TsvAirrParser(airrInputFile, cloneId, nodeNames);
        if (!tsvAirrParser.getClonalFamToAIIRobjs().containsKey(cloneId)) {
            if (!tsvAirrParser.isCloneIdPresent()) {
                error = "There is no clone id " + cloneId + " in the change-o airr format file!";
            }
            else {
                error = "The clone id " + cloneId + " was found in the change-o airr format file but the sequence_ids " +
                        "do not fit with the ones in the related newick tree (Igphyml file)! " +
                        "\nPlease change sequence_ids accordingly.";
            }
        }

        return error;
    }

    /*
     * Get the newick tree for one clone
     */
    private String getNewickTree(String cloneId) throws IOException {
        igPhyMLParser = new IgPhyMLParser(igPhyMLfile);
        return igPhyMLParser.getNewickTree(cloneId);
    }

    private void getNodeNames(String newickTree) {
        //First we store the Ig that will be in the tree, not all of them because of the "--collapse" option!!!
        String newicktree = newickTree.replaceAll("\\\"", "");
        //logger.debug("TREE is: " + newicktree);
        BinaryTree<String> binaryTree = BinaryTree.parseNewick(newicktree);
        //logger.debug(binaryTree.toString());
        //logger.debug("///////////");
        String stringTree = binaryTree.toTreeRelationship();
        //logger.debug(stringTree);
        Scanner scanner = new Scanner(stringTree);
        scanner.useDelimiter("\\n");
        String firstbp = null;
        String uca = null;
        // first use a Scanner to get each line
        while (scanner.hasNext()) {
            String line = scanner.next();
            String[] cells = line.split(" ");
            if (line.contains("_GER")) {
                uca = line.trim();
                nodeNames.add(uca);
            } else if (firstbp == null && cells[0].length() < 1) {
                firstbp = line.trim();
                nodeNames.add(firstbp);
            } else {
                nodeNames.add(cells[0]);
                nodeNames.add(cells[1]);
            }
        }
        //logger.debug("First bp is " + firstbp);
        //logger.debug("UCA is " + uca);
        //logger.debug("\nNodes in the tree: " + nodeNames + "\n");
    }

    /*
     * Then we get all sequences, compare them if they are some identical
     */
    private void storeSequencesAirr(String cloneId) throws IOException {
        //Then we store the Ig sequences from the AIRR file
        //Here we have to be careful with indels. We will remove all '.' that are in commons in all sequences, and
        //replace the remaining ones by '-'.
        ArrayList<TsvAirrParser.AIRRobject> airrRobjects = tsvAirrParser.getAirrObjectsFromFamily(cloneId);
        HashMap<String, String> idToSeqToProcess = new HashMap<>();
        for (TsvAirrParser.AIRRobject airRobject : airrRobjects) {
            String sequenceId = airRobject.getSequence_id();
            //be careful here, to check it works well with indel sequences --> YES
            String seq = airRobject.getValue("sequence_alignment");
            idToSeqToProcess.put(sequenceId, seq);
        }

        //we store the shared dots '.'
        Map.Entry<String, String> entry = idToSeqToProcess.entrySet().iterator().next();
        String firstSeq = entry.getValue();
        int i = 0;
        ArrayList<Integer> pointsToRemove = new ArrayList<>();
        //fixed the 29.01.21 if all sequences have their first nuc missing it does not work (Jun data!)
        boolean begin=true;
        for (char n : firstSeq.toCharArray()) {
            if (n == '.') {
                boolean remove = true;
                for (String id : idToSeqToProcess.keySet()) {
                    if (idToSeqToProcess.get(id).charAt(i) != '.') {
                        remove = false;
                        break;
                    }
                }
                if (remove && !begin) {
                    pointsToRemove.add(i);
                }
            }
            else {
                begin=false;
            }
            i++;
        }

        //we reprocess the sequences: remove the shared dots and replace the other dots by '-'
        //If deletions are at the beginning of the sequence, we REMOVE them too (bug fixed the 12.05.20)
        String referenceSeq = null;
        for (String id : idToSeqToProcess.keySet()) {
            String seq = "";
            int in = 0;
            for (char n : idToSeqToProcess.get(id).toCharArray()) {
                if (n == '.' && !pointsToRemove.contains(in)) {
                    seq += "-";

                } else if (n != '.') {
                    seq += n;
                }
                in++;
            }
            idToSequence.put(id, seq);
            //logger.debug("STORE SEQ FOR "+id+" SEQ "+seq);
            if (referenceSeq == null) {
                referenceSeq = seq;
            }
        }

        //And finally we store the BP sequences
        FastaFileParser fastaFileParser = new FastaFileParser(igPhyMLfastafile);
        LinkedHashMap<String, String> idToSeq = fastaFileParser.getSameOrderFastaIdToSequence();
        for (String id : idToSeq.keySet()) {
            if (nodeNames.contains(id)) {
                String newID;
                if (id.contains("_GERM")) {
                    newID = "UCA";
                    originalBPidtoBPid.put(id, newID);
                } else {
                    newID = "BP" + id.split("_")[1];
                    originalBPidtoBPid.put(id, newID);
                }
                //Special case here, IgPhyML can add some nucleotides at the end, we remove them to have all sequences aligned!
                String seq = idToSeq.get(id);
                if (seq.length() > referenceSeq.length()) {
                    int endIndexToRemove = seq.length() - (seq.length() - referenceSeq.length());
                    seq = seq.substring(0, endIndexToRemove);
                }
                idToSequence.put(newID, seq);
            }
        }

        ////logger.debug("************************");
        // Store the ids per sequence
        for (String id : idToSequence.keySet()) {
            String seq = idToSequence.get(id);
            //logger.debug(id+": "+seq);
            ArrayList<String> ids = new ArrayList<>();
            if (sequenceToIds.containsKey(seq)) {
                ids = sequenceToIds.get(seq);
            }
            ids.add(id);
            sequenceToIds.put(seq, ids);
        }
        ////logger.debug("************************");
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
     * Then we get all sequences, compare them if they are some identical
     */
    private void storeSequencesDnaml() throws IOException {
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
        fileReader.close();
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
    private void rebuildTree(String cloneId) throws Exception {

        if (isDnamlInput) {
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
                            //logger.debug("-----------------------SET ROOT----------" + parentId);
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
                                // //logger.debug("Parent " + parentNode.getNodeId() +
                                // " and child " + childNode.getNodeId());
                            }
                        }
                        // //logger.debug("Parent " + parentId + " and child " +
                        // childId);
                        index++;
                    }
                }
            }
            fileReader.close();
        }

        //parse newick tree from IgPhyML
        else {
            String newicktree = igPhyMLParser.getNewickTree(cloneId).replaceAll("\\\"", "");
            //logger.debug("TREE is: " + newicktree);
            BinaryTree<String> binaryTree = BinaryTree.parseNewick(newicktree);
            String stringTree = binaryTree.toTreeRelationship();
            //logger.debug(stringTree);
            String firstbp = null;
            //we set the UCA first
            String uca = getUCA(stringTree);
            if (originalBPidtoBPid.containsKey(uca)) {
                uca = originalBPidtoBPid.get(uca);
            }
            //logger.debug("We get the UCA: " + uca);
            Node parNode = processNode(uca, true);
            rootNode = parNode;
            // first use a Scanner to get each line
            Scanner scanner = new Scanner(stringTree);
            scanner.useDelimiter("\\n");
            while (scanner.hasNext()) {
                String line = scanner.next();
                String[] cells = line.split(" ");
                if (!(line.contains("_GER") && cells[0].length() < 1)) {
                    if (firstbp == null && cells[0].length() < 1) {
                        firstbp = line.trim();
                        if (originalBPidtoBPid.containsKey(firstbp)) {
                            firstbp = originalBPidtoBPid.get(firstbp);
                        }
                        //logger.debug("We set relationship with BP1: "+firstbp);
                        Node childNode = processNode(firstbp, false);
                        String parentSeq = idToSequence.get(uca);
                        String childSeq = idToSequence.get(firstbp);
                        if (!parentSeq.equals(childSeq)) {
                            // the 2 sequences are not identical, we can set the parental
                            // relationship between them
                            // if this child doesnt exist yes (case of duplicate
                            // sequences) BUG fixed the 20.03.15
                            boolean isAlreadyHere = false;
                            for (Node k : parNode.getChildren()) {
                                if (k.getNodeId().equals(childNode.getNodeId())) {
                                    //logger.debug("child already stored: " + k.getNodeId());
                                    isAlreadyHere = true;
                                }
                            }
                            if (!isAlreadyHere) {
                                //logger.debug("PARENT UCA " + uca + " has CHILD BP " + firstbp);
                                parNode.addChild(childNode);
                                childNode.setParent(parNode);
                            }
                        }
                    } else {
                        String parentId = cells[0].trim();
                        String childId = cells[1].trim();
                        boolean parentIsRoot = false;
                        if (parentId.contains("_GER")) {
                            parentIsRoot = true;
                        }
                        if (originalBPidtoBPid.containsKey(parentId)) {
                            parentId = originalBPidtoBPid.get(parentId);
                        }
                        if (originalBPidtoBPid.containsKey(childId)) {
                            childId = originalBPidtoBPid.get(childId);
                        }
                        //logger.debug("Processing " + parentId + " with " + childId);
                        Node parentNode = processNode(parentId, parentIsRoot);
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
                                    //logger.debug("child already stored: " + k.getNodeId() + " chilnodeId " + childId);
                                    isAlreadyHere = true;
                                }
                            }
                            if (!isAlreadyHere) {
                                //logger.debug("PARENT " + parentId + " has CHILD " + childId);
                                parentNode.addChild(childNode);
                                childNode.setParent(parentNode);
                            }
                        } else {
                            //logger.debug("PARENT " + parentId + " and CHILD " + childId + " have same sequences!");
                        }
                    }
                }


            }
            /*//logger.debug("First bp is " + firstbp);
            //logger.debug("UCA is " + uca);*/
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
            //ON 06.05.20 for dnaml input only
            boolean seqToCheck = false;
            if (sequence.contains("R") || sequence.contains("Y") || sequence.contains("S") || sequence.contains("M")
                    || sequence.contains("K") || sequence.contains("W")) {
                sequence = processSpecialNuc(node);
                //logger.debug("Changing UPAC nuc for node " + node.getNodeId());
                seqToCheck = true;
            }

            // Finally we check that this modified node sequence is not an existing one
            if (seqToCheck && isDnamlInput) {
                for (Node otherNode : nodesToCheck) {
                    if (!otherNode.getNodeId().equals(node.getNodeId())) {
                        String seq = idToSequence.get(otherNode.getNodeId());
                        if (seq.equals(sequence)) {
                            logger.warn("!!!!!! By replacing a UPAC nuc we got the same sequence for " + node.getNodeId()
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

    private String getUCA(String stringTree) {
        Scanner scanner = new Scanner(stringTree);
        scanner.useDelimiter("\\n");
        // first use a Scanner to get each line
        while (scanner.hasNext()) {
            String line = scanner.next();
            if (line.contains("_GER")) {
                return line.trim();
            }
        }
        return null;
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

    private Node processNode(String id, boolean isRoot) throws Exception {
        //System.out.println("Process node: " + id);
        String sequence = idToSequence.get(id);
        //bug found from IgPhyML
        if (sequence ==null ){
            boolean addMissingId=false;
            //check that it is not the case where you have seqId_1 and the seq is identical to the seqId
            if (id.matches("(.*)_1")){
                String idToFound = id.substring(0,id.length()-2);
                for (String id2:idToSequence.keySet()){
                    if (id2.equals(idToFound)){
                        idToSequence.put(id,idToSequence.get(id2));
                        //System.out.println("Find "+id2+" for "+id);
                        addMissingId=true;
                        break;
                    }
                }
            }
            if (!addMissingId) {
                JOptionPane.showMessageDialog(new JFrame(), "ERROR: no sequence found in the AIRR file or in the fasta file for sequence_id '" + id + "'", "No sequence found",
                        JOptionPane.ERROR_MESSAGE);
                throw new Exception("ERROR: no sequence found in the AIRR file or in the fasta file for sequence_id '" + id + "'");
            }
        }
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
                node.setSequence(sequence); //BUG fixed the 06.05.20
                //logger.debug("FOR BP "+id+" we call it first "+nodeId);
                sequenceToNode.put(sequence, node);
                // we create an Ig and link it to the node
                // //logger.debug("Set nodeId: " + nodeId + " with seq " +
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
                 //logger.debug("For " + id + " we already have " +node.getNodeId() + " with the same sequence");
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
                // //logger.debug("Create NODE: " + id);
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
        // //logger.debug("processed node: "+node.getNodeId());
        if (!nodes.contains(node)) {
            //logger.debug("...we add it to nodes array " + node.getNodeId());
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
                //logger.debug("---- IMGTgapped, process node "+node.getNodeId());
                if (!node.isRoot()) {
                    String sequence = node.getImgtFormatSequence();
                    if (sequence == null) { // BP case!
                        //logger.debug("No IMGT format sequence for "+node.getNodeId());
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

    private void createMatrixTreeFile() throws IOException {
        File matrixFile = new File(System.getProperty("user.dir") + fs + "output" + fs + projectName + fs + projectName + "_matrixTree.tsv");
        BufferedWriter out = new BufferedWriter(new FileWriter(matrixFile));
        ArrayList<Node> orderedNodes = new ArrayList<>();
        writeNodeAndChildrenIds(out, rootNode, orderedNodes);
        out.write(ls);
        for (Node node : orderedNodes) {
            out.write(node.getNodeId());
            ArrayList<Node> children = node.getChildren();
            //determine the relation
            for (Node node2 : orderedNodes) {
                int edge = 0;
                for (Node kid : children) {
                    if (kid.getNodeId().equals(node2.getNodeId())) {
                        edge = kid.getNumberOfNucMutationsWithParent();
                        break;
                    }
                }
                out.write("\t" + edge);
            }
            out.write(ls);
        }
        out.close();
    }

    private void writeNodeAndChildrenIds(BufferedWriter out, Node node, ArrayList<Node> orderedNodes) throws IOException {
        orderedNodes.add(node);
        if (!node.isRoot()) {
            out.write("\t");
        }
        out.write(node.getNodeId());
        for (Node kid : node.getChildren()) {
            writeNodeAndChildrenIds(out, kid, orderedNodes);
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
        ////logger.debug("SET-LEVEL: "+node.getNodeId());
        ArrayList<Node> children = node.getChildren();
        for (int c = 0; c < children.size(); c++) {
            Node child = children.get(c);
            ////logger.debug("SET-LEVEL-KID: "+child.getNodeId());
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
                // //logger.debug("!!! For node " + node.getNodeId() + ", at
                // position: " + position + " we got a " + nuc);
                // //logger.debug("In the children, we have number of A " +
                // Anumber + " and number of G " + Gnumber);
                if (Anumber > Gnumber) {
                    replacedNuc = "A";
                } else {
                    replacedNuc = "G";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                // //logger.debug("--> previous sequence was: " + sequence + " and
                // new one is " + newSequence);
                sequence = newSequence;
            } else if (nuc == 'Y') { // it is a T or C
                int[] nucNumber = getNucNumber(children, position, nuc);
                int Tnumber = nucNumber[0];
                int Cnumber = nucNumber[1];
                // //logger.debug("!!! For node " + node.getNodeId() + ", at
                // position: " + position + " we got a " + nuc);
                // //logger.debug("In the children, we have number of T " +
                // Tnumber + " and number of C " + Cnumber);
                if (Tnumber > Cnumber) {
                    replacedNuc = "T";
                } else {
                    replacedNuc = "C";
                }
                newSequence = sequence.substring(0, position) + replacedNuc + sequence.substring(position + 1);
                // //logger.debug("--> previous sequence was: " + sequence + " and
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
            // //logger.debug("--> previous sequence was: "+sequence);

            position++;
        }
        return newSequence;
    }

}
