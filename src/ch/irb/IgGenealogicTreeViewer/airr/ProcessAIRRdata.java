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

   This class will store the FR/CDR boundaries and the IMGT gapped format sequences
   that are found in the AIRR format changeo file
 */
package ch.irb.IgGenealogicTreeViewer.airr;

import ch.irb.nodes.Node;


import java.util.ArrayList;

public class ProcessAIRRdata {

    private ArrayList<Node> nodes;
    private String cloneId;
    private TsvAirrParser tsvAirrParser;

    public ProcessAIRRdata(TsvAirrParser tsvAirrParser, String cloneId, ArrayList<Node> nodes) throws Exception {
        this.tsvAirrParser = tsvAirrParser;
        this.cloneId = cloneId;
        this.nodes = nodes;
        parseData();
    }

    /*
    We parse the AIRR format file and get one of the Ig that is in the tree.
    We get the FR/CDRs positions and the imgt gapped format, and then apply them all sequences.
     */
    private void parseData() throws Exception {
        ArrayList<TsvAirrParser.AIRRobject> airRobjects = tsvAirrParser.getAirrObjectsFromFamily(cloneId);
        String imgtGapped = null;
        String cdrfrregions = null;
        //be careful here, we have to take the longest sequence, in case of indel!!
        int longestSeq = 0;
        TsvAirrParser.AIRRobject refAirrObj = null;
        for (TsvAirrParser.AIRRobject airRobject : airRobjects) {
            String seq = airRobject.getValue("sequence_alignment");
            //Fixed 29.01.21 to keep the deletion at the begin FINALLY NOT USED HERE
            /*String modifSeq = "";
            if (seq.matches("\\.+(\\w.+).*")) {
                System.out.println("DELETION for " + seq);
                boolean isBeg = true;
                for (char n : seq.toCharArray()) {
                    if (n == '.' && isBeg) {
                        modifSeq += '-';
                    } else {
                        if (n != '.') {
                            modifSeq += n;
                        }
                        isBeg = false;
                    }
                }
                System.out.println("      " + modifSeq);
            } else {
                modifSeq = seq;
            }
            String seqWithoutDot = modifSeq.replaceAll("N", "");*/
            //Fixed the 12.09.20 by removing N
            String seqWithoutDot = seq.replaceAll("\\.", "").replaceAll("N", "");

            if (seqWithoutDot.length() > longestSeq) {
                longestSeq = seqWithoutDot.length();
                refAirrObj = airRobject;
                imgtGapped = seq;
            }

        }


        if (refAirrObj != null) {
            cdrfrregions = getCDRFRregions(refAirrObj);
        }

        //none Ig have complete FR/CDRs information, we dont use the FR/CDRs region, neither the gapped format and send a warning to the user
        if (cdrfrregions == null) {
            throw new Exception("The Igs do not have all FR/CDR regions.\nWe will not use the " +
                    "FRs and CDRs information, neither the imgt gapped format.");
        }

        //we set the info for all nodes
        else {
            for (Node node : nodes) {
                String seq = node.getSequence();
                int index = 0;
                String imgtSequence = "";
                for (char n : imgtGapped.toCharArray()) {
                    String nuc = String.valueOf(n);
                    String toAdd = ".";
                    if (!nuc.equals(".")) {
                        toAdd = String.valueOf(seq.charAt(index));
                        index++;
                    }
                    imgtSequence += toAdd;
                }
                node.setImgtFormatSequence(imgtSequence);
                node.setCdr_fr_regions(cdrfrregions);
            }
        }
    }

    private String getCDRFRregions(TsvAirrParser.AIRRobject airRobject) {
        //check is this sequence has some deletion at the begining of the sequence!
        String alignSeq = airRobject.getValue("sequence_alignment");
        boolean getFirstNuc = false;
        int numberOfDeletion = 0;
        for (char n : alignSeq.toCharArray()) {
            if (n == '.') {
                if (!getFirstNuc) {
                    numberOfDeletion++;
                }
            } else {
                getFirstNuc = true;
                break;
            }
        }
        String seq = airRobject.getValue("sequence_alignment").replaceAll("\\.", "");

        String fr1 = airRobject.getValue("fwr1").replaceAll("\\.", "");
        if (fr1.length() == 0) {
            return null;
        }
        //special case of N I put at the begining of the sequence!!
        int index = seq.indexOf(fr1) + fr1.length() + numberOfDeletion;
        String frcdrRegions = "" + index;

        String cdr1 = airRobject.getValue("cdr1").replaceAll("\\.", "");
        if (cdr1.length() == 0) {
            return null;
        }
        index = seq.indexOf(cdr1) + cdr1.length() + numberOfDeletion;
        frcdrRegions += " " + index;

        String fr2 = airRobject.getValue("fwr2").replaceAll("\\.", "");
        if (fr2.length() == 0) {
            return null;
        }
        index = seq.indexOf(fr2) + fr2.length() + numberOfDeletion;
        frcdrRegions += " " + index;

        String cdr2 = airRobject.getValue("cdr2").replaceAll("\\.", "");
        if (cdr2.length() == 0) {
            return null;
        }
        index = seq.indexOf(cdr2) + cdr2.length() + numberOfDeletion;
        frcdrRegions += " " + index;


        String fr3 = airRobject.getValue("fwr3").replaceAll("\\.", "");
        if (fr3.length() == 0) {
            return null;
        }
        index = seq.indexOf(fr3) + fr3.length() + numberOfDeletion;
        frcdrRegions += " " + index;

        String cdr3 = airRobject.getValue("cdr3").replaceAll("\\.", "");
        if (cdr3.length() == 0) {
            return null;
        }
        index = seq.indexOf(cdr3) + cdr3.length() + numberOfDeletion;
        frcdrRegions += " " + index;

        String fr4 = airRobject.getValue("fwr4").replaceAll("\\.", "");
        if (fr4.length() == 0) {
            return null;
        }
        index = seq.indexOf(fr4) + fr4.length() + numberOfDeletion;
        frcdrRegions += " " + index;

        if (index != seq.length() + numberOfDeletion) {
            System.out.println("!!!!!!!!!!!!! PROBLEM here with FR4 index " + index + " not equal to seq length "
                    + seq.length());
        }

        return frcdrRegions;
    }
}
