import Entity.Task;
import Entity.Worker;
import Utils.Variance;

import java.util.Map;

public class MV {


    public Map<Integer, Task> taskMap;
    public Map<Integer, Worker> workerMap;

    public MV(Map<Integer, Task> taskMap, Map<Integer, Worker> workerMap) {
        this.taskMap = taskMap;
        this.workerMap = workerMap;
    }

    public double calculate_accuracy(){
        double total_true = 0;
        for (Task task : taskMap.values()) {
            int[] labels = {0,0,0,0,0,0};
            Map<Integer, Integer> worker_and_answer = task.assigned_worker_and_answer;
            for (Integer workerId : worker_and_answer.keySet()) {
                labels[worker_and_answer.get(workerId)]++;
            }

            int max_cnt = 0;
            int aggregate_label = 0;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] > max_cnt){
                    max_cnt = labels[i];
                    aggregate_label = i;
                }
            }

            task.setAggregateLabel(aggregate_label);


            // for accuracy
            if (task.trueLabel[0] == task.getAggregateLabel() || task.trueLabel[1] == task.getAggregateLabel()){
                total_true++;
            }

        }

        System.out.print("True Answer:" + "\t" + total_true + "\t");

        return total_true / taskMap.size();
    }


    public static void start(double mu, double epsilon, int lambda, double sybil_accuracy, String dataset){

        int iteration = 1;

        DataParameter.datasetName = dataset;
        double total_acc = 0;
        double total_time = 0;
        double[] std_arr = new double[iteration];
        double max_acc = 0;
        double min_acc = 1;

        for (int iter = 0; iter < iteration; iter++) {

            Initialize initialize = new Initialize(mu, epsilon, lambda, sybil_accuracy);
            Output output = initialize.init();

            MV mv = new MV(output.taskMap, output.workerMap);
            long startTime = System.nanoTime();
            double accuracy = mv.calculate_accuracy();
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
//            System.out.println("RUN" + (iter + 1) + ":\t" + acc + "%" + "\t\tTime:" + String.format("%.2f", time) + "ms");
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
