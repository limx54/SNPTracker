/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.util.download;

import java.io.BufferedWriter;
import java.io.FileWriter;
import org.cobi.util.net.ProxyBean;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.cobi.util.coding.MD5File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 * @author Miaoxin Li
 */
public class HttpClient4DownloadTask extends DownloadTask implements Callable<String> {

    /**
     * ��ʼ����
     *
     * @throws Exception
     */
    @Override
    public String call() throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        File trueSavedFile = new File(localPath);
        try {

            //Set proxy host and port   
            if (proxyBean != null && proxyBean.getProxyHost() != null && proxyBean.getProxyPort() != null) {
                HttpHost proxy = new HttpHost(proxyBean.getProxyHost(), Integer.parseInt(proxyBean.getProxyPort()));
                if (proxyBean.getProxyUserName() != null && proxyBean.getProxyPassword() != null) {
                    // Set Credentials
                    CredentialsProvider credsProvider = new BasicCredentialsProvider();
                    //Set auhorizaiton name and password 
                    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(proxyBean.getProxyUserName(), proxyBean.getProxyPassword());
                    //create Credentials
                    credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
                    httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                    ((DefaultHttpClient) httpClient).setCredentialsProvider(credsProvider);
                } else {
                    httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                }
            }

            done = false;
            long[] totalContentLength = new long[1];
            boolean acceptRanges = getDownloadFileInfo(httpClient, totalContentLength);
            long availalableCount = 0;
            //read available  task information
            File taskFile = new File(localPath + ".task");

            if (taskFile.exists()) {
                taskRandomFile = new RandomAccessFile(taskFile, "rw");
                taskBean = readTaskBean(taskRandomFile);

                if (!trueSavedFile.exists() || taskBean.getContentLength() != totalContentLength[0]) {
                    taskBean = createTaskBean(acceptRanges, totalContentLength[0]);
                    trueSavedFile.createNewFile();
                    RandomAccessFile raf = new RandomAccessFile(trueSavedFile.getCanonicalPath() + ".save", "rw");
                    raf.setLength(totalContentLength[0]);
                    raf.close();

                } else {
                    long[] startOffset = taskBean.getSectionsOffset();
                    long[] endOffset = taskBean.getSectionsEnd();
                    int sectNum = taskBean.getSectionCount();
                    availalableCount = startOffset[0];
                    for (int i = 1; i < sectNum; i++) {
                        availalableCount += (startOffset[i] - endOffset[i - 1] - 1);
                    }

                }
            } else {
                trueSavedFile.createNewFile();
                RandomAccessFile raf = new RandomAccessFile(trueSavedFile.getCanonicalPath() + ".save", "rw");
                raf.setLength(totalContentLength[0]);
                raf.close();

                taskRandomFile = new RandomAccessFile(taskFile, "rw");
                taskBean = createTaskBean(acceptRanges, totalContentLength[0]);
                writeTaskBean(taskRandomFile, taskBean);
            }

            //lunch multiple threads for download
            launchDownloadThreads(taskBean, availalableCount, totalContentLength[0]);

            if (taskRandomFile != null) {
                taskRandomFile.close();
            }

            //do something after donwload
            if (receivedCount >= (totalContentLength[0] - availalableCount)) {
                if (taskFile.exists()) {
                    taskFile.delete();
                }

                File file = new File(trueSavedFile.getAbsolutePath() + ".save");
                if (trueSavedFile.exists()) {
                    trueSavedFile.delete();
                }

                file.renameTo(trueSavedFile);
                if (dataMd5 != null) {
                    String curMd5 = MD5File.getFileMD5StringStable(trueSavedFile);
                    if (dataMd5.equals(curMd5)) {
                        done = true;
                        showInfo(availalableCount, totalContentLength[0]);
                        fireTaskComplete();
                        BufferedWriter out = new BufferedWriter(new FileWriter(trueSavedFile.getCanonicalPath() + ".md5"));
                        out.write(dataMd5);
                        out.close();

                    } else {
                        done = true;
                        showInfo(availalableCount, totalContentLength[0]);
                        fireTaskComplete();
                    }
                }

                //if the download does not pass the MD5 checking then re-download completely
                while (!done) {
                    trueSavedFile.createNewFile();
                    RandomAccessFile raf = new RandomAccessFile(trueSavedFile.getCanonicalPath() + ".save", "rw");
                    raf.setLength(totalContentLength[0]);
                    raf.close();
                    availalableCount = 0;
                    taskRandomFile = new RandomAccessFile(taskFile, "rw");
                    taskBean = createTaskBean(acceptRanges, totalContentLength[0]);
                    writeTaskBean(taskRandomFile, taskBean);

                    //launch multiple threads for download
                    launchDownloadThreads(taskBean, availalableCount, totalContentLength[0]);

                    if (taskRandomFile != null) {
                        taskRandomFile.close();
                    }
                    //do something after donwload
                    if (receivedCount >= (totalContentLength[0] - availalableCount)) {
                        if (taskFile.exists()) {
                            taskFile.delete();
                        }

                        file = new File(trueSavedFile.getAbsolutePath() + ".save");
                        if (trueSavedFile.exists()) {
                            trueSavedFile.delete();
                        }

                        file.renameTo(trueSavedFile);
                        if (dataMd5 != null) {
                            String curMd5 = MD5File.getFileMD5StringStable(trueSavedFile);
                            if (dataMd5.equals(curMd5)) {
                                done = true;
                                showInfo(availalableCount, totalContentLength[0]);
                                fireTaskComplete();
                            }
                        } else {
                            done = true;
                            showInfo(availalableCount, totalContentLength[0]);
                            fireTaskComplete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            return trueSavedFile.getName() + " not finished!";
        } finally {
            try {
                if (taskRandomFile != null) {
                    taskRandomFile.close();
                }
                httpClient.getConnectionManager().shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return trueSavedFile.getName() + " downloaded";
    }

    /**
     * ������������߳�
     *
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void launchDownloadThreads(final DownloadTaskBean taskBean, long availalableCount, long totalContentLength) throws IOException, FileNotFoundException, Exception {

        DownloadThreadListener listener = new DownloadThreadListener() {

            @Override
            public void afterPerDown(DownloadThreadEvent event) {
                synchronized (object) {
                    try {
                        HttpClient4DownloadTask.this.receivedCount += event.getCount();
                        writeOffsetTaskBean(taskRandomFile, taskBean);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void downCompleted(DownloadThreadEvent event) {
                threads.remove(event.getTarget());
                if (getDebug()) {
                    System.out.println("ʣ���߳���" + threads.size());
                }
            }
        };

        long startPosition = 0;
        long endPosition = 0;
        int secCount = 0;


        secCount = 0;
//create download threads, here the thread pool is used
        ExecutorService exec = Executors.newFixedThreadPool(threadNum);
        CompletionService<String> serv = new ExecutorCompletionService<String>(exec);
        int runningThread = 0;
        beginTime = System.currentTimeMillis();
        while (secCount < threadNum) {
            startPosition = taskBean.getSectionsOffset()[secCount];
            endPosition = taskBean.getSectionsEnd()[secCount];
            //note: the following code is to calculate length of current section
            if (startPosition >= endPosition) {
                if (getDebug()) {
                    String msg = "Section " + (secCount + 1) + " has finished before. ";
                    System.out.println(msg);
                }
                secCount++;
                continue;
            } else {
                HttpClient4DownloadThread thread = new HttpClient4DownloadThread(secCount, taskBean, proxyBean);
                thread.addDownloadListener(listener);
                serv.submit(thread);
                threads.add(thread);
                runningThread++;
            }
            secCount++;
        }

        final ScheduledExecutorService moniterScheduler = Executors.newScheduledThreadPool(1);
        final ScheduledFuture<?> moniterSchedulerrHandle = moniterScheduler.scheduleAtFixedRate(new Moniter(availalableCount, totalContentLength), 1, 2, SECONDS);

        for (int index = 0; index < runningThread; index++) {
            Future<String> task = serv.take();
            String download = task.get();
            if (getDebug()) {
                System.out.println(download);
            }
        }
        exec.shutdown();
        moniterSchedulerrHandle.cancel(true);
        moniterScheduler.shutdown();

    }

    private void fireTaskComplete() throws Exception {
        if (listeners.isEmpty()) {
            return;
        }
        for (DownloadTaskListener listener : listeners) {
            listener.taskCompleted();
        }
    }

    private boolean getDownloadFileInfo(HttpClient httpClient, long[] dwonloadContentLength) throws IOException, ClientProtocolException, Exception {
        HttpHead httpHead = new HttpHead(url);
        HttpResponse response = httpClient.execute(httpHead);
        boolean acceptRanges = false;
        //��ȡHTTP״̬��
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != 200) {
            throw new Exception("Failed to connect " + url);
        }
        if (getDebug()) {
            for (Header header : response.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }
        }

        //Content-Length
        Header[] headers = response.getHeaders("Content-Length");
        if (headers.length > 0) {
            dwonloadContentLength[0] = Long.valueOf(headers[0].getValue());
        }
        //Accept-Ranges
//		headers = response.getHeaders("Accept-Ranges");
//		if(headers.length > 0)
//			acceptRanges = true;

        httpHead.abort();

//		if(!acceptRanges){
        httpHead = new HttpHead(url);
        httpHead.addHeader("Range", "bytes=0-" + dwonloadContentLength[0]);
        response = httpClient.execute(httpHead);
        if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 206) {
            acceptRanges = true;
        }
        httpHead.abort();
//		}
        //��֧�ֶ��߳�����ʱ
        if (!acceptRanges) {
            if (getDebug()) {
                System.out.println("�õ�ַ��֧�ֶ��߳�����");
            }
            if (acceptRanges) {
                threadNum = 1;
            }
        }
        return acceptRanges;
    }

    /**
     * ���������������
     *
     * @param url Ŀ���ַ
     * @param localPath ���ر���·��
     * @param threadCount �߳�����
     */
    public HttpClient4DownloadTask(String url, String localPath, int threadCount, ProxyBean probean) {
        this.url = url;
        this.threadNum = threadCount;
        this.localPath = localPath;
        this.proxyBean = probean;
    }

    public HttpClient4DownloadTask(String url, int threadCount, ProxyBean probean) {
        this.url = url;
        this.threadNum = threadCount;
        this.proxyBean = probean;
    }

    /**
     * ���Ա������ļ�������
     */
    public String guessFileName() throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        try {

            //Set proxy host and port   
            if (proxyBean.getProxyHost() != null && proxyBean.getProxyPort() != null) {
                HttpHost proxy = new HttpHost(proxyBean.getProxyHost(), Integer.parseInt(proxyBean.getProxyPort()));
                if (proxyBean.getProxyUserName() != null && proxyBean.getProxyPassword() != null) {
                    // Set Credentials
                    CredentialsProvider credsProvider = new BasicCredentialsProvider();
                    //Set auhorizaiton name and password 
                    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(proxyBean.getProxyUserName(), proxyBean.getProxyPassword());
                    //create Credentials
                    credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
                    httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                    ((DefaultHttpClient) httpClient).setCredentialsProvider(credsProvider);
                } else {
                    httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                }
            }
            HttpHead httpHead = new HttpHead(url);
            HttpResponse response = httpClient.execute(httpHead);
            String contentDisposition = null;
            if (response.getStatusLine().getStatusCode() == 200) {
                //Content-Disposition
                Header[] headers = response.getHeaders("Content-Disposition");
                if (headers.length > 0) {
                    contentDisposition = headers[0].getValue();
                }
            }
            httpHead.abort();

            if (contentDisposition != null && contentDisposition.startsWith("attachment")) {
                return contentDisposition.substring(contentDisposition.indexOf("=") + 1);
            } else if (Pattern.compile("(/|=)([^/&?]+\\.[a-zA-Z]+)").matcher(url).find()) {
                Matcher matcher = Pattern.compile("(/|=)([^/&?]+\\.[a-zA-Z]+)").matcher(url);
                String s = "";
                while (matcher.find()) //�����һ��URL�ϵĿ����ļ�����Ϊ���β²�Ľ��
                {
                    s = matcher.group(2);
                }
                return s;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return "UnknowName.temp";
    }

    public static void main(String[] args) throws IOException {

        try {
            long startTime, endTime;
            startTime = System.currentTimeMillis();
            String urlFolder = "http://smpserv.hku.hk:13080/kggweb/download/";

            String[] resourceFiles = {"gene.zip", "mergedRSID.zip", "1.d.zip", "2.d.zip", "3.d.zip", "4.d.zip", "5.d.zip",
                "6.d.zip", "7.d.zip", "8.d.zip", "9.d.zip", "10.d.zip", "11.d.zip", "12.d.zip",
                "13.d.zip", "14.d.zip", "15.d.zip", "16.d.zip", "17.d.zip", "18.d.zip",
                "19.d.zip", "20.d.zip", "21.d.zip", "22.d.zip", "X.d.zip", "Y.d.zip", "MT.d.zip"
            };


            //   String[] resourceFiles = {"gene.zip", "mergedRSID.zip", "1.d.zip"};
            //  HttpClient4DownloadTask.setDebug(true); //���õ���
            ExecutorService exec = Executors.newFixedThreadPool(2);

            String txtLocal = "D:/tmp/";
            for (int i = 0; i < resourceFiles.length; i++) {
                final HttpClient4DownloadTask task = new HttpClient4DownloadTask(urlFolder + resourceFiles[i], 9, null);
                File filePath = new File(txtLocal);

                if (filePath.isDirectory()) {
                    task.setLocalPath(txtLocal + task.guessFileName());
                } else {
                    task.setLocalPath(txtLocal);
                }


                task.addTaskListener(new DownloadTaskListener() {

                    @Override
                    public void autoCallback(DownloadTaskEvent event) {
                        int progess = (int) (event.getTotalDownloadedCount() * 100.0 / event.getTotalCount());
                        System.out.println(progess + " " + event.getRealTimeSpeed() + " " + event.getGlobalSpeed());
                    }

                    @Override
                    public void taskCompleted() throws Exception {
                        /*
                        File savedFile = new File(task.getLocalPath());
                        Zipper ziper = new Zipper();
                        if (task.getLocalPath().endsWith("zip")) {
                        ziper.extractZip(savedFile.getCanonicalPath(), savedFile.getParent());
                        
                        savedFile.delete();
                        } else if (task.getLocalPath().endsWith(".gz")) {
                        ziper.extractTarGz(task.getLocalPath(), savedFile.getParent());
                        savedFile.delete();
                        }
                         */
                    }

                    /*
                    if (fullPathTrueSavedFile.indexOf("hapmap") >= 0) {
                    GlobalManager.resourceFileSize.put("hapmap/" + trueSavedFile.getName(), trueSavedFile.length());
                    } else {
                    GlobalManager.resourceFileSize.put(trueSavedFile.getName(), trueSavedFile.length());
                    }
                     *
                     */
                });


                //   task.call();
                exec.submit(task);
            }
            exec.shutdown();
            StringBuilder inforString = new StringBuilder();
            inforString.append("The Overall Lapsed Time: ");
            endTime = System.currentTimeMillis();
            inforString.append((endTime - startTime) / 1000.0);
            inforString.append(" Seconds.\n");
            System.out.println("\n" + inforString.toString());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
