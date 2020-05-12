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
   
 * This class was made to create an excel file with the mutations between 2 nodes.
 * This is NOT USED anymore (made for Leon).
 * To remove or to improve (make a tsv file)  
 */
package ch.irb.IgGenealogicTreeViewer;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ch.irb.nodes.NodeGraph;
import ch.irb.translation.Translator;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * @author Mathilde
 *         This class will create an excel file where 1 sheet is 1 node. For each node we print out:
 *         -the FRW/CDR regions
 *         And for each region:
 *         - the R/S ratio (line 2)
 *         - the number of mutation (line 3)
 *         - the mutations with the nucleotide position (from line 4)
 */
public class CreateExcellFileWithMutations {
    static Logger logger = Logger.getLogger(CreateExcellFileWithMutations.class);
    private TreeMap<String, MutationsGraph> allMutations = new TreeMap<String, MutationsGraph>();
    private File excellFile;
    private boolean isDNA;
    private String[] regions = {"FR1", "CDR1", "FR2", "CDR2", "FR3", "CDR3", "FR4"};
    private TreeMap<Integer, String> positionNucToCdrFrRegions = new TreeMap<Integer, String>();

    public CreateExcellFileWithMutations(TreeMap<String, MutationsGraph> allMutations, File xmlFile,
                                         TreeMap<Integer, String> positionNucToCdrFrRegions, boolean isDNA) {
        this.allMutations = allMutations;
        this.positionNucToCdrFrRegions = positionNucToCdrFrRegions;
        this.isDNA = isDNA;
        String xmlFileName = xmlFile.getAbsolutePath();
        String xmlFileNameWithoutExt = xmlFileName.substring(0, xmlFileName.indexOf(".xml"));
        // check that the file doesnt exist yet
        excellFile = new File(xmlFileNameWithoutExt + "_MUTATIONS.xls");
        if (!excellFile.exists()) {
            writeExcellFile();
        }
    }

    private void writeExcellFile() {
        try {
            WritableWorkbook workbook = Workbook.createWorkbook(excellFile);
            int sheetIndex = 0;
            for (MutationsGraph mutationsGraph : allMutations.values()) {
                NodeGraph node = mutationsGraph.getNode();
                String nodeSequence = node.getSequence();
                TreeMap<String, ArrayList<String>> regionToMutations = new TreeMap<String, ArrayList<String>>();
                WritableSheet sheet = workbook.createSheet(mutationsGraph.getNodeId(), sheetIndex);
                // first we write the regions
                int column = 0;
                int row = 0;
                for (String region : regions) {
                    Label label = new Label(column, row, region);
                    sheet.addCell(label);
                    column++;
                }

                // Then the mutations from row 3
                String[] mut = mutationsGraph.getMutationsWithParent().split(",");
                for (int i = 1; i < mut.length; i++) {
                    String[] muta = mut[i].split(":");
                    String position = muta[0];
                    int pos = Integer.parseInt(position);
                    String mutationToWrite = pos + 1 + ":" + muta[1];// be careful we write the position +1 for the user!!
                    String region = positionNucToCdrFrRegions.get(pos);

                    ArrayList<String> mutations = new ArrayList<String>();
                    if (regionToMutations.containsKey(region)) {
                        mutations = regionToMutations.get(region);
                    }
                    mutations.add(mut[i]);
                    regionToMutations.put(region, mutations);

                    row = 3 + mutations.indexOf(mut[i]);
                    column = Arrays.asList(regions).indexOf(region);
                    Label label = new Label(column, row, mutationToWrite);
                    sheet.addCell(label);
                }

                // Then the number of mutations for each region
                for (String region : regions) { //regionToMutations.keySet()
                    ArrayList<String> mutations = regionToMutations.get(region);
                    if (mutations == null) {
                        mutations = new ArrayList<String>();
                    }
                    int mutationsNumber = mutations.size();
                    row = 2;
                    column = Arrays.asList(regions).indexOf(region);
                    Label label = new Label(column, row, String.valueOf(mutationsNumber));
                    sheet.addCell(label);

                    // and finally the ratio R/S of the mutations
                    int R = 0;
                    int S = 0;
                    for (String mutation : mutations) {
                        String[] muta = mutation.split(":");
                        String position = muta[0];
                        String codon = getCodon(Integer.valueOf(position), nodeSequence);
                        String parentSequence = node.getParent().getSequence();
                        String codonParent = getCodon(Integer.valueOf(position), parentSequence);
                        Translator translator = new Translator(codon, isDNA);
                        String aa = translator.getProteinSequence();
                        translator = new Translator(codonParent, isDNA);
                        String parentAA = translator.getProteinSequence();
                        if (aa.equals(parentAA)) // this is a silent mutation
                        {
                            S++;
                        } else {
                            R++;
                        }
                    }
                    BigDecimal ratio = new BigDecimal(0);
                    if (S == 0) {
                        S = 1; // to avoid a division by zero
                    }
                    if (R != 0) { // all the mutations are silent
                        BigDecimal r = new BigDecimal(R);
                        BigDecimal mutNumber = new BigDecimal(S);
                        // logger.warn("R is "+r.toString()+" and mut is "+mutNumber.toString());
                        ratio = r.divide(mutNumber, 2, BigDecimal.ROUND_UP);
                    }
                    row = 1;
                    Label lab = new Label(column, row, ratio.toString());
                    sheet.addCell(lab);
                }
                sheetIndex++;
            }
            workbook.write();
            workbook.close();

        } catch (IOException | WriteException e) {
            e.printStackTrace();
        }
    }

    private String getCodon(int position, String sequence) {
        // logger.debug("Position is " + position + " with node sequence " + nodeSequence);
        String codon = null;
        if (position % 3 == 0) { // the nuc is in first, we take the 2 next ones
            if (position + 3 > sequence.length()) {
                codon = sequence.substring(position);
            } else {
                codon = sequence.substring(position, position + 3);
            }
        } else if ((position + 1) % 3 == 0) { // nuc in the end of the codon
            if (position + 1 > sequence.length()) {
                codon = sequence.substring(position - 2);
            } else {
                codon = sequence.substring(position - 2, position + 1);
            }
        } else {
            // if ((position + 2) % 3 == 0) // nuc in the middle
            if (position + 2 > sequence.length()) {
                codon = sequence.substring(position - 1);
            } else {
                codon = sequence.substring(position - 1, position + 2);
            }
        }
        return codon;
    }

}
