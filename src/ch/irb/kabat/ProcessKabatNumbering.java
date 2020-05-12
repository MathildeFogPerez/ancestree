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

   This class will connect to http://www.bioinf.org.uk/abs/abnum/
   using their web-servies interface.
   It will get the Kabat numbering from the heavy or light chain (prot)
 */

package ch.irb.kabat;

import java.io.IOException;
import java.util.TreeMap;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

public class ProcessKabatNumbering {
    static Logger logger = Logger.getLogger(ProcessKabatNumbering.class);
    private String aaSequence;
    private TreeMap<Integer, String> fromPositionToKabatnumbering = new TreeMap<>();
    private TreeMap<Integer, String> fromPositionToAA = new TreeMap<>();

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        try {
            ProcessKabatNumbering processKabatNumbering = new ProcessKabatNumbering(
                    "QSVLTQPPSASGTPGQRVTISCSGSSSNIGSNTVNWYQQLTGTAPKLLIYSNNQRPSGVPDRFSGSKSGTSASLAISGLQSEDEADYYCAVWHDSLDGWVFGGGTKLTVL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ProcessKabatNumbering(String aaSequence) throws ClientProtocolException, IOException {
        // TO KEEP in cas it happens again!! throw new IOException("Until the Abnum website doesnt work");
        this.aaSequence = aaSequence;
        for (int i = 0; i < aaSequence.length(); i++) {
            fromPositionToAA.put(i, String.valueOf(aaSequence.charAt(i)));
        }
        String aaSequenceWithoutDeletion = this.aaSequence.replaceAll("-", "");
        launchKabatWebSite(aaSequenceWithoutDeletion);

        if (fromPositionToKabatnumbering.size()==0) throw new ClientProtocolException("No kabat numbering found via Abnum website");

        logger.debug("Number of positions "+fromPositionToKabatnumbering.size());
        /*for (int pos: fromPositionToKabatnumbering.keySet()){
            System.out.println(pos+": "+fromPositionToKabatnumbering.get(pos));
        }*/
    }

    private void launchKabatWebSite(String aaSequenceWithoutDeletion) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        try {
            //http://www.bioinf.org.uk/cgi-bin/abnum/abnum.pl?plain=1&aaseq= Before 28.11.19, NOW.cgi
           // http://www.bioinf.org.uk/abs/abnum/abnumws_pl.txt
            HttpGet httpget = new HttpGet("http://www.bioinf.org.uk/abs/abnum/abnum.cgi??plain=1&aaseq="
                   + aaSequenceWithoutDeletion + "&scheme=-k");
            // logger.debug("executing request " + httpget.getURI());
            // Create a response handler
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpget, responseHandler);
            storeKabatNumbering(responseBody);

        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    private void storeKabatNumbering(String kabatOutput) {
        String[] entry = kabatOutput.split("\n");
        int index = 0;
        for (String ent : entry) {
            //we remove all HTML tags
            String[] en = ent.replaceAll("\\<[^>]*>","").split("\\s");
             //logger.debug(en[0]+" for "+en[1]);
            if (en[0].length() > 1 && !ent.contains("Results")) {
                String kabatNumb = en[0].substring(1);// to get rid of the H or L
                Integer id = index;
                boolean kabatNumberingFound = false;
                while (!kabatNumberingFound) {
                    if (!en[1].equals("-") && en[1].equals(fromPositionToAA.get(id))) {// kabat output
                        fromPositionToKabatnumbering.put(id, kabatNumb);
                        // logger.debug("we keep  position " + id.toString() + ": " + kabatNumb);
                        index++;
                        kabatNumberingFound = true;
                    } else if (fromPositionToAA.containsKey(id)) { // we have a deletion, we put a deletion such as
                        // kabat
                        // numbering
                        if (fromPositionToAA.get(id).equals("-")) {
                            fromPositionToKabatnumbering.put(id, "-");
                            // logger.debug("we keep deletion position " + id.toString() + ": -");
                            // then we have to go one after
                            index++;
                            id = index;
                        } else
                        // case of a deletion from Kabat website
                        {
                            kabatNumberingFound = true; // to go out of this loop
                        }
                    } else {
                        kabatNumberingFound = true; // to go out of this loop
                    }
                }
            }
        }
    }

    /**
     * @return the fromPositionToKabatnumbering
     */
    public TreeMap<Integer, String> getFromPositionToKabatnumbering() {
        return fromPositionToKabatnumbering;
    }
}
