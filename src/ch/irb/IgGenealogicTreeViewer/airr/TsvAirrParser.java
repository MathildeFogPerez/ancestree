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

   This class will parse an AIRR format file coming from changeo pipeline, after clustering and creating germline.
   It will store all data with a hashmap key/value corresponding to the header of the file
   It will store all clonal families by their clone_id
 */

package ch.irb.IgGenealogicTreeViewer.airr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/*

 */
public class TsvAirrParser {

    private boolean isCloneIdPresent=true;

    public static void main(String[] args) {
        try {
            TsvAirrParser tsvAirrFormatParser = new TsvAirrParser(new File(args[0]), "1", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TreeMap<String, ArrayList<AIRRobject>> clonalFamToAIIRobjs = new TreeMap<>();

    public TsvAirrParser(File tsvAirrFile, String cloneId, HashSet<String> nodeNames) throws IOException {
        //System.out.println("processing clone_id "+cloneId);
        BufferedReader fileReader = new BufferedReader(new FileReader(tsvAirrFile));
        String line = "";
        int index = 0;
        int cloneIdIndex = 0;
        HashMap<Integer, String> indexToHeader = new HashMap<>();
        while ((line = fileReader.readLine()) != null) {
            String[] cells = line.split("\t");
            if (index == 0) {
                int i = 0;
                for (String cell : cells) {
                    indexToHeader.put(i, cell);
                    if (cell.equals("clone_id")) {
                        cloneIdIndex = i;
                    }
                    i++;
                }
            } else {
                //we store only the sequences belonging to the cloneId and the nodes in the newick tree(to go faster)
                if (cells[cloneIdIndex].equals(cloneId)) {
                    isCloneIdPresent=true;
                    if (nodeNames.contains(cells[0])) {
                        LinkedHashMap<String, String> airrKeyToValue = new LinkedHashMap<>();
                        int j = 0;
                        for (String cell : cells) {
                            String header = indexToHeader.get(j);
                            airrKeyToValue.put(header, cell);
                            j++;
                        }
                        AIRRobject airRobject = new AIRRobject(cells[0], airrKeyToValue);
                        String clone_id = airrKeyToValue.get("clone_id");
                        ArrayList<AIRRobject> airRobjects = new ArrayList<>();
                        if (clonalFamToAIIRobjs.containsKey(clone_id)) {
                            airRobjects = clonalFamToAIIRobjs.get(clone_id);
                        }
                        airRobjects.add(airRobject);
                        clonalFamToAIIRobjs.put(clone_id, airRobjects);
                    }else{
                      //  System.out.println("This node name is not in the igphyml tree "+cells[0]);
                    }
                }
            }
            index++;
        }
        index--;
        fileReader.close();
        //System.out.println("Number of heavy sequence processed " + index);
        //System.out.println("Number of clonal families processed " + clonalFamToAIIRobjs.size());

    }

    public TreeMap<String, ArrayList<AIRRobject>> getClonalFamToAIIRobjs() {
        return clonalFamToAIIRobjs;
    }

    public ArrayList<AIRRobject> getAirrObjectsFromFamily(String cloneId) {
        ArrayList<AIRRobject> airRobjects = null;
        if (clonalFamToAIIRobjs.containsKey(cloneId)) {
            airRobjects = clonalFamToAIIRobjs.get(cloneId);
        }
        return airRobjects;
    }

    public boolean isCloneIdPresent() {
        return isCloneIdPresent;
    }

    public class AIRRobject {
        private String sequence_id;
        private LinkedHashMap<String, String> airrKeyToValue; //already provided by changeo output (airr format)

        public AIRRobject(String sequence_id, LinkedHashMap<String, String> airrKeyToValue) {
            setSequence_id(sequence_id);
            setAirrKeyToValue(airrKeyToValue);
        }

        public String getSequence_id() {
            return sequence_id;
        }

        public void setSequence_id(String sequence_id) {
            this.sequence_id = sequence_id;
        }

        public LinkedHashMap<String, String> getAirrKeyToValue() {
            return airrKeyToValue;
        }

        public void setAirrKeyToValue(LinkedHashMap<String, String> airrKeyToValue) {
            this.airrKeyToValue = airrKeyToValue;
        }

        public String getValue(String airrkey) {
            return airrKeyToValue.get(airrkey);
        }


    }
}
