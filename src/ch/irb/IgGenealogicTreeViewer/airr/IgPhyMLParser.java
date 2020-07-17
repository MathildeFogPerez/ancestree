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

    This class will parse the igphyml file (tab format) and get the newick tree of the clone id chosen by the user
 */
package ch.irb.IgGenealogicTreeViewer.airr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class IgPhyMLParser {

    private HashMap<String,String>cloneIdToNewickTree = new HashMap<>();

    public  IgPhyMLParser (File igPhyMLFile) throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(igPhyMLFile.getPath()));
        String line;
        int index=0;
        int tree_index=0;
        while ((line = fileReader.readLine()) != null) {
            String[] cells = line.split("\t");
            if (index==0){
                int i=0;
                for (String cell: cells){
                    if (cell.equals("TREE")){
                        tree_index=i;
                    }
                    i++;
                }
            }
            if (index>1){
                cloneIdToNewickTree.put(cells[0],cells[tree_index]);
            }
            index++;
        }
        fileReader.close();
        //System.out.println("Number of clones/trees made by IgPhyML: "+cloneIdToNewickTree.size());
    }

    public String getNewickTree(String cloneId){
        if (cloneIdToNewickTree.containsKey(cloneId)){
            return cloneIdToNewickTree.get(cloneId);
        }
        return null;
    }
}
