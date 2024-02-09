import Entity.Task;
import Entity.Worker;
import Utils.Chi;
import Utils.Variance;

import java.util.*;

public class CATD {


    public Map<Integer, Task> taskMap = new HashMap<>();
    public Map<Integer, Worker> workerMap = new HashMap<>();

    private boolean add_approval = false; // Comparison experiment (page 10 of paper)


    public CATD(Map<Integer, Task> taskMap, Map<Integer, Worker> workerMap, boolean add_approval) {
        this.taskMap = taskMap;
        this.workerMap = workerMap;
        this.add_approval =  add_approval;
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
            boolean temp = task.getAggregateLabel() == task.trueLabel[0] || task.getAggregateLabel() == task.trueLabel[1];
            acc += (temp ? 1 : 0);
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
        while (++iteration < 8 && !isEnd) {

            // init workers' weight, if weight == -1
            for (int workerId : batchWorker) {
                Worker worker = workerMap.get(workerId);
                if (worker.getWeight() == -1) {
                    worker.setWeight(1.0 / (double) (workerMap.size()));
                }
            }

            isEnd = update_label(batchWorker, batchTask);

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

            // The task worker finished in this batch
            int worker_finished_task_batch = 0;

            // all the task in this batch
            for (int taskId : batchTask) {
                int aggregateLabel = taskMap.get(taskId).getAggregateLabel();
                if (taskIdArr.contains(taskId)) {
                    indicator += Math.pow(aggregateLabel - worker.finished_task_and_answer.get(taskId)==0?0:1, 2);
                    worker_finished_task_batch++;
                }
            }
            worker.setIndicator(Math.sqrt(indicator));

            double upper_bound = new Chi().get_chi_square_upper_bound(0.85, worker_finished_task_batch);

            worker.setWeight(upper_bound/worker.getIndicator());


        }
    }

    /**
     * update tasks' labels by the CRH algorithm
     *
     * @param batchWorker all the workers associated with the current batch
     * @param batchTask   N tasks associated with the current batch
     */
    public boolean update_label(List<Integer> batchWorker, List<Integer> batchTask) {

        int difference  = 0;
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
            if (aggregatedLabel != task.getAggregateLabel()) {
                difference++;
            }
            task.setAggregateLabel((int) (aggregatedLabel));


        }

        // In CATD, we let the algorithm iter for more times
        return false;

    }


    public static void start(double mu, double epsilon, int lambda, double sybil_accuracy, int iteration, int batch, String dataset, boolean add_approval){

        DataParameter.datasetName = dataset;
        double total_acc = 0;
        double total_time = 0;
        double[] std_arr = new double[iteration];
        double max_acc = 0;
        double min_acc = 1;

        for (int iter = 0; iter < iteration; iter++) {

            Initialize initialize = new Initialize(mu, epsilon, lambda, sybil_accuracy);
            Output output = initialize.init();


            CATD crh = new CATD(output.taskMap, output.workerMap, add_approval);
            long startTime = System.nanoTime();
            double accuracy = crh.calculate_accuracy(batch);
            long endTime = System.nanoTime();
            if (accuracy > max_acc){
                max_acc = accuracy;
            }
            if (accuracy < min_acc){
                min_acc = accuracy;
            }
            std_arr[iter] = accuracy;

            double time = (endTime - startTime) * 1e-6;

            String acc = String.format("%.3f", accuracy * 100); // convert to percentage
            System.out.println("RUN" + (iter + 1) + ":\t" + acc + "%" + "\t\tTime:" + String.format("%.2f", time) + "ms");
            total_acc += accuracy;
            total_time+= time;
        }


        System.out.println();
        System.out.println("Sybil-Parameter: " + "mu=" + mu + ", epsilon=" + epsilon + ", lambda=" + lambda + ", sybil_accuracy=" +sybil_accuracy);
        System.out.println("Average Accuracy: " + String.format("%.3f", total_acc / iteration * 100) + "%");
        System.out.println("Max Accuracy: " + String.format("%.3f",max_acc * 100) + "%");
        System.out.println("Min Accuracy: " + String.format("%.3f",min_acc * 100) + "%");
        System.out.println("Average Time: " + String.format("%.2f", total_time / iteration ) + "ms");

        double std = Variance.get_std_error(std_arr);
        System.out.println("Standard Error: " + String.format("%.4f",std));

    }
}
