/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.util.download;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.cobi.util.net.ProxyBean;

/**
 *
 */
public class HttpClient4DownloadThread extends DownloadThread implements Callable<String> {

    public HttpClient4DownloadThread(int sectionID, DownloadTaskBean taskBean, ProxyBean proxyBean) {
        super(sectionID, taskBean, proxyBean);
    }

    @Override
    public String call() throws Exception {
        if (HttpClient4DownloadTask.getDebug()) {
            System.out.println("Thread:" + sectionID + " Start:" + taskBean.getSectionsOffset()[sectionID] + "-" + taskBean.getSectionsEnd()[sectionID]);
        }
        HttpClient httpClient = new DefaultHttpClient();


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


        try {
            HttpGet httpGet = new HttpGet(taskBean.getDownURL());

            if (taskBean.isIsRange()) {// 
                httpGet.addHeader("Range", "bytes=" + taskBean.getSectionsOffset()[sectionID] + "-" + taskBean.getSectionsEnd()[sectionID]);
            }
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpClient4DownloadTask.getDebug()) {
                for (Header header : response.getAllHeaders()) {
                    System.out.println(header.getName() + ":" + header.getValue());
                }
                System.out.println("statusCode:" + statusCode);
            }
            if (statusCode == 206 || (statusCode == 200 && !taskBean.isIsRange())) {
                InputStream inputStream = response.getEntity().getContent();
                // BufferedInputStream bis = new BufferedInputStream(is, temp.length);
                //��������д��
                RandomAccessFile outputStream = new RandomAccessFile(taskBean.getSaveFile() + ".save", "rw");
                //��ָ��λ��
                long offset = taskBean.getSectionsOffset()[sectionID];
                outputStream.seek(taskBean.getSectionsOffset()[sectionID]);
                int count = 0;
                byte[] buffer = new byte[10 * 1024];

                while (notStop && (count = inputStream.read(buffer, 0, buffer.length)) > 0) {
                    outputStream.write(buffer, 0, count);
                    offset += count;
                    taskBean.getSectionsOffset()[sectionID] = offset;
                    //���������¼�
                    fireAfterPerDown(new DownloadThreadEvent(this, count));

                }
                outputStream.close();
                if (notStop) {
                    EntityUtils.consume(response.getEntity());
                }
            }
            httpGet.abort();
        } finally {
            //������������¼�
            fireDownCompleted(new DownloadThreadEvent(this, taskBean.getSectionsEnd()[sectionID]));
            if (HttpClient4DownloadTask.getDebug()) {
                System.out.println("End:" + taskBean.getSectionsOffset()[sectionID] + "-" + taskBean.getSectionsEnd()[sectionID]);
            }
            httpClient.getConnectionManager().shutdown();
        }
        return "Download thread " + sectionID + " over";
    }
}
