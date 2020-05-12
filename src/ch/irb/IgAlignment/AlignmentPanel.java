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
 * 
 * 
 * This class displays the alignment between the Igs.
 * First we have the UCA, and then the others taking in account their clusters in the tree.
 * The user can select some of these sequences.
 * It can zoom in/out, can change mode (nucleotidic or amino acid), and can select the positions he wants to see.
 */

package ch.irb.IgAlignment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.apache.log4j.Logger;

import ch.irb.IgGenealogicTreeViewer.SetImgtInfo;
import ch.irb.kabat.ProcessKabatNumbering;
import ch.irb.nodes.NodeGraph;
import ch.irb.translation.AminoAcidProperties;
import ch.irb.translation.Codon;
import ch.irb.translation.Translator;

@SuppressWarnings("serial")
public class AlignmentPanel extends JPanel {
    static Logger logger = Logger.getLogger(AlignmentPanel.class);
    static AminoAcidProperties aminoAcidProperties = new AminoAcidProperties();
    static Color redColor = new Color(255, 159, 159);
    static Color pinkColor = new Color(255, 191, 191);
    static Color cyanColor = new Color(223, 255, 255);
    static Color blueColor = new Color(170, 170, 255);
    static Color greenColor = new Color(179, 255, 179);
    static Color yellowColor = new Color(255, 255, 138);
    static Color lightGreyColor = new Color(211, 211, 211);
    static String OS = System.getProperty("os.name").toLowerCase();
    static RegionBorder regionBorder = new RegionBorder();
    static Border whiteBorder = new WhiteBorder();
    static Border compound = BorderFactory.createCompoundBorder(regionBorder,
            BorderFactory.createLineBorder(Color.white));
    private Font fontId;
    private Font fontId2;
    private Font regionFontZoomIn = new Font("Arial", Font.BOLD, 35); // was 27
    private Font regionFontZoomOut = new Font("Arial", Font.BOLD, 22); // was 16
    private Font regionFontZoomIn_forVL = new Font("Arial", Font.BOLD, 18);
    private Font regionFontZoomOut_forVL = new Font("Arial", Font.BOLD, 12);
    private GridBagConstraints c = new GridBagConstraints();
    private ArrayList<NodeGraph> allNodeGraphs = new ArrayList<NodeGraph>();
    private NodeGraph rootNode = null;
    private ArrayList<NodeGraph> isNodeACluster = new ArrayList<NodeGraph>();
    private ArrayList<NodeGraph> orderedNodeGraphs = new ArrayList<NodeGraph>();
    private HashMap<NodeGraph, TreeMap<Integer, Color>> fromNodeIndexesToColor = new HashMap<NodeGraph, TreeMap<Integer, Color>>();
    private HashMap<NodeGraph, ArrayList<Integer>> fromNodeIndexesToDashes = new HashMap<NodeGraph, ArrayList<Integer>>();
    private int mode = 1; // 1 is nucleotide mode, 2 is AA by chemistry,
    //3 is AA by highlighting
    private boolean zoomMode = true; // only in zoom mode the user can
    // select/deselect columns
    private int boxesNumber = 0;
    private CheckColumnsPanel checkColumnPanel = null;
    private TreeMap<Integer, Boolean> selectedColumns = new TreeMap<Integer, Boolean>();
    private AlignmentPanel alignmentPanel = this;
    private AllSequencesPanel sequencesPanel;
    private PositionsPanel positionsPanel = null;
    private SequencesIdPanel sequenceIdPanel = null;
    private CdrFrPanel cdrFrPanel = null;
    private SetImgtInfo setImgtInfo = null;
    private TreeMap<Integer, String> positionToCdrFrRegions = new TreeMap<Integer, String>();
    private HashMap<String, ArrayList<Integer>> fromRegionToColumns = new HashMap<String, ArrayList<Integer>>();
    private ArrayList<Integer> indexWithRegionBorder = new ArrayList<>();
    private HashMap<String, Boolean> regionON = new HashMap<String, Boolean>();
    private ArrayList<JCheckBoxMenuItem> checkColumns = new ArrayList<JCheckBoxMenuItem>();
    private boolean updateSequenceIdsPanel = false;

    public AlignmentPanel(ArrayList<NodeGraph> allNodeGraphs, int mode, boolean zoomMode,
                          TreeMap<Integer, Boolean> selectedColumns) {
        this.mode = mode;
        this.zoomMode = zoomMode;
        // we set the font Id, used for the boxes and the Ig names
        if (zoomMode) {
            fontId = new Font("Arial", Font.BOLD, 25);// 20
            fontId2 = new Font("Arial", Font.PLAIN, 28);// 20
        } else {
            fontId = new Font("Arial", Font.BOLD, 15);
            fontId2 = new Font("Arial", Font.PLAIN, 15);
        }
        this.setBackground(Color.white);
        this.setLayout(new GridBagLayout());

        this.allNodeGraphs = allNodeGraphs;
        // set the SetImgtInfo object with a node
        NodeGraph aNode = allNodeGraphs.get(0);
        setImgtInfo = new SetImgtInfo(aNode);
        if (mode == 1) {
            positionToCdrFrRegions = setImgtInfo.getPositionNucToCdrFrRegions();
        } else {
            positionToCdrFrRegions = setImgtInfo.getPositionAAToCdrFrRegions();
        }
        // we set the boxes number (nuc or aa)
        if (mode == 1) {
            boxesNumber = aNode.getSequence().length();
        } else {
            boxesNumber = aNode.getProteinSequence().length();
        }
        if (selectedColumns == null) {// we check all the buttons if we have to
            // (change of mode for example)
            for (int i = 0; i < boxesNumber; i++) {
                this.selectedColumns.put(i, Boolean.TRUE);
            }
        } else {
            this.selectedColumns = selectedColumns;
        }
        setNodesOrder();
        setBoxesColors();
        setRegionBorders();
        // we initialize the sequences panel to get some variable value (like
        // box width)
        setComponents();
    }

    /*
     * This method set the order of the Ig from the top to the bottom
     */
    private void setNodesOrder() {
        for (NodeGraph node : allNodeGraphs) {
            // logger.info("processing node " + node.getNodeId());
            if (node.isRoot()) {
                rootNode = node;
            }
        }
        // we set the BP or Ig cluster; when all the children are Igs
        for (NodeGraph node : allNodeGraphs) {
            ArrayList<NodeGraph> children = node.getChildren();
            int isChildIg = 0;
            for (NodeGraph child : children) {
                if (!child.isBP()) {
                    isChildIg += 1;
                }
            }
            if (isChildIg == children.size()) {
                isNodeACluster.add(node);
            }
        }

        // we start from the UCA
        orderedNodeGraphs.add(rootNode);
        setChildrenOrder(rootNode);

    }

    /*
     * This method is used to have the sequence aligned in the same "order" that they are displayed in the tree
     * From top to bottom of the tree
     */
    private void setChildrenOrder(NodeGraph node) {
        ArrayList<NodeGraph> children = node.getChildren();
        TreeMap<Integer, ArrayList<NodeGraph>> igChildren = new TreeMap<Integer, ArrayList<NodeGraph>>();
        TreeMap<Integer, ArrayList<NodeGraph>> bpChildren = new TreeMap<Integer, ArrayList<NodeGraph>>();
        for (NodeGraph child : children) {
            int mutNumWithParent = child.getNumberOfNucMutationsWithParent();
            Integer key = mutNumWithParent;
            ArrayList<NodeGraph> nodes = new ArrayList<NodeGraph>();
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
            ArrayList<NodeGraph> nodes = igChildren.get(mutation);
            for (NodeGraph child : nodes) {
                if (!orderedNodeGraphs.contains(child)) {
                    orderedNodeGraphs.add(child);
                }
                setOrderForCluster(child);
            }
        }

        // Then the BP
        mutations = new Integer[bpChildren.size()];
        bpChildren.keySet().toArray(mutations);
        Arrays.sort(mutations); // we sort the mutations numbers
        for (Integer mutation : mutations) {
            ArrayList<NodeGraph> nodes = bpChildren.get(mutation);
            for (NodeGraph child : nodes) {
                if (!orderedNodeGraphs.contains(child)) {
                    orderedNodeGraphs.add(child);
                }
                setOrderForCluster(child);
            }
        }

        // Finally we run the method foreach child
        for (NodeGraph child : children) {
            setChildrenOrder(child);
        }
    }

    private void setOrderForCluster(NodeGraph node) {
        if (isNodeACluster.contains(node)) {
            ArrayList<NodeGraph> children = node.getChildren();
            for (NodeGraph child : children) {
                if (!orderedNodeGraphs.contains(child)) {
                    orderedNodeGraphs.add(child);
                }
                setOrderForCluster(child);
            }
        }
    }

    /*
     * we store the color of each box, depending on the mode the user chose
     */
    private void setBoxesColors() {
        TreeMap<Integer, Color> fromIndexToColorROOT = new TreeMap<Integer, Color>();
        for (NodeGraph node : orderedNodeGraphs) {
            // logger.info("setBox color for orederned node: "+node.getNodeId());
            //we display if the user checked the ID
            if (!node.equals(rootNode) && allNodeGraphs.contains(node)) {//
                // logger.info("........setBox color for ordered node:
                // "+node.getNodeId());
                TreeMap<Integer, Color> fromIndexToColor = new TreeMap<Integer, Color>();
                String sequence = node.getSequence();
                if (mode != 1) {
                    sequence = node.getProteinSequence();
                }
                int pos = 0;
                for (char box : sequence.toCharArray()) {
                    String rootSequence = rootNode.getSequence();
                    if (mode != 1) {
                        rootSequence = rootNode.getProteinSequence();
                    }
                    String rootBox = String.valueOf(rootSequence.charAt(pos));
                    // logger.info("Compare box "+box+" with rootBox
                    // "+rootBox);
                    if (!rootBox.equals(String.valueOf(box))) { // there is a
                        // mutation

                        Color color = null;
                        if (mode == 1) { // nucleotide mode: if mutation, grey
                            // for UCA, red or green for the Ig,
                            // and dashes if reverse/double mutation
                            //logger.debug("Processing node " + node.getNodeId() + " with its parent "
                            //		+ node.getParent().getNodeId());
                            String nodeSequence = node.getSequence();
                            // HERE we compare the codon with the UCA codon!!
                            // To do like IMGT output website
                            String codon = new Codon(nodeSequence, pos).getCodon();
                            String codonParent = new Codon(rootSequence, pos).getCodon();
                            Translator translator = new Translator(codon, true);
                            String aa = translator.getProteinSequence();
                            translator = new Translator(codonParent, true);
                            String parentAA = translator.getProteinSequence();
                            if (aa.equals(parentAA)) {
                                color = greenColor;
                            } else {
                                color = redColor;
                            }
                            if (fromNodeIndexesToColor.containsKey(node)) {
                                fromIndexToColor = fromNodeIndexesToColor.get(node);
                            }
                            fromIndexToColor.put(pos, color);
                            fromNodeIndexesToColor.put(node, fromIndexToColor);

                            // we put the position in grey in the UCA
                            if (fromNodeIndexesToColor.containsKey(rootNode)) {
                                fromIndexToColorROOT = fromNodeIndexesToColor.get(rootNode);
                            }
                            fromIndexToColorROOT.put(pos, lightGreyColor);
                            fromNodeIndexesToColor.put(rootNode, fromIndexToColorROOT);

                        } else if (mode == 2) {// prot mode: chemistry mode: 1
                            // color for a AA group (hydrophobic,
                            // hydrophilic, charged )
                            String aminoAcid = String.valueOf(box);
                            color = getColorByChemistryAA(aminoAcid);
                            // logger.debug("we got a color for aa "+aminoAcid+"
                            // is "+color);
                            if (fromNodeIndexesToColor.containsKey(node)) {
                                fromIndexToColor = fromNodeIndexesToColor.get(node);
                            }
                            fromIndexToColor.put(pos, color);
                            fromNodeIndexesToColor.put(node, fromIndexToColor);

                            // we also get the color for the UCA
                            if (fromNodeIndexesToColor.containsKey(rootNode)) {
                                fromIndexToColorROOT = fromNodeIndexesToColor.get(rootNode);
                            }
                            {
                                color = getColorByChemistryAA(String.valueOf(rootSequence.charAt(pos)));
                            }
                            fromIndexToColorROOT.put(pos, color);
                            fromNodeIndexesToColor.put(rootNode, fromIndexToColorROOT);

                        } else if (mode == 3) {// prot mode, by highlighting:
                            // grey GL , mut in red
                            color = redColor;

                            if (fromNodeIndexesToColor.containsKey(node)) {
                                fromIndexToColor = fromNodeIndexesToColor.get(node);
                            }
                            fromIndexToColor.put(pos, color);
                            fromNodeIndexesToColor.put(node, fromIndexToColor);

                            // we put the position in grey in the UCA
                            if (fromNodeIndexesToColor.containsKey(rootNode)) {
                                fromIndexToColorROOT = fromNodeIndexesToColor.get(rootNode);
                            }
                            fromIndexToColorROOT.put(pos, lightGreyColor);
                            fromNodeIndexesToColor.put(rootNode, fromIndexToColorROOT);
                        }
                    }
                    pos++;
                }
                if (mode == 1) {// we check also the double/reverse mutation
                    ArrayList<Integer> reverseOrDoublePositions = new ArrayList<Integer>();
                    String revMutation = node.getReverseInformation();
                    if (revMutation != null) {
                        String[] revMut = revMutation.split(" // Reverse at position ");
                        for (int i = 1; i < revMut.length; i++) {
                            String[] muta = revMut[i].split(":");
                            String position = muta[0];
                            if (fromNodeIndexesToDashes.containsKey(node)) {
                                reverseOrDoublePositions = fromNodeIndexesToDashes.get(node);
                            }
                            if (!reverseOrDoublePositions.contains(Integer.valueOf(position))) {
                                reverseOrDoublePositions.add(Integer.valueOf(position));
                            }
                            fromNodeIndexesToDashes.put(node, reverseOrDoublePositions);
                        }
                    }
                    String doubleMutation = node.getDoubleMutationInformation();
                    if (doubleMutation != null) {
                        String[] douMut = doubleMutation.split(" // Double Mutation at position ");
                        for (int i = 1; i < douMut.length; i++) {
                            String[] muta = douMut[i].split(":");
                            String position = muta[0];
                            if (fromNodeIndexesToDashes.containsKey(node)) {
                                reverseOrDoublePositions = fromNodeIndexesToDashes.get(node);
                            }
                            if (!reverseOrDoublePositions.contains(Integer.valueOf(position))) {
                                reverseOrDoublePositions.add(Integer.valueOf(position));
                            }
                            fromNodeIndexesToDashes.put(node, reverseOrDoublePositions);
                        }
                    }
                }
            }
        }
    }

    private Color getColorByChemistryAA(String aminoAcid) {
        Color color = Color.white;
        String property = aminoAcidProperties.getAminoAcidProperty(aminoAcid);
        if (property != null) {
            if (property.equals("hydrophobic")) {
                color = yellowColor;
            } else if (property.equals("hydrophilic")) {
                color = blueColor;
            } else if (property.equals("charged")) {
                color = greenColor;
            } else if (property.equals("deletion")) {
                color = Color.white;
            }
        }
        return color;
    }

    /*
     * we store the boxes that have to show the CDR/Fr region borders
     */
    private void setRegionBorders() {
        String region = "FR1";
        for (int i = 0; i < boxesNumber; i++) {
            String reg = positionToCdrFrRegions.get(i);
            // logger.debug("For position "+i+" we have region "+reg);
            if (!reg.equals(region)) {
                // logger.debug("create a jbutton with lenght " + lenght + " for
                // region " + region);
                indexWithRegionBorder.add(i);
                region = reg;
            }
        }
    }

    /*
     * this method set all the component of this panel
     */
    private void setComponents() {
        // we initialize first the sequencesPanel because it contains some
        // variable used for the other panels
        sequencesPanel = new AllSequencesPanel();
        // we put the CDR/FR panel
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 1;
        c.insets = new Insets(0, 25, 6, 10);
        cdrFrPanel = new CdrFrPanel();
        cdrFrPanel.setOpaque(true);
        this.add(cdrFrPanel, c);
        // we put the positions panel
        c.gridy = 1;
        c.insets = new Insets(0, 14, 0, 10);
        positionsPanel = new PositionsPanel();
        this.add(positionsPanel, c);
        // we put the sequences IDs panel
        c.gridx = 0;
        if (zoomMode) {
            c.gridy = 3;
        } else {
            c.gridy = 2;
        }
        sequenceIdPanel = new SequencesIdPanel();
        sequenceIdPanel.setOpaque(true);
        c.insets = new Insets(0, 0, 10, 0);
        this.add(sequenceIdPanel, c);

        // we put the radio buttons panel to select/unselect columns if we are
        // in zoom mode
        if (zoomMode) {
            checkColumnPanel = new CheckColumnsPanel();
            c.gridx = 1;
            c.gridy = 2;
            c.insets = new Insets(0, 10, 0, 10);
            checkColumnPanel.setOpaque(true);
            this.add(checkColumnPanel, c);
            c.gridy = 3;
        } else {
            c.gridy = 2;
        }
        // we add the sequences panel
        c.gridx = 1;
        c.insets = new Insets(0, 0, 10, 10);
        sequencesPanel.setOpaque(true);
        this.add(sequencesPanel, c);
        revalidate();
        repaint();
    }

    /*
     * This panel display the Ig names the user selected (all by default) in the
     * order we calculated previously
     */
    public class SequencesIdPanel extends JPanel {
        public SequencesIdPanel() {
            setLayout(new GridLayout(0, 1));
            setBackground(Color.white);
            for (NodeGraph node : orderedNodeGraphs) {
                if (allNodeGraphs.contains(node)) {// we display if the user
                    // checked the ID
                    String id = node.getNodeId();
                    // logger.debug("we display id " + id);
                    if (node.hasDuplicateNodes()) {
                        ArrayList<NodeGraph> dup = node.getDuplicatedNodes();
                        for (NodeGraph du : dup) {
                            id += ", " + du.getNodeId();
                        }
                    }
                    JLabel label = new JLabel(id);
                    label.setFont(fontId2);
                    if (!updateSequenceIdsPanel) {
                        Border border = BorderFactory.createLineBorder(Color.white);
                        label.setBorder(border);
                    }
                    label.setPreferredSize(new Dimension((int) label.getPreferredSize().getWidth() + 5,
                            sequencesPanel.getBoxHeight()));
                    label.setBackground(Color.white);
                    label.setOpaque(true);
                    add(label);
                }
            }
        }
    }

    /*
     * This panel display the CDR/FR regions, each region is a button that the
     * user can select or unselect in order to show or not show the mutations
     * below
     */
    public class CdrFrPanel extends JPanel {
        public CdrFrPanel() {
            Font regionFont = regionFontZoomIn;
            if (!zoomMode) {
                regionFont = regionFontZoomOut;
            }
            int aaCDR2 = setImgtInfo.getNumberAAForCDR2();
            if (aaCDR2 <= 3) {
                if (zoomMode) {
                    regionFont = regionFontZoomIn_forVL;
                } else {
                    regionFont = regionFontZoomOut_forVL;
                }
            }
            setBackground(Color.white);
            FlowLayout flowLayout = new FlowLayout(FlowLayout.LEADING, 0, 2);
            setLayout(flowLayout);
            int toAdd = 2;
            // logger.debug("toadd is "+toAdd);
            if (updateSequenceIdsPanel) {
                toAdd = 0;
            }
            setPreferredSize(new Dimension(
                    boxesNumber * (sequencesPanel.getBoxWidth() + toAdd) + sequencesPanel.getBoxWidth() + 6, 60));
            // 6 is for the region borders
            String region = "FR1";
            int lenght = 0;
            ArrayList<Integer> columns = new ArrayList<Integer>();
            int index = 0;
            for (int i = 0; i < boxesNumber; i++) {
                String reg = positionToCdrFrRegions.get(i);
                // logger.debug("For position "+i+" we have region "+reg);
                if (!reg.equals(region)) {
                    // logger.debug("create a jbutton with lenght " + lenght + "
                    // for region " + region);
                    for (int j = index; j < i; j++) {
                        columns.add(j);
                    }
                    fromRegionToColumns.put(region, columns);
                    if (!regionON.containsKey(region)) { // to initialize it the
                        // first time
                        regionON.put(region, Boolean.TRUE);
                    }
                    columns = new ArrayList<Integer>();
                    index = i;
                    JButton button = new JButton(region);
                    button.setBorder(BorderFactory.createLineBorder(Color.black));
                    button.setFont(regionFont);
                    if (OS.matches(".*mac.*")) { // in order the color of the
                        // button appears on MAC
                        button.setOpaque(true);
                    }
                    if (region.matches("CDR.*")) {
                        button.setBackground(pinkColor);
                    } else {
                        button.setBackground(cyanColor);
                    }
                    if (region.equals("CDR2") && (aaCDR2 <= 3) && (mode != 1)) {
                        button.setText("CD2");
                        button.setMargin(new Insets(0, 0, 0, 0));
                    }
                    button.setActionCommand(region);
                    button.addActionListener(new ButtonListener());
                    button.setPreferredSize(new Dimension(lenght * (sequencesPanel.getBoxWidth() + toAdd), 55));
                    add(button);
                    region = reg;
                    lenght = 1;
                } else {
                    lenght++;
                }
            }
            lenght--;
            for (int j = index; j < boxesNumber; j++) {
                columns.add(j);
            }
            fromRegionToColumns.put(region, columns);
            JButton button = new JButton(region);
            if (!regionON.containsKey(region)) { // to initialize it the first
                // time
                regionON.put(region, Boolean.TRUE);
            }
            button.addActionListener(new ButtonListener());
            button.setActionCommand(region);
            if (region.matches("CDR.*")) {
                button.setBackground(pinkColor);
            } else {
                button.setBackground(cyanColor);
            }
            if (OS.matches(".*mac.*")) { // in order the color of the button
                // appears on MAC
                button.setOpaque(true);
            }
            button.setFont(regionFont);
            button.setBorder(BorderFactory.createLineBorder(Color.black));
            int w = ((lenght) * (sequencesPanel.getBoxWidth() + toAdd)) + (sequencesPanel.getBoxWidth()) - 1;
            button.setPreferredSize(new Dimension(w, 55));
            add(button);
        }
    }

    /*
     * This class is the listener for the Region buttons
     */
    private class ButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            JButton button = (JButton) event.getSource();
            String command = button.getActionCommand();
            ArrayList<Integer> columns = fromRegionToColumns.get(command);
            if (regionON.get(command)) {
                for (Integer index : columns) {
                    selectedColumns.put(index, Boolean.FALSE);
                }
                regionON.put(command, Boolean.FALSE);
            } else {
                for (Integer index : columns) {
                    selectedColumns.put(index, Boolean.TRUE);
                }
                regionON.put(command, Boolean.TRUE);
            }
            // update the display
            alignmentPanel.removeAll();
            setComponents();
        }
    }

    /*
     * This panel writes the position of the nucleotide and the kabat numbering
     * for the AA.
     */
    public class PositionsPanel extends JPanel {
        public PositionsPanel() {
            TreeMap<Integer, String> fromPositionToKabatnumbering = new TreeMap<>();
            if (mode != 1) { // AA mode
                // we get the Kabat numbering
                try {
                    // we get the kabat numbering
                    String protSeqForKabat = rootNode.getProteinSequence();
                    ProcessKabatNumbering processKabatNumbering = new ProcessKabatNumbering(protSeqForKabat);
                    fromPositionToKabatnumbering = processKabatNumbering.getFromPositionToKabatnumbering();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this,
                            "No Kabat numbering could be found.\n We use the default amino acid position.",
                            "Could not connect to Abnum web site", JOptionPane.WARNING_MESSAGE);
                }
            }
            setLayout(new GridLayout(1, boxesNumber));
            setBackground(Color.white);
            int toAdd = 2;
            if (updateSequenceIdsPanel) {
                toAdd = 0;
            }
            setPreferredSize(new Dimension(boxesNumber * (sequencesPanel.getBoxWidth() + toAdd) + 6, 58));
            for (int i = 0; i < boxesNumber; i++) {
                String positionToWrite = String.valueOf(i + 1);
                if (mode != 1 && fromPositionToKabatnumbering.size()>1) {
                    positionToWrite = fromPositionToKabatnumbering.get(new Integer(i));
                }
                if (positionToWrite != null) {
                    MyRotatedLabel lab = new MyRotatedLabel(positionToWrite);
                    lab.setOpaque(true);
                    add(lab);
                }
                setOpaque(true);
            }
        }
    }

    public class MyRotatedLabel extends JPanel {
        private String txt;

        public MyRotatedLabel(String txt) {
            this.txt = txt;
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Font font = new Font("Courier", Font.BOLD, 18);
            // set the derived font in the Graphics2D context
            Graphics2D g2d = ((Graphics2D) g);
            g2d.setFont(font);
            this.setBackground(Color.white);
            g2d.rotate(Math.toRadians(90.0));
            g2d.drawString(txt, 3, 1);
            g2d.dispose();
        }

    }

    /*
     * This panel allows the user (in zoom mode only) to check/uncheck a
     * column/position on the sequence
     */
    public class CheckColumnsPanel extends JPanel {
        public CheckColumnsPanel() {
            setLayout(new GridLayout(1, boxesNumber));
            setBackground(Color.white);
            int toAdd = 2;
            if (updateSequenceIdsPanel) {
                toAdd = 0;
            }
            setPreferredSize(new Dimension(boxesNumber * (sequencesPanel.getBoxWidth() + toAdd) + 6, 18));
            for (int i = 0; i < boxesNumber; i++) {
                final JCheckBoxMenuItem check = new JCheckBoxMenuItem();
                check.setAlignmentX(LEFT_ALIGNMENT);
                check.setIconTextGap(0);
                check.setBackground(Color.white);
                if (selectedColumns.get(i)) {
                    check.setSelected(true);
                }
                final int index = i;
                add(check);
                check.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        if (check.isSelected()) {
                            selectedColumns.put(index, Boolean.TRUE);
                        } else {
                            selectedColumns.put(index, Boolean.FALSE);
                        }
                        // update the display
                        alignmentPanel.removeAll();
                        setComponents();
                    }
                });
                checkColumns.add(check);
            }
        }
    }

    /*
     * This panel displays each sequence. We have then one panel for each
     * sequence.
     */
    public class AllSequencesPanel extends JPanel {

        private int boxWidth = 0;
        private int boxHeight = 0;

        public AllSequencesPanel() {
            this.setOpaque(true);
            this.setBackground(Color.white);
            this.setOpaque(true);
            this.setLayout(new GridLayout(0, 1));
            for (NodeGraph node : orderedNodeGraphs) {
                if (allNodeGraphs.contains(node)) {// we display if the user
                    // checked the ID
                    SequencePanel sequencePanel = new SequencePanel(node);
                    this.add(sequencePanel);
                }
            }
            int numbUnselectedColumns = 0;
            for (Integer ind : selectedColumns.keySet()) {
                if (!selectedColumns.get(Integer.valueOf(ind))) {
                    numbUnselectedColumns++;
                }
            }
            updateSequenceIdsPanel = numbUnselectedColumns == selectedColumns.size();
        }

        private class SequencePanel extends JPanel {

            public SequencePanel(NodeGraph node) {
                String nodeId = node.getNodeId();
                String fullNodeId = nodeId;
                if (node.hasDuplicateNodes()) {
                    for (NodeGraph dup : node.getDuplicatedNodes()) {
                        fullNodeId += "," + dup.getNodeId();
                    }
                }
                this.setOpaque(true);
                this.setBackground(Color.white);
                String sequence = node.getSequence();
                if (mode != 1) {
                    sequence = node.getProteinSequence();
                }
                this.setLayout(new GridLayout(1, boxesNumber));
                char[] boxes = sequence.toCharArray();
                int index = 0;
                TreeMap<Integer, Color> fromIndexToColor = new TreeMap<>();
                ArrayList<Integer> reverseOrDoubleMutations = new ArrayList<>();
                if (fromNodeIndexesToColor.containsKey(node)) {
                    fromIndexToColor = fromNodeIndexesToColor.get(node);
                }
                if (fromNodeIndexesToDashes.containsKey(node)) {
                    reverseOrDoubleMutations = fromNodeIndexesToDashes.get(node);
                }
                for (char box : boxes) {
                    JLabel label = new JLabel(String.valueOf(box), JLabel.CENTER);
                    label.setToolTipText(fullNodeId);
                    label.setFont(fontId);
                    if (selectedColumns.get(index)) {
                        if (fromIndexToColor.containsKey(index)) {
                            Color color = fromIndexToColor.get(index);
                            label.setBackground(color);
                            label.setOpaque(true);
                            if (!indexWithRegionBorder.contains(index)) {
                                label.setBorder(whiteBorder);
                            }
                            if (indexWithRegionBorder.contains(index)) {
                                label.setBorder(regionBorder);
                            }
                        } else if (!node.isRoot()) {
                            label.setText(".");
                        }
                        if (reverseOrDoubleMutations.contains(index)) {
                            label.setBorder(BorderFactory.createDashedBorder(null));
                        }
                    } else if (!node.isRoot() && !fromIndexToColor.containsKey(index)) {
                        label.setText(".");
                    }

                    if (label.getBorder() == null && indexWithRegionBorder.contains(index)) {
                        label.setBorder(compound);
                    }
                    this.add(label);
                    int boxW = (int) label.getPreferredSize().getWidth();
                    int boxH = (int) label.getPreferredSize().getHeight();
                    if (boxWidth < boxW) {
                        boxWidth = boxW;
                    }
                    if (boxHeight < boxH) {
                        boxHeight = boxH;
                    }
                    index++;
                }
            }
        }

        /**
         * @return the boxWidth
         */
        public int getBoxWidth() {
            return boxWidth;
        }

        /**
         * @return the boxHeight
         */
        public int getBoxHeight() {
            return boxHeight;
        }
    }

    /**
     * @return the selectedColumns
     */
    public TreeMap<Integer, Boolean> getSelectedColumns() {
        return selectedColumns;
    }

    /**
     * @return the updateSequenceIdsPanel
     */
    public boolean isUpdateSequenceIdsPanel() {
        return updateSequenceIdsPanel;
    }

    /**
     * @return the positionToCdrFrRegions
     */
    public TreeMap<Integer, String> getPositionToCdrFrRegions() {
        return positionToCdrFrRegions;
    }

    /**
     * @return the allNodeGraphs
     */
    public ArrayList<NodeGraph> getAllNodeGraphs() {
        return allNodeGraphs;
    }

    /**
     * @return the orderedNodeGraphs
     */
    public ArrayList<NodeGraph> getOrderedNodeGraphs() {
        return orderedNodeGraphs;
    }

    /**
     * @return the fromNodeIndexesToColor
     */
    public HashMap<NodeGraph, TreeMap<Integer, Color>> getFromNodeIndexesToColor() {
        return fromNodeIndexesToColor;
    }

    /**
     * @return the fromNodeIndexesToDashes
     */
    public HashMap<NodeGraph, ArrayList<Integer>> getFromNodeIndexesToDashes() {
        return fromNodeIndexesToDashes;
    }

    /**
     * @return the mode
     */
    public int getMode() {
        return mode;
    }

    /**
     * @return the sequenceIdPanel
     */
    public SequencesIdPanel getSequenceIdPanel() {
        return sequenceIdPanel;
    }

    /**
     * @return the setImgtInfo
     */
    public SetImgtInfo getSetImgtInfo() {
        return setImgtInfo;
    }

    /**
     * @return the fromRegionToColumns
     */
    public HashMap<String, ArrayList<Integer>> getFromRegionToColumns() {
        return fromRegionToColumns;
    }

    /**
     * @return the indexWithRegionBorder
     */
    public ArrayList<Integer> getIndexWithRegionBorder() {
        return indexWithRegionBorder;
    }

}
