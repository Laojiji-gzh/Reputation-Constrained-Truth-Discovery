import Entity.Task;
import Entity.Worker;
import Utils.Variance;

import javax.management.StandardMBean;
import java.util.*;

public class CRH {


    public Map<Integer, Task> taskMap = new HashMap<>();
    public Map<Integer, Worker> workerMap = new HashMap<>();

    private boolean add_approval = false; // Comparison experiment (page 10 of paper)

    public CRH(Map<Integer, Task> taskMap, Map<Integer, Worker> workerMap, boolean add_approval) {
        this.taskMap = taskMap;
        this.workerMap = workerMap;
        this.add_approval = add_approval;
    }

    public double calculate_accuracy(int batch) {

        List<Integer> taskPool = new ArrayList<>(taskMap.keySet());
        Collections.shuffle(taskPool);

        while (taskPool.size() != 0) {
            List<Integer> batchTask = new ArrayList<>();    // 10 task in a batch

            // randomly select 10 task, that's totally equal to the Batch Processing
            for (int i = 0; i < batch && i < taskPool.size(); i++) {
                int taskId = taskPool.get(i);
                batchTask.add(taskId);
                taskPool.remove((Integer) taskId);
            }
            batchProcess(batchTask);
        }

        // for accuracy
        double acc = 0;
        for (Task task : taskMap.values()) {

            if (task.trueLabel[0] == task.getAggregateLabel() || task.trueLabel[1] == task.getAggregateLabel()){
                acc++;
            }
        }
        acc = acc / taskMap.size();
        return acc;
    }


    public void batchProcess(List<Integer> batchTask) {

        int iteration = 0;

        // construct the batchWorker, from the batchTask
        Set<Integer> temp = new HashSet<>();
        for (int taskId : batchTask) {
            Task task = taskMap.get(taskId);
            temp.addAll(task.assigned_worker_and_answer.keySet());
        }
        List<Integer> batchWorker = new ArrayList<>(temp);


        // start Truth Discovery
        boolean isEnd = false;

        if (add_approval) {
            double total_approval = 0;
            for (int workerId : batchWorker) {
                Worker worker = workerMap.get(workerId);
                total_approval += worker.getApproval_rate();
            }

            for (int workerId : batchWorker) {
                Worker worker = workerMap.get(workerId);
                worker.setWeight(worker.getApproval_rate() / total_approval);
            }
        }

        // the iteration may not enough in other dataset!!
        while (++iteration < 10 && !isEnd) {
            // init workers' weight, if weight == -1
            for (int workerId : batchWorker) {
                Worker worker = workerMap.get(workerId);
                if (worker.getWeight() == -1 && !add_approval) {
                    worker.setWeight(1.0 / (double) (workerMap.size()));
                }
            }

            update_label(batchWorker, batchTask);

            update_weight(batchWorker, batchTask);

        }

    }

    /**
     * update workers' weights by the CRH algorithm
     *
     * @param batchWorker all the workers associated with the current batch
     * @param batchTask   N tasks associated with the current batch
     */
    public void update_weight(List<Integer> batchWorker, List<Integer> batchTask) {

        for (int workerId : batchWorker) {
            double indicator = 0;
            Worker worker = workerMap.get(workerId);

            // store all the task the worker finished
            List<Integer> taskIdArr = new ArrayList<>(worker.finished_task_and_answer.keySet());

            // all the task in this batch
            for (int taskId : batchTask) {
                int aggregateLabel = taskMap.get(taskId).getAggregateLabel();
                if (taskIdArr.contains(taskId)) {
                    indicator += (aggregateLabel == worker.finished_task_and_answer.get(taskId) ? 0 : 1); // this place must use d(l_{i,j}, l_i), because the object function can only resolve the minimal function
                }
            }
            worker.setIndicator(indicator);
        }

        // update the weight of workers
        double totalIndicator = 0;
        for (int workerId : batchWorker) {
            totalIndicator += workerMap.get(workerId).getIndicator();
        }
        for (int workerId : batchWorker) {
            Worker worker = workerMap.get(workerId);
            worker.setWeight(-Math.log(worker.getIndicator() / totalIndicator));
        }
    }

    /**
     * update tasks' labels by the CRH algorithm
     *
     * @param batchWorker all the workers associated with the current batch
     * @param batchTask   N tasks associated with the current batch
     */
    public void update_label(List<Integer> batchWorker, List<Integer> batchTask) {


        for (int taskId : batchTask) {
            double[] labels = {0, 0, 0, 0, 0};
            Task task = taskMap.get(taskId);
            for (int aggregateLabel = 0; aggregateLabel < labels.length; aggregateLabel++) {
                for (int workerId : task.assigned_worker_and_answer.keySet()) {
                    Worker worker = workerMap.get(workerId);
                    labels[aggregateLabel] += (aggregateLabel == worker.finished_task_and_answer.get(taskId) ? 1 : 0) * worker.getWeight();
                }
            }
            double aggregatedLabel = -1;
            double max_label = 0;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] > max_label) {
                    max_label = labels[i];
                    aggregatedLabel = i;
                }
            }
            task.setAggregateLabel((int) (aggregatedLabel));
        }


    }


    public static void start(double mu, double epsilon, int lambda, double sybil_accuracy, int iteration, int batch, String dataset, boolean add_approval) {

        DataParameter.datasetName = dataset;
        double total_acc = 0;
        double total_time = 0;
        double[] std_arr = new double[iteration];
        double max_acc = 0;
        double min_acc = 1;

        // used to store the label of each iteration
        Map<Integer, ArrayList<Integer>> iteration_label = new HashMap<>();




        for (int iter = 0; iter < iteration; iter++) {

            Initialize initialize = new Initialize(mu, epsilon, lambda, sybil_accuracy);
            Output output = initialize.init();

            CRH crh = new CRH(output.taskMap, output.workerMap, add_approval);


            long startTime = System.nanoTime();
            double accuracy = crh.calculate_accuracy(batch);
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




            // another operation for a new idea
            for (Integer taskId : crh.taskMap.keySet()) {
                int aggregateLabel = crh.taskMap.get(taskId).getAggregateLabel();

                if (iteration_label.containsKey(taskId)) {
                    List<Integer> labels = iteration_label.get(taskId);
                    labels.add(aggregateLabel);
                } else {
                    iteration_label.put(taskId, new ArrayList<>());
                    iteration_label.get(taskId).add(aggregateLabel);
                }
            }

            if (iter == iteration-1){
                for (Integer taskId : iteration_label.keySet()) {
                    List<Integer> labels = iteration_label.get(taskId);
                    int[] a = {0,0,0,0,0};
                    for (Integer label : labels) {
                        if (label!=-1) a[label]++;
                    }

                    // for information entropy
                    double[] p_arr = {0,0,0,0,0};
                    for (int i = 0; i < p_arr.length; i++) {
                        double p = (double)(a[i]+1) / iteration;
                        p_arr[i] = - p * Math.log(p);
                    }
                    double info_entropy = 0;
                    for (double v : p_arr) {
                        info_entropy+=v;
                    }

                    // set information entropy
                    crh.taskMap.get(taskId).setInfo_entropy(info_entropy);


                    // set new aggregated label
                    int index = -1;
                    int max_num = -1;
                    for (int i = 0; i < a.length; i++) {
                        if (max_num < a[i]){
                            max_num = a[i];
                            index = i;
                        }
                    }
                    crh.taskMap.get(taskId).setAggregateLabel(index);
                }



                double total = 0;
                double ac = 0;

                System.out.println("ERROR TASK: ");
                for (Integer taskId : crh.taskMap.keySet()) {
                    Task task = crh.taskMap.get(taskId);
                    total++;
                    if (task.getAggregateLabel() == task.trueLabel[0] || task.getAggregateLabel() == task.trueLabel[1]){
                        ac ++;
                    }
                    else {
                        System.out.print(taskId + "(" + String.format("%.4f", task.getInfo_entropy()) + ")" + " ");
                    }
                }

                System.out.println();
                System.out.println("ERROR NUM: " + (total - ac));
                System.out.println("NEW METHOD:" + ac/total);
            }


        }






        System.out.println();
        System.out.println("Sybil-Parameter: " + "mu=" + mu + ", epsilon=" + epsilon + ", lambda=" + lambda + ", sybil_accuracy=" + sybil_accuracy);
        System.out.println("Average Accuracy: " + String.format("%.3f", total_acc / iteration * 100) + "%");
        System.out.println("Max Accuracy: " + String.format("%.3f", max_acc * 100) + "%");
        System.out.println("Min Accuracy: " + String.format("%.3f", min_acc * 100) + "%");
        System.out.println("Average Time: " + String.format("%.2f", total_time / iteration) + "ms");

        double std = Variance.get_std_error(std_arr);
        System.out.println("Standard Error: " + String.format("%.4f", std));


    }
}
