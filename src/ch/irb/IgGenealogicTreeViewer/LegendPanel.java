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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;

import org.apache.log4j.Logger;

/**
 * @author Mathilde This class is the legend panel, it will display the color
 *         for each year, and the shape of the node: - an ovale if it is a B
 *         cell (underlined if pre immunisation, non underlined if post immu) -
 *         a rectangle if it si a plasma cell
 */
@SuppressWarnings("serial")
public class LegendPanel extends JPanel {
    static Logger logger = Logger.getLogger(LegendPanel.class);
    private ColorByYear colorByYear;
    private IgTreePanel igTreePanel;
    private boolean isHidden = false;
    final static float dash1[] = {5.0f};
    final static BasicStroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5.0f, dash1,
            0.0f);
    final static BasicStroke plain = new BasicStroke();
    private SearchPanel searchPanel = new SearchPanel();

    public LegendPanel(IgTreePanel igTreePanel) {
        this.igTreePanel = igTreePanel;
        this.setPreferredSize(new Dimension(220, 800));
        this.setBackground(new Color(Integer.parseInt("efeff2", 16)));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new NodeLegendPanel());
        add(new TreeZoomer().getUIPanel());
        add(searchPanel.getUIPanel());
        add(new ResetPanel().getUIPanel());
    }

    public void setColorByYear(ColorByYear colorByYear) {
        this.colorByYear = colorByYear;
    }

    /**
     * @return the isHidden
     */
    public boolean isHidden() {
        return isHidden;
    }

    private class NodeLegendPanel extends JPanel {

        public NodeLegendPanel() {
            setMaximumSize(new Dimension(200, 600));
        }

        public void paintComponent(Graphics g) {
            Graphics2D gd2 = (Graphics2D) g;
            g.setColor((new Color(Integer.parseInt("efeff2", 16))));
            g.fillRect(0, 0, getWidth(), getHeight());
            draw(gd2);
        }

        public void draw(Graphics2D g) {
            int x = 10;
            int y = 10;
            Font font1 = new Font("Arial", Font.BOLD, 13);
            Font font2 = new Font("Arial", Font.PLAIN, 13);
            if (colorByYear != null) {
                for (Entry<Integer, Color> entry : colorByYear.getEntries()) {
                    if (!entry.getKey().toString().equals("0")) {
                        g.setColor(entry.getValue());
                        g.fillRect(x, y, 50, 30);
                        g.setColor(Color.black);
                        g.drawRect(x, y, 50, 30);
                        g.drawString(entry.getKey().toString(), x + 60, y + 18);
                        y += 60;
                    }
                }
            } else if (igTreePanel.hasReadsInId()) {
                g.setFont(font1);
                g.setColor(Color.black);
                g.drawString("Reads frequency:", x, y + 19);
                g.setFont(font2);
                y += 40;
                g.setColor(new Color(255, 204, 204));
                g.fillRect(x, y, 50, 20);
                g.setColor(Color.black);
                g.drawRect(x, y, 50, 20);
                g.drawString("0-1%", x + 60, y + 18);
                y += 30;
                g.setColor(new Color(255, 153, 153));
                g.fillRect(x, y, 50, 20);
                g.setColor(Color.black);
                g.drawRect(x, y, 50, 20);
                g.drawString("2-5%", x + 60, y + 18);
                y += 30;
                g.setColor(new Color(255, 102, 102));
                g.fillRect(x, y, 50, 20);
                g.setColor(Color.black);
                g.drawRect(x, y, 50, 20);
                g.drawString("6-10%", x + 60, y + 18);
                y += 30;
                g.setColor(new Color(255, 51, 51));
                g.fillRect(x, y, 50, 20);
                g.setColor(Color.black);
                g.drawRect(x, y, 50, 20);
                g.drawString("11-20%", x + 60, y + 18);
                y += 30;
                g.setColor(new Color(255, 0, 0));
                g.fillRect(x, y, 50, 20);
                g.setColor(Color.black);
                g.drawRect(x, y, 50, 20);
                g.drawString("21-100%", x + 60, y + 18);
                y += 20;
            }
           /* y += 30;
            g.setFont(font2);
            g.setColor(Color.white);
            g.fillRect(x, y, 70, 30);
            g.setColor(Color.black);
            g.drawRect(x, y, 70, 30);
            x += 80;
            y += 20;
            g.drawString("Plasma cell", x, y);
            x = 10;
            y += 25;
            g.setColor(Color.white);
            g.fillOval(x, y, 70, 30);
            g.setColor(Color.black);
            g.drawOval(x, y, 70, 30);
            x += 80;
            y += 10;
            g.drawString("B cell", x, y);
            y += 15;
            g.drawString("post immunization", x, y);
            x = 10;
            y += 25;
            g.setColor(Color.white);
            g.fillOval(x, y, 70, 30);
            g.setColor(Color.black);
            g.setStroke(dashed);
            g.drawOval(x, y, 70, 30);
            // g.drawOval(x - 1, y - 1, 72, 32);
            x += 80;
            y += 10;
            g.drawString("B cell", x, y);
            y += 15;
            g.drawString("pre immunization", x, y);*/

           //16.12.19 We draw only one type of cell
            x = 10;
            y += 25;
            g.setColor(Color.white);
            g.fillOval(x, y, 70, 30);
            g.setColor(Color.black);
            g.drawOval(x, y, 70, 30);
            x += 80;
            y += 20;
            g.drawString("B cell", x, y);

            x = 10;
            y += 25;
            Polygon polygon = getHexagon(x, y, 70, 30);
            g.setStroke(plain);
            g.setColor(Color.white);
            g.fillPolygon(polygon);
            g.setColor(Color.black);
            g.drawPolygon(polygon);
            x += 80;
            y += 20;
            g.drawString("Branch Point", x, y);

            // TO ADD add the code coloring for EC 50 in the legend!!
            x = 20;
            y += 40;
            String EC = "EC";
            String fifty = "50";
            String op1 = "<= 20 ng/ml";
            String op2 = "20 >";
            String op2bis = "<= 200 ng/ml";
            String op3 = "> 200 ng/ml";
            Font smallerFont = new Font("Arial", Font.PLAIN, 11);
            Color color = new Color(198, 0, 0);// red
            g.setColor(color);
            g.setFont(font1);
            g.drawString(EC, x, y);
            x += 20;
            g.setFont(smallerFont);
            g.drawString(fifty, x, y);
            x += 20;
            g.setFont(font1);
            g.drawString(op1, x, y);

            x = 20;
            y += 25;
            g.setFont(font1);
            color = new Color(0, 138, 0); // green
            g.setColor(color);
            g.drawString(op2, x, y);
            x += 35;
            g.drawString(EC, x, y);
            x += 20;
            g.setFont(smallerFont);
            g.drawString(fifty, x, y);
            x += 17;
            g.setFont(font1);
            g.drawString(op2bis, x, y);

            x = 20;
            y += 25;
            color = new Color(0, 0, 64);// blue
            g.setColor(color);
            g.setFont(font1);
            g.drawString(EC, x, y);
            x += 20;
            g.setFont(smallerFont);
            g.drawString(fifty, x, y);
            x += 20;
            g.setFont(font1);
            g.drawString(op3, x, y);

        }

        private Polygon getHexagon(int X, int Y, int w, int h) {
            int x1 = X + (w / 9);
            int x2 = (int) (X + (float) (8 * w / 9));
            int x3 = X + w;
            int x4 = (int) (X + (float) (8 * w / 9));
            int x5 = X + (w / 9);
            int x6 = X;
            int[] x = {x1, x2, x3, x4, x5, x6};
            int[] y = {Y, Y, Y + h / 2, Y + h, Y + h, Y + h / 2};
            Polygon polygon = new Polygon();
            for (int i = 0; i < 6; i++) {
                polygon.addPoint(x[i], y[i]);
            }
            return polygon;
        }
    }

    private class TreeZoomer {

        public TreeZoomer() {
        }

        public JPanel getUIPanel() {
            final JButton zoomIn = new JButton("Zoom IN"), zoomOut = new JButton("Zoom OUT");
            ActionListener l = new ActionListener() {
                int inc;

                public void actionPerformed(ActionEvent e) {
                    JButton button = (JButton) e.getSource();
                    if (button == zoomIn) {
                        inc = 1;
                    } else {
                        inc = -1;
                    }
                    igTreePanel.setScale(inc, new Point(0, 0));
                }
            };
            zoomIn.addActionListener(l);
            zoomOut.addActionListener(l);
            JPanel panel = new JPanel();
            panel.setMaximumSize(new Dimension(220, 50));
            panel.setBackground((new Color(Integer.parseInt("efeff2", 16))));
            panel.add(zoomIn);
            panel.add(zoomOut);
            return panel;
        }
    }

    private class SearchPanel {
        private JTextArea textarea = new JTextArea(1, 10);

        public SearchPanel() {
        }

        public JPanel getUIPanel() {

            final JButton button = new JButton("Search");
            final JPanel panel = new JPanel();
            ActionListener l = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == button) {
                        String name = textarea.getText();
                        textarea.selectAll();
                        boolean found = igTreePanel.setSearchedNode(name);
                        if (!found) {
                            JOptionPane.showMessageDialog(panel, "The id '" + name + "' was not found.",
                                    "Name not found", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            };
            textarea.setEditable(true);
            textarea.setFont(new Font("Arial", Font.PLAIN, 12));
            JPopupMenu jPopupMenu = new JPopupMenu();
            jPopupMenu.add(new CopyAction(textarea, "Copy", "Copy", KeyEvent.VK_C));
            jPopupMenu.add(new PasteAction(textarea, "Paste", "Paste", KeyEvent.VK_V));
            textarea.setComponentPopupMenu(jPopupMenu);
            textarea.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                }

                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        button.doClick();
                        String txt = textarea.getText().trim();
                        textarea.setText(txt);

                    }
                }
            });
            button.addActionListener(l);

            panel.setMaximumSize(new Dimension(200, 40));
            panel.setBackground(new Color(Integer.parseInt("efeff2", 16)));
            panel.add(textarea);
            panel.add(button);
            return panel;
        }

        public void resetSearchText() {
            textarea.setText("");
        }
    }


    private class ResetPanel {

        public ResetPanel() {
        }

        public JPanel getUIPanel() {

            final JButton button = new JButton("Reset");

            ActionListener l = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    igTreePanel.resetSearchNode();
                    searchPanel.resetSearchText();
                }
            };
            button.addActionListener(l);
            JPanel panel = new JPanel();
            panel.setMaximumSize(new Dimension(200, 50));
            panel.setBackground(new Color(Integer.parseInt("efeff2", 16)));
            panel.add(button);
            return panel;
        }
    }

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

    private class PasteAction extends AbstractAction {
        JTextArea jTextArea = new JTextArea();

        public PasteAction(JTextArea jTextArea, String text, String desc, Integer mnemonic) {
            super(text);
            this.jTextArea = jTextArea;
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            Clipboard clbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            TransferHandler transferHandler = jTextArea.getTransferHandler();
            transferHandler.importData(jTextArea, clbrd.getContents(jTextArea));
        }
    }

}
