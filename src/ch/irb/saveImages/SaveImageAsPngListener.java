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
package ch.irb.saveImages;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;

import org.apache.log4j.Logger;

import ch.irb.currentDirectory.GetSetCurrentDirectory;

/**
 * This class is used to save the panel (with or without the LegendPanel) into PNG images
 */
public class SaveImageAsPngListener implements ActionListener {
    static Logger logger = Logger.getLogger(SaveImageAsPngListener.class);
    private Container container;
    private GetSetCurrentDirectory getSetCurrentDir;

    public SaveImageAsPngListener(Container container, GetSetCurrentDirectory getSetCurrentDir) {
        this.container = container;
        this.getSetCurrentDir = getSetCurrentDir;
        //logger.debug("CONTAINER "+container.getWidth()+" and h "+container.getHeight());
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {

        final JFileChooser fileChooser = new JFileChooser() {
            public void approveSelection() {
                File f = getSelectedFile();
                if (!f.getAbsolutePath().matches(".*\\.png")) {
                    f = new File(f.getAbsolutePath() + ".png");
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
        fileChooser.setDialogTitle("Save as .png");
        int choix = fileChooser.showSaveDialog(container);
        if (choix != JFileChooser.APPROVE_OPTION) {
            return;
        }
        getSetCurrentDir.setCurrentDirectory(fileChooser.getCurrentDirectory());
        File file = fileChooser.getSelectedFile();
        if (!file.getAbsolutePath().matches(".*\\.png")) {
            if (!file.getAbsolutePath().matches("\\.")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
        }

        final String formatName = "png";

        BufferedImage image = new BufferedImage(container.getWidth(), container.getHeight(), // igTreePanel
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        container.paint(g2);

        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext(); ) {
            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier
                    .createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
                continue;
            }

            try {
                setDPI(metadata);
            } catch (IIOInvalidTreeException e1) {
                e1.printStackTrace();
            }

            ImageOutputStream stream = null;
            try {
                stream = ImageIO.createImageOutputStream(file);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            try {
                writer.setOutput(stream);
                writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;
        }

    }


    private void setDPI(IIOMetadata metadata) throws IIOInvalidTreeException {

        // for PMG, it's dots per millimeter we set the DPI to 1000
        double dotsPerMilli = 1.0 * 1000 / 10 / 2.54;

        IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
        horiz.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
        vert.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horiz);
        dim.appendChild(vert);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dim);

        metadata.mergeTree("javax_imageio_1.0", root);
    }

}
