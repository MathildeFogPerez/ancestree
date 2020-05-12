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
 *
 *  This class parses the BASELINe output txt file and store the related data (pval and sigma values for each Ig)
 */
package ch.irb.IgGenealogicTreeViewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class ClipParser {
    static Logger logger = Logger.getLogger(ClipParser.class);
    @SuppressWarnings("unused")
    private File file;
    private HashMap<String, String[]> igToPvalues = new HashMap<String, String[]>();
    private HashMap<String, String[]> igToSigmaValues = new HashMap<String, String[]>();
    private ArrayList<String> igInOrder = new ArrayList<String>();

    public ClipParser(File file) {
        this.file = file;
        Scanner scanner;
        try {
            scanner = new Scanner(new FileReader(file.getPath()));
            String lineSeparator = "\n";
            scanner.useDelimiter(lineSeparator);
            int line = 0; // first line
            // first use a Scanner to get each line
            while (scanner.hasNext()) {
                String lineToParse = scanner.next();
                if (line > 0) {
                    processLineBASELINe(lineToParse);
                }
                line++;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void processLineBASELINe(String line) {
        String[] sigmaVals = new String[2];
        String[] pVals = new String[2];
        String[] parsed = line.split("\t");
        String id = parsed[0];
        String ig = null;
        if (!id.equals("Type")) {
            ig = parsed[1];
            sigmaVals[0] = parsed[10];
            sigmaVals[1] = parsed[13];
            igToSigmaValues.put(ig, sigmaVals);
            // the pValues are used for the code coloring
            pVals[0] = parsed[16];
            pVals[1] = parsed[17];
            igToPvalues.put(ig, pVals);
            igInOrder.add(ig);
            //logger.debug("For " + ig + ", pVal CDR is " + pVals[0] + " and pVal FR is " + pVals[1]);
        }
    }

    /**
     * @return the igToPvalues
     */
    public HashMap<String, String[]> getIgToPvalues() {
        return igToPvalues;
    }

    /**
     * @return the igToSigmaValues
     */
    public HashMap<String, String[]> getIgToSigmaValues() {
        return igToSigmaValues;
    }

    public String[] getSigmaValues(String ig) {
        if (igToSigmaValues.containsKey(ig)) {
            return igToSigmaValues.get(ig);
        } else {
            return null;
        }
    }

    public String[] getPValues(String ig) {
        if (igToPvalues.containsKey(ig)) {
            return igToPvalues.get(ig);
        } else {
            return null;
        }
    }

    /**
     * @return the igInOrder
     */
    public ArrayList<String> getIgInOrder() {
        return igInOrder;
    }

}
