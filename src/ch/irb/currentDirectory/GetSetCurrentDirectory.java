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
   
   This class is used to get and set the current directory (used to load or save file)
 */

package ch.irb.currentDirectory;

import java.io.File;

public class GetSetCurrentDirectory {
    private File currentDirectory = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory();

    public GetSetCurrentDirectory() {
    }

    /**
     * @return the currentDirectory
     */
    public File getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }
}
