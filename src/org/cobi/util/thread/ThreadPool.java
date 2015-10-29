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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
//reference

/**
 *
 
 */
public class ThreadPool extends ThreadGroup {

    private List threads = new LinkedList();
    private TaskQueue queue;

    public ThreadPool(TaskQueue queue) {
        super("Thread-Pool");
        this.queue = queue;
    }

    public synchronized void addWorkerThread() {
        Thread t = new WorkerThread(this, queue);
        threads.add(t);
        t.start();
    }

    public synchronized void removeWorkerThread() {
        if (threads.size() > 0) {
            WorkerThread t = (WorkerThread) threads.remove(0);
            t.shutdown();
        }
    }

    public void jointAllWorkerThread() {
        if (threads.size() > 0) {
            try {
                Iterator it = threads.iterator();
                while (it.hasNext()) {
                    WorkerThread t = (WorkerThread) it.next();
                    t.join();
                }
            } catch (Exception ex) {
            }
        }

    }

    public synchronized void currentStatus() {
        StringBuffer info=new StringBuffer();
        info.append("-----------------------------------------------");
        info.append("Thread count = " + threads.size());
        Iterator it = threads.iterator();
        while (it.hasNext()) {
            WorkerThread t = (WorkerThread) it.next();
            info.append(t.getName() + ": " + (t.isIdle() ? "idle" : "busy"));
        }
        info.append("-----------------------------------------------");
        System.out.println(info.toString());        
    }
}
