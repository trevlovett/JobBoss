package com.trevor;

import java.util.Comparator;

// used by our PriorityQueue to sort (ascending) by earliestStart
public class TaskComparator implements Comparator<Task> {
    @Override
    public int compare(Task x, Task y) {
        if (x.earliestStart < y.earliestStart)
            return -1;
        else if (x.earliestStart > y.earliestStart)
            return 1;
        else
            return 0;
    }
}
