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
 *  This is the biggest class (frame) of AncesTree GUI, it will draw a tree (made from a dnaml output file)
 *  and allows the user to interact with it.
 */

package ch.irb.IgGenealogicTreeViewer;

import java.awt.Adjustable;
import java.awt.Container;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import ch.irb.IgGenealogicTreeViewer.AncesTreeConverter.IgphymlTreeChooserFrame;
import org.apache.log4j.Logger;

import ch.irb.IgAlignment.IgAlignmentFrame;
import ch.irb.IgGenealogicTreeViewer.AncesTreeConverter.InputParser;
import ch.irb.IgGenealogicTreeViewer.AncesTreeConverter.DnamlTreeChooserFrame;
import ch.irb.ManageFastaFiles.FastaFormatException;
import ch.irb.currentDirectory.GetSetCurrentDirectory;
import ch.irb.nodes.Node;
import ch.irb.nodes.NodeGraph;
import ch.irb.saveImages.ExportAsEPSListener;
import ch.irb.saveImages.SaveImageAsPngListener;

/**
 * @author Mathilde This is the main frame of the AncesTree GUI. This frame will allow the user to create
 * a tree using a dnaml output file or to directly load
 * a XML file to display his tree of interest. Via this GUI, the user can also save the image of the tree in a
 * PNG format.
 */

@SuppressWarnings("serial")
public class IgTreeViewerFrame extends JFrame implements WindowListener {
    static Logger logger = Logger.getLogger(IgTreeViewerFrame.class);
    private String xmlFilePath = null;
    private IgTreeViewerFrame igTreeViewerFrame = this;
    private GetSetCurrentDirectory getSetCurrentDir = new GetSetCurrentDirectory();
    private IgTreeReader igTreeReader = null;
    private IgTreePanel igTreePanel = null;
    private LegendPanel legendPanel = null;
    private JScrollPane jScrollPane = null;
    private JMenuItem newTreeItem = new JMenuItem("Create new tree (dnaml)");
    private JMenuItem newIgphymlTreeItem = new JMenuItem("Create new tree (IgPhyML)");
    private JMenuItem openItem = new JMenuItem("Load tree");
    private JMenuItem saveTreeItem = new JMenuItem("Save tree");
    private JMenuItem loadCLIPData = new JMenuItem("Load BASELINe data");
    private JMenuItem alignmentItem = new JMenuItem("Alignment");
    private CLIPdata clipData = null;
    private JCheckBoxMenuItem fadeButton = new JCheckBoxMenuItem("Fading ON");
    private JMenuItem saveItem = new JMenuItem("Export image as .PNG");
    private JMenuItem saveItemWithLegend = new JMenuItem("Export image (with legend) as .PNG");
    private JMenuItem saveItemEPS = new JMenuItem("Export image as .EPS");
    private JMenuItem saveLegendEPS = new JMenuItem("Export legend as .EPS");
    private SaveTreeListener saveTreeListener;
    private AlignmentListener alignmentListener;
    private SaveImageAsPngListener saveItemListenerPanel;
    private SaveImageAsPngListener saveItemListenerWithLegend;
    private ExportAsEPSListener exportAsEPSListenerPanel;
    private ExportAsEPSListener exportAsEPSListenerLegend;
    private SpringLayout layout;
    private Container contentPane;
    private boolean isDnamlTree = true; // TODO to keep in case we use IgTreeMaker class (not used since 2016)

    /**
     * The first constructor (without argument) is used when we launch the GUI
     * from scratch (without selecting the fasta files)
     */
    public IgTreeViewerFrame() {
        super();
        try {
            java.net.URL url = ClassLoader.getSystemResource("ch/irb/IgGenealogicTreeMaker/resources/icon.png");
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(url);
            this.setIconImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setTitle("AncesTree");
        if (isDnamlTree) {
            this.setTitle("AncesTree (ig tree explorer)");
        }
        this.setSize(700, 900);
        this.setLocationRelativeTo(null);
        this.addWindowListener(this);
        final Container contentPane = this.getContentPane();
        this.contentPane = contentPane;
        final SpringLayout layout = new SpringLayout();
        this.layout = layout;
        contentPane.setLayout(layout);

        // Add a menu
        JMenu file = new JMenu("Menu");
        file.add(newTreeItem);
        newTreeItem.addActionListener(new CreateNewTreeListener());
        file.add(newIgphymlTreeItem);
        newIgphymlTreeItem.addActionListener(new CreateNewIgphymlTreeListener());
        file.add(openItem);
        saveTreeItem.setEnabled(false);
        file.add(saveTreeItem);
        file.add(alignmentItem);
        alignmentItem.setEnabled(false);
        file.add(loadCLIPData);
        loadCLIPData.setEnabled(false);
        loadCLIPData.addActionListener(new LoadClipDataListener());
        fadeButton.setSelected(true);
        fadeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                AbstractButton aButton = (AbstractButton) event.getSource();
                boolean selected = aButton.getModel().isSelected();
                igTreePanel.setFadingON(selected);
            }
        });
        file.add(fadeButton);
        JMenu exportMenu = new JMenu("Export");
        saveItem.setEnabled(false);
        exportMenu.add(saveItem);
        saveItemWithLegend.setEnabled(false);
        exportMenu.add(saveItemWithLegend);
        saveItemEPS.setEnabled(false);
        exportMenu.add(saveItemEPS);
        saveLegendEPS.setEnabled(false);
        exportMenu.add(saveLegendEPS);

        // adding action listener to menu items
        openItem.addActionListener(new LoadTreeListener());

        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        bar.add(file);
        bar.add(exportMenu);

        setVisible(true);
    }

    /**
     * The second constructor (with the XML file of the tree such as argument)
     * is used when we launch the GUI from scratch (without selecting the fasta
     * files)
     */
    public IgTreeViewerFrame(String xmlFilePath) {
        this();
        this.xmlFilePath = xmlFilePath;
        try {
            legendPanel = new LegendPanel(igTreePanel);
            igTreeReader = new IgTreeReader(xmlFilePath, isDnamlTree);
            igTreePanel = new IgTreePanel(igTreeReader, getSetCurrentDir);
            legendPanel.setColorByYear(igTreePanel.getColorByYear());
            String title = "AncesTree (ig tree viewer)              ";
            if (!isDnamlTree) {
                title = "AncesTree              ";
            }
            setTitle(title + igTreeReader.getProjectName());
            saveTreeListener = new SaveTreeListener();
            alignmentListener = new AlignmentListener();
            saveItemListenerPanel = new SaveImageAsPngListener(igTreePanel, getSetCurrentDir);
            saveItemListenerWithLegend = new SaveImageAsPngListener(getContentPane(), getSetCurrentDir);
            exportAsEPSListenerPanel = new ExportAsEPSListener(igTreePanel, getSetCurrentDir);
            exportAsEPSListenerLegend = new ExportAsEPSListener(legendPanel, getSetCurrentDir);
            saveTreeItem.addActionListener(saveTreeListener);
            alignmentItem.addActionListener(alignmentListener);
            saveItem.addActionListener(saveItemListenerPanel);
            saveItemWithLegend.addActionListener(saveItemListenerWithLegend);
            saveItemEPS.addActionListener(exportAsEPSListenerPanel);
            saveLegendEPS.addActionListener(exportAsEPSListenerLegend);
            jScrollPane = new JScrollPane();
            jScrollPane.getViewport().setView(igTreePanel);
            jScrollPane.addMouseWheelListener(new MyMouseWheelListerner());
            this.getContentPane().add(jScrollPane);
            this.getContentPane().add(legendPanel);
            layout.putConstraint(SpringLayout.EAST, legendPanel, 5, SpringLayout.EAST, contentPane);
            layout.putConstraint(SpringLayout.WEST, jScrollPane, 5, SpringLayout.WEST, contentPane);

            layout.putConstraint(SpringLayout.EAST, jScrollPane, 5, SpringLayout.WEST, legendPanel);
            layout.putConstraint(SpringLayout.NORTH, jScrollPane, 5, SpringLayout.NORTH, contentPane);

            layout.putConstraint(SpringLayout.WEST, contentPane, 5, SpringLayout.WEST, jScrollPane);
            layout.putConstraint(SpringLayout.SOUTH, contentPane, 5, SpringLayout.SOUTH, jScrollPane);
            layout.putConstraint(SpringLayout.NORTH, contentPane, 5, SpringLayout.NORTH, jScrollPane);

            saveTreeItem.setEnabled(true);
            saveItem.setEnabled(true);
            saveItemWithLegend.setEnabled(true);
            saveItemEPS.setEnabled(true);
            saveLegendEPS.setEnabled(true);
            if (igTreePanel.isImgtFormatLoaded()) {
                alignmentItem.setEnabled(true);
                loadCLIPData.setEnabled(true);
            } else {
                alignmentItem.setEnabled(false);
                loadCLIPData.setEnabled(false);
            }
            setVisible(true);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            JOptionPane.showMessageDialog(new JFrame(), "The XML file you loaded was not processed by dnaml.",
                    "Wrong XML file", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), e.getMessage(),
                    "Tree too big", JOptionPane.ERROR_MESSAGE);
            return;
        }

    }

    private String selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new XMLFilter());
        fileChooser.setCurrentDirectory(getSetCurrentDir.getCurrentDirectory());
        int returnVal = fileChooser.showDialog(this, "Select XML file");
        if (returnVal == 0) { // The user selected a XML file
            getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
            return (fileChooser.getSelectedFile()).getAbsolutePath();
        }
        return null;
    }

    /**
     * This class is used to load a XML file to display a new tree in the GUI
     */
    private class LoadTreeListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String newXMLFile = selectFiles();
            if (newXMLFile != null) {
                updateIgTreeViewerFrame(newXMLFile);
            }
        }

    }

    public void updateIgTreeViewerFrame(String newXMLFile) {
        xmlFilePath = newXMLFile;
        try {
            igTreeReader = new IgTreeReader(newXMLFile, isDnamlTree);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), e.getMessage(),
                    "Tree too big", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (jScrollPane != null) {
            jScrollPane.remove(igTreePanel);
            getContentPane().remove(jScrollPane);
        }
        if (legendPanel != null) {
            getContentPane().remove(legendPanel);
        }
        igTreePanel = new IgTreePanel(igTreeReader, getSetCurrentDir);
        // set the color to LegendPanel
        legendPanel = new LegendPanel(igTreePanel);
        legendPanel.setColorByYear(igTreePanel.getColorByYear());
        String title = "AncesTree (dnaml tree viewer)              ";
        if (!isDnamlTree) {
            title = "AncesTree              ";
        }
        setTitle(title + igTreeReader.getProjectName());
        jScrollPane = new JScrollPane();
        jScrollPane.getViewport().setView(igTreePanel);
        jScrollPane.removeMouseWheelListener(jScrollPane.getMouseWheelListeners()[0]);
        jScrollPane.addMouseWheelListener(new MyMouseWheelListerner());
        getContentPane().add(jScrollPane);
        getContentPane().add(legendPanel);
        layout.putConstraint(SpringLayout.EAST, legendPanel, 5, SpringLayout.EAST, contentPane);
        layout.putConstraint(SpringLayout.WEST, jScrollPane, 5, SpringLayout.WEST, contentPane);

        layout.putConstraint(SpringLayout.EAST, jScrollPane, 5, SpringLayout.WEST, legendPanel);
        layout.putConstraint(SpringLayout.NORTH, jScrollPane, 5, SpringLayout.NORTH, contentPane);

        layout.putConstraint(SpringLayout.WEST, contentPane, 5, SpringLayout.WEST, jScrollPane);
        layout.putConstraint(SpringLayout.SOUTH, contentPane, 5, SpringLayout.SOUTH, jScrollPane);
        layout.putConstraint(SpringLayout.NORTH, contentPane, 5, SpringLayout.NORTH, jScrollPane);

        if (saveItem.isEnabled()) { // we have to remove the previous listener
            // because the image from the
            // previous tree will be saved, not the new
            // one
            saveTreeItem.removeActionListener(saveTreeListener);
            alignmentItem.removeActionListener(alignmentListener);
            saveItem.removeActionListener(saveItemListenerPanel);
            saveItemWithLegend.removeActionListener(saveItemListenerWithLegend);
            saveItemEPS.removeActionListener(exportAsEPSListenerPanel);
            saveLegendEPS.removeActionListener(exportAsEPSListenerLegend);
        }
        saveTreeItem.setEnabled(true);
        saveItem.setEnabled(true);
        saveItemEPS.setEnabled(true);
        saveItemWithLegend.setEnabled(true);
        saveLegendEPS.setEnabled(true);
        saveTreeListener = new SaveTreeListener();
        alignmentListener = new AlignmentListener();
        saveItemListenerPanel = new SaveImageAsPngListener(igTreePanel, getSetCurrentDir);
        saveItemListenerWithLegend = new SaveImageAsPngListener(getContentPane(), getSetCurrentDir);
        exportAsEPSListenerPanel = new ExportAsEPSListener(igTreePanel, getSetCurrentDir);
        exportAsEPSListenerLegend = new ExportAsEPSListener(legendPanel, getSetCurrentDir);
        saveTreeItem.addActionListener(saveTreeListener);
        alignmentItem.addActionListener(alignmentListener);
        saveItem.addActionListener(saveItemListenerPanel);
        saveItemWithLegend.addActionListener(saveItemListenerWithLegend);
        saveItemEPS.addActionListener(exportAsEPSListenerPanel);
        saveLegendEPS.addActionListener(exportAsEPSListenerLegend);
        if (igTreePanel.isImgtFormatLoaded()) {
            alignmentItem.setEnabled(true);
            loadCLIPData.setEnabled(true);
        } else {
            alignmentItem.setEnabled(false);
            loadCLIPData.setEnabled(false);
        }
        setVisible(true);

    }

    private class CreateNewTreeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                @SuppressWarnings("unused")
                DnamlTreeChooserFrame dnamlTreeChooserFrame = new DnamlTreeChooserFrame(igTreeViewerFrame);
            } catch (FastaFormatException e) {
                e.printStackTrace();
            }
        }

    }

    private class CreateNewIgphymlTreeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                @SuppressWarnings("unused")
                IgphymlTreeChooserFrame dnamlTreeChooserFrame = new IgphymlTreeChooserFrame(igTreeViewerFrame);
            } catch (FastaFormatException e) {
                e.printStackTrace();
            }
        }

    }

    private class SaveTreeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            saveTree();
        }

    }

    private void saveTree() {
        try {
            String tmpFilePath = xmlFilePath + "_TMP.xml";
            JAXBContext context = JAXBContext.newInstance(InputParser.class);
            Unmarshaller um = context.createUnmarshaller();
            FileReader fileReader = new FileReader(xmlFilePath);
            InputParser igTreeMaker = (InputParser) um.unmarshal(fileReader);
            // Modify the Node into IgTreeMaker, if the user add the EC 50
            ArrayList<NodeGraph> nodeGraphs = igTreePanel.getAllNodeGraphs();
            Node rootNode = igTreeMaker.getRootNode();
            for (NodeGraph nodeGraph : nodeGraphs) {
                // we process this only when the use add or change the EC 50,
                // comment1 and comment2
                String infoFromNode = nodeGraph.getEC50();
                String dataType = "EC50";
                processNodeToAddInfo(rootNode, nodeGraph, infoFromNode, dataType);
                infoFromNode = nodeGraph.getComment1();
                dataType = "comment1";
                processNodeToAddInfo(rootNode, nodeGraph, infoFromNode, dataType);
                infoFromNode = nodeGraph.getComment2();
                dataType = "comment2";
                processNodeToAddInfo(rootNode, nodeGraph, infoFromNode, dataType);
            }
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            Writer w = null;
            w = new FileWriter(tmpFilePath);
            m.marshal(igTreeMaker, w);
            fileReader.close();
            w.close();

            // Once everything is complete, delete old file..
            File oldFile = new File(xmlFilePath);
            oldFile.delete();

            // And rename tmp file's name to old file name
            File newFile = new File(tmpFilePath);
            newFile.renameTo(oldFile);

        } catch (JAXBException | IOException e) {
            e.printStackTrace();
        }
        // we dont want the user to be prompt again if he close the window and
        // had already saved the tree
        igTreePanel.setNewDataSaved(false);
    }

    private void processNodeToAddInfo(Node node, NodeGraph nodeGraph, String infoFromNode, String dataType) {
        String info = null;
        if (infoFromNode != null) {
            info = nodeGraph.getNodeId() + ":" + infoFromNode + "//";
        }

        // we have the same node, we set the EC50
        if (node.getNodeId().equals(nodeGraph.getNodeId())) {
            if (dataType.equals("EC50")) {
                node.setEC50(info);
            } else if (dataType.equals("comment1")) {
                node.setComment1(info);
            } else if (dataType.equals("comment2")) {
                node.setComment2(info);
            }
            return;
        } else if (node.getNodeId().matches(".*" + nodeGraph.getNodeId() + ".*")) { // node
            // graph
            // is
            // a
            // duplicate
            String previousInfo = null;
            if (dataType.equals("EC50")) {
                previousInfo = node.getEC50();
            } else if (dataType.equals("comment1")) {
                previousInfo = node.getComment1();
            } else if (dataType.equals("comment2")) {
                previousInfo = node.getComment2();
            }

            if (previousInfo == null) {
                previousInfo = "";
            }
            String infoToAdd = previousInfo;
            if (previousInfo.matches(".*" + nodeGraph.getNodeId() + ".*")) { // we
                // have
                // to
                // remove
                // the
                // old
                // Ec50
                int index = previousInfo.indexOf(nodeGraph.getNodeId());
                String interm = previousInfo.substring(index);
                int secIndex = interm.indexOf("//");
                String start = previousInfo.substring(0, index);
                String end = previousInfo.substring(secIndex + 2);
                infoToAdd = start + end;
            }
            if (info != null) {
                infoToAdd += info;
            }
            if (infoToAdd.length() == 0) {
                infoToAdd = null;
            }
            if (dataType.equals("EC50")) {
                node.setEC50(infoToAdd);
            } else if (dataType.equals("comment1")) {
                node.setComment1(infoToAdd);
            } else if (dataType.equals("comment2")) {
                node.setComment2(infoToAdd);
            }
            return;
        } else if (node.getChildrenForXmlFile() != null) { // we look into the
            // kids
            ArrayList<Node> children = node.getChildrenForXmlFile();
            for (int i = 0; i < children.size(); i++) {
                Node nod = children.get(i);
                processNodeToAddInfo(nod, nodeGraph, infoFromNode, dataType);
            }
        }
    }

    /**
     * This class is used to load the CLIP data coming from the website:
     * "http://clip.med.yale.edu/sel/index.php". These data are stored in a text
     * file that I parse. The coloring code for the pValue is the same than the
     * website. From now (14.06.13) we use BASELINe:
     * "http://selection.med.yale.edu/baseline/"
     */
    private class LoadClipDataListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(getSetCurrentDir.getCurrentDirectory());
            fileChooser.setFileFilter(new TxtFilter());
            fileChooser.setDialogTitle("Load BASELINe data");
            int choix = fileChooser.showOpenDialog(igTreePanel);
            if (choix != JFileChooser.APPROVE_OPTION) {
                return;
            }
            getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
            File file = fileChooser.getSelectedFile();
            String[] sigmaVals = new String[2];
            String[] pValues = new String[2];
            ClipParser clipParser = new ClipParser(file);
            sigmaVals = clipParser.getSigmaValues("All sequences combined");
            pValues = clipParser.getPValues("All sequences combined");
            if (pValues == null) {
                JOptionPane.showMessageDialog(contentPane,
                        "Any BASELINe data were found in " + file.getName() + " file!", "BASELINe data error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            ArrayList<String> igInOrder = clipParser.getIgInOrder();
            clipData = new CLIPdata(pValues[0], pValues[1], sigmaVals[0], sigmaVals[1]);
            igTreePanel.setClipData(clipData, clipParser, igInOrder);
            igTreeViewerFrame.setVisible(true);
        }

    }

    private class AlignmentListener implements ActionListener {

        @SuppressWarnings("unused")
        @Override
        public void actionPerformed(ActionEvent arg0) {
            IgAlignmentFrame igAlignmentFrame = new IgAlignmentFrame(igTreeReader, getSetCurrentDir);
        }

    }

    /**
     * @return the legendPanel
     */
    public LegendPanel getLegendPanel() {
        return legendPanel;
    }

    @Override
    public void windowActivated(WindowEvent arg0) {
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        // Check if the data were modified, if yes we open a dialog to allow the
        // user to save the tree
        if (xmlFilePath != null) {
            if (igTreePanel.isNewDataSaved()) {
                Object[] options = {"Save tree", "Don't save tree"};
                int n = JOptionPane.showOptionDialog(this, "Do you want to save the tree with the changes you made? ",
                        "Save tree", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                        options[0]);
                if (n == JOptionPane.YES_OPTION) {
                    saveTree();
                }
            }
        }
        // here we want to check that the user has saved the tree with the new
        // changes
        System.exit(EXIT_ON_CLOSE);
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
    }

    private class MyMouseWheelListerner implements MouseWheelListener {

        public void mouseWheelMoved(final MouseWheelEvent e) {
            if (e.isControlDown()) {
                int inc = 1;
                if (e.getWheelRotation() < 0) {
                    inc = 1;
                } else {
                    inc = -1;
                }
                Point pt = e.getPoint();
                igTreePanel.setScale(inc, pt);
            } else if (e.isShiftDown()) {
                // Horizontal scrolling
                Adjustable adj = jScrollPane.getHorizontalScrollBar();
                int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
                adj.setValue(adj.getValue() + scroll);
            } else {
                // Vertical scrolling
                Adjustable adj = jScrollPane.getVerticalScrollBar();
                int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
                adj.setValue(adj.getValue() + scroll);
            }
        }

    }

}
