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
   
 *  This class displays a frame with the BASELINe output: Ig id + pvalues for CDRs and FRs
 *  and their related colors (see BASELINe website: http://selection.med.yale.edu/baseline/)
 */
package ch.irb.IgGenealogicTreeViewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;

import org.apache.log4j.Logger;

import ch.irb.IgAlignment.MenuScroller;
import ch.irb.currentDirectory.GetSetCurrentDirectory;
import ch.irb.saveImages.ExportAsEPSListener;
import ch.irb.saveImages.SaveImageAsPngListener;

@SuppressWarnings("serial")
public class ClipDataFrame extends JFrame {
    static Logger logger = Logger.getLogger(MutationsFrame.class);
    private int firstColumn_width = 130;
    static PValueCodeColor pValueCodeColor = new PValueCodeColor();
    static Color intenseRed = new Color(255, 0, 0);
    static Color lightRed = new Color(244, 188, 189);
    static Color intenseGreen = new Color(0, 153, 0);
    static Color lightGreen = new Color(183, 227, 199);
    static Color greyColor = new Color(239, 239, 239);
    private ClipJPanel jPanel = new ClipJPanel();
    private HashMap<String, String[]> igToPvalues = new HashMap<String, String[]>();
    private HashMap<String, String[]> igToSigmaValues = new HashMap<String, String[]>();
    private ArrayList<String> igInOrder = new ArrayList<String>();
    private ArrayList<String> igInOrderToDisplay = new ArrayList<String>();
    private JMenu file = new JMenu("Export");
    private JMenu showMenu = new JMenu("Show");
    private JMenuItem exportItemEPS = new JMenuItem("Export image as .EPS");
    private JMenuItem exportItemPNG = new JMenuItem("Export image as .PNG");
    private JFrame frame = this;
    private JScrollPane jScrollPane = new JScrollPane();
    private ArrayList<JCheckBoxMenuItem> checkList = new ArrayList<JCheckBoxMenuItem>();
    private Font normalFont = new Font("Arial", Font.PLAIN, 20);
    private FontRenderContext frc;

    public ClipDataFrame(JComponent parentComponent, ClipParser clipParser, ArrayList<String> igInOrder,
                         GetSetCurrentDirectory getSetCurrentDir) {
        file.add(exportItemEPS);
        exportItemEPS.addActionListener(new ExportAsEPSListener(jPanel, getSetCurrentDir));
        file.add(exportItemPNG);
        exportItemPNG.addActionListener(new SaveImageAsPngListener(jPanel, getSetCurrentDir));
        this.igToPvalues = clipParser.getIgToPvalues();
        this.igToSigmaValues = clipParser.getIgToSigmaValues();
        this.igInOrder = igInOrder;
        this.igInOrderToDisplay = igInOrder;
        setShowMenu();
        try {
            java.net.URL url = ClassLoader.getSystemResource("ch/irb/IgGenealogicTreeMaker/resources/icon.png");
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(url);
            this.setIconImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setTitle("BASELINe output, focused test");
        this.setResizable(true);
        this.setBackground(greyColor);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocation(200, 200);
        int height = igToSigmaValues.size() * 40 + 150;
        int width = firstColumn_width + 120 + 120 + 80;
        jPanel.setPreferredSize(new Dimension(width, height));
        jScrollPane.getViewport().setView(jPanel);
        jPanel.setOpaque(true);
        frame.getContentPane().add(jScrollPane);
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        // we allow the showMenu to be 'scrollable' if there are too many Igs
        if (igInOrder.size() > 30) {
            MenuScroller.setScrollerFor(showMenu, 20, 75, 5, 8);
        }
        bar.add(showMenu);
        bar.add(file);
        this.pack();
        this.setVisible(true);
    }

    private void setShowMenu() {
        AffineTransform affinetransform = new AffineTransform();
        final FontRenderContext frc = new FontRenderContext(affinetransform, true, true);
        final Font normalFont = new Font("Arial", Font.PLAIN, 20);
        for (String ig : igInOrder) {
            JCheckBoxMenuItem check = new JCheckBoxMenuItem(ig, true);
            checkList.add(check);
            // to avoid the menu to close
            check.setUI(new StayOpenCheckBoxMenuItemUI());
            // here calculate the width
            int textwidth = (int) (normalFont.getStringBounds(ig, frc).getWidth());
            if (textwidth > (firstColumn_width + 30)) {
                firstColumn_width = (int) (textwidth + 40);
            }
            showMenu.add(check);
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
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Arial", Font.PLAIN, 15));
        SelectedNodeListener selectedNodeListener = new SelectedNodeListener();
        okButton.addActionListener(selectedNodeListener);
        showMenu.add(okButton);
    }

    class StayOpenCheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {

        @Override
        protected void doClick(MenuSelectionManager msm) {
            menuItem.doClick(0);
        }
    }

    class SelectedNodeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            igInOrderToDisplay = new ArrayList<String>();
            firstColumn_width = 130;
            for (JCheckBoxMenuItem checkItem : checkList) {
                if (checkItem.isSelected()) {
                    igInOrderToDisplay.add(checkItem.getText());
                    // here calculate the width
                    int textwidth = (int) (normalFont.getStringBounds(checkItem.getText(), frc).getWidth());
                    if (textwidth + 30 > firstColumn_width) {
                        firstColumn_width = (int) (textwidth + 40);
                    }
                }
            }
            jScrollPane.remove(jPanel);
            getContentPane().remove(jScrollPane);
            jPanel = new ClipJPanel();
            int height = igInOrderToDisplay.size() * 40 + 150;
            // Get the width
            int width = firstColumn_width + 120 + 120 + 70;
            jPanel.setPreferredSize(new Dimension(width, height));// 440
            jScrollPane.getViewport().setView(jPanel);
            jPanel.setOpaque(true);
            frame.getContentPane().add(jScrollPane);
            frame.pack();
        }

    }


    private class ClipJPanel extends JPanel {

        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
            draw(g2d);
        }

        public void draw(Graphics2D gd2) {
            gd2.setColor(Color.WHITE);
            int x = firstColumn_width + 30; // 160
            int y = 20;

            gd2.setFont(normalFont);
            gd2.setColor(new Color(183, 183, 183));
            gd2.fillRect(x, y, 240, 45);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 240, 45);
            // finally we decided to use only the pValue
            gd2.drawString("P value", x + 90, y + 28);
            y += 45;
            gd2.setColor(new Color(226, 226, 226));
            gd2.fillRect(x, y, 120, 40);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 120, 40);
            gd2.setFont(normalFont);
            gd2.drawString("CDR", x + 39, y + 29);
            x += 120;
            gd2.setColor(new Color(226, 226, 226));
            gd2.fillRect(x, y, 120, 40);
            gd2.setColor(Color.black);
            gd2.drawRect(x, y, 120, 40);
            gd2.drawString("FWR", x + 37, y + 29);
            x = 30; // 120
            y += 40;
            Font font = gd2.getFont();
            frc = gd2.getFontMetrics().getFontRenderContext();
            for (String ig : igInOrderToDisplay) {
                // the p-values are for the code coloring
                String pValCDR = igToPvalues.get(ig)[0];
                String pValFWR = igToPvalues.get(ig)[1];
                //String sigmaCDR = igToSigmaValues.get(ig)[0];
                //String sigmaFWR = igToSigmaValues.get(ig)[1];
                gd2.drawString(ig, x + 30, y + 29);
                gd2.setColor(Color.black);
                gd2.drawRect(x + 10, y, firstColumn_width, 40);
                x += firstColumn_width;
                Color color = pValueCodeColor.getColor(pValCDR);
                gd2.setColor(color);
                gd2.fillRect(x, y, 120, 40);
                gd2.setColor(Color.black);
                gd2.drawRect(x, y, 120, 40);
                Rectangle2D rect = font.getStringBounds(pValCDR, frc); // sigmaCDR
                int xMiddle = (int) (x + 60 - (rect.getWidth() / 2));
                int yMiddle = (int) (y + 53 - rect.getHeight());
                gd2.drawString(pValCDR, xMiddle, yMiddle); // sigmaCDR
                x += 120;
                color = pValueCodeColor.getColor(pValFWR);
                gd2.setColor(color);
                gd2.fillRect(x, y, 120, 40);
                gd2.setColor(Color.black);
                gd2.drawRect(x, y, 120, 40);
                rect = font.getStringBounds(pValFWR, frc); // sigmaFWR
                xMiddle = (int) (x + 60 - (rect.getWidth() / 2));
                yMiddle = (int) (y + 53 - rect.getHeight());
                gd2.drawString(pValFWR, xMiddle, yMiddle);// sigmaFWR
                y += 40;
                x = 30;
            }

        }

    }

}
