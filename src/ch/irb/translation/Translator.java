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

package ch.irb.translation;

import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

/**
 * @author Mathilde This class take the DNA sequence and translates it into a protein sequence
 */
public class Translator {
    static Logger logger = Logger.getLogger(Translator.class);
    private String dnaSequence;
    private String protSequence = "";
    private boolean isDNA;
    private HashMap<String, String> codons = new HashMap<String, String>();

    public Translator(String dnaSequence, boolean isDNA) {
        setDNA(isDNA);
        this.dnaSequence = dnaSequence;
        if (!isDNA) {
            protSequence = dnaSequence;
        } else {
            setCodonsTable();
            //bug fixed the 12.09.20
            if (dnaSequence != null) {
                translate();
            }
        }
    }

    private void translate(){
        char[] nuc = dnaSequence.toCharArray();
        String codon = "";
        int nucleotide = 0;
        for (int i = 0; i < nuc.length; i++) {
            nucleotide++;
            if (nucleotide == 1 || nucleotide == 2 || nucleotide == 3) {
                codon = codon.concat(String.valueOf(nuc[i]));
            }
            if (nucleotide == 3) {
                // logger.debug("CODON is "+codon);
                String aa = codons.get(codon);
                if (aa == null) {
                    aa = "X"; //'X' for not know codon
                }
                protSequence = protSequence.concat(aa);
                nucleotide = 0;
                codon = "";
            }
        }
    }

    private void setCodonsTable() {
        codons.put("ATT", "I");
        codons.put("ATC", "I");
        codons.put("ATA", "I");
        codons.put("CTT", "L");
        codons.put("CTC", "L");
        codons.put("CTA", "L");
        codons.put("CTG", "L");
        codons.put("TTA", "L");
        codons.put("TTG", "L");
        codons.put("GTT", "V");
        codons.put("GTC", "V");
        codons.put("GTA", "V");
        codons.put("GTG", "V");
        codons.put("TTT", "F");
        codons.put("TTC", "F");
        codons.put("ATG", "M");
        codons.put("TGT", "C");
        codons.put("TGC", "C");
        codons.put("GCT", "A");
        codons.put("GCC", "A");
        codons.put("GCA", "A");
        codons.put("GCG", "A");
        codons.put("GGT", "G");
        codons.put("GGC", "G");
        codons.put("GGA", "G");
        codons.put("GGG", "G");
        codons.put("CCT", "P");
        codons.put("CCC", "P");
        codons.put("CCA", "P");
        codons.put("CCG", "P");
        codons.put("ACT", "T");
        codons.put("ACA", "T");
        codons.put("ACG", "T");
        codons.put("ACC", "T");
        codons.put("TCT", "S");
        codons.put("TCC", "S");
        codons.put("TCA", "S");
        codons.put("TCG", "S");
        codons.put("AGT", "S");
        codons.put("AGC", "S");
        codons.put("TAT", "Y");
        codons.put("TAC", "Y");
        codons.put("TGG", "W");
        codons.put("CAA", "Q");
        codons.put("CAG", "Q");
        codons.put("AAT", "N");
        codons.put("AAC", "N");
        codons.put("CAT", "H");
        codons.put("CAC", "H");
        codons.put("GAA", "E");
        codons.put("GAG", "E");
        codons.put("GAT", "D");
        codons.put("GAC", "D");
        codons.put("AAA", "K");
        codons.put("AAG", "K");
        codons.put("CGT", "R");
        codons.put("CGC", "R");
        codons.put("CGA", "R");
        codons.put("CGG", "R");
        codons.put("AGA", "R");
        codons.put("AGG", "R");
        codons.put("TAA", "*"); // *=stop codon!
        codons.put("TAG", "*"); // *=stop codon!
        codons.put("TGA", "*"); // *=stop codon!
        codons.put("---", "-"); // DELETION
    }

    public String getProteinSequence() {
        return protSequence;
    }

    /**
     * @return the isDNA
     */
    public boolean isDNA() {
        return isDNA;
    }

    /**
     * @param isDNA the isDNA to set
     */
    public void setDNA(boolean isDNA) {
        this.isDNA = isDNA;
    }

}
