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

   This class will store the IMGT data: sequence in IMGT format + FR/CDR boundaries
   Usually we put the IMGAT data for the UCA here (found with IMGT/V-QUEST)
 */
package ch.irb.imgt;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.MatchResult;

import ch.irb.ManageFastaFiles.FastaFileParser;
import ch.irb.nodes.Node;

public class ProcessIMGTData {

    private File imgtFile;
    private ArrayList<Node> nodes;
    public ProcessIMGTData(File imgtFile, ArrayList<Node> nodes) throws Exception {
        this.imgtFile = imgtFile;
        this.nodes = nodes;
        parseData();
    }

    private void parseData() throws Exception {
        FastaFileParser fastaFileParser = new FastaFileParser(imgtFile);
        HashMap<String, String> fastaIdToSequence = fastaFileParser.getFastaIdToSequence();
        // there is only 1 entry
        Entry<String, String> entry = fastaIdToSequence.entrySet().iterator().next();
        String fastaId = entry.getKey();
        String imgtFormatRef = entry.getValue();
        if (!isValidSequenceLength(imgtFormatRef)) {
            //TODO check better here when there is deletion in all Igs but UCA!!
            //throw new Exception("The sequence in the IMGT data file does not correspond to one of the input sequences!");
        }
        //Here we check better that the use has entered the 5 boundaries (FR1, CDR1, FR2, CDR2 and CDR3)
        String[] cells = fastaId.split("\\s+");
        if (cells.length != 7){
            throw new Exception("The FastaID should contain :" +
                    " \"UCA FR1endPosition CDR1endPosition FR2endPosition CDR2endPosition FR3endPosition CDR3endPosition\"" +
                    " (nucleotide position).");
        }
        // we get the boundaries of the regions
        Scanner scanner = new Scanner(fastaId);
        String cDR_FR_regions = "";
        if (scanner.findInLine("(\\d+\\s+){5}\\d+\\s*$") != null) {
            MatchResult result = scanner.match();
            cDR_FR_regions = result.group();
        }
        scanner.close();

        // we store the info
        for (Node node : nodes) {
            String sequence = node.getSequence();
            String imgtSequence = "";
            int index = 0;
            for (char n : imgtFormatRef.toCharArray()) {
                String nuc = String.valueOf(n);
                String toAdd = ".";
                if (!nuc.equals(".")) {
                    toAdd = String.valueOf(sequence.charAt(index));
                    index++;
                }
                imgtSequence += toAdd;
            }
            node.setImgtFormatSequence(imgtSequence);
            int totalLenght = sequence.length(); // IMGT format
            cDR_FR_regions += " " + totalLenght; // this number is the end of
            // FR4
            node.setCdr_fr_regions(cDR_FR_regions);
        }
    }


    private boolean isValidSequenceLength(String sequence) {
        boolean isOk = true;
        String seqToCheck = sequence.replaceAll("\\.", ""); //we remove the '.' from the IMGT format
        int length = seqToCheck.length();
        //this lenght has to be == to the max length of the nodes
        int max = 0;
        for (Node node : nodes) {
            int nodeLength = node.getSequence().length();
            if (nodeLength > max) {
                max = nodeLength;
            }
        }
        if (length != max) {
            isOk = false;
        }
        return isOk;
    }

}
