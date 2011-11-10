package com.trevor;

import java.util.ArrayList;

// abstraction of a task, basically just a struct of time start/stop values + some metadata
public class Task {
    public int id, time, staff;
    public String name;

    public int earliestStart = 0;
    public int latestStart = 0;
    public int latestFinish = 0;
    public int earliestFinish;
    public int slack;

    public ArrayList<Integer> predecessorIDs;
    public int cntPredecessors;

    public Task(int n) {
        this.id = n;
    }

    public Task(int id, String name, int time, int staff, ArrayList<Integer> predecessorIDs) {
        this.id = id;
        this.name = name;
        this.time = time;
        this.staff = staff;
        this.predecessorIDs = predecessorIDs;
        this.cntPredecessors = predecessorIDs.size();
    }

    // given a time t, will either start or stop a task depending on whether 
    // t = start or stop time of task
    public int run(int t, StringBuffer log) {
        if (t == this.earliestStart) {
            log.append("\tStarting: " + this.id + "\n");
            return this.staff;
        }
        else if (t == this.earliestStart + time) {
            log.append("\tFinished: " + this.id + "\n");
            return -this.staff;
        }
        else {
            return 0;
        }
    }

    // same as above, but with slack-- can be useful in debugging
    public int runWithSlack(int t, StringBuffer log) {
        if (t == this.latestStart) {
            log.append("\tStarting: " + this.id + "\n");
            return this.staff;
        }
        else if (t == this.latestStart + time) {
            log.append("\tFinished: " + this.id + "\n");
            return -this.staff;
        }
        else {
            return 0;
        }
    }

    // String output used by Scheduler
    public String toString() {
        String predString = "";
        for (Integer id: this.predecessorIDs) {
            predString += id.toString() + ", ";
        }

        return "\n[" + this.id + "] " + this.name + "\n\tTime to finish: " + this.time + "\n\tManpower required: " + this.staff + "\n\tSlack : " + this.slack + "\n\tLatest starting time : " + this.latestStart;
    }
}
