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

   This Frame will allow the user to select: the dnaml output file and the IMGT data fasta file.
   The IMGT data file is optional.
 */
package ch.irb.IgGenealogicTreeViewer.AncesTreeConverter;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import ch.irb.IgGenealogicTreeViewer.IgTreeViewerFrame;
import ch.irb.ManageFastaFiles.FastaFilter;
import ch.irb.ManageFastaFiles.FastaFormatException;
import ch.irb.currentDirectory.GetSetCurrentDirectory;

@SuppressWarnings("serial")
public class DnamlTreeChooserFrame extends JFrame implements ActionListener {

    private IgTreeViewerFrame igTreeViewerFrame;
    private GetSetCurrentDirectory getSetCurrentDir = new GetSetCurrentDirectory();
    private File imgtFile;
    private File dnamlFile;
    private boolean hasReadsInId = false;
    private JButton chooseDnamlButton = new JButton("Select the dnaml output file");
    private JButton loadIMGTFormatButton = new JButton("Load IMGT info");
    private JButton runButton = new JButton("RUN");
    private JCheckBox checkBoxNGS = new JCheckBox("NGS data: number of reads are in Ig ID");
    private Font font = new Font("Dialog", Font.PLAIN, 15);

    public static void main(String[] args) {
        try {
            @SuppressWarnings("unused")
            DnamlTreeChooserFrame frame = new DnamlTreeChooserFrame(null);
        } catch (FastaFormatException e) {
            e.printStackTrace();
        }
    }

    public DnamlTreeChooserFrame(IgTreeViewerFrame igTreeViewerFrame) throws FastaFormatException {
        this.igTreeViewerFrame = igTreeViewerFrame;
        //first we create an output folder if it doesnt exist yet
        String currentDirPath = System.getProperty("user.dir");
        File outputDir = new File(currentDirPath + System.getProperty("file.separator") + "output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        try {
            java.net.URL url = ClassLoader.getSystemResource("ch/irb/IgGenealogicTreeMaker/resources/icon.png");
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(url);
            this.setIconImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.setTitle("AncesTree: dnaml tree chooser");
        this.setSize(400, 200);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setBackground(Color.white);
        Container panel = this.getContentPane();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(Color.white);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        chooseDnamlButton.setFont(font);
        chooseDnamlButton.addActionListener(this);
        panel.add(chooseDnamlButton, c);
        c.gridx = 1;
        loadIMGTFormatButton.setFont(font);
        loadIMGTFormatButton.addActionListener(this);
        panel.add(loadIMGTFormatButton, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 5;
        JPanel whitePanel = new JPanel();
        whitePanel.setBackground(Color.white);
        panel.add(whitePanel, c);
        c.gridx = 0;
        c.gridy = 6;
        c.gridheight = 1;
        runButton.addActionListener(this);
        // the runButton is enabled until all the fasta files are selected
        runButton.setEnabled(false);
        runButton.setFont(new Font("Dialog", Font.BOLD, 23));
        panel.add(runButton, c);
        JPanel whitePanel2 = new JPanel();
        whitePanel2.setBackground(Color.white);
        panel.add(whitePanel2, c);
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.gridheight = 5;
        panel.add(whitePanel2, c);
        c.gridx = 0;
        c.gridy = 12;
        c.gridheight = 1;
        String message = "i.e.: A181546_22 means A181546 Ig sequence with 22 reads";
        checkBoxNGS.setFont(font);
        checkBoxNGS.setSelected(false);
        checkBoxNGS.setToolTipText(message);
        checkBoxNGS.addActionListener(this);
        panel.add(checkBoxNGS, c);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chooseDnamlButton) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(getSetCurrentDir.getCurrentDirectory());
            int returnVal = fileChooser.showDialog(this, "Select");
            if (returnVal == 0) {
                dnamlFile = fileChooser.getSelectedFile();
                //the run button is enable
                runButton.setEnabled(true);
                // keep the directory in memory
                getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
            }
        } else if (e.getSource() == loadIMGTFormatButton) {
            try {
                loadIMGTFormatFile();
            } catch (FastaFormatException | IOException e1) {
                JOptionPane
                        .showMessageDialog(this, e1.getMessage(), "IMGT Fasta File error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == runButton) {
            //Finally launch the parser
            try {
                launchParser();
            } catch (FastaFormatException e1) {
                e1.printStackTrace();
            }
        } else if (e.getSource() == checkBoxNGS) {
            hasReadsInId = checkBoxNGS.isSelected();
        }

    }

    private void loadIMGTFormatFile() throws FastaFormatException, IOException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FastaFilter());
        fileChooser.setCurrentDirectory(getSetCurrentDir.getCurrentDirectory());
        int returnVal = fileChooser.showDialog(this, "Load IMGT format file");
        if (returnVal == 0) { // The user selected a file
            imgtFile = fileChooser.getSelectedFile();
            getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
        }
    }

    private void launchParser() throws FastaFormatException {
        try {
            @SuppressWarnings("unused")
            InputParser parser = new InputParser(dnamlFile, imgtFile, igTreeViewerFrame, hasReadsInId);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
