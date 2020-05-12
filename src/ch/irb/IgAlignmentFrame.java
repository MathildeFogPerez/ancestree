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
   
 * This class launches a frame to show the alignment between the Igs coming from AncesTree
 * There are 3 modes: nucleotidic mode, where the mutations appear in gray for the GL, green for silent and red for non silent mutations.
 * An amino acid mode by chemistry, where the hydrophobic mutations are in yellow, the hydrophilic in green and the charged in blue.
 * An amini acid mode by highlighting, where the mutations are in gray for the GL and red for the Igs. 
 */

package ch.irb.IgAlignment;

import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;

import org.apache.log4j.Logger;

import ch.irb.IgGenealogicTreeMaker.Ig;
import ch.irb.IgGenealogicTreeViewer.IgTreeReader;
import ch.irb.IgGenealogicTreeViewer.SetImgtInfo;
import ch.irb.currentDirectory.GetSetCurrentDirectory;
import ch.irb.kabat.ProcessKabatNumbering;
import ch.irb.nodes.NodeGraph;
import ch.irb.saveImages.ExportAsEPSListener;
import ch.irb.saveImages.SaveImageAsPngListener;
import ch.irb.saveImages.TSVFilter;

@SuppressWarnings("serial")
public class IgAlignmentFrame extends JFrame {
    private JFrame thisFrame = this;
    static Logger logger = Logger.getLogger(IgAlignmentFrame.class);
    private ArrayList<NodeGraph> allNodeGraphs = new ArrayList<NodeGraph>();
    private ArrayList<NodeGraph> nodeGraphsToShow = new ArrayList<NodeGraph>();
    private JScrollPane jScrollPane = new JScrollPane();
    private AlignmentPanel alignmentPanel;
    private int mode = 1; // by default we display the nucleotide mode
    private boolean zoomMode = true;
    private JMenu exportMenu = new JMenu("Export");
    private JMenu showMenu = new JMenu("Show");
    private JMenu zoomMenu = new JMenu("Zoom");
    private JMenu modeMenu = new JMenu("Mode");
    private JMenuItem exportItemEPS = new JMenuItem("Export image as .EPS");
    private JMenuItem exportItem_shorted = new JMenuItem("Shrink image");
    private JMenuItem exportItem_withoutLetter = new JMenuItem("Mutations Map");
    private JMenuItem exportItemPNG = new JMenuItem("Export image as .PNG");
    private JMenuItem exportItemExcel = new JMenuItem("Export protein alignment as .TSV");
    private ExportAsEPSListener exportAsEPSListener;
    private SaveImageAsPngListener saveImageAsPngListener;
    private GetSetCurrentDirectory getSetCurrentDir;
    private JRadioButton zoomInButton = new JRadioButton("Zoom in");
    private JRadioButton zoomOutButton = new JRadioButton("Zoom out");
    private JRadioButton nucModeItem = new JRadioButton("Nucleotidic mode");
    private JRadioButton aaChemistryModeItem = new JRadioButton("Amino acid by chemistry mode");
    private JRadioButton aaHighlightingcModeItem = new JRadioButton("Amino acid by highlighting mode");
    final ArrayList<JCheckBoxMenuItem> checkList = new ArrayList<JCheckBoxMenuItem>();
    private HashMap<String, NodeGraph> fromIdToNodeGraph = new HashMap<String, NodeGraph>();
    public static final String ls = System.getProperty("line.separator");
    public static final String fs = System.getProperty("file.separator");

    public IgAlignmentFrame(IgTreeReader igTreeReader, final GetSetCurrentDirectory getSetCurrentDir) {
        this.getSetCurrentDir = getSetCurrentDir;
        try {
            java.net.URL url = ClassLoader.getSystemResource("ch/irb/IgGenealogicTreeMaker/resources/icon.png");
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(url);
            this.setIconImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setTitle("AncesTree Alignment");
        allNodeGraphs = igTreeReader.getAllNodeGraphs();
        for (NodeGraph nodeGraph : allNodeGraphs) {
            if (!nodeGraph.isADuplicateNode()) {
                nodeGraphsToShow.add(nodeGraph);
            }
        }
        alignmentPanel = new AlignmentPanel(nodeGraphsToShow, mode, zoomMode, null);
        alignmentPanel.setOpaque(true);
        jScrollPane.getViewport().setView(alignmentPanel);
        this.getContentPane().add(jScrollPane);

        // set the menus
        ModeListener modeListener = new ModeListener();
        // mode menu
        nucModeItem.setSelected(true);
        nucModeItem.addActionListener(modeListener);
        nucModeItem.setActionCommand("nucMode");
        nucModeItem.setToolTipText("green silent, red non silent mutations");
        aaChemistryModeItem.addActionListener(modeListener);
        aaChemistryModeItem.setActionCommand("aaChemMode");
        aaChemistryModeItem.setToolTipText("yellow hydrophobic, blue hydrophilic and green charged amino acids");
        aaHighlightingcModeItem.addActionListener(modeListener);
        aaHighlightingcModeItem.setActionCommand("aaHighMode");
        aaHighlightingcModeItem.setToolTipText("amino acid changes in red");
        modeMenu.add(nucModeItem);
        modeMenu.add(aaChemistryModeItem);
        modeMenu.add(aaHighlightingcModeItem);
        // Group the radio buttons, like this only one can be selected
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(nucModeItem);
        modeGroup.add(aaChemistryModeItem);
        modeGroup.add(aaHighlightingcModeItem);

        // show menu to select the sequences
        for (NodeGraph node : allNodeGraphs) {
            if (!node.isADuplicateNode()) {
                String id = node.getNodeId();
                if (node.hasDuplicateNodes()) {
                    ArrayList<NodeGraph> dup = node.getDuplicatedNodes();
                    for (NodeGraph du : dup) {
                        id += ", " + du.getNodeId();
                    }
                }
                fromIdToNodeGraph.put(id, node);
                JCheckBoxMenuItem check = new JCheckBoxMenuItem(id, true);
                checkList.add(check);
                // to avoid the menu to close
                check.setUI(new StayOpenCheckBoxMenuItemUI());
                if (node.isRoot()) {
                    check.setEnabled(false); // the UCA should be always checked
                }
                showMenu.add(check);
            }
        }
        JButton selectAllButton = new JButton("Select all");
        selectAllButton.setFont(new Font("Arial", Font.ITALIC, 15));
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (JCheckBoxMenuItem checkItem : checkList) {
                    checkItem.setSelected(true);
                }
            }
        });
        showMenu.add(selectAllButton);
        JButton deSelectAllButton = new JButton("Deselect all");
        deSelectAllButton.setFont(new Font("Arial", Font.ITALIC, 15));
        deSelectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (JCheckBoxMenuItem checkItem : checkList) {
                    if (checkItem.isEnabled()) {
                        checkItem.setSelected(false);
                    }
                }
            }
        });
        showMenu.add(deSelectAllButton);
        JButton deSelectBPButton = new JButton("Deselect all BPs");
        deSelectBPButton.setFont(new Font("Arial", Font.ITALIC, 15));
        deSelectBPButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (JCheckBoxMenuItem checkItem : checkList) {
                    if (checkItem.isEnabled() && String.valueOf(checkItem.getSelectedObjects()[0]).contains("BP")) {
                        checkItem.setSelected(false);
                    }
                }
            }
        });
        showMenu.add(deSelectBPButton);
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Arial", Font.PLAIN, 15));
        SelectedNodeListener selectedNodeListener = new SelectedNodeListener();
        okButton.addActionListener(selectedNodeListener);
        showMenu.add(okButton);
        // zoom in or out buttons
        zoomInButton.setSelected(true);
        zoomInButton.addActionListener(modeListener);
        zoomInButton.setActionCommand("zoomIn");
        zoomOutButton.addActionListener(modeListener);
        zoomOutButton.setActionCommand("zoomOut");
        zoomMenu.add(zoomInButton);
        zoomMenu.add(zoomOutButton);
        // Group the radio buttons, like this only one can be selected
        ButtonGroup zoomGroup = new ButtonGroup();
        zoomGroup.add(zoomInButton);
        zoomGroup.add(zoomOutButton);

        // export menu
        exportAsEPSListener = new ExportAsEPSListener(alignmentPanel, getSetCurrentDir);
        exportItemEPS.addActionListener(exportAsEPSListener);
        exportMenu.add(exportItemEPS);
        saveImageAsPngListener = new SaveImageAsPngListener(alignmentPanel, getSetCurrentDir);
        exportItemPNG.addActionListener(saveImageAsPngListener);
        exportMenu.add(exportItemPNG);
        exportItem_shorted.addActionListener(new ActionListener() {
            @SuppressWarnings("unused")
            @Override
            public void actionPerformed(ActionEvent arg0) {
                ExportShorterSequenceFrame exportShorterSequenceFrame = new ExportShorterSequenceFrame(false);
            }
        });
        exportMenu.add(exportItem_shorted);
        exportItem_withoutLetter.addActionListener(new ActionListener() {
            @SuppressWarnings("unused")
            @Override
            public void actionPerformed(ActionEvent arg0) {
                ExportShorterSequenceFrame exportShorterSequenceFrame = new ExportShorterSequenceFrame(true);
            }
        });
        exportMenu.add(exportItem_withoutLetter);
        exportItemExcel.addActionListener(new ActionListener() {
            @SuppressWarnings("unused")
            @Override
            public void actionPerformed(ActionEvent arg0) {
                ExportAsTSV export = new ExportAsTSV();
            }
        });
        exportMenu.add(exportItemExcel);
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        bar.add(modeMenu);
        // we allow the showMenu to be 'scrollable' if there are too many Igs
        if (allNodeGraphs.size() > 30) {
            MenuScroller.setScrollerFor(showMenu, 20, 75, 5, 8);
        }
        bar.add(showMenu);
        bar.add(zoomMenu);
        bar.add(exportMenu);
        this.pack();
        this.setVisible(true);
    }

    public void updateGUI(boolean zoomChanged) {
        TreeMap<Integer, Boolean> selectedColumns = null;
        if (zoomChanged) {
            selectedColumns = alignmentPanel.getSelectedColumns();
        }
        jScrollPane.remove(alignmentPanel);
        getContentPane().remove(jScrollPane);
        jScrollPane = new JScrollPane();
        alignmentPanel = new AlignmentPanel(nodeGraphsToShow, mode, zoomMode, selectedColumns);
        alignmentPanel.setOpaque(true);
        exportItemEPS.removeActionListener(exportAsEPSListener);
        exportAsEPSListener = new ExportAsEPSListener(alignmentPanel, getSetCurrentDir);
        exportItemEPS.addActionListener(exportAsEPSListener);
        exportItemPNG.removeActionListener(saveImageAsPngListener);
        saveImageAsPngListener = new SaveImageAsPngListener(alignmentPanel, getSetCurrentDir);
        exportItemPNG.addActionListener(saveImageAsPngListener);
        jScrollPane.getViewport().setView(alignmentPanel);
        getContentPane().add(jScrollPane);
        jScrollPane.revalidate();
        revalidate();
        repaint();
    }

    class SelectedNodeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() != null) {
                // logger.fatal("check button");
                nodeGraphsToShow = new ArrayList<NodeGraph>();
                for (JCheckBoxMenuItem checkItem : checkList) {
                    // logger.fatal("check list "+checkItem.getText());
                    if (checkItem.isSelected()) {
                        String id = checkItem.getText();
                        // logger.fatal("Is selected: " + id);
                        NodeGraph nodeG = fromIdToNodeGraph.get(id);
                        nodeGraphsToShow.add(nodeG);
                    }
                }
                updateGUI(true);
            }
        }

    }

    class StayOpenCheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {

        @Override
        protected void doClick(MenuSelectionManager msm) {
            menuItem.doClick(0);
        }
    }

    class ModeListener implements ActionListener {
        public ModeListener() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean zoomChanged = false;
            JRadioButton button = (JRadioButton) e.getSource();
            if (e.getActionCommand().equals("zoomIn")) {
                zoomChanged = true;
                if (button.isSelected()) {
                    zoomMode = true;
                } else {
                    zoomMode = false;
                }
            } else if (e.getActionCommand().equals("zoomOut")) {
                zoomChanged = true;
                if (button.isSelected()) {
                    zoomMode = false;
                } else {
                    zoomMode = true;
                }
            } else if (e.getActionCommand().equals("nucMode")) {
                if (button.isSelected()) {
                    mode = 1;
                }
            } else if (e.getActionCommand().equals("aaChemMode")) {
                if (button.isSelected()) {
                    mode = 2;
                }
            } else if (e.getActionCommand().equals("aaHighMode")) {
                if (button.isSelected()) {
                    mode = 3;
                }
            }
            updateGUI(zoomChanged);
        }
    }

    /*
     * This class opens a frame where the user can export the image in EPS or
     * PNG format. All the columns unselected are not show in this view.
     */
    class ExportShorterSequenceFrame extends JFrame {
        public ExportShorterSequenceFrame(boolean shorterView) {
            try {
                java.net.URL url = ClassLoader.getSystemResource("ch/irb/IgGenealogicTreeMaker/resources/icon.png");
                Toolkit kit = Toolkit.getDefaultToolkit();
                Image img = kit.createImage(url);
                this.setIconImage(img);
            } catch (Exception e) {
                e.printStackTrace();
            }
            JScrollPane jScrollPane = new JScrollPane();
            ExportAlignmentPanel exportAlignmentPanel = new ExportAlignmentPanel(alignmentPanel, shorterView);
            jScrollPane.getViewport().setView(exportAlignmentPanel);
            getContentPane().add(jScrollPane);
            // the menu
            JMenu exportMenu = new JMenu("Export");
            JMenuItem exportItemEPS = new JMenuItem("Export image as .EPS");
            JMenuItem exportItemPNG = new JMenuItem("Export image as .PNG");
            ExportAsEPSListener exportAsEPSListener = new ExportAsEPSListener(exportAlignmentPanel, getSetCurrentDir);
            exportItemEPS.addActionListener(exportAsEPSListener);
            exportMenu.add(exportItemEPS);
            SaveImageAsPngListener saveImageAsPngListener = new SaveImageAsPngListener(exportAlignmentPanel,
                    getSetCurrentDir);
            exportItemPNG.addActionListener(saveImageAsPngListener);
            exportMenu.add(exportItemPNG);
            JMenuBar bar = new JMenuBar();
            setJMenuBar(bar);
            bar.add(exportMenu);
            setTitle("Alignment image TO EXPORT");
            pack();
            setVisible(true);
        }
    }

    /*
     * This class is used to create an excel file of the alignment
     */
    class ExportAsTSV {

        private File file;
        private ArrayList<Ig> igs = new ArrayList<>();
        private int length = 0;
        private ArrayList<String> kabats = new ArrayList<>();
        private HashMap<Integer, String> posToConservedAA = new HashMap<>();
        private ArrayList<NodeGraph> orderedNodes = new ArrayList<>();
        private TreeMap<Integer, String> positionToCdrFrRegions;

        public ExportAsTSV() {
            selectFiles();
            processData();
            if (file != null) {
                try {
                    writeFile();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(thisFrame,
                            "You must close the file " + file.getName() + " in order to proceed", "Cannot access file",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }

        private void selectFiles() {
            final JFileChooser fileChooser = new JFileChooser() {
                public void approveSelection() {
                    File f = getSelectedFile();
                    if (!f.getAbsolutePath().matches(".*\\.tsv")) {
                        f = new File(f.getAbsolutePath() + ".tsv");
                    }
                    if (f.exists() && getDialogType() == SAVE_DIALOG) {
                        int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file",
                                JOptionPane.YES_NO_CANCEL_OPTION);
                        switch (result) {
                            case JOptionPane.YES_OPTION:
                                super.approveSelection();
                                return;
                            case JOptionPane.CANCEL_OPTION:
                                cancelSelection();
                                return;
                            default:
                                return;
                        }
                    }
                    super.approveSelection();
                }
            };
            fileChooser.setCurrentDirectory(getSetCurrentDir.getCurrentDirectory());
            fileChooser.setFileFilter(new TSVFilter());
            fileChooser.setDialogTitle("Save as .tsv");
            int choix = fileChooser.showSaveDialog(thisFrame);
            if (choix != JFileChooser.APPROVE_OPTION) {
                return;
            }
            getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
            file = fileChooser.getSelectedFile();
            if (!file.getAbsolutePath().matches(".*\\.tsv")) {
                if (!file.getAbsolutePath().matches("\\.")) {
                    file = new File(file.getAbsolutePath() + ".tsv");
                }
            }
        }

        private void processData() {
            orderedNodes = alignmentPanel.getOrderedNodeGraphs();
            for (NodeGraph node : orderedNodes) {
                if (node.isRoot()) {
                    // we set the conserved AA
                    length = node.getProteinSequence().length();
                    int i = 0;
                    for (char a : node.getProteinSequence().toCharArray()) {
                        posToConservedAA.put(i, String.valueOf(a));
                        i++;
                    }
                    // we get the kabat positions
                    TreeMap<Integer, String> fromPositionToKabatnumbering;
                    ProcessKabatNumbering processKabatNumbering;
                    try {
                        processKabatNumbering = new ProcessKabatNumbering(node.getProteinSequence());
                        fromPositionToKabatnumbering = processKabatNumbering.getFromPositionToKabatnumbering();
                        for (int pos = 0; pos < length; pos++) {
                            if (fromPositionToKabatnumbering.size() > 1) {
                                kabats.add(fromPositionToKabatnumbering.get(pos));
                            } else {
                                kabats.add(String.valueOf(pos + 1));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // we get the FR/CDR positions
                    SetImgtInfo setImgtInfo = new SetImgtInfo(node);
                    positionToCdrFrRegions = setImgtInfo.getPositionAAToCdrFrRegions();

                }
                if (!node.isADuplicateNode()) {
                    String id = node.getNodeId();
                    if (node.hasDuplicateNodes()) {
                        ArrayList<NodeGraph> dup = node.getDuplicatedNodes();
                        for (NodeGraph du : dup) {
                            id += ", " + du.getNodeId();
                        }
                    }
                    Ig ig = new Ig(id, node.getProteinSequence());
                    igs.add(ig);
                }
            }
            //logger.warn("Number of igs processed: " + igs.size());
        }

        private void writeFile() throws IOException {
            ArrayList<Integer> posToOneDisplay = new ArrayList<>();

            BufferedWriter out = new BufferedWriter(new FileWriter(file));

            for (int pos = 0; pos < length; pos++) {
                out.write("\t" + positionToCdrFrRegions.get(pos));
            }
            out.write(ls);
            for (String kab : kabats) {
                // logger.debug("Add kabat "+kabat);
                out.write("\t" + kab);
            }
            out.write(ls);
            igLoop:
            for (Ig ig : igs) {
                boolean igIsSelected = false;
                for (JCheckBoxMenuItem checkItem : checkList) {
                    if (checkItem.isSelected() && String.valueOf(checkItem.getSelectedObjects()[0]).equals(ig.getFastaId())) {
                        igIsSelected = true;
                        break;
                    }
                }
                if (!igIsSelected) {
                    continue igLoop;
                }
                out.write(ig.getFastaId());
                int pos = 0;
                for (char a : ig.getOriginalSequence().toCharArray()) {
                    String aa = String.valueOf(a);
                    String text = aa;
                    String conservedAA = posToConservedAA.get(pos);
                    // logger.debug("At kabat "+kabat+" the conserved AA is
                    // "+conservedAA+" and the AA is "+aa);
                    if (conservedAA.equals(aa)) { // same AA than the conserved
                        // one
                        if (posToOneDisplay.contains(pos)) { // the conserved AA
                            // is already
                            // shown we show
                            // a dot
                            text = ".";

                        } else {// we show the AA
                            posToOneDisplay.add(pos);
                        }
                    }
                    out.write("\t" + text);
                    pos++;
                }
                out.write(ls);
            }
            out.close();
        }
    }

}
