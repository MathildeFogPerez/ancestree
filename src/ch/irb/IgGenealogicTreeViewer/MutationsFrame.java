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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import ch.irb.Layout.SpringUtilities;
import ch.irb.currentDirectory.GetSetCurrentDirectory;
import ch.irb.kabat.ProcessKabatNumbering;
import ch.irb.nodes.NodeGraph;
import ch.irb.saveImages.ExportAsEPSListener;
import ch.irb.saveImages.SaveImageAsPngListener;
import ch.irb.translation.Codon;
import ch.irb.translation.Translator;

/**
 * @author Mathilde This class is the frame where all the nucleotidic mutations
 *         (i.e.: 12: A-C) between a node and its parent will be displayed. In
 *         "rose pale", it means the mutations are shared somewhere else in the
 *         tree. All the mutations are clickable buttons. If the border of the
 *         button is green, it means the mutation is silent, if it is red, the
 *         mutation is non-silent. If the user clicks on a button, it will
 *         highlight the other places (=mutations' numbers) in the tree where
 *         there is a mutation at this position. If it is exactly the same
 *         nucleotidic change, it will be highlighted in yellow, if not, it will
 *         be highlighted in pink. The AA change is show and the mutations
 *         numbers will be surrounded by one box. If it is a different AA change
 *         it will be surrounded by 2 boxes and so on..
 *         In the case the user download the IMGT data, we display the mutations
 *         below the CDR and FR regions. We also display the ratio R/S (non
 *         silent mutations/silent mutations). An excel file is created too, and
 *         stored all these information (1 sheet per node).
 **/

@SuppressWarnings("serial")
public class MutationsFrame extends JFrame implements ActionListener {
    static Logger logger = Logger.getLogger(MutationsFrame.class);
    static String OS = System.getProperty("os.name").toLowerCase();
    private JPanel jPanel = new JPanel(new SpringLayout());
    private JScrollPane jScrollPane = new JScrollPane();
    private CdrFrImagePanel cdrFrImagePanel;
    private IgTreePanel igTreePanel;
    private TreeMap<String, MutationsGraph> allMutationsGraph = new TreeMap<String, MutationsGraph>();
    private String mutationsWithParent;
    private String dnaMutationsWithParent;
    private String rootNodeSequence;
    private String rootNodeProtSequence;
    private boolean isDNA = true;
    private TreeMap<Integer, String> mutations = new TreeMap<Integer, String>();
    private TreeMap<Integer, String> revMutations = new TreeMap<Integer, String>();
    private TreeMap<Integer, String> doubleMutations = new TreeMap<Integer, String>();
    // The following treemap is used when the user loaded the IMGT format for
    // his sequences
    private TreeMap<Integer, String> positionNucToCdrFrRegions = new TreeMap<Integer, String>();
    private TreeMap<Integer, String> positionAAToCdrFrRegions = new TreeMap<Integer, String>();
    private TreeMap<String, ArrayList<String>> regionToMutations = new TreeMap<String, ArrayList<String>>();
    private TreeMap<String, String> rToS_Ratio = new TreeMap<String, String>();
    private TreeMap<String, Integer> mutationsNumber_per_region = new TreeMap<String, Integer>();
    private String[] regions = {"FR1", "CDR1", "FR2", "CDR2", "FR3", "CDR3", "FR4"};
    private int maxNumberOfButtons = 0;
    private ArrayList<JButton> allButtons = new ArrayList<JButton>();
    private String nodeId;
    private String nodeSequence;
    private NodeGraph node;
    private Border redBorder = new LineBorder(new Color(Integer.parseInt("FF3030", 16)), 2);
    private Border greenBorder = new LineBorder(new Color(50, 205, 50), 2);
    private Color buttonColor = new Color(255, 252, 186);
    private Color greyColor = new Color(239, 239, 239);
    private JMenu exportMenu = new JMenu("Export");
    private JMenuItem exportItemEPS = new JMenuItem("Export image as .EPS");
    private JMenuItem exportItemPNG = new JMenuItem("Export image as .PNG");
    private JMenu showMenu = new JMenu("Show");
    private JRadioButton showDnaItem = new JRadioButton("Show DNA sequence");
    private JRadioButton showAAItem = new JRadioButton("Show protein sequence");
    static String dnaString = "dna";
    static String aaString = "aa";
    private TreeMap<Integer, String> fromPositionToKabatnumbering = new TreeMap<Integer, String>();
    private GetSetCurrentDirectory getSetCurrentDir;
    private ExportAsEPSListener exportAsEPSListener;
    private SaveImageAsPngListener saveImageAsPngListener;


    public MutationsFrame(final IgTreePanel igTreePanel, String mutationsWithParent,
                          final TreeMap<String, MutationsGraph> allMutationsGraph, NodeGraph node, NodeGraph rootNode,
                          SetImgtInfo setImgtInfo, boolean isDNA, GetSetCurrentDirectory getSetCurrentDir) {
        this.getSetCurrentDir = getSetCurrentDir;
        this.node = node;
        this.nodeId = node.getNodeId();
        this.nodeSequence = node.getSequence();
        this.igTreePanel = igTreePanel;
        this.isDNA = isDNA;
        this.mutationsWithParent = mutationsWithParent;
        dnaMutationsWithParent = mutationsWithParent;
        this.allMutationsGraph = allMutationsGraph;
        this.rootNodeSequence = rootNode.getSequence();
        rootNodeProtSequence = rootNode.getProteinSequence();
        this.setTitle("Mutations for " + nodeId);
        boolean exceptionCatched = false;
        try {
            // we get the kabat numbering
            String protSeqForKabat = rootNode.getProteinSequence();//before was node
            ProcessKabatNumbering processKabatNumbering = new ProcessKabatNumbering(protSeqForKabat);
            fromPositionToKabatnumbering = processKabatNumbering.getFromPositionToKabatnumbering();
        } catch (IOException e) {
            exceptionCatched = true;
            JOptionPane.showMessageDialog(this,
                    "No Kabat numbering could be found.\n We use the default amino acid position.",
                    "Could not connect to Abnum web site", JOptionPane.WARNING_MESSAGE);
        }
        if (fromPositionToKabatnumbering.size() == 0 && !exceptionCatched) {
            JOptionPane.showMessageDialog(this,
                    "Because there are some deletion(s) or X amino acid in this sequence, no Kabat numbering could be found.\n We use the default amino acid position.",
                    "No Kabat numbering found", JOptionPane.WARNING_MESSAGE);
        }
        if (setImgtInfo != null) {
            positionNucToCdrFrRegions = setImgtInfo.getPositionNucToCdrFrRegions();
            positionAAToCdrFrRegions = setImgtInfo.getPositionAAToCdrFrRegions();
        }
        if (positionNucToCdrFrRegions.size() == 0) { // no IMGT info available
            this.setSize(200, 300);
            this.setResizable(false);
            this.setBackground(Color.WHITE);
            this.setLocationRelativeTo(null);
            initComponents();
            jScrollPane.getViewport().setView(jPanel);
            jPanel.setOpaque(true);
            this.getContentPane().add(jScrollPane);
        } else {
            assignMutationToARegion();
            calculateRToSRatio();
            this.setResizable(false);
            this.setBackground(greyColor);
            this.setLocationRelativeTo(null);
            jPanel = new JPanel(new GridBagLayout());
            int height = maxNumberOfButtons * 35 + 130; // 30
            jPanel.setPreferredSize(new Dimension(730, height));
            initComponentsForDisplayedCdrFrRegions();
            jScrollPane.getViewport().setView(jPanel);
            jPanel.setOpaque(true);
            this.getContentPane().add(jScrollPane);
        }
        // we add the possibility to export these mutations (with or without the
        // FR/CDR info)
        exportMenu.add(exportItemEPS);
        exportAsEPSListener = new ExportAsEPSListener(jPanel, getSetCurrentDir);
        exportItemEPS.addActionListener(exportAsEPSListener);
        exportMenu.add(exportItemPNG);
        saveImageAsPngListener = new SaveImageAsPngListener(jPanel, getSetCurrentDir);
        exportItemPNG.addActionListener(saveImageAsPngListener);
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        showMenu.add(showDnaItem);
        if (isDNA) {
            showDnaItem.setSelected(true);
        } else {
            showAAItem.setSelected(true);
            showDnaItem.setEnabled(false);
        }
        showDnaItem.setActionCommand(dnaString);
        showDnaItem.addActionListener(this);
        showMenu.add(showAAItem);
        showAAItem.setActionCommand(aaString);
        showAAItem.addActionListener(this);
        bar.add(showMenu);
        bar.add(exportMenu);
        // Group the radio buttons, like this only one can be selected
        ButtonGroup group = new ButtonGroup();
        group.add(showDnaItem);
        group.add(showAAItem);

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                for (Entry<String, MutationsGraph> entry : allMutationsGraph.entrySet()) {
                    MutationsGraph mutationsGraph = entry.getValue();
                    mutationsGraph.setPink(false);
                    mutationsGraph.setBoxed(0);
                    mutationsGraph.setYellow(false);
                }
                igTreePanel.setEveryThingIsGrey(false);
                igTreePanel.repaint();
                dispose();
            }
        });
        this.pack();
        this.setVisible(true);
    }

    private void initComponentsForDisplayedCdrFrRegions() {
        // First we set a Map where the key is the region and the value an array
        // which contains the mutations
        // Then we display the CDR and FR regions
        jPanel.setBackground(greyColor);
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 7;
        c.gridx = 0;
        c.gridy = 0;
        cdrFrImagePanel = new CdrFrImagePanel();
        cdrFrImagePanel.setPreferredSize(new Dimension(730, 90));
        jPanel.add(cdrFrImagePanel, c);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        // we put mutations below each region: FR1, CDR1, FR2, CDR2, FR3, CDR3,
        // FR4
        int leftInset = 13;
        int rightInset = 0;
        for (String region : regions) {
            // logger.warn("Process region " + region);
            c.gridy = 1;
            switch (region) {
                case "FR1":
                    c.gridx = 0;
                    break;
                case "CDR1":
                    c.gridx = 1;
                    leftInset = 12;
                    rightInset = 1;
                    break;
                case "FR2":
                    c.gridx = 2;
                    leftInset = 11;
                    rightInset = 1;
                    break;
                case "CDR2":
                    c.gridx = 3;
                    leftInset = 11;
                    rightInset = 2;
                    break;
                case "FR3":
                    c.gridx = 4;
                    leftInset = 10;
                    rightInset = 3;
                    break;
                case "CDR3":
                    c.gridx = 5;
                    leftInset = 11;
                    rightInset = 1;
                    break;
                case "FR4":
                    c.gridx = 6;
                    leftInset = 9;
                    rightInset = 10;
                    break;
            }
            c.insets = new Insets(3, leftInset, 3, rightInset);
            ArrayList<String> mutations = regionToMutations.get(region);
            if (mutations == null) {
                JPanel littlePanel = new JPanel();
                littlePanel.setPreferredSize(new Dimension(90, 30));
                littlePanel.setBackground(greyColor);
                jPanel.add(littlePanel, c); // new JLabel(" ")
            } else {
                for (String mutation : mutations) {
                    String[] muta = mutation.split(":");
                    String position = muta[0];
                    String mutatio = muta[1];
                    int newPosition = Integer.parseInt(position) + 1;
                    String mutationInTheProg = position.toString() + ":" + mutatio;
                    JButton jButton = new JButton(new HighLightAction(position, mutationInTheProg));
                    jButton.setPreferredSize(new Dimension(90, 30)); // 90 25
                    jButton.setFont(new Font("Arial", Font.PLAIN, 15));
                    // check if this mutation is shared elsewhere, if yes the
                    // button will have some yellow background
                    for (Entry<String, MutationsGraph> entry : allMutationsGraph.entrySet()) {
                        MutationsGraph mutationsGraph = entry.getValue();
                        String allMutations = mutationsGraph.getMutationsWithParent();
                        if (!isDNA) {
                            allMutations = mutationsGraph.getAaMutationsWithParent();
                        }
                        if (allMutations != null && !nodeId.equals(entry.getKey())) {
                            if (allMutations.matches(".*," + position + ":.*")) {
                                jButton.setBackground(buttonColor);
                                if (OS.matches(".*mac.*"))// in order the color
                                // of the button
                                // appears on MAC
                                {
                                    jButton.setOpaque(true);
                                }
                            }
                        }
                    }
                    if (!isDNA) {
                        jButton.setBorder(redBorder);
                    } else {
                        // The border of the button will be in green if it's a
                        // silent mutation and in red if it's not
                        int pos = Integer.parseInt(position);
                        String codon;
                        String codonParent;
                        String parentSequence = node.getParent().getSequence();
                        if (!isDNA) {
                            codon = String.valueOf(nodeSequence.charAt(pos));
                            codonParent = String.valueOf(rootNodeProtSequence.charAt(pos));
                        } else {
                            codon = new Codon(nodeSequence, pos).getCodon();
                            codonParent = new Codon(parentSequence, pos).getCodon();
                        }
                        Translator translator = new Translator(codon, isDNA);
                        String aa = translator.getProteinSequence();
                        translator = new Translator(codonParent, isDNA);
                        String parentAA = translator.getProteinSequence();
                        if (aa.equals(parentAA)) {
                            jButton.setBorder(greenBorder);
                        } else {
                            jButton.setBorder(redBorder);
                        }
                    }
                    String positionToWrite = String.valueOf(newPosition);
                    // in the case we have the kabat Numbering we display it
                    //chnaged the 22.10.19 because the Kabat website does not work anamyore
                    if (!isDNA && fromPositionToKabatnumbering.containsKey(Integer.valueOf(position))) {
                        positionToWrite = fromPositionToKabatnumbering.get(Integer.valueOf(position));
                    }
                    String text = positionToWrite + ": " + mutatio;
                    jButton.setText(text);
                    jPanel.add(jButton, c);
                    allButtons.add(jButton);
                    c.gridy += 1;
                }
            }
        }

    }

    /*
     * The use wants to see the AA mutations, we have to change the DNA
     * mutations with the AA mutations
     */
    private String processAAMutationsWithParent() {
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
        return aaMutations;
    }

    private void assignMutationToARegion() {
        String[] mut = mutationsWithParent.split(",");
        for (int i = 1; i < mut.length; i++) {
            String[] muta = mut[i].split(":");
            String position = muta[0];
            int pos = Integer.parseInt(position);
            String region = positionNucToCdrFrRegions.get(pos);
            if (!isDNA) {
                region = positionAAToCdrFrRegions.get(pos);
            }
            ArrayList<String> mutations = new ArrayList<String>();
            if (regionToMutations.containsKey(region)) {
                mutations = regionToMutations.get(region);
            }
            mutations.add(mut[i]);

            int buttonsNumber = mutations.size();
            if (buttonsNumber > maxNumberOfButtons) {
                maxNumberOfButtons = buttonsNumber;
            }
            regionToMutations.put(region, mutations);
        }
    }

    private void calculateRToSRatio() {
        String parentSequence = node.getParent().getSequence();
        for (String region : regionToMutations.keySet()) {
            ArrayList<String> mutations = regionToMutations.get(region);
            mutationsNumber_per_region.put(region, mutations.size());
            int R = 0;
            int S = 0;
            for (String mutation : mutations) {
                String[] muta = mutation.split(":");
                String position = muta[0];
                int pos;
                pos = Integer.valueOf(position);
                String codon = new Codon(nodeSequence, pos).getCodon();
                String codonParent = new Codon(parentSequence, pos).getCodon();
                Translator translator = new Translator(codon, true);
                String aa = translator.getProteinSequence();
                translator = new Translator(codonParent, true);
                String parentAA = translator.getProteinSequence();
                if (aa.equals(parentAA)) { // this is a silent mutation
                    S++;
                } else {
                    R++;
                }
            }
            String rToS = R + "/" + S;
            rToS_Ratio.put(region, rToS);
        }

    }

    /**
     * This method initialize components in the case we dont have the IMGT data
     */
    private void initComponents() {

        String[] mut = mutationsWithParent.split(",");
        for (int i = 1; i < mut.length; i++) {
            String[] muta = mut[i].split(":");
            String position = muta[0];
            String mutation = muta[1];
            String[] nucleotides = mutation.split("->");
            String nuc1 = nucleotides[0];
            String nuc2 = nucleotides[1];
            int index;
            index = Integer.valueOf(position);
            String rootNuc = "";
            if (isDNA) {
                rootNuc = rootNodeSequence.substring(index, index + 1);
            } else
            // we have a prot seq
            {
                rootNuc = rootNodeProtSequence.substring(index, index + 1);
            }
            if (nuc2.equals(rootNuc)) { // it is a reverse!
                revMutations.put(Integer.valueOf(position), mutation);
            } else if (!nuc1.equals(rootNuc)) { // it is a double mutation!
                doubleMutations.put(Integer.valueOf(position), mutation);
            } else { // it is a single mutation
                mutations.put(Integer.valueOf(position), mutation);
            }
        }
        int jPanelRows = 0;
        if (mutations.size() > 0) {
            setPanel(false, false, true);
            jPanelRows += 2;
        }
        if (revMutations.size() > 0) {
            setPanel(false, true, false);
            jPanelRows += 2;
        }
        if (doubleMutations.size() > 0) {
            setPanel(true, false, false);
            jPanelRows += 2;
        }
        SpringUtilities.makeCompactGrid(jPanel, jPanelRows, 1, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad
    }

    public void setPanel(boolean doubleMut, boolean reverseMut, boolean singleMut) {
        String textLabel = "Single mutations: ";
        TreeMap<Integer, String> specialMutations = new TreeMap<Integer, String>();
        if (doubleMut) {
            textLabel = "Double mutations: ";
            specialMutations = doubleMutations;
        } else if (reverseMut) {
            textLabel = "Reverse mutations: ";
            specialMutations = revMutations;
        } else if (singleMut) {
            specialMutations = mutations;
        }
        JLabel label = new JLabel(textLabel);
        label.setFont(new Font("Arial", Font.PLAIN, 17));
        jPanel.add(label);
        JPanel panel = new JPanel(new SpringLayout());
        Integer[] positions = new Integer[specialMutations.size()];
        specialMutations.keySet().toArray(positions);
        Arrays.sort(positions); // we sort the position
        for (Integer position : positions) {
            // we add +1 to the position!!
            Integer newPosition = position + 1;
            String mutationInTheProg = position.toString() + ":" + specialMutations.get(position);
            JButton jButton = new JButton(new HighLightAction(position.toString(), mutationInTheProg));
            jButton.setPreferredSize(new Dimension(90, 30));
            jButton.setFont(new Font("Arial", Font.PLAIN, 15));
            // check if this mutation is shared elsewhere, if yes the button
            // will have some yellow background
            for (Entry<String, MutationsGraph> entry : allMutationsGraph.entrySet()) {
                MutationsGraph mutationsGraph = entry.getValue();
                String mutations = mutationsGraph.getMutationsWithParent();
                if (!isDNA) {
                    mutations = mutationsGraph.getAaMutationsWithParent();
                }
                if (mutations != null && !nodeId.equals(entry.getKey())) {
                    if (mutations.matches(".*," + position.toString() + ":.*")) {
                        jButton.setBackground(buttonColor);
                        if (OS.matches(".*mac.*"))// in order the color of the
                        // button appears on MAC
                        {
                            jButton.setOpaque(true);
                        }
                    }
                }
            }
            // if we process AA it will be red in all cases!
            if (!isDNA) {
                jButton.setBorder(redBorder);
            } else {
                // The border of the button will be in green if it's a silent
                // mutation and in red if it's not

                int pos = position.intValue();
                String parentSequence = node.getParent().getSequence();
                String codon = new Codon(nodeSequence, pos).getCodon();
                String codonParent = new Codon(parentSequence, pos).getCodon();
                Translator translator = new Translator(codon, isDNA);
                String aa = translator.getProteinSequence();
                translator = new Translator(codonParent, isDNA);
                String parentAA = translator.getProteinSequence();
                if (aa.equals(parentAA)) {
                    jButton.setBorder(greenBorder);
                } else {
                    jButton.setBorder(redBorder);
                }
            }
            String positionToWrite = String.valueOf(newPosition);
            if (fromPositionToKabatnumbering.containsKey(Integer.valueOf(position)))
                if (!isDNA) {
                    positionToWrite = fromPositionToKabatnumbering.get(Integer.valueOf(position));
                }
            String text = positionToWrite + ": " + specialMutations.get(position);
            jButton.setText(text);
            panel.add(jButton);
            allButtons.add(jButton);
        }
        double div = (double) positions.length / 4;
        if (positions.length % 4 != 0) {
            div += 0.25;
        }
        int r = (int) Math.ceil(div);
        int cols = 4;
        int mutationsNumber = positions.length;
        while (mutationsNumber != (r * cols)) {
            JLabel lab = new JLabel("");
            lab.setPreferredSize(new Dimension(90, 30));
            panel.add(lab);
            mutationsNumber++;
        }
        // int rows = (int) Math.floor((double)positions.length / 4);
        SpringUtilities.makeCompactGrid(panel, r, cols, // rows, cols
                6, 6, // initX, initY
                5, 5); // xPad, yPad

        jPanel.add(panel);
    }


    /**
     * @return the mutationsWithParent
     */
    public String getMutationsWithParent() {
        return mutationsWithParent;
    }

    /**
     * @param mutationsWithParent the mutationsWithParent to set
     */
    public void setMutationsWithParent(String mutationsWithParent) {
        this.mutationsWithParent = mutationsWithParent;
    }

    /**
     * This class will go through all the MutationGraph objects in order to
     * highlight the mutation numbers that get the selected mutation in common.
     *
     * @author Mathilde
     */
    class HighLightAction extends AbstractAction {
        String position;
        String mutation;
        String aminoAcid;
        Translator translator;
        TreeMap<String, Integer> aaToBoxes = new TreeMap<>();
        int boxesCounter = 1;

        public HighLightAction(String position, String mutation) {
            super(mutation);
            this.position = position;
            this.mutation = mutation;
            if (isDNA) {
                String codon = new Codon(nodeSequence, Integer.valueOf(position)).getCodon();
                translator = new Translator(codon, isDNA);
                aminoAcid = translator.getProteinSequence();
            } else {
                aminoAcid = mutation.split("->")[1];
            }
            aaToBoxes.put(aminoAcid, 1);
        }

        public void actionPerformed(ActionEvent e) {
            // We change the text color of the button to show better that it was
            // selected
            JButton but = (JButton) e.getSource();
            but.setForeground(new Color(181, 0, 0));
            for (JButton otherButton : allButtons) {
                if (otherButton != but) {
                    otherButton.setForeground(Color.black);
                }
            }

            // here check the other mutationsGraph to highlight them!!
            for (Entry<String, MutationsGraph> entry : allMutationsGraph.entrySet()) {
                MutationsGraph mutationsGraph = entry.getValue();
                String mutations = mutationsGraph.getMutationsWithParent();
                if (!isDNA) {
                    mutations = mutationsGraph.getAaMutationsWithParent();
                }
                if (mutations != null) {
                    // First, we put in yellow the nucleotidic mutation that are
                    // the same
                    // then in pink if they occur at the same position but are
                    // different
                    boolean hasMutation = false;
                    if (mutations.matches(".*," + mutation + ",.*")) {
                        mutationsGraph.setYellow(true);
                        mutationsGraph.setPink(false);
                        hasMutation = true;
                    } else if (mutations.matches(".*," + position + ":.*")) {
                        mutationsGraph.setPink(true);
                        mutationsGraph.setYellow(false);
                        hasMutation = true;
                    }
                    // Second, we put some box to show the same amino acid
                    // changes
                    if (hasMutation) {
                        String aa;
                        String parentAA;
                        if (isDNA) {
                            String parentSequence = mutationsGraph.getNode().getParent().getSequence();
                            String codon = new Codon(mutationsGraph.getNodeSequence(), Integer.valueOf(position))
                                    .getCodon();
                            String codonParent = new Codon(parentSequence, Integer.valueOf(position)).getCodon();
                            translator = new Translator(codon, isDNA);
                            aa = translator.getProteinSequence();
                            translator = new Translator(codonParent, isDNA);
                            parentAA = translator.getProteinSequence();
                        } else {
                            aa = String.valueOf(
                                    mutationsGraph.getNodeProtSequence().charAt(Integer.valueOf(position)));
                            parentAA = String.valueOf(mutationsGraph.getNode().getParent().getProteinSequence()
                                    .charAt(Integer.valueOf(position)));
                        }
                        int aaPosition = Integer.parseInt(position);
                        if (isDNA) {
                            aaPosition = Integer.parseInt(position) / 3;
                        }
                        aaPosition += 1; // we add +1 for the user
                        String positionToWrite = String.valueOf(aaPosition);
                       if (fromPositionToKabatnumbering.containsKey(aaPosition - 1)) {
                            positionToWrite = fromPositionToKabatnumbering.get(aaPosition - 1);
                        }
                        mutationsGraph.setAAchange(positionToWrite + ": " + parentAA + "->" + aa);
                        if (aaToBoxes.containsKey(aa)) {
                            int boxesNumber = aaToBoxes.get(aa).intValue();
                            mutationsGraph.setBoxed(boxesNumber);
                        } else {
                            boxesCounter += 1;
                            aaToBoxes.put(aa, boxesCounter);
                            mutationsGraph.setBoxed(boxesCounter);
                        }
                    } else {
                        mutationsGraph.setBoxed(0);
                        mutationsGraph.setPink(false);
                        mutationsGraph.setYellow(false);
                    }
                }
            }
            igTreePanel.setEveryThingIsGrey(true); // used for the fading effect
            igTreePanel.repaint();
        }

    }

    /**
     * This class draws the CDR FR regions in a JPanel
     *
     * @author Mathilde
     */
    private class CdrFrImagePanel extends JPanel {

        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int x = 10;
            drawRegion(g2, false, 1, x);
            x += 100 + 2;
            drawRegion(g2, true, 1, x);
            x += 100 + 2;
            drawRegion(g2, false, 2, x);
            x += 100 + 2;
            drawRegion(g2, true, 2, x);
            x += 100 + 2;
            drawRegion(g2, false, 3, x);
            x += 100 + 2;
            drawRegion(g2, true, 3, x);
            x += 100 + 2;
            drawRegion(g2, false, 4, x);
            x += 100 + 2;
        }

        private void drawRegion(Graphics2D g2, boolean isCDR, int regionNumber, int x) {
            Font fonte = new Font("Arial", Font.PLAIN, 22);
            g2.setFont(fonte);
            Color frColor = new Color(221, 255, 255);
            Color cdrColor = new Color(Integer.parseInt("FA8072", 16));
            Color myColor = frColor;
            if (isCDR) {
                myColor = cdrColor;
            }
            int y = 10;
            int width = 100;
            int height = 40;
            g2.setColor(myColor);
            g2.fillRect(x, y, width, height);
            myColor = Color.black;
            g2.setColor(myColor);
            g2.drawRect(x, y, width, height);
            String text = "FR";
            if (isCDR) {
                text = "CDR";
            }
            text += regionNumber;
            int xForText = x + 30;
            if (isCDR) {
                xForText -= 5;
            }
            g2.drawString(text, xForText, y + 27);
            if (rToS_Ratio.containsKey(text)) {
                String ratio = rToS_Ratio.get(text);
                g2.setFont(new Font("Arial", Font.PLAIN, 19));
                g2.drawString(ratio, x + 37, y + 67);
            }
        }
    }

    /*
     * This method will listen if the user choose the DNA or the AA mutations to
     * show in this frame If it is the AA, we want the Kabat Numbering
     */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(dnaString)) {
            isDNA = true;
            setMutationsWithParent(dnaMutationsWithParent);
        } else if (command.equals(aaString)) {
            isDNA = false;
            String aaMutations = processAAMutationsWithParent();
            setMutationsWithParent(aaMutations);
        }
        // we remove the previous display
        allButtons.clear();
        regionToMutations.clear();
        mutationsNumber_per_region.clear();
        revMutations.clear();
        doubleMutations.clear();
        mutations.clear();

        jScrollPane.remove(jPanel);
        getContentPane().remove(jScrollPane);
        jPanel = new JPanel(new SpringLayout());
        if (positionNucToCdrFrRegions.size() > 0) {
            assignMutationToARegion();
            jPanel = new JPanel(new GridBagLayout());
        }
        this.setResizable(false);
        this.setBackground(greyColor);
        if (positionNucToCdrFrRegions.size() > 0) {
            initComponentsForDisplayedCdrFrRegions();
        } else {
            initComponents();
        }

        exportItemEPS.removeActionListener(exportAsEPSListener);
        exportAsEPSListener = new ExportAsEPSListener(jPanel, getSetCurrentDir);
        exportItemEPS.addActionListener(exportAsEPSListener);
        exportItemPNG.removeActionListener(saveImageAsPngListener);
        saveImageAsPngListener = new SaveImageAsPngListener(jPanel, getSetCurrentDir);
        exportItemPNG.addActionListener(saveImageAsPngListener);
        jScrollPane.getViewport().setView(jPanel);
        jPanel.setOpaque(true);
        this.getContentPane().add(jScrollPane);
        this.pack();
    }
}
