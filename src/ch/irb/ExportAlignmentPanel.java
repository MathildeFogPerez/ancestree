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
   
 * This panel displays the alignment view that the user previously chose by selecting/unselecting the positions of the nucleotide or the sequence.
 * The interest to have this panel is to make the image shorter to export when the user unselected a lot of positions. 
 */

package ch.irb.IgAlignment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.apache.log4j.Logger;

import ch.irb.IgGenealogicTreeViewer.SetImgtInfo;
import ch.irb.kabat.ProcessKabatNumbering;
import ch.irb.nodes.NodeGraph;
import ch.irb.translation.AminoAcidProperties;

@SuppressWarnings("serial")
public class ExportAlignmentPanel extends JPanel {
	static Logger logger = Logger.getLogger(AlignmentPanel.class);
    static AminoAcidProperties aminoAcidProperties = new AminoAcidProperties();
    static Color redColor = new Color(255, 159, 159);
    static Color pinkColor = new Color(255, 191, 191);
    static Color cyanColor = new Color(223, 255, 255);
    static Color blueColor = new Color(170, 170, 255);
    static Color greenColor = new Color(179, 255, 179);
    static Color yellowColor = new Color(255, 255, 138);
    static Color lightGreyColor = new Color(211, 211, 211);
    static Color superLightGreyColor = new Color(245, 245, 245);
    static String OS = System.getProperty("os.name").toLowerCase();
    static Border empytyBorder = BorderFactory.createEmptyBorder();
    static RegionBorder regionBorder = new RegionBorder();
    static ShortRegionBorder shortRegionBorder = new ShortRegionBorder();
    static Border whiteBorder = new WhiteBorder();
    static BottomWhiteBorder bottomWhiteBorder = new BottomWhiteBorder();
    static Border compound = BorderFactory.createCompoundBorder(regionBorder,
            BorderFactory.createLineBorder(Color.white));
    private Font fontId = new Font("Arial", Font.BOLD, 25); // 16
    private Font fontIgId = new Font("Arial", Font.PLAIN, 28); // 16 25
    private Font regionFontZoomOut = new Font("Arial", Font.BOLD, 27);
    private GridBagConstraints c = new GridBagConstraints();
    private ArrayList<NodeGraph> allNodeGraphs = new ArrayList<NodeGraph>();
    private NodeGraph rootNode = null;
    private ArrayList<NodeGraph> orderedNodeGraphs = new ArrayList<NodeGraph>();
    private HashMap<NodeGraph, TreeMap<Integer, Color>> fromNodeIndexesToColor = new HashMap<NodeGraph, TreeMap<Integer, Color>>();
    private HashMap<NodeGraph, ArrayList<Integer>> fromNodeIndexesToDashes = new HashMap<NodeGraph, ArrayList<Integer>>();
    private int mode = 1; // 1 is nucleotide mode, 2 is AA by chemistry, 3 is AA
    // by highlighting
    private int boxesNumber = 0;
    private String sequence;
    private TreeMap<Integer, Boolean> selectedColumns = new TreeMap<Integer, Boolean>();
    private AllSequencesPanel sequencesPanel;
    private PositionsPanel positionsPanel = null;
    private SequencesIdPanel sequenceIdPanel = null;
    private CdrFrPanel cdrFrPanel = null;
    private TreeMap<Integer, String> positionToCdrFrRegions = new TreeMap<Integer, String>();
    private ArrayList<Integer> lastPositionOfARegion = new ArrayList<Integer>();
    private HashMap<String, Integer> fromRegionToBoxesNumber = new HashMap<String, Integer>();
    private ArrayList<Integer> indexWithRegionBorder = new ArrayList<>();
    private boolean updateSequenceIdsPanel = false;
    private ArrayList<Integer> fromBoxIndexToSequenceIndex = new ArrayList<Integer>();
    private boolean shorterView = false;
    private SetImgtInfo setImgtInfo = null;

    public ExportAlignmentPanel(AlignmentPanel alignmentPanel, boolean shorterView) {
        setBackground(Color.white);
        setLayout(new GridBagLayout());
        this.shorterView = shorterView;
        mode = alignmentPanel.getMode();
        updateSequenceIdsPanel = alignmentPanel.isUpdateSequenceIdsPanel();
        allNodeGraphs = alignmentPanel.getAllNodeGraphs();
        for (NodeGraph node : allNodeGraphs) {
            if (node.isRoot()) {
                rootNode = node;
            }
        }
        NodeGraph aNode = allNodeGraphs.get(0);
        setImgtInfo = new SetImgtInfo(aNode); // set the IMGT info
        sequence = aNode.getSequence();
        if (mode != 1) {
            sequence = aNode.getProteinSequence();
        }
        positionToCdrFrRegions = alignmentPanel.getPositionToCdrFrRegions();
        selectedColumns = alignmentPanel.getSelectedColumns();
        orderedNodeGraphs = alignmentPanel.getOrderedNodeGraphs();
        fromNodeIndexesToColor = alignmentPanel.getFromNodeIndexesToColor();
        fromNodeIndexesToDashes = alignmentPanel.getFromNodeIndexesToDashes();
        indexWithRegionBorder = alignmentPanel.getIndexWithRegionBorder();
        // we initialize the sequences panel to get some variable value (like
        // box width)
        setComponents();
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
        c.insets = new Insets(0, 0, 6, 10);
        cdrFrPanel = new CdrFrPanel();
        cdrFrPanel.setOpaque(true);
        this.add(cdrFrPanel, c);
        // we put the positions panel if we are not in shorter view
        if (!shorterView) {
            c.gridy = 1;
            if (mode == 1) {
                c.insets = new Insets(0, 14, 0, 10);
            } else {
                c.insets = new Insets(0, 0, 0, 0);
            }
            positionsPanel = new PositionsPanel();
            this.add(positionsPanel, c);
        }
        // we put the sequences IDs panel
        c.gridx = 0;
        c.gridy += 1;
        sequenceIdPanel = new SequencesIdPanel();
        sequenceIdPanel.setOpaque(true);
        c.insets = new Insets(0, 0, 10, 0);
        this.add(sequenceIdPanel, c);

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
                    if (node.hasDuplicateNodes()) {
                        ArrayList<NodeGraph> dup = node.getDuplicatedNodes();
                        for (NodeGraph du : dup) {
                            id += ", " + du.getNodeId();
                        }
                    }
                    JLabel label = new JLabel(id);
                    label.setFont(fontIgId);
                    label.setBackground(Color.white);
                    if (!updateSequenceIdsPanel) {
                        Border border = BorderFactory.createLineBorder(Color.white);
                        label.setBorder(border);
                    }
                    label.setPreferredSize(
                            new Dimension((int) label.getPreferredSize().getWidth(), sequencesPanel.getBoxHeight()));
                    label.setOpaque(true);
                    add(label);
                }
            }
        }
    }

    /*
     * This panel displays the CDR/Fr regions,
     */
    public class CdrFrPanel extends JPanel {
        public CdrFrPanel() {
            int aaCDR2 = setImgtInfo.getNumberAAForCDR2();
            Font regionFont = regionFontZoomOut;
            setBackground(Color.white);
            FlowLayout flowLayout = new FlowLayout(FlowLayout.CENTER, 0, 2);
            setLayout(flowLayout);
            int toAdd = 2;
            if (updateSequenceIdsPanel) {
                toAdd = 0;
            }
            setPreferredSize(new Dimension(
                    boxesNumber * (sequencesPanel.getBoxWidth() + toAdd) + sequencesPanel.getBoxWidth() + 6, 60)); // 6
            // is
            // for
            // the
            // region
            // borders
            String region = "FR1";
            for (int i = 0; i < sequence.length(); i++) {
                String reg = positionToCdrFrRegions.get(i);
                if (!reg.equals(region)) {
                    lastPositionOfARegion.add(i - 1);
                    JButton button = new JButton(region);
                    button.setFont(regionFont);
                    button.setBorder(BorderFactory.createLineBorder(Color.black));
                    if (OS.matches(".*mac.*")) {// in order the color of the
                        // button appears on MAC
                        button.setOpaque(true);
                    }
                    if (region.matches("CDR.*")) {
                        button.setBackground(pinkColor);
                    } else {
                        button.setBackground(cyanColor);
                    }
                    button.setMargin(new Insets(0, 0, 0, 0));
                    if (region.equals("CDR2") && (aaCDR2 <= 3) && (mode != 1)) {
                        button.setText("CD2");
                    }
                    int boxesNumberForthisRegion = fromRegionToBoxesNumber.get(region).intValue();
                    button.setPreferredSize(
                            new Dimension(boxesNumberForthisRegion * (sequencesPanel.getBoxWidth() + toAdd), 55));
                    add(button);
                    region = reg;
                }
            }
            lastPositionOfARegion.add(sequence.length() - 1);
            JButton button = new JButton(region);
            button.setFocusable(false);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setBorder(BorderFactory.createLineBorder(Color.black));
			if (region.matches("CDR.*")) {
				button.setBackground(pinkColor);
			} else {
				button.setBackground(cyanColor);
			}
			if (OS.matches(".*mac.*"))// in order the color of the button
			// appears on MAC
			{
				button.setOpaque(true);
			}
            button.setFont(regionFont);
            int boxesNumberForthisRegion = fromRegionToBoxesNumber.get(region).intValue();
            int w = ((boxesNumberForthisRegion) * (sequencesPanel.getBoxWidth() + toAdd)) - 1;
            button.setPreferredSize(new Dimension(w, 55));
            add(button);
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
                String positionToWrite = " ";
                if (fromBoxIndexToSequenceIndex.get(i) != null) {
                    int index = fromBoxIndexToSequenceIndex.get(i);
                    positionToWrite = String.valueOf(index + 1);
                    if (mode != 1 && fromPositionToKabatnumbering.size()>1) {
                        positionToWrite = fromPositionToKabatnumbering.get(index);
                    }
                }
                MyRotatedLabel lab = new MyRotatedLabel(positionToWrite);
                lab.setOpaque(true);
                add(lab);
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
     * This panel displays each sequence. We have then one panel for each
     * sequence.
     */
    public class AllSequencesPanel extends JPanel {

        private int boxWidth = 0;
        private int boxHeight = 0;
        private TreeMap<Integer, Boolean> isAGapToThisPosition = new TreeMap<Integer, Boolean>();

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
                this.setLayout(new GridLayout(1, 0));// 1, boxesNumber
                char[] boxes = sequence.toCharArray();
                int index = 0;
                TreeMap<Integer, Color> fromIndexToColor = new TreeMap<Integer, Color>();
                ArrayList<Integer> reverseOrDoubleMutations = new ArrayList<Integer>();
                if (fromNodeIndexesToColor.containsKey(node)) {
                    fromIndexToColor = fromNodeIndexesToColor.get(node);
                }
                if (fromNodeIndexesToDashes.containsKey(node)) {
                    reverseOrDoubleMutations = fromNodeIndexesToDashes.get(node);
                }
                boolean wasAGap = false;
                for (char box : boxes) {
                    if (selectedColumns.get(index)) {
                        JPanel panel = new JPanel(); // used in shorter view
                        // set the size of the panel using the font
                        int width = 3;// 2
                        if (mode != 1) {
                            width = 8;
                        }
                        FontMetrics metric = getFontMetrics(fontId);
                        int height = metric.getHeight();
                        panel.setPreferredSize(new Dimension(width, height));
                        JLabel label = new JLabel(String.valueOf(box), JLabel.CENTER); // we
                        // display
                        // the
                        // nucleotide
                        // in
                        // the
                        // non-shorter
                        // view
                        label.setToolTipText(fullNodeId);
                        label.setFont(fontId);
                        if (fromIndexToColor.containsKey(index)) {
                            Color color = fromIndexToColor.get(index);
                            label.setBackground(color);
                            panel.setBackground(color);
                            panel.setOpaque(true);
                            label.setOpaque(true);
                            if (!indexWithRegionBorder.contains(index)) {
                                label.setBorder(whiteBorder);
                                panel.setBorder(bottomWhiteBorder);
                            }
                            if (indexWithRegionBorder.contains(index)) {
                                label.setBorder(regionBorder);
                                panel.setBorder(shortRegionBorder);
                            }
                        } else {
							if (!node.isRoot()) {
								label.setText(".");
							}
                            panel.setBackground(superLightGreyColor);
                            panel.setBorder(bottomWhiteBorder);
                        }
                        if (reverseOrDoubleMutations.contains(index)) {
                            label.setBorder(BorderFactory.createDashedBorder(null));
                            panel.setBorder(BorderFactory.createDashedBorder(null));
                        }
                        if (label.getBorder() == null && indexWithRegionBorder.contains(index)) {
                            label.setBorder(compound);
                            panel.setBorder(shortRegionBorder);
                        }
                        if (!shorterView) {
                            this.add(label);
                        } else {
                            this.add(panel);
                        }
                        if (node.equals(rootNode)) {
                            boxesNumber += 1;
                            int boxesNumberForThisRegion = 0;
                            String region = positionToCdrFrRegions.get(index);
							if (fromRegionToBoxesNumber.containsKey(region)) {
								boxesNumberForThisRegion = fromRegionToBoxesNumber.get(region).intValue();
							}
                            boxesNumberForThisRegion += 1;
                            fromRegionToBoxesNumber.put(region, boxesNumberForThisRegion);
                            fromBoxIndexToSequenceIndex.add(index);
                            wasAGap = false;
                        }
                        // Get the width and height of the box
                        if (!shorterView) {
                            int boxW = (int) label.getPreferredSize().getWidth();
                            int boxH = (int) label.getPreferredSize().getHeight();
                            if (boxWidth < boxW) {
                                boxWidth = boxW;
                            }
                            if (boxH > boxHeight) {
                                boxHeight = boxH;
                            }
                        } else {
                            boxWidth = (int) panel.getPreferredSize().getWidth();
                            boxHeight = (int) panel.getPreferredSize().getHeight();
                        }
                    } else {
                        if (node.equals(rootNode)) {
                            int boxesNumberForThisRegion = 0;
                            String region = positionToCdrFrRegions.get(index);
                            if (fromRegionToBoxesNumber.containsKey(region)) {
                                boxesNumberForThisRegion = fromRegionToBoxesNumber.get(region).intValue();
                            }
                            int minBoxesNumberForThisRegion = 3;
                            if (shorterView) {
                                minBoxesNumberForThisRegion = 10;
                            }
                            if (!wasAGap || boxesNumberForThisRegion < minBoxesNumberForThisRegion) {// we
                                // display
                                // a
                                // gap
                                wasAGap = addAGap(index, boxesNumberForThisRegion, region);
                            } else if (lastPositionOfARegion.contains(index)) {
                                addAGap(index, boxesNumberForThisRegion, region);
                            }
                        } else {
                            if (isAGapToThisPosition.containsKey(index)) {
                                JLabel panel = new JLabel();
                                if (shorterView) {
                                    panel.setText("-");
                                    panel.setHorizontalAlignment(SwingConstants.CENTER);
                                }
                                if (shorterView) {
                                    panel.setPreferredSize(new Dimension(boxWidth, boxHeight));
                                }
                                panel.setBackground(Color.white);
                                if (indexWithRegionBorder.contains(index)) {
                                    if (shorterView) {
                                        panel.setBorder(shortRegionBorder);
                                    } else {
                                        panel.setBorder(compound);
                                    }
                                }
                                this.add(panel);
                            }

                        }
                    }
                    index++;
                }
            }

            private boolean addAGap(int index, int boxesNumberForThisRegion, String region) {
                JLabel panel = new JLabel();
                if (shorterView) {
                    panel.setPreferredSize(new Dimension(boxWidth, boxHeight));
                    panel.setText("-");
                    panel.setHorizontalAlignment(SwingConstants.CENTER);
                }
                if (indexWithRegionBorder.contains(index)) {
                    panel.setBorder(compound);
                    if (shorterView) {
                        panel.setBorder(shortRegionBorder);
                    }
                }
                panel.setBackground(Color.white);
                this.add(panel);
                boxesNumber += 1;
                boxesNumberForThisRegion += 1;
                fromRegionToBoxesNumber.put(region, boxesNumberForThisRegion);
                fromBoxIndexToSequenceIndex.add(null);
                isAGapToThisPosition.put(index, Boolean.TRUE);
                return true;
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
}
