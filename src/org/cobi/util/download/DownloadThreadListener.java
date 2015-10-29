/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cobi.util.download;

/**
 *
 * @author mxli
 */
public interface DownloadThreadListener {
 
    public void afterPerDown( DownloadThreadEvent event);

   
    public void downCompleted( DownloadThreadEvent event);
}
