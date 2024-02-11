import Entity.Task;
import Entity.Worker;
import Utils.Variance;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RCTD {

    public Map<Integer, Task> taskMap = new HashMap<>();
    public Map<Integer, Worker> workerMap = new HashMap<>();
    private int step;
    private double p;


    public RCTD(Map<Integer, Task> taskMap, Map<Integer, Worker> workerMap, double p, int step) {
        this.p = p;
        this.step = step;
        this.taskMap = taskMap;
        this.workerMap = workerMap;

        // calculate average accuracy
        double average_acc = 0;
        for (Worker worker : workerMap.values()) {
            average_acc += worker.getAccuracy();
        }
        DataParameter.averageAcc = average_acc / workerMap.size();
    }


    /**
     * The average accuracy of the worker in this dataset
     * @return
     */
    public double get_ave_acc(){
        double trueNum = 0;
        for (Task task : taskMap.values()) {
            for (Integer workerId : task.assigned_worker_and_answer.keySet()) {
                Integer answer = task.assigned_worker_and_answer.get(workerId);
                if (answer == task.trueLabel[0] || answer == task.trueLabel[1]){
                    trueNum++;
                }
            }
        }

        return trueNum/(double) (taskMap.size() * DataParameter.taskForWorkerNum);
    }
    /**
     * @return
     */
    public double calculate_accuracy(int batch) {

        List<Integer> taskPool = new ArrayList<>(taskMap.keySet());
        Collections.shuffle(taskPool);

        while (taskPool.size() != 0) {
            List<Integer> batchTask = new ArrayList<>();    // n task in a batch
            for (int i = 0; i < batch && i < taskPool.size(); i++) {
                int taskId = taskPool.get(i);
                batchTask.add(taskId);
                taskPool.remove((Integer) taskId);
            }

            // construct the batchWorker, from the batchTask
            Set<Integer> temp = new HashSet<>();
            for (int taskId : batchTask) {
                Task task = taskMap.get(taskId);
                temp.addAll(task.assigned_worker_and_answer.keySet());
            }
            List<Integer> batchWorker = new ArrayList<>(temp);
            batchProcess(batchTask, batchWorker);
        }

        // for accuracy
        double acc = 0;
        for (Task task : taskMap.values()) {

            if (task.trueLabel[0] == task.getAggregateLabel() || task.trueLabel[1] == task.getAggregateLabel()){
                acc++;
            }
        }

//        System.out.print("True Answer:" + acc + "\t");
        acc = acc / taskMap.size();


        try {
            BufferedWriter w1 = new BufferedWriter(new FileWriter("data/Toloka5/accuracy.csv"));
            for (Integer workerid : workerMap.keySet()) {
                w1.write(workerid + "\t" + String.format("%.3f",workerMap.get(workerid).getAccuracy() * 100) + "%"+ "\t" + workerMap.get(workerid).getTaskCount() +"\n");
            }
            w1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return acc;
    }

    public void batchProcess(List<Integer> batchTask, List<Integer> batchWorker) {

        int iteration = 0;
        boolean isEnd = false;

        // start Truth Discovery
        while (++iteration < 50 && !isEnd) {
            // init workers' weight, if weight == -1
            for (int workerId : batchWorker) {
                Worker worker = workerMap.get(workerId);
                if (worker.getWeight() == -1) {
                    worker.setWeight(1.0 / (double) batchWorker.size());    // initialization: 1/N
                }
            }

            isEnd = update_label(batchWorker, batchTask);
            update_weight(batchWorker, batchTask);

        }


    }

    public void update_weight(List<Integer> batchWorker, List<Integer> batchTask) {

        // set each workers' indicator function, the indicator function only related to the tasks in batchTask
        for (int workerId : batchWorker) {
            double indicator = 0;
            Worker worker = workerMap.get(workerId);

            // store all the task the worker finished
            List<Integer> taskIdArr = new ArrayList<>(worker.finished_task_and_answer.keySet());

            // all the task in THIS batch
            for (Integer taskId : batchTask) {
                int aggregateLabel = taskMap.get(taskId).getAggregateLabel();
                if (taskIdArr.contains(taskId)) {
                    int worker_ans = worker.finished_task_and_answer.get(taskId);
                    indicator += (aggregateLabel == worker_ans ? 1 : 0);
                }
            }
            worker.setIndicator(indicator);
        }

        // transform batchWorker and batchTask to the Map
        Map<Integer, Worker> batchWorkerMap = new HashMap<>();
        Map<Integer, Task> batchTaskMap = new HashMap<>();

        for (Integer workerId : batchWorker) {
            batchWorkerMap.put(workerId, workerMap.get(workerId));
        }
        for (Integer taskId : batchTask) {
            batchTaskMap.put(taskId, taskMap.get(taskId));
        }

        // update weight by RDTD
        TruthDiscovery_RDTD td = new TruthDiscovery_RDTD(batchWorkerMap, batchTaskMap, p, step);
        td.run();

    }

    public boolean update_label(List<Integer> batchWorker, List<Integer> batchTask) {

        int difference = 0;

        for (int taskId : batchTask) {
            double[] labels = {0, 0, 0, 0, 0, 0};
            Task task = taskMap.get(taskId);
            for (int aggregateLabel = 0; aggregateLabel < labels.length; aggregateLabel++) {
                for (int workerId : task.assigned_worker_and_answer.keySet()) {
                    Worker worker = workerMap.get(workerId);
                    labels[aggregateLabel] += (aggregateLabel == worker.finished_task_and_answer.get(taskId) ? 1 : 0) * worker.getWeight();
                }
            }
            int aggregatedLabel = -1;
            double max_label = 0;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] > max_label) {
                    max_label = labels[i];
                    aggregatedLabel = i;
                }
            }
            if (aggregatedLabel != task.getAggregateLabel()) {
                difference++;
            }
            task.setAggregateLabel(aggregatedLabel);

        }

        return difference == 0;

    }

    public static void start(double mu, double epsilon, int lambda, double sybil_accuracy, double p, int step, int iteration, int batch, String dataset, double k, double m, boolean is_wilson) {



        // check the parameter
        if (p < 0.7 || p > 1) {
            System.out.println("p should between 0.8 to 1");
            return;
        }
        if (step < 1 || step > 4) {
            System.out.println("step should between 1 to 4");
            return;
        }

        DataParameter.datasetName = dataset;
        double total_acc = 0;
        double total_time = 0;
        double[] std_arr = new double[iteration];
        double max_acc = 0;
        double min_acc = 1;

        // used to store the label of each iteration
        Map<Integer, ArrayList<Integer>> iteration_label = new HashMap<>();

        Output output = null;
        System.out.println("Time Cost is not Include the Initialization");


        for (int iter = 0; iter < iteration; iter++) {

            Initialize initialize = new Initialize(mu, epsilon, lambda, sybil_accuracy, p, step, dataset, k, m, is_wilson);
            output = initialize.init();

            RCTD RCTD = new RCTD(output.taskMap, output.workerMap, p, step);

            long startTime = System.nanoTime();
            double accuracy = RCTD.calculate_accuracy(batch);
            long endTime = System.nanoTime();
            if (accuracy > max_acc) {
                max_acc = accuracy;
            }
            if (accuracy < min_acc) {
                min_acc = accuracy;
            }
            std_arr[iter] = accuracy;

            double time = (endTime - startTime) * 1e-6;

            String acc = String.format("%.3f", accuracy * 100); // convert to percentage
            System.out.println("RUN" + (iter + 1) + ":\t" + acc + "%" + "\t\tTime:" + String.format("%.2f", time) + "ms");
            total_acc += accuracy;
            total_time += time;



            for (Integer taskId : RCTD.taskMap.keySet()) {
                int aggregateLabel = RCTD.taskMap.get(taskId).getAggregateLabel();

                if (iteration_label.containsKey(taskId)) {
                    List<Integer> labels = iteration_label.get(taskId);
                    labels.add(aggregateLabel);
                } else {
                    iteration_label.put(taskId, new ArrayList<>());
                    iteration_label.get(taskId).add(aggregateLabel);
                }
            }

            // A test, not associated with RCTD!!!
            if (iter == iteration-1){
                for (Integer taskId : iteration_label.keySet()) {
                    List<Integer> labels = iteration_label.get(taskId);
                    int[] a = {0,0,0,0,0,0};
                    for (Integer label : labels) {
                        if (label!=-1) a[label]++;
                    }
                    int index = -1;
                    int max_num = -1;
                    for (int i = 0; i < a.length; i++) {
                        if (max_num < a[i]){
                            max_num = a[i];
                            index = i;
                        }
                    }
                    RCTD.taskMap.get(taskId).setAggregateLabel(index);
                }
                double total = 0;
                double ac = 0;
                for (Integer taskId : RCTD.taskMap.keySet()) {
                    Task task = RCTD.taskMap.get(taskId);
                    total++;
                    if (task.getAggregateLabel() == task.trueLabel[0] || task.getAggregateLabel() == task.trueLabel[1]){
                        ac ++;
                    }
                }

//                System.out.println("NEW METHOD:" + ac/total);
            }



        }


        System.out.println();
        System.out.println("Dataset: " + dataset + "\n" + "Sybil-Parameter: " + "mu=" + mu + ", epsilon=" + epsilon + ", lambda=" + lambda + ", sybil_accuracy=" + sybil_accuracy + "\n" + "Penalty Parameter: " + "step=" + step + ", p="+ p + "\n" + "Approval Rate Simulation: "+ " k = " + String.format("%.2f",output.k) + ", m = " + String.format("%.2f",output.m));
        System.out.println("Average Accuracy: " + String.format("%.3f", total_acc / iteration * 100) + "%");
        System.out.println("Max Accuracy: " + String.format("%.3f", max_acc * 100) + "%");
        System.out.println("Min Accuracy: " + String.format("%.3f", min_acc * 100) + "%");
        System.out.println("Average Time: " + String.format("%.2f", total_time / iteration) + "ms");

        double std = Variance.get_std_error(std_arr);
        System.out.println("Standard Error: " + String.format("%.4f", std));

    }


}
