// (c) 2008-2009 Miaoxin Li
// This file is distributed as part of the IGG source code package
// and may not be redistributed in any form, without prior written
// permission from the author. Permission is granted for you to
// modify this file for your own personal use, but modified versions
// must retain this copyright notice and must not be distributed.

// Permission is granted for you to use this file to compile IGG.

// All computer programs have bugs. Use this file at your own risk.
// Saturday, January 17, 2009
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.util.thread;
// reference http://doc.linuxpk.com/44867.html

/**
 *
 
 */
public class WorkerThread extends Thread {

    private static int count = 0;
    private boolean busy = false;
    private boolean stop = false;
    private TaskQueue queue;

    public WorkerThread(ThreadGroup group, TaskQueue queue) {
        super(group, "Worker-" + count);
        count++;
        this.queue = queue;
    }

    public WorkerThread(TaskQueue queue) {
        count++;
        this.queue = queue;
    }

    public void shutdown() {
        stop = true;
        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException ie) {
        }
    }

    public boolean isIdle() {
        return !busy;

    }

    @Override
    public void run() {
        System.out.println(getName() + " start.");
        // ((IGG3View) GlobalVariables.currentApplication.getMainView()).setBriefRunningInfor(getName() + " start.");
        //GlobalVariables.addInforLog(getName() + " start.");
        while (!stop) {
            Task task = queue.getTask();
            if (task != null) {
                busy = true;
                task.execute();
                busy = false;
            } else {
                stop = true;
            }
        }
        System.out.println(getName() + " end.");
    //((IGG3View) GlobalVariables.currentApplication.getMainView()).setBriefRunningInfor(getName() + " end.");
    // GlobalVariables.addInforLog(getName() + " end.");
    }
}
