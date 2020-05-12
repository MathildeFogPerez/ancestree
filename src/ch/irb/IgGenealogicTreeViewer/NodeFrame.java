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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.TransferHandler;

import org.apache.log4j.Logger;

import ch.irb.Layout.SpringUtilities;
import ch.irb.nodes.NodeGraph;

/**
 * @author Mathilde This class is a frame which will display the information related to a node: - its immunization
 *         information if there is (not for BP) - its dna sequence - its proteic sequence From now this frame can also
 *         take new input for the node (i.e: Ec50)
 */
@SuppressWarnings("serial")
public class NodeFrame extends JFrame {
    static Logger logger = Logger.getLogger(NodeFrame.class);
    private JFrame frame = this;
    private IgTreePanel igTreePanel;
    private JPanel jPanel = new JPanel(new SpringLayout());
    private JTextField ec50Field = new JTextField();
    private JTextField comment1Area = new JTextField();
    private JRadioButton showComment1Item = new JRadioButton("Show");
    private JRadioButton hideComment1Item = new JRadioButton("Hide");
    private boolean showComment1 = true;
    private JTextArea comment2Area = new JTextArea(2, 10);
    private NodeGraph node;
    private final static String newline = "\n";

    public NodeFrame(NodeGraph node, JComponent parentComponent, IgTreePanel igTreePanel) {
        this.node = node;
        this.igTreePanel = igTreePanel;
        this.setTitle("Node " + node.getNodeId());
        this.setSize(200, 300);
        this.setBackground(Color.WHITE);
        this.setLocationRelativeTo(parentComponent);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
        jPanel.setOpaque(true);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(jPanel, BorderLayout.CENTER);
        this.pack();
        this.setVisible(true);
    }

    private void initComponents() {
        int lineNumber = 1;
        int pairsNumber = 2;
        // First we write the name of the Ig in order to make it editable for the user (to copy it)

        JLabel idLabel = new JLabel("Name: ");
        jPanel.add(idLabel);
        JTextArea nodeIdArea = new JTextArea(1, 25);
        nodeIdArea.setEditable(false);
        String id = node.getNodeId();
        nodeIdArea.setText(id);
        JPopupMenu jPopupMenu3 = new JPopupMenu();
        jPopupMenu3.add(new CopyAction(nodeIdArea, "copy", "copy", KeyEvent.VK_C));
        nodeIdArea.setComponentPopupMenu(jPopupMenu3);
        idLabel.setLabelFor(nodeIdArea);
        jPanel.add(nodeIdArea);
        lineNumber += 1;

        // Then we set the JPanel for the immu info, and dna and AA sequences
        if (node.getImmunizationInfoToDisplay() != null) {
            JLabel infoLabel = new JLabel("Immunization information: ", JLabel.TRAILING);
            jPanel.add(infoLabel);
            JTextArea infoArea = new JTextArea(" " + node.getImmunizationInfoToDisplay(), 1, 20);
            infoArea.setEditable(false);
            infoLabel.setLabelFor(infoArea);
            jPanel.add(infoArea);
            lineNumber += 1;
        }
        int firstIndex = 0;
        int secondIndex = 0;
        @SuppressWarnings("unused")
        String cutSequence = "";
        boolean cut = true;

        if (node.isDNA()) {
            lineNumber += 1;
            JLabel nucSeqLabel = new JLabel("Nucleotidic sequence: ", JLabel.TRAILING);
            jPanel.add(nucSeqLabel);
            JTextArea nucArea = new JTextArea(5, 25);
            nucArea.setEditable(false);
            String nuc = node.getSequence();

            while (cut) {
                firstIndex = secondIndex;
                secondIndex = firstIndex + 40;
                if (secondIndex > nuc.length()) {
                    secondIndex = nuc.length();
                }
                String nucOne = nuc.substring(firstIndex, secondIndex);
                nucArea.append(nucOne + newline);
                if (secondIndex == nuc.length()) {
                    cut = false;
                }
            }
            JPopupMenu jPopupMenu = new JPopupMenu();
            jPopupMenu.add(new CopyAction(nucArea, "copy", "copy", KeyEvent.VK_C));
            nucArea.setComponentPopupMenu(jPopupMenu);
            JScrollPane jScrollPane1 = new JScrollPane(nucArea);
            nucSeqLabel.setLabelFor(jScrollPane1);
            jPanel.add(jScrollPane1);
        }
        JLabel aaSeqLabel = new JLabel("Amino acid sequence: ", JLabel.TRAILING);
        jPanel.add(aaSeqLabel);
        JTextArea aaArea = new JTextArea(4, 25);
        aaArea.setEditable(false);
        String aa = node.getProteinSequence();
        firstIndex = 0;
        secondIndex = 0;
        cut = true;
        while (cut) {
            firstIndex = secondIndex;
            secondIndex = firstIndex + 40;
            if (secondIndex > aa.length()) {
                secondIndex = aa.length();
            }
            String aaOne = aa.substring(firstIndex, secondIndex);
            aaArea.append(aaOne + newline);
            if (secondIndex == aa.length()) {
                cut = false;
            }
        }
        JPopupMenu jPopupMenu2 = new JPopupMenu();
        jPopupMenu2.add(new CopyAction(aaArea, "copy", "copy", KeyEvent.VK_C));
        aaArea.setComponentPopupMenu(jPopupMenu2);
        JScrollPane jScrollPane2 = new JScrollPane(aaArea);
        aaSeqLabel.setLabelFor(jScrollPane2);
        jPanel.add(jScrollPane2);

        // Then we add the JTextField to add/modify the EC50
        lineNumber += 1;
        String ec50Value = node.getEC50();
        JLabel EC50Label = new JLabel("EC 50 (ng/ml): ");
        String text = ec50Value;
        if (text == null) {
            text = "Enter the EC 50 value(s) here";
            ec50Field.setFont(new Font("TimesRoman", Font.ITALIC, 11));
        }
        ec50Field.setText(text);

        EC50Label.setLabelFor(ec50Field);
        EC50Label.setToolTipText("If you want to enter several values, separate them by a comma");

        ec50Field.addMouseListener(new MouseListener() {

            // we want to delete the text "Enter you EC 50 here" when the user wants to enter a value
            public void mouseReleased(MouseEvent arg0) {
                if (ec50Field.getText().equals("Enter the EC 50 value(s) here")) {
                    ec50Field.setText("");
                }
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseClicked(MouseEvent arg0) {
            }
        });

        JButton okButton = new JButton("Save");
        okButton.setForeground(new Color(5, 73, 9));
        okButton.addActionListener(new SaveDataListener());

        // Layout
        jPanel.add(EC50Label);
        JPanel ec50Panel = new JPanel(new GridLayout(1, 2));
        ec50Panel.add(ec50Field);
        ec50Panel.add(new JLabel());
        jPanel.add(ec50Panel);

        // add the comments' panels

        // the first comment can de display in the GUI via the "show" radio button
        boolean isCommentToShow = node.isShowComment1();
        JLabel comment1Label = new JLabel("Comment 1");
        lineNumber += 1;
        jPanel.add(comment1Label);
        JPanel com1Panel = new JPanel(new GridLayout(1, 2));
        comment1Area.setText(node.getComment1());
        com1Panel.add(comment1Area);
        comment1Area.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel showHidePanel = new JPanel(new GridLayout(1, 2));
        if (isCommentToShow) {
            showComment1Item.setSelected(true);
        } else {
            hideComment1Item.setSelected(true);
        }
        showComment1Item.addActionListener(new HideShowListener());
        hideComment1Item.addActionListener(new HideShowListener());
        showHidePanel.add(showComment1Item);
        showHidePanel.add(hideComment1Item);
        ButtonGroup group = new ButtonGroup();
        group.add(showComment1Item);
        group.add(hideComment1Item);
        com1Panel.add(showHidePanel);
        jPanel.add(com1Panel);

        // the second comment is seen only when the user open this frame
        JLabel comment2Label = new JLabel("Comment 2");
        lineNumber += 1;
        jPanel.add(comment2Label);
        JPanel com2Panel = new JPanel(new GridLayout(1, 1));
        JScrollPane areaComScrollPane = new JScrollPane(comment2Area);
        com2Panel.add(areaComScrollPane);
        comment2Area.setFont(new Font("Arial", Font.PLAIN, 14));
        comment2Area.setText(node.getComment2());
        jPanel.add(com2Panel);

        // add the save button
        lineNumber += 1;
        jPanel.add(new JLabel());
        jPanel.add(okButton);

        SpringUtilities.makeCompactGrid(jPanel, lineNumber, pairsNumber, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad

    }

    // this class is used to copy the sequences using the right click of the mouse
    private class CopyAction extends AbstractAction {
        JTextArea jTextArea = new JTextArea();

        public CopyAction(JTextArea jTextArea, String text, String desc, Integer mnemonic) {
            super(text);
            this.jTextArea = jTextArea;
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            Clipboard clbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            TransferHandler transferHandler = jTextArea.getTransferHandler();
            transferHandler.exportToClipboard(jTextArea, clbrd, TransferHandler.COPY);
        }
    }

    /*
     * this class listens if the user wants to show or hide the comment 1 field into the GUI
     */
    class HideShowListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == showComment1Item) {
                showComment1 = true;
            } else if (e.getSource() == hideComment1Item) {
                showComment1 = false;
            }
        }

    }

    /*
     * This class checks what the user enter (EC50, comment1 to show or hide and comment2) in order to save it later
     * into the XML file. The display of the tree can be updated too.
     */
    class SaveDataListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            // First we check if the user set an EC50
            String input = ec50Field.getText();
            if (!input.equals("Enter the EC 50 value(s) here")) {
                boolean hasDeletedEc50 = false;
                if (input.equals("")) { // finally the user doesnt want to set the EC 50
                    node.setEC50(null);
                    hasDeletedEc50 = true;
                }
                if (!hasDeletedEc50) {
                    //HERE allow a comma, to add different EC50 values, one color for each
                    if (input.contains(",")){
                        JOptionPane.showMessageDialog(frame, "You entered different EC50values, they will appear" +
                                        " in different colors.",
                                "Different EC 50 values", JOptionPane.WARNING_MESSAGE);
                    }else { //check that is a a number, otherwise send a error message
                        try {
                            String in = input.replace("~", "");
                            new java.math.BigDecimal(in);
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(frame, "The EC 50 value you entered is not a valid number.",
                                    "Wrong EC 50 value", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    node.setEC50(input);
                }
            }

            // Then we check if the user entered some comments
            String comment1 = comment1Area.getText();
            if (comment1 != null) {
                if (comment1.length() > 0) {
                    node.setComment1(comment1);
                } else {
                    node.setComment1(null);
                }
                node.setShowComment1(showComment1);
            }
            String comment2 = comment2Area.getText();
            if (comment2 != null) {
                if (comment2.length() > 0) {
                    node.setComment2(comment2);
                } else {
                    node.setComment2(null);
                }
            }

            //then we set if the user wants to show or hide the first comment
            node.setShowComment1(showComment1);

            // we store the value in the nodeGraph object
            // then we update the display of the tree
            igTreePanel.setNewDataSaved(true);
            igTreePanel.repaint();
            frame.dispose();
        }

    }

}
