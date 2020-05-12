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

   This class stores the color related to the pValues from BASELINe analysis.
   The colors are the same than the BASELINe website.
 */

package ch.irb.IgGenealogicTreeViewer;

import java.awt.Color;
import java.math.BigDecimal;

import org.apache.log4j.Logger;

public class PValueCodeColor {
    static Color red1 = new Color(255, 0, 0);
    static Color red2 = new Color(255, 66, 66);
    static Color red3 = new Color(255, 104, 104);
    static Color red4 = new Color(255, 151, 151);
    static Color red5 = new Color(255, 236, 236); //new Color(255, 196, 196);
    static Color green1 = new Color(0, 133, 0);
    static Color green2 = new Color(0, 200, 50);
    static Color green3 = new Color(0, 255, 64);
    static Color green4 = new Color(128, 255, 129);
    static Color green5 = new Color(233, 255, 233);//new Color(255, 196, 196);
    static Logger logger = Logger.getLogger(PValueCodeColor.class);

    public PValueCodeColor() {
    }

    public Color getColor(String pValue) {
        if (pValue.matches(".*NA.*")) {
            return Color.white;
        }
        boolean isNegatif = false;
        if (pValue.matches("-.*")) {
            isNegatif = true;
        }
        BigDecimal pVal = new BigDecimal(pValue);
        BigDecimal absVal = pVal.abs();
        if ((absVal.compareTo(new BigDecimal(0.005)) == -1) || (absVal.compareTo(new BigDecimal(0.005)) == -0)) {
            if (isNegatif) {
                return green1;
            } else {
                return red1;
            }
        } else if ((absVal.compareTo(new BigDecimal(0.01)) == -1) || (absVal.compareTo(new BigDecimal(0.01)) == -0)) {
            if (isNegatif) {
                return green2;
            } else {
                return red2;
            }
        } else if ((absVal.compareTo(new BigDecimal(0.05)) == -1) || (absVal.compareTo(new BigDecimal(0.05)) == -0)) {
            if (isNegatif) {
                return green3;
            } else {
                return red3;
            }
        } else if ((absVal.compareTo(new BigDecimal(0.1)) == -1) || (absVal.compareTo(new BigDecimal(0.1)) == -0)) {
            if (isNegatif) {
                return green4;
            } else {
                return red4;
            }
        } else {
            if (isNegatif) {
                return green5;
            } else {
                return red5;
            }
        }
    }

}
