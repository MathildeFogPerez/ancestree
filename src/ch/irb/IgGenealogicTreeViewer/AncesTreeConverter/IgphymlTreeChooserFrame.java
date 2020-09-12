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

   This Frame will allow the user to select change-o in AIRR format file, the IgPhyML file and the clone_id
 */
package ch.irb.IgGenealogicTreeViewer.AncesTreeConverter;

import ch.irb.IgGenealogicTreeViewer.IgTreeViewerFrame;
import ch.irb.IgGenealogicTreeViewer.airr.AirrFilter;
import ch.irb.IgGenealogicTreeViewer.airr.TabFilter;
import ch.irb.ManageFastaFiles.FastaFormatException;
import ch.irb.currentDirectory.GetSetCurrentDirectory;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("serial")
public class IgphymlTreeChooserFrame extends JFrame implements ActionListener {

    private IgTreeViewerFrame igTreeViewerFrame;
    private GetSetCurrentDirectory getSetCurrentDir = new GetSetCurrentDirectory();
    private File changeoAirrFile;
    private boolean changeoOk = false;
    private boolean igphymlOk = false;
    private File igphymlFile;
    private String cloneId;
    private String[] clonesIds;
    private JButton chooseChangeoButton = new JButton("Select Change-O file (AIRR format)");
    private JButton chooseIgphymlButton = new JButton("Select IgPhyML file");
    private JLabel cloneIdLabel2 = new JLabel("    ");
    private JButton runButton = new JButton("RUN");
    private Font font = new Font("Dialog", Font.PLAIN, 15);


    public static void main(String[] args) {
        try {
            @SuppressWarnings("unused")
            IgphymlTreeChooserFrame frame = new IgphymlTreeChooserFrame(null);
        } catch (FastaFormatException e) {
            e.printStackTrace();
        }
    }

    public IgphymlTreeChooserFrame(IgTreeViewerFrame igTreeViewerFrame) throws FastaFormatException {
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

        this.setTitle("AncesTree: IgPhyML tree chooser");
        this.setSize(360, 250);
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
        c.gridwidth = 2;
        chooseChangeoButton.setFont(font);
        chooseChangeoButton.addActionListener(this);
        panel.add(chooseChangeoButton, c);
        c.gridy += 1;
        c.gridheight = 5;
        JPanel whitePanel = new JPanel();
        whitePanel.setBackground(Color.white);
        panel.add(whitePanel, c);
        c.gridy += 5;
        c.gridheight = 1;
        chooseIgphymlButton.setFont(font);
        chooseIgphymlButton.addActionListener(this);
        panel.add(chooseIgphymlButton, c);
        c.gridy += 1;
        c.gridheight = 5;
        JPanel whitePanel3 = new JPanel();
        whitePanel3.setBackground(Color.white);
        panel.add(whitePanel3, c);
        c.gridy += 5;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        JLabel cloneIdLabel = new JLabel("Clone id to process:   ");
        panel.add(cloneIdLabel, c);
        c.gridx = 1;
        panel.add(cloneIdLabel2, c);
        c.gridy += 1;
        c.gridheight = 5;
        c.gridwidth = 2;
        c.gridx = 0;
        JPanel whitePanel2 = new JPanel();
        whitePanel2.setBackground(Color.white);
        panel.add(whitePanel2, c);
        c.gridy += 5;
        c.gridheight = 1;
        runButton.addActionListener(this);
        // the runButton is enabled until all the fasta files are selected
        runButton.setEnabled(false);
        runButton.setFont(new Font("Dialog", Font.BOLD, 23));
        panel.add(runButton, c);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chooseChangeoButton) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new AirrFilter());
            fileChooser.setCurrentDirectory(getSetCurrentDir.getCurrentDirectory());
            int returnVal = fileChooser.showDialog(this, "Select change-o in AIRR format file");
            if (returnVal == 0) {
                changeoAirrFile = fileChooser.getSelectedFile();
                //here check that we have an AIRR format file with all needed value!
                try {
                    checkValidAIRRfile();
                } catch (Exception ex) {
                    changeoOk = false;
                    JOptionPane.showMessageDialog(this, ex.getMessage(),
                            "File not in AIRR format or missing information", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                //the run button is enable if we have both files
                if (changeoOk && igphymlOk) {
                    runButton.setEnabled(true);
                }
                // keep the directory in memory
                getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
            }
        }
        if (e.getSource() == chooseIgphymlButton) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new TabFilter());
            fileChooser.setCurrentDirectory(getSetCurrentDir.getCurrentDirectory());
            int returnVal = fileChooser.showDialog(this, "Select IgPhyML file");
            if (returnVal == 0) {
                igphymlFile = fileChooser.getSelectedFile();
                try {
                    checkValidIgphymlFile();
                } catch (Exception ex) {
                    igphymlOk = false;
                    JOptionPane.showMessageDialog(this, ex.getMessage(),
                            "File not in IgPhyML format or missing information", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (igphymlOk) {
                    JOptionPane jop = new JOptionPane();
                    String input = (String) jop.showInputDialog(null,
                            "Select the clone id to process",
                            "Clone id",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            clonesIds,
                            clonesIds[0]);
                    if (input == null) {
                        return;
                    }
                    cloneId = input.split("\\s")[0];
                    cloneIdLabel2.setText(cloneId);
                }
                //the run button is enable
                if (changeoOk && igphymlOk) {
                    runButton.setEnabled(true);
                }
                // keep the directory in memory
                getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
            }
        } else if (e.getSource() == runButton) {
            //Finally launch the parser
            try {
                launchParser();
            } catch (FastaFormatException ex) {
                ex.printStackTrace();
            }

        }
    }

    private void checkValidAIRRfile() throws Exception {
        BufferedReader fileReader = new BufferedReader(new FileReader(changeoAirrFile));
        String line = "";
        int index = 0;
        String header = "";
        while ((line = fileReader.readLine()) != null) {
            if (index == 0) {
                header = line;
                break;
            }
            index++;
        }
        fileReader.close();
        //check that we have the required field!!
        ArrayList<String> requiredAirrFields = new ArrayList<>();
        requiredAirrFields.add("sequence_id");
        requiredAirrFields.add("sequence");
        requiredAirrFields.add("rev_comp");
        requiredAirrFields.add("productive");
        requiredAirrFields.add("v_call");
        requiredAirrFields.add("d_call");
        requiredAirrFields.add("j_call");
        requiredAirrFields.add("sequence_alignment");
        requiredAirrFields.add("germline_alignment");
        requiredAirrFields.add("junction");
        requiredAirrFields.add("junction_aa");
        requiredAirrFields.add("v_cigar");
        requiredAirrFields.add("d_cigar");
        requiredAirrFields.add("j_cigar");
        for (String requiredFied : requiredAirrFields) {
            if (!header.contains(requiredFied)) {
                throw new Exception("The data are not in AIRR format in '" + changeoAirrFile.getName() + "'.");
            }
        }

        if (!header.contains("\tclone_id\t")) {
            throw new Exception("No clone_id field found 'in " + changeoAirrFile.getName() + "'.");
        }
        if (!header.contains("\tfwr1\t")) {
            throw new Exception("No fwr1 field found in '" + changeoAirrFile.getName() + "'.");
        }
        if (!header.contains("\tfwr2\t")) {
            throw new Exception("No fwr2 field found in '" + changeoAirrFile.getName() + "'.");
        }
        if (!header.contains("\tfwr3\t")) {
            throw new Exception("No fwr3 field found in '" + changeoAirrFile.getName() + "'.");
        }
        if (!header.contains("\tfwr4\t")) {
            throw new Exception("No fwr4 field found in '" + changeoAirrFile.getName() + "'.");
        }
        if (!header.contains("\tcdr1\t")) {
            throw new Exception("No cdr1 field found in '" + changeoAirrFile.getName() + "'.");
        }
        if (!header.contains("\tcdr2\t")) {
            throw new Exception("No cdr2 field found in '" + changeoAirrFile.getName() + "'.");
        }
        if (!header.contains("\tcdr3\t")) {
            throw new Exception("No cdr3 field found in '" + changeoAirrFile.getName() + "'.");
        }
        changeoOk = true;
    }

    private void checkValidIgphymlFile() throws Exception {
        ArrayList<String> cloneIdsArray = new ArrayList<>();
        //The cloneIds list is sorted by size, we want to sort it by name too
        TreeMap<Integer, ArrayList<String>> sizeToCloneIds = new TreeMap<>();
        BufferedReader fileReader = new BufferedReader(new FileReader(igphymlFile));
        String line = "";
        int index = 0;
        String header = "";
        while ((line = fileReader.readLine()) != null) {
            if (index == 0) {
                header = line;
            } else if (index > 1) { //we store the clone Ids
                String[] cells = line.split("\t");
                if (cells == null) {
                    throw new Exception("The IgPhyML file '" + igphymlFile.getName() + "' is not a TAB file.");
                }
                //System.out.println("We store clone id "+cells[0]);
                cloneIdsArray.add(cells[0] + " (NSEQ= " + cells[1] + ")");
                ArrayList<String> cloneIds= new ArrayList<>();
                int size= Integer.parseInt(cells[1]);
                if (sizeToCloneIds.containsKey(size)){
                    cloneIds= sizeToCloneIds.get(size);
                }
                cloneIds.add(cells[0] + " (NSEQ= " + cells[1] + ")");
                sizeToCloneIds.put(size,cloneIds);
            }
            index++;
        }
        fileReader.close();
        if (!header.matches("CLONE.*TREE")) {
            throw new Exception("CLONE and TREE fields are not found in the IgPhyML file '" + igphymlFile.getName() + "'.");
        }
        File bpFastaFile = new File(igphymlFile.getParent() + System.getProperty("file.separator") + igphymlFile.getName()
                .replace(".tab", "_hlp_asr.fasta"));
        if (!bpFastaFile.exists()) {
            //try with the new version
            if (!bpFastaFile.exists()) {
                throw new Exception("The fasta file which contains the tree intermediate sequences is not found: '" + bpFastaFile.getName() + "'." +
                        "\n Please provide it or change the name accordingly.");
            }
        }

        //The cloneIds list is sorted by size, we want to sort it by name too
        TreeMap<Integer,ArrayList<String>>sortedSizeToCloneIds = new TreeMap<>(Collections.reverseOrder());
        for (Integer size: sizeToCloneIds.keySet()){
            ArrayList<String> sortedCloneIds = sizeToCloneIds.get(size);
            Collections.sort(sortedCloneIds, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    Integer numb1= Integer.valueOf(o1.split(" ")[0]);
                    Integer numb2= Integer.valueOf(o2.split(" ")[0]);
                    return numb2.compareTo(numb1);
                }
            });
            sortedSizeToCloneIds.put(size,sortedCloneIds);
        }


        clonesIds = new String[cloneIdsArray.size()];
        int i = 0;
        for (Integer size:sortedSizeToCloneIds.keySet()) {
            for (String clone: sortedSizeToCloneIds.get(size)){
            //for (String clone : cloneIdsArray) {
                clonesIds[i] = clone;
                i++;
            }
        }
        //System.out.println("We have clones ids "+clonesIds);
        igphymlOk = true;
    }


    private void launchParser() throws FastaFormatException {
        try {
            @SuppressWarnings("unused")
            InputParser parser = new InputParser(changeoAirrFile, igphymlFile, cloneId, igTreeViewerFrame);
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
