/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.util.download;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author MX Li
 */
public class TimedUrlConnection implements Observer {

    private URLConnection ucon = null;
    private int time = 300000;//max   time   out
    private boolean connected = false;

    public TimedUrlConnection(URLConnection ucon, int time) {
        this.ucon = ucon;
        this.time = time;
    }

    public boolean connect() {
        ObservableURLConnection ouc = new ObservableURLConnection(ucon);
        ouc.addObserver(this);
        Thread ouct = new Thread(ouc);
        ouct.start();
        try {
            ouct.join(time);
        } catch (InterruptedException i) {
            //false,   but   should   already   be   false
        }
        return (connected);
    }

    @Override
    public void update(Observable o, Object arg) {
        connected = true;
    }//end   of   public   void   update(Observable   o,   Object   arg)
}

class ObservableURLConnection extends Observable implements Runnable {

    private URLConnection ucon;

    public ObservableURLConnection(URLConnection ucon) {
        this.ucon = ucon;
    }//end   of   constructor

    @Override
    public void run() {
        try {
            ucon.connect();
            setChanged();
            notifyObservers();
        } catch (IOException e) {
        }
    }//end   of   public   void   run()
}
