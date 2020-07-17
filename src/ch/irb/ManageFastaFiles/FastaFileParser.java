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
package ch.irb.ManageFastaFiles;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * @author Mathilde This class parses a fasta file it will store the information like the following: IgName to sequence
 */
public class FastaFileParser {

    private HashMap<String, String> fastaIdToSequence = new HashMap<>();
    private LinkedHashMap<String, String> sameOrderFastaIdToSequence = new LinkedHashMap<>();
    private File file = null;

    public FastaFileParser(File file) throws IOException {
        this.file = file;
        parseFile();
    }

    private void parseFile() throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(file.getPath()));
        String line = "";
        String fastaId=null;
        String sequence="";
        while ((line = fileReader.readLine()) != null) {
            if (line.matches(">.*")){ //new entry
                //we record the previous entry
                if (fastaId!=null && sequence.length()>0){
                    String seq = sequence.toUpperCase().trim();
                    fastaIdToSequence.put(fastaId, seq);
                    sameOrderFastaIdToSequence.put(fastaId, seq);
                }
                fastaId= line.replace(">","").trim();
                //System.out.println(fastaId);
                sequence="";
            }
            else {
                sequence+= line.trim();
            }
        }

        //we store the last entry
        if (fastaId!=null && sequence.length()>0){
            String seq = sequence.toUpperCase().trim();
            fastaIdToSequence.put(fastaId, seq);
            sameOrderFastaIdToSequence.put(fastaId, seq);
        }
    }


    public HashMap<String, String> getFastaIdToSequence() {
        return fastaIdToSequence;
    }

    public LinkedHashMap<String, String> getSameOrderFastaIdToSequence() {
        return sameOrderFastaIdToSequence;
    }

    public HashMap<String, ArrayList<String>> getSeqToFastaIds() {
        HashMap<String, ArrayList<String>> seqToIds = new HashMap<>();
        ;
        for (String id : fastaIdToSequence.keySet()) {
            String seq = fastaIdToSequence.get(id);
            ArrayList<String> ids = new ArrayList<>();
            if (seqToIds.containsKey(seq)) {
                ids = seqToIds.get(seq);
            }
            ids.add(id);
            seqToIds.put(seq, ids);
        }
        return seqToIds;
    }

}
