import com.trevor.Scheduler;

public class JobBoss {
    public static void format() {
        System.err.println("format: java JobBoss projectName.txt manpower");
    }

    public static void main(String args[]) {
        try {
            if (args.length != 2) {
                format();
                System.exit(1);
            }
           
            String filename = args[0];
            String manpower = args[1];

            Scheduler sched = new Scheduler(filename, Integer.parseInt(manpower));

            if (sched.printCycles()) {
                System.exit(1);
            }
            
            sched.startTasks(true);
            sched.findSlacks();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
