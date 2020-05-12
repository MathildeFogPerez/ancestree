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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Mathilde
 *         This class will create a fasta file. The input will be the name of the file, then  a hashMap
 *         containing all the IDs with their sequences
 */
public class FastaFileMaker {

    private String filePath;
    private HashMap<String, String> fastaIdToSequence = new HashMap<>();

    public FastaFileMaker(String filePath, HashMap<String, String> fastaIdToSequence) {
        setFilePath(filePath);
        setFastaIdTosequence(fastaIdToSequence);
        try {
            createFastaFile();
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    private void createFastaFile() throws FileAlreadyExistsException {
        // Create the empty file with default permissions, etc.
        File file = new File(filePath);
        if (file.exists()) {
            throw new FileAlreadyExistsException(filePath);
        } else {
            try {
                FileWriter fstream = new FileWriter(filePath);
                BufferedWriter out = new BufferedWriter(fstream);
                for (Map.Entry<String, String> entry : fastaIdToSequence.entrySet()) {
                    String fastaId = entry.getKey();
                    String sequence = entry.getValue();
                    out.write(">" + fastaId
                            + System.getProperty("line.separator") + sequence
                            + System.getProperty("line.separator"));
                }
                // Close the output stream
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public HashMap<String, String> getFastaIdTosequence() {
        return fastaIdToSequence;
    }

    public void setFastaIdTosequence(HashMap<String, String> fastaIdTosequence) {
        this.fastaIdToSequence = fastaIdTosequence;
    }

}
