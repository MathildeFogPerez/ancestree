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

 	This class will extract the related codon from a sequence for a given nucleotidic position
 */

package ch.irb.translation;

import org.apache.log4j.Logger;

public class Codon {
    static Logger logger = Logger.getLogger(Codon.class);
    private String codon = null;
    private String sequence;
    private String parentSequence;
    private String nucleotide;
    private int position;

    public Codon(String sequence, int position) {
        this.sequence = sequence;
        this.position = position;
        codon = getCodonFromSequence();

    }


    private String getCodonFromSequence() {
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
            if (position + 2 > sequence.length()) {
                codon = sequence.substring(position - 1);
            } else {
                codon = sequence.substring(position - 1, position + 2);
            }
        }
        return codon;
    }

    public String getCodon() {
        return codon;
    }
}
