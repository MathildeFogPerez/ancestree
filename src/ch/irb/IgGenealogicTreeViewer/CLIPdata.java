/* Copyright 2020 - Mathilde Foglierini Perez

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
 * What we call here CLIP data are now the BASELINe data (former software was called CLIP)
 * This class stores the BASELINe data.
 */

package ch.irb.IgGenealogicTreeViewer;

public class CLIPdata {
    private String pValCDR;
    private String pValFR;
    private String sigmaCDR;
    private String sigmaFR;

    public CLIPdata(String pValCDR, String pValFR, String sigmaCDR, String sigmaFR) {
        this.sigmaCDR = sigmaCDR;
        this.sigmaFR = sigmaFR;
        this.pValCDR = pValCDR;
        this.pValFR = pValFR;
    }

    /**
     * @return the pValCDR
     */
    public String getpValCDR() {
        return pValCDR;
    }

    /**
     * @return the sigmaCDR
     */
    public String getSigmaCDR() {
        return sigmaCDR;
    }

    /**
     * @return the sigmaFR
     */
    public String getSigmaFR() {
        return sigmaFR;
    }

    /**
     * @return the pValFR
     */
    public String getpValFR() {
        return pValFR;
    }

}
