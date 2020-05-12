
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
   
   This class will store the information related to each IG: its ID, its cell, its immunization info and its sequence.
 */
package ch.irb.IgGenealogicTreeMaker;

import java.util.Scanner;

import org.apache.log4j.Logger;


/**
 * @author Mathilde
 *         This class will store the information related to each IG: its ID, its cell, its immunization info and its sequence
 */
public class Ig {
    static Logger logger = Logger.getLogger(Ig.class);

    private String fastaId;
    private String originalSequence;
    private String alignedSequence;
    private String imgtFormatSequence = null;
    private String CDR_FR_regions = null;
    private boolean isGermLine = false;
    private boolean isDuplicateSequence = false;
    private String name;
    private String cellInfo = null;
    private String preImmunizationYear = null;
    private String daysAfterImmunization = null;
    private String postImmunizationYear = null;
    private String immunizationInfo = null;

    public Ig(String fastaId, String sequence) {
        setFastaId(fastaId);
        setOriginalSequence(sequence);
    }

    private void setFastaId(String fastaId) {
        this.fastaId = fastaId;
        String[] splittedFastaId = fastaId.split("\\s+");
        if (splittedFastaId != null) {
            for (String input : splittedFastaId) {
                @SuppressWarnings("resource")
                Scanner s = new Scanner(input);
                if (input.matches("B")) {// The Ig was isolated in a B cell
                    logger.debug("Get information B cell");
                    setCellInfo(input);
                } else if (input.matches("PC")) { // The Ig was isolated in a plasma
                    // cell
                    logger.debug("Get information plasma cell");
                    cellInfo = "PC";
                    setCellInfo(input);
                } else if (input.matches("post(\\d+)")) { // Year when Ig was
                    // derivated post
                    // immunization
                    s.useDelimiter("post");
                    if (s.hasNextInt()) {
                        postImmunizationYear = String.valueOf(s.nextInt());
                        logger.debug("Get information post immunization year "
                                + postImmunizationYear);
                    }
                } else if (input.matches("d(\\d+)")) { // days after
                    // immunization
                    s.useDelimiter("d");
                    if (s.hasNextInt()) {
                        daysAfterImmunization = String.valueOf(s.nextInt());
                        logger.debug("Get information days after immunization "
                                + daysAfterImmunization);
                    }
                } else if (input.matches("pre(\\d+)")) { // year before immunization
                    s.useDelimiter("pre");
                    if (s.hasNextInt()) {
                        preImmunizationYear = String.valueOf(s.nextInt());
                        logger.debug("Get information pre immunization year "
                                + preImmunizationYear);
                    }
                } else { //we have the geneBankId
                    if (name == null) {
                        setName(input);
                    }
                }
            }
            if (preImmunizationYear != null) {
                setImmunizationInfo("pre" + preImmunizationYear);
            } else if (postImmunizationYear != null) {
                setImmunizationInfo("post" + postImmunizationYear + "  d" + daysAfterImmunization);
            }
        }
    }

    public String getCellInfo() {
        return cellInfo;
    }

    public void setCellInfo(String cellInfo) {
        this.cellInfo = cellInfo;
    }

    public String getDaysAfterImmunization() {
        return daysAfterImmunization;
    }

    public void setDaysAfterImmunization(String daysAfterImmunization) {
        this.daysAfterImmunization = daysAfterImmunization;
    }

    public String getPostImmunizationYear() {
        return postImmunizationYear;
    }

    public void setPostImmunizationYear(String postImmunizationYear) {
        this.postImmunizationYear = postImmunizationYear;
    }

    public String getName() {
        return name;
    }

    public String getOriginalSequence() {
        return originalSequence;
    }

    private void setOriginalSequence(String originalSequence) {
        this.originalSequence = originalSequence;
    }

    /**
     * @return the imgtFormatSequence
     */
    public String getImgtFormatSequence() {
        return imgtFormatSequence;
    }

    /**
     * @param imgtFormatSequence the imgtFormatSequence to set
     */
    public void setImgtFormatSequence(String imgtFormatSequence) {
        this.imgtFormatSequence = imgtFormatSequence;
    }

    /**
     * @return the cDR_FR_regions
     */
    public String getCDR_FR_regions() {
        return CDR_FR_regions;
    }

    /**
     * @param cDR_FR_regions the cDR_FR_regions to set
     */
    public void setCDR_FR_regions(String cDR_FR_regions) {
        int totalLenght = getOriginalSequence().length(); // IMGT format
        cDR_FR_regions += " " + totalLenght; // this number is the end of FR4
        CDR_FR_regions = cDR_FR_regions;
    }

    public String getAlignedSequence() {
        return alignedSequence;
    }

    public void setAlignedSequence(String alignedSequence) {
        this.alignedSequence = alignedSequence;
    }

    private void setName(String name) {
        this.name = name;
    }

    public String getFastaId() {
        return fastaId;
    }

    public boolean isGermLine() {
        return isGermLine;
    }

    public void setGermLine(boolean isGermLine) {
        this.isGermLine = isGermLine;
    }

    public boolean isDuplicateSequence() {
        return isDuplicateSequence;
    }

    public void setDuplicateSequence(boolean isDuplicateSequence) {
        this.isDuplicateSequence = isDuplicateSequence;
    }

    /**
     * @return the immunizationInfo
     */
    public String getImmunizationInfo() {
        return immunizationInfo;
    }

    public void setImmunizationInfo(String immunizationInfo) {
        this.immunizationInfo = immunizationInfo;
    }
}
