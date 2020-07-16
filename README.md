
AncesTree: an interactive immunoglobulin lineage tree visualizer
=============
AncesTree is a Graphic User Interface (GUI) that displays immunoglobulin lineage trees processed by dnaml from PHYLIP package or by IgPhyML (https://igphyml.readthedocs.io/en/latest/overview.html).

AncesTree is distributed as a cross-platform application (jar file) that can run under Windows, Linux and Mac OS. 

It only needs Java Runtime Environment (JRE) 12 or higher (https://www.oracle.com/technetwork/java/javase/downloads/index.html).

### RUNNING AncesTree ###
   
#     
   
- **Launch AncesTree GUI.**
    After downloading AncesTree.jar, in a terminal:
    
       ```
       java -jar AncesTree.jar
       ```
#     
  
- **Loading the file of interest.**
    To run AncesTree, there are two options:
    - a dnaml output file from Phylip package is needed, it is a text file that contains a phylogenic tree and the theoretical reconstructed sequences. Optionally , a fasta file can be loaded to get all AncesTree features available.The file should contain the UCA (Unmutated Common Ancestor) sequence in IMGT format and its related CDR/FR regions boundaries.  
    - a change-O in AIRR format file (https://changeo.readthedocs.io/en/stable/overview.html) and its related IgPhyML files (tab and fasta).
   
#     
   
- **Interaction with phylogenic tree.**
    Once the tree is displayed in the GUI, the user can interact with the tree:

    * Get information about a node (an Ig or a theoretical reconstructed sequence)
    * Get information about the mutations between the nodes : nucleotide or amino acid (Kabat numbering)
    * Visualize the alignment between the sequence using different mode: nucleotide, amino acid by chemistry or amino acid by highlighting mode 
    * Visualize [BASELINe](http://selection.med.yale.edu/baseline/) output which estimates positive and negative selection from immunoglobulin sequences 

### DOCUMENTATION ###

A detailed documentation can be found here: [Wiki](https://github.com/MathildeFogPerez/ancestree/wiki)

### LICENSE ###

Copyright 2020 - Mathilde Foglierini Perez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


### LIBRARIES ###

The [Apache Commons Codec](https://commons.apache.org/proper/commons-codec/) libraries used were sourced from the Apache Software Foundation and are distributed under the apache 2.0 license.
The [Apache HttpComponents](https://hc.apache.org/) libraries used were sourced from the Apache Software Foundation and are distributed under the apache 2.0 license.
The [Logging Services](http://logging.apache.org/log4j/2.x/) libraries used were sourced from the Apache Software Foundation and are distributed under the apache 2.0 license.
The [XML Graphics Commons](https://xmlgraphics.apache.org/commons/) libraries used were sourced from the Apache Software Foundation and are distributed under the apache 2.0 license.
The [JAXB](https://jaxb.java.net/) librairies are distributed under [CDDL v1.1 and GPL v2 licenses](https://glassfish.java.net/public/CDDL+GPL_1_1.html).
The [JExcel](http://www.andykhan.com/jexcelapi/) API is  distributed under the [GNU Lesser General Public Licence](http://www.gnu.org/copyleft/lesser.html).

### CITE ###

AncesTree: an interactive immunoglobulin lineage tree visualizer.
Mathilde Foglierini, Leontios Pappas, Antonio Lanzavecchia, Davide Corti, Laurent Perez.
PLoS Comput Biol. 2020 Jul 10;16(7):e1007731. [DOI: 10.1371/journal.pcbi.1007731] (https://journals.plos.org/ploscompbiol/article?id=10.1371/journal.pcbi.1007731)

### CONTACT ###

mathilde.perez@irb.usi.ch
