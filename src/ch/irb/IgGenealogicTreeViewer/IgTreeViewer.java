/*
   Copyright 2020 - Mathilde Foglierini Perez

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   This is the main class to launch AncesTree.jar
 */

package ch.irb.IgGenealogicTreeViewer;

import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class IgTreeViewer {

    public static String xmlFilePath = null;
    static Logger logger = Logger.getLogger(IgTreeViewer.class);

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        disableAccessWarnings();
        if ((args != null) && (args.length > 0)) {
            xmlFilePath = args[0];
            if (args.length > 1) {
                xmlFilePath = "";
                int index = 0;
                for (String arg : args) {
                    xmlFilePath += arg;
                    if (index < args.length) {
                        xmlFilePath += " ";
                    }
                    index++;
                }
            }
            IgTreeViewerFrame igTreeViewerFrame = new IgTreeViewerFrame(xmlFilePath);
        } else {
            IgTreeViewerFrame igTreeViewerFrame2 = new IgTreeViewerFrame();
        }
    }

    @SuppressWarnings("unchecked")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

}
