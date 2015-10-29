/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.snptracker;

import java.io.File;

/**
 *
 * @author mxli
 */
public class GlobalManager {

    public static String LOCAL_FOLDER = "./";
    public static String RESOURCE_PATH = LOCAL_FOLDER + "resources/";
    public static String LOCAL_COPY_FOLDER = LOCAL_FOLDER + "updated/";

    public static void initialVariables() throws Exception {
        String path = GlobalManager.class.getResource("/resources/").getPath();
        //normally, it will be like this file:/D:/home/mxli/MyJava/KGGSeq/dist/snptracker.jar!/resource/
        //System.out.println(path);

        int index = path.indexOf("snptracker.jar");
        if (index > 0) {
            path = path.substring(6, index);
            File localFolder = new File(path);
            if (localFolder.exists()) {
                LOCAL_FOLDER = localFolder.getCanonicalPath();
            }
        }

       // System.out.println(LOCAL_FOLDER);
        RESOURCE_PATH = LOCAL_FOLDER + "/resources/";
        LOCAL_COPY_FOLDER = LOCAL_FOLDER + "/updated/";
    }
}
