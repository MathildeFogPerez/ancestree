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
   
 *  This class is used to create the color for each year in the case the user gave
 *  some immunization info for each Ig
 */

package ch.irb.IgGenealogicTreeViewer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * @author Mathilde
 *         This class is used to assign a color to a year. 2009 will always be in red, and 2008 in blue
 *         since they are pandemic years. The next 6 colors are chosen, then after 6 they are picked randomly
 */

public class ColorByYear {

    static Logger logger = Logger.getLogger(ColorByYear.class);
    private int[] years;
    private TreeMap<Integer, Color> yearToColor = new TreeMap<Integer, Color>();
    final ArrayList<Color> colors = new ArrayList<Color>();

    public ColorByYear(int[] years) {
        this.years = years;
        createColors();
        setColor();
    }

    private void createColors() {

        colors.add(new Color(Integer.parseInt("FEBFBF", 16)));// rose pale (previous FA8072)
        colors.add(new Color(Integer.parseInt("d2e1e1", 16))); // light cyan	 B4CDCD
        colors.add(new Color(Integer.parseInt("FED980", 16))); // orange previous EE7600
        colors.add(new Color(Integer.parseInt("E5FEDF", 16))); // vert pale  (previous CCFEBF)
        colors.add(new Color(Integer.parseInt("CDB5CD", 16))); // mauve
        colors.add(new Color(Integer.parseInt("FFF68F", 16))); // yellow pale 	#FFFF00
        colors.add(new Color(Integer.parseInt("E0EEE0", 16))); // bleu pale
        colors.add(new Color(Integer.parseInt("FA8072", 16))); // red


    }

    private void setColor() {
        TreeMap<Integer, Integer> occurencesOfYears = new TreeMap<Integer, Integer>();
        for (Integer year : years) {
            if (!occurencesOfYears.containsKey(year)) {
                occurencesOfYears.put(year, 1);
            } else {
                Integer occ = occurencesOfYears.get(year);
                occurencesOfYears.put(year, occ.intValue() + 1);
            }
        }
        Integer[] occ = new Integer[occurencesOfYears.size()];
        occurencesOfYears.values().toArray(occ);
        Arrays.sort(occ, Collections.reverseOrder()); // we sort by occurences
        //2009 will always be red and 2008 in blue
        int i = 2;
        for (Integer occurence : occ) {
            for (Entry<Integer, Integer> entry : occurencesOfYears.entrySet()) {
                if (entry.getKey().toString().equals("2009")) {
                    yearToColor.put(2009, colors.get(0));
                } else if (entry.getKey().toString().equals("2008")) {
                    yearToColor.put(2008, colors.get(1));
                } else if ((entry.getValue().toString()).equals(occurence.toString())) {
                    if (!yearToColor.containsKey(entry.getKey())) {
                        yearToColor.put(entry.getKey(), colors.get(i));
                        i++;
                    }
                }
                if (i >= colors.size()) {
                    colors.add(new Color(getRandomInteger(), getRandomInteger(), getRandomInteger()));
                }
            }
        }
        yearToColor.put(0, Color.white); // the default color
    }

    public Color getColorForYear(Integer year) {
        return (yearToColor.get(year));
    }

    public Set<Entry<Integer, Color>> getEntries() {
        return yearToColor.entrySet();
    }

    // this method is used to create random RGB colors in the case we have more than 10 different years to deal with
    private int getRandomInteger() {
        int aStart = 0;
        int aEnd = 255;
        Random random = new Random();
        if (aStart > aEnd) {
            throw new IllegalArgumentException("Start cannot exceed End.");
        }
        //get the range, casting to long to avoid overflow problems
        long range = (long) aEnd - (long) aStart + 1;
        // compute a fraction of the range, 0 <= frac < range
        long fraction = (long) (range * random.nextDouble());
        int randomNumber = (int) (fraction + aStart);
        return randomNumber;
    }
}
