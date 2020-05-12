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

/**
 * @author Mathilde
 *         We have to be sure to have the correct fasta format for the sequence and also that if there are deletions, they should
 *         be a multiple of 3 and they should be in the frame
 */
@SuppressWarnings("serial")
public class FastaFormatException extends Exception {
    static Logger logger = Logger.getLogger(FastaFormatException.class);
    private String message;


    public FastaFormatException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
