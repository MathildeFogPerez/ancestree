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

package ch.irb.IgGenealogicTreeViewer;

import java.util.ArrayList;
import java.util.TreeMap;

import ch.irb.nodes.NodeGraph;

/**
 * @author Mathilde
 *         This class is used to assign each nucleotidic position to a region: FR1, CDR1, FR2, CDR2, FR3, CDR3 and FR4
 *         The information about the regions come from the IMGT file that the user made and load at the beginning of AncesTree
 */
public class SetImgtInfo {

    private TreeMap<Integer, String> positionNucToCdrFrRegions = new TreeMap<>();
    private TreeMap<Integer, String> positionAAToCdrFrRegions = new TreeMap<>();
    private int aaCDR2;

    // This method will link all the nucleotidic position to each CDR FR regions
    // If the UCA doesnt have deletion, we will take by default, its boundaries region
    public SetImgtInfo(NodeGraph node) {
        ArrayList<Integer> fromCDR2ToNucPositions = new ArrayList<>();
        String[] infos = node.getCdr_fr_regions().split("\\s+");
        int start = 0;
        int index = 1;
        for (String position : infos) {
            for (int i = start; i < Integer.parseInt(position); i++) {
                int aaPos = i / 3;
                switch (index) {
                    case 1:
                        positionNucToCdrFrRegions.put(i, "FR1");
                        positionAAToCdrFrRegions.put(aaPos, "FR1");
                        break;
                    case 2:
                        positionNucToCdrFrRegions.put(i, "CDR1");
                        positionAAToCdrFrRegions.put(aaPos, "CDR1");
                        break;
                    case 3:
                        positionNucToCdrFrRegions.put(i, "FR2");
                        positionAAToCdrFrRegions.put(aaPos, "FR2");
                        break;
                    case 4:
                        positionNucToCdrFrRegions.put(i, "CDR2");
                        positionAAToCdrFrRegions.put(aaPos, "CDR2");
                        fromCDR2ToNucPositions.add(i);
                        break;
                    case 5:
                        positionNucToCdrFrRegions.put(i, "FR3");
                        positionAAToCdrFrRegions.put(aaPos, "FR3");
                        break;
                    case 6:
                        positionNucToCdrFrRegions.put(i, "CDR3");
                        positionAAToCdrFrRegions.put(aaPos, "CDR3");
                        break;
                    case 7:
                        positionNucToCdrFrRegions.put(i, "FR4");
                        positionAAToCdrFrRegions.put(aaPos, "FR4");
                        break;
                }
            }
            start = Integer.parseInt(position);
            index++;
        }
        //get number of AA for CDR1,2 and 3
        int numbNuc = 0;
        for (@SuppressWarnings("unused") Integer position : fromCDR2ToNucPositions) {
            numbNuc++;
        }
        aaCDR2 = numbNuc / 3;

    }


    public int getNumberAAForCDR2() {
        return aaCDR2;
    }

    /**
     * @return the positionNucToCdrFrRegions
     */
    public TreeMap<Integer, String> getPositionNucToCdrFrRegions() {
        return positionNucToCdrFrRegions;
    }

    /**
     * @return the positionAAToCdrFrRegions
     */
    public TreeMap<Integer, String> getPositionAAToCdrFrRegions() {
        return positionAAToCdrFrRegions;
    }
}
