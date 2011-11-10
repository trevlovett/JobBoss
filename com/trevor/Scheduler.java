package com.trevor;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Stack;

// Scheduler maintains a DAG of task-nodes and provides cycle-checking, simulated job-execution,
// and slack-computation functions.

public class Scheduler {
    // array of tasks
    private Task[] taskPool;

    // array of adjacency lists 
    private ArrayList[] adjLists;

    // default max manpower 
    private int manpower = 999;

    // build a new task graph from file, set manpower
    public Scheduler(String filename, int manpower) {
        try {
            this.manpower = manpower;

            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;

            line = br.readLine();
            int numTasks = Integer.parseInt(line);

            // counting from 1..numTasks
            this.taskPool = new Task[numTasks+1];
            this.adjLists = new ArrayList[numTasks+1];

            // skip blank line
            br.readLine();

            while ((line = br.readLine()) != null) {
                // tokenize by whitespace
                String[] toks = line.split("\\s+");
                ArrayList<Integer> predecessorIDs = new ArrayList();

                int k = 4;
                while (!toks[k].equals("0")) {
                    int id = Integer.parseInt(toks[k]);
                    predecessorIDs.add(id);
                    k++;
                }

                int taskID = Integer.parseInt(toks[0]);
                String name = toks[1];
                int time = Integer.parseInt(toks[2]);
                int staff = Integer.parseInt(toks[3]);

                // finally, task and its predcessor ID's are created and stored
                this.taskPool[taskID] = new Task(taskID, name, time, staff, predecessorIDs);
            }

            // initialize adjacency lists
            for (int k=1; k < this.taskPool.length; k++)
                this.adjLists[k] = new ArrayList();

            // build adjacency lists by reversing info in the predecessorIDs lists
            for (int k=1; k < this.taskPool.length; k++) {
                for (Integer id: this.taskPool[k].predecessorIDs)
                    this.adjLists[id.intValue()].add(k);
            }

        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /* 
       Performs a topological sort/traversal of the graph, computing earliestStart for all tasks.
       Every task-node visited is placed in a priority queue, where the priority = earliestStart.
       This provides a fast way of sorting the tasks for use in the simulated execution.
       More details within.
    */
    public void startTasks(boolean output) throws Exception {
        // queue to store nodes to visit in topological sort
        LinkedList<Task> queue = new LinkedList();

        // counts nodes visited
        int counter = 0;

        // priority queue to store tasks in sorted order (by earliestStart)
        Comparator<Task> comparator = new TaskComparator();
        PriorityQueue<Task> priorityQueue = 
            new PriorityQueue<Task>(8, comparator);

        // the first nodes to be visited will be the ones without any predecessors 
        for (int k=1; k < this.taskPool.length; k++) {
            if (this.taskPool[k].cntPredecessors == 0) {
                queue.add(this.taskPool[k]);
            }
        }

        // topological sorting -- main loop
        while (!queue.isEmpty()) {
            Task task = queue.poll(); // dequeue

            //  computation of earliestStart.
            //  
            if (task.predecessorIDs.size() > 0) {
   		        int maxTime = -1;
                int startTime = 0;

                // find max start-time for all of this nodes predecessors
                for (Integer predID : task.predecessorIDs) {
                    Task predTask = this.taskPool[((Integer)predID).intValue()];
                    startTime = predTask.time + predTask.earliestStart;
                    if (startTime > maxTime) {
                        maxTime = startTime;
                    }
                }

                // the earliest this task can occur is the max-start-time of its predecessors
                task.earliestStart = maxTime;

                // we destroyed this val in the sort, so reset it here
                task.cntPredecessors = task.predecessorIDs.size();
            }
            // end computation

            // put task in priority queue for fast and easy sort
            priorityQueue.add(task);
            ++counter;

            // If we can find adjacent node without visited predecessors, visit it next
            for (Object adjTaskId : this.adjLists[task.id]) {
                Task adjTask = this.taskPool[((Integer)adjTaskId).intValue()];
                if (--adjTask.cntPredecessors == 0)
                    queue.add(adjTask);
            }
        }

        // althout we check for cycles elsewhere, here's another
        if (counter != this.taskPool.length - 1) 
            throw new Exception("Cycle found");

        // because the iterator() for PriorityQueue is not guaranteed to
        // provide elements in the expected order, I dump all sorted elems into an ArrayList
        ArrayList<Task> runningTasks = new ArrayList();
        while (!priorityQueue.isEmpty()) {
            Task task = priorityQueue.remove();
            runningTasks.add(task);
        }

        // Prepare for the output

        // compute last time slice
        Task lastTask = runningTasks.get(runningTasks.size() - 1);
        int tEnd = lastTask.earliestStart + lastTask.time;

        int totalStaff = 0;
        int prevTotalStaff = 0;

        // Loop through the list of tasks in order of earliestStart and run them
        for (int t = 0; t <= tEnd; t++) {
            StringBuffer log = new StringBuffer();
            for (Task task : runningTasks) {
                totalStaff += task.run(t, log);
            }

            if (totalStaff > this.manpower) {
                throw new Exception("Manpower demanded (" + totalStaff + ") exceeds limit of " + this.manpower);
            }

            if (output && totalStaff != prevTotalStaff) {
                System.out.println("\nTime: " + t);
                System.out.print(log);
                System.out.println("\tCurrent staff: " + totalStaff);
            }
            prevTotalStaff = totalStaff;
        }

        if (output) System.out.println("\n**** Shortest possible project execution is " + tEnd + "****\n");
    }

    /* Compute latestStart and slack for all nodes in graph */
    public void findSlacks() throws Exception {
        LinkedList<Task> queue = new LinkedList();

        for (int id=1; id < this.adjLists.length; id++) {
            // find all nodes without successors -- we will visit these first
            if (adjLists[id].isEmpty()) {
                Task task = this.taskPool[id];

                // these tasks have to finish on time, so their latestFinish is just..
                task.latestFinish = task.earliestStart + task.time;
                // and no slacking allowed
                task.slack = 0;
                // earliest start-time is their latest
                task.latestStart = task.earliestStart;

                // put them in the queue, first to be visited
                queue.add(this.taskPool[id]);
            }
        }

        while (!queue.isEmpty()) {
            Task task = queue.poll(); // dequeue

            if (!this.adjLists[task.id].isEmpty()) {
                // find minimum start time of all adjacent task-nodes
                int minStart = Integer.MAX_VALUE;
                for (Object adjTaskID: this.adjLists[task.id]) {
                    Task adjTask = this.taskPool[((Integer)adjTaskID).intValue()];
                    if (adjTask.earliestStart < minStart) 
                        minStart = adjTask.earliestStart; 
                }

                // latest time we can finish is the minimum start time of all adjacent nodes
                task.latestFinish = minStart;

                // slack computation, straightforward:
                task.slack = (task.latestFinish - task.earliestStart) - task.time;

                // latest start time
                task.latestStart = task.earliestStart + task.slack;
            }

            // visit the predecessors of this node next
            for (Integer predID : task.predecessorIDs) {
                queue.add(this.taskPool[predID.intValue()]);
            }
        }

        // print the tasks in order of ID
        for (int i=1; i < this.taskPool.length; i++) {
            System.out.println(this.taskPool[i]);
        }
    }

    /* returns true if cycle is found, false if not 
       calls 'visit' which uses depth-first-search to identify cycles,
       and prints cycle-path (in the form of ID's) to console */
    public boolean printCycles() {
        ArrayList<Task> visitedList = new ArrayList();
        HashMap<Task, Integer> visitedHash = new HashMap();
        for (int k=1; k < this.taskPool.length; k++)
            if (this.taskPool[k].cntPredecessors == 0) 
                if (visit(this.taskPool[k], visitedHash, visitedList)) return true;

        System.out.println("No cycles found.");
        return false;
    }
   
    /* 
       workhorse of printCycles--performs a DFS and keeps track of visited nodes
       with a HashMap and ArrayList.
       HashMap is for quick identification of re-visits
       ArrayList stores path info for display
    */
    private boolean visit(Task task, 
                       HashMap<Task, Integer>visitedHash, 
                       ArrayList<Task>visitedList)
    {
        if (visitedHash.containsKey(task)) {
            System.out.println("Cycle found.");
            for (int i = 0; i < visitedList.size(); i++) {
                if (visitedList.get(i) == task) {
                    for (int j = i; j < visitedList.size(); j++) 
                        System.out.print(visitedList.get(j).id + ", ");
                    System.out.println(task.id);
                    return true;
                }
            }
        }
        else {
            visitedList.add(task);
            visitedHash.put(task, 1);
            for (Object adjTaskID: this.adjLists[task.id]) {
                Task adjTask = this.taskPool[((Integer)adjTaskID).intValue()];
                return visit(adjTask, visitedHash, visitedList);
            }
        }
        return false;
    }

    // TESTS ------------------------------------------------------------------

    // unit test--checks if the slacks cause any unwanted overlaps between tasks and successors
    public void testSlack() throws Exception {
        LinkedList<Task> queue = new LinkedList();
        int counter = 0;
        Comparator<Task> comparator = new TaskComparator();
        PriorityQueue<Task> priorityQueue = 
            new PriorityQueue<Task>(8, comparator);

        for (int k=1; k < this.taskPool.length; k++) {
            if (this.taskPool[k].cntPredecessors == 0) {
                queue.add(this.taskPool[k]);
            }
        }

        while (!queue.isEmpty()) {
            Task task = queue.poll(); // dequeue

            if (task.predecessorIDs.size() > 0) {
   		        int maxTime = -1;
                int startTime = 0;

                for (Integer predID : task.predecessorIDs) {
                    Task predTask = this.taskPool[((Integer)predID).intValue()];
                    startTime = predTask.time + predTask.earliestStart;
                    if (startTime > maxTime) {
                        maxTime = startTime;
                    }
                }
                task.earliestStart = maxTime;
                task.cntPredecessors = task.predecessorIDs.size();
            }

            priorityQueue.add(task);
            ++counter;

            for (Object adjTaskId : this.adjLists[task.id]) {
                Task adjTask = this.taskPool[((Integer)adjTaskId).intValue()];
                if (--adjTask.cntPredecessors == 0)
                    queue.add(adjTask);
            }
        }

        if (counter != this.taskPool.length - 1) 
            throw new Exception("Cycle found");

        ArrayList<Task> runningTasks = new ArrayList();

        while (!priorityQueue.isEmpty()) {
            Task task = priorityQueue.remove();
            for (Object adjTaskId : this.adjLists[task.id]) {
                Task adjTask = this.taskPool[((Integer)adjTaskId).intValue()];
                System.out.println("[" + task.id + "] task.latestFinish =  " + task.latestFinish + ", adjTask.latestStart = " + adjTask.latestStart);
                if (task.latestFinish > adjTask.latestStart) {
                    throw new Exception("Slack error");
                }
            }
        }
    }
}

