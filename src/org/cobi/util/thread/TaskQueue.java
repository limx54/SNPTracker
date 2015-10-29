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

import java.util.LinkedList;
import java.util.List;

/**
 *
 
 */
public class TaskQueue {

    private List queue = new LinkedList();
    public synchronized Task getTask() {
        /*
        //note: the following code enable the program to wait for any task, which is a usfull function for web server
        while (queue.size() == 0) {
        try {
        this.wait();
        } catch (InterruptedException ie) {
        return null;
        }
        }
         */
        if ((queue.size() == 0)) {
            return null;
        } else {
            return (Task) queue.remove(0);
        }
    }

    public synchronized void putTask(Task task) {

        queue.add(task);

    // this.notifyAll();

    }
}
