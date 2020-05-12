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

   This class stores the "chemical" properties of each amino acid
 */
package ch.irb.translation;

import java.util.HashMap;

public class AminoAcidProperties {

    private HashMap<String, String> aminoAcidProperty = new HashMap<String, String>();

    public AminoAcidProperties() {
        setProperties();
    }

    private void setProperties() {
        aminoAcidProperty.put("S", "hydrophilic");
        aminoAcidProperty.put("T", "hydrophilic");
        aminoAcidProperty.put("C", "hydrophilic");
        aminoAcidProperty.put("Y", "hydrophilic");
        aminoAcidProperty.put("N", "hydrophilic");
        aminoAcidProperty.put("Q", "hydrophilic");

        aminoAcidProperty.put("G", "hydrophobic");
        aminoAcidProperty.put("A", "hydrophobic");
        aminoAcidProperty.put("V", "hydrophobic");
        aminoAcidProperty.put("L", "hydrophobic");
        aminoAcidProperty.put("I", "hydrophobic");
        aminoAcidProperty.put("M", "hydrophobic");
        aminoAcidProperty.put("F", "hydrophobic");
        aminoAcidProperty.put("W", "hydrophobic");
        aminoAcidProperty.put("P", "hydrophobic");

        aminoAcidProperty.put("D", "charged");
        aminoAcidProperty.put("E", "charged");
        aminoAcidProperty.put("K", "charged");
        aminoAcidProperty.put("R", "charged");
        aminoAcidProperty.put("H", "charged");

        aminoAcidProperty.put("-", "deletion");
    }

    public String getAminoAcidProperty(String aminoAcid) {
        return aminoAcidProperty.get(aminoAcid);
    }
}
