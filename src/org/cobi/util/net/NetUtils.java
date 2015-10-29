/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.util.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.cobi.snptracker.Constants;
import static org.cobi.snptracker.Constants.URL_FOLDER;
import org.cobi.snptracker.GlobalManager;
import org.cobi.util.download.DownloadTaskEvent;
import org.cobi.util.download.DownloadTaskListener;
import org.cobi.util.download.HttpClient4API;
import org.cobi.util.download.HttpClient4DownloadTask;

/**
 *
 * @author mxli
 */
public class NetUtils implements Constants {

    public static void updateLocal() throws Exception {
        for (int i = 0; i < LIB_FILE_PATHES.length; i++) {
            File copiedFile = new File(GlobalManager.LOCAL_COPY_FOLDER + File.separator + LIB_FILE_PATHES[i]);
            File targetFile = new File(GlobalManager.LOCAL_FOLDER + File.separator + LIB_FILE_PATHES[i]);
            //a file with size less than 1k is not normal
            if (copiedFile.length() > 1024 && copiedFile.length() != targetFile.length() && LIB_FILE_PATHES[i].indexOf("jar") >= 0) {
                copyFile(targetFile, copiedFile);
            }
        }
    }

    public static void copyFile(File targetFile, File sourceFile) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            if (!sourceFile.exists()) {
                return;
            }

            if (targetFile.exists()) {
                if (!targetFile.delete()) {
                    targetFile.deleteOnExit();
                    //System.err.println("Cannot delete " + targetFile.getCanonicalPath());
                }
            }

            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(targetFile);

            byte[] buffer = new byte[1024 * 5];
            int size;
            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
                out.flush();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
        }
    }

    public static boolean isConnected() {
        String url = URL_FOLDER + LIB_FILE_PATHES[0];

        URLConnection urlconn = null;
        try {
            URL u = new URL(url);
            urlconn = u.openConnection();
            urlconn.setConnectTimeout(1000);
            urlconn.connect();
            /*
             TimedUrlConnection timeoutconn = new TimedUrlConnection(urlconn, 5000);//time   out:   100seconds
             boolean bconnectok = timeoutconn.connect();
             if (bconnectok == false) {
             //urlconn   fails   to   connect   in   100seconds
             return false;
             } else {
             //connect   ok
             return true;
             }
             *
             */
            return true;
        } catch (SocketTimeoutException ex) {
            // Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (ConnectException ex) {
            // Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (MalformedURLException ex) {
            //Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            // Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
        }
    }

    public static boolean downloadLibFile() throws Exception {
        final List<String> updatedLocalFiles = new ArrayList<String>();
        final List<String> updatedLocalFileMD5s = new ArrayList<String>();
        for (int i = 0; i < LIB_FILE_PATHES.length; i++) {
            File newLibFile = new File(GlobalManager.LOCAL_COPY_FOLDER + File.separator + LIB_FILE_PATHES[i]);
            File newLibFileMd5 = new File(GlobalManager.LOCAL_COPY_FOLDER + File.separator + LIB_FILE_PATHES[i] + ".md5");
            String url = URL_FOLDER + LIB_FILE_PATHES[i];
            // String md5Remote = HttpClient4API.getContent(url, null);

            if (!newLibFile.exists()) {
                updatedLocalFiles.add(LIB_FILE_PATHES[i]);
                // updatedLocalFileMD5s.add(md5Remote);
            } else {
                long netFileLen = HttpClient4API.getContentLength(url, null);
                long fileSize = newLibFile.length();

                if (fileSize != netFileLen) {
                    updatedLocalFiles.add(LIB_FILE_PATHES[i]);
                    //updatedLocalFileMD5s.add(md5Remote);
                }
            }
        }

        if (updatedLocalFiles.isEmpty()) {
            return false;
        }

        System.out.println("Updating libraries...");
        int filesNum = updatedLocalFiles.size();
        int MAX_TASK = 1;
        int runningThread = 0;
        ExecutorService exec = Executors.newFixedThreadPool(MAX_TASK);
        CompletionService serv = new ExecutorCompletionService(exec);
        for (int i = 0; i < filesNum; i++) {
            File newLibFile = new File(GlobalManager.LOCAL_COPY_FOLDER + File.separator + updatedLocalFiles.get(i));
            final HttpClient4DownloadTask task = new HttpClient4DownloadTask(URL_FOLDER + updatedLocalFiles.get(i), 9, null);
            File libFolder = newLibFile.getParentFile();
            if (!libFolder.exists()) {
                libFolder.mkdirs();
            }
            task.setLocalPath(newLibFile.getCanonicalPath());
            // task.setDataMd5(updatedLocalFileMD5s.get(i));
            task.addTaskListener(new DownloadTaskListener() {

                @Override
                public void autoCallback(DownloadTaskEvent event) {
                    int progess = (int) (event.getTotalDownloadedCount() * 100.0 / event.getTotalCount());
                    System.out.println(progess + " RealTimeSpeed: " + event.getRealTimeSpeed() + " GlobalSpeed: " + event.getGlobalSpeed());

                }

                @Override
                public void taskCompleted() throws Exception {
                    File copiedFile = new File(GlobalManager.LOCAL_COPY_FOLDER + File.separator + task.getLocalPath());
                    File targetFile = new File(GlobalManager.LOCAL_FOLDER + File.separator + task.getLocalPath());
                    //a file with size less than 1k is not normal
                    if (copiedFile.length() > 1024 && copiedFile.length() != targetFile.length()) {
                        copyFile(targetFile, copiedFile);
                    }
                }
            });
            //   task.call();
            serv.submit(task);
            runningThread++;
        }

        for (int index = 0; index < runningThread; index++) {
            Future task = serv.take();
            String download = (String) task.get();
        }
        exec.shutdown();
        System.out.println("The library of has been updated! Please re-initiate this application!");
        return true;
    }

    public static void downloadResource() throws Exception {
        final List<File> updatedLocalFiles = new ArrayList<File>();
        final List<String> updatedLocalFileMD5s = new ArrayList<String>();
        for (int i = 0; i < RESOURCE_FILE_PATHES.length; i++) {
            File newLibFile = new File(GlobalManager.RESOURCE_PATH + File.separator + RESOURCE_FILE_PATHES[i]);
            File newLibFileMd5 = new File(GlobalManager.RESOURCE_PATH + File.separator + RESOURCE_FILE_PATHES[i] + ".md5");
            String url = URL_FOLDER + RESOURCE_FILE_PATHES[i] + ".md5";
            String md5Remote = HttpClient4API.getContent(url, null);

            if (!newLibFile.exists()) {
                updatedLocalFiles.add(newLibFile);
                updatedLocalFileMD5s.add(md5Remote);
            } else {
                if (!newLibFileMd5.exists()) {
                    updatedLocalFiles.add(newLibFile);
                    updatedLocalFileMD5s.add(md5Remote);
                } else {
                    String md5Local = NetUtils.readAFile(newLibFileMd5.getCanonicalPath());
                    if (!md5Remote.equals(md5Local)) {
                        updatedLocalFiles.add(newLibFile);
                        updatedLocalFileMD5s.add(md5Remote);
                    }
                }
            }

        }

        if (updatedLocalFiles.isEmpty()) {
            System.out.println("No file(s) need to been updated!");
            return;
        }
        int filesNum = updatedLocalFiles.size();
        int MAX_TASK = 1;
        int runningThread = 0;
        ExecutorService exec = Executors.newFixedThreadPool(MAX_TASK);
        CompletionService serv = new ExecutorCompletionService(exec);

        System.out.println("Updating file(s)...");

        for (int i = 0; i < filesNum; i++) {
            final HttpClient4DownloadTask task = new HttpClient4DownloadTask(URL_FOLDER + updatedLocalFiles.get(i).getName(), 9, null);
            File libFolder = updatedLocalFiles.get(i).getParentFile();
            if (!libFolder.exists()) {
                libFolder.mkdirs();
            }
            task.setLocalPath(updatedLocalFiles.get(i).getCanonicalPath());
            task.setDataMd5(updatedLocalFileMD5s.get(i));
            task.addTaskListener(new DownloadTaskListener() {

                @Override
                public void autoCallback(DownloadTaskEvent event) {
                    int progess = (int) (event.getTotalDownloadedCount() * 100.0 / event.getTotalCount());
                    System.out.println(progess + " RealTimeSpeed: " + event.getRealTimeSpeed() + " GlobalSpeed: " + event.getGlobalSpeed());
                }

                @Override
                public void taskCompleted() throws Exception {
                }
            });
            //   task.call();
            serv.submit(task);
            runningThread++;
        }
        for (int index = 0; index < runningThread; index++) {
            Future task = serv.take();
            String download = (String) task.get();
        }
        exec.shutdown();

        
        /* Create new mergeFile 
        for (int i = 0; i < RESOURCE_FILE_PATHES.length; i++) {
            if (RESOURCE_FILE_PATHES[i].endsWith("RsMergeArch.bcp.gz")) {
                CreateOwnMergeFile cMF = new CreateOwnMergeFile();
                OpenIntIntHashMap rM = cMF.readMergeBackground(GlobalManager.RESOURCE_PATH + File.separator + RESOURCE_FILE_PATHES[i]);
                cMF.createOwnMergeFile(rM, GlobalManager.RESOURCE_PATH + File.separator + RESOURCE_FILE_PATHES[i]);                
            }
        }
        */
        System.out.println(filesNum + " file(s) have been updated! ");
    }

    public static String readAFile(String filePath) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
}
