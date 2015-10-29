/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.util.download;

/**
 *
 * @author mxli
 */
public interface DownloadTaskListener {

    public void autoCallback(DownloadTaskEvent event);

    public void taskCompleted( ) throws Exception;
}
