import Entity.Task;
import Entity.Worker;
import Utils.Willonson;

import javax.swing.plaf.synth.SynthButtonUI;
import java.io.*;
import java.nio.Buffer;
import java.util.*;

public class Initialize {
    public Map<Integer, Task> taskMap = new HashMap<>();
    public Map<Integer, Worker> workerMap = new HashMap<>();

    private double p;
    private double step;
    private double mu;
    private double epsilon;
    private int lambda;
    private double sybil_accuracy;
    private String name;
    private double k;
    private double m;
    private boolean is_wilson = true;


    public Initialize(double mu, double epsilon, int lambda, double sybil_accuracy) {
        this.mu = mu;
        this.epsilon = epsilon;
        this.lambda = lambda;
        this.sybil_accuracy = sybil_accuracy;
    }

    public Initialize(double mu, double epsilon, int lambda, double sybil_accuracy, double p, int step, String name, double k, double m, boolean is_wilson) {
        this.p = p;
        this.step = step;
        this.mu = mu;
        this.epsilon = epsilon;
        this.lambda = lambda;
        this.sybil_accuracy = sybil_accuracy;
        this.name = name;
        this.k = k;
        this.m = m;
        this.is_wilson = is_wilson;
    }

    public Output init() {

        // readIn the answer.csv and truth.csv
        String rawFileName = "answer.csv";
        readFile(rawFileName);
        // we read the file twice to insert the Sybil workers
        String sybilFileName = sybil_insert(mu, epsilon, lambda, sybil_accuracy);
        readFile(sybilFileName);


        set_worker_accuracy();
        double[] k_and_m = set_worker_approval(workerMap);
        return new Output(taskMap, workerMap,k_and_m[0],k_and_m[1]);

    }

    /**
     * init all the workers' accuracy
     */
    public void set_worker_accuracy() {
        for (Worker worker : workerMap.values()) {
            worker.setTaskCount(worker.finished_task_and_answer.size());
            double accuracy = 0;
            for (int taskId : worker.finished_task_and_answer.keySet()) {
                boolean temp = (taskMap.get(taskId).trueLabel[0] == worker.finished_task_and_answer.get(taskId)) || (taskMap.get(taskId).trueLabel[1] == worker.finished_task_and_answer.get(taskId));
                accuracy += (temp ? 1 : 0);
            }
            worker.setAccuracy(accuracy / worker.getTaskCount());
        }
    }

    /**
     * init all the workers' approval-rate
     */
    public double[] set_worker_approval(Map<Integer, Worker> workerMap) {

        // random seed used to avoid simulation fail
        int random_seed_id = 2;
        Result result = null;

        while ((result = new TranslateApproval(random_seed_id).getApproval(k, m, name, workerMap))==null){
            random_seed_id++;
        }
        Map<Integer, double[]> worker_id_app_total = result.worker_id_App_total;
        for (Integer worker_id : worker_id_app_total.keySet()) {

            double app = worker_id_app_total.get(worker_id)[0];
            double total = worker_id_app_total.get(worker_id)[1];


            double approvalrate = 0;
            if (is_wilson){
                Willonson willonson = new Willonson((int)(app * total), (int)(total), 0.5);
                approvalrate = willonson.calculateLowerBound();
            }else{
                approvalrate = app;
            }

            workerMap.get(worker_id).setApproval_rate(approvalrate);
        }

        return  new double[]{result.delta_k, result.delta_m};

    }

    /**
     * @param mu sybil proportion
     * @param epsilon Sybil worker concluding with a probability of 1-epsilon
     * @param lambda the number of attackers, each attack control several Sybil worker
     * @param sybil_accuracy Sybil workers' accuracy
     * @return workers' answer FILE
     */
    public String sybil_insert(double mu, double epsilon, int lambda, double sybil_accuracy) {

        // set Parameter : mu
        List<Integer> sybil_arr = new ArrayList<>();
        // some dataset number the worker and task from zero >>>> 0,1,2..., and other from one >>>> 1,2,3....
        for (int i = 1; i < DataParameter.workerNum-1; i++) {
            sybil_arr.add(i + 1);
        }
        Collections.shuffle(sybil_arr);
        sybil_arr = sybil_arr.subList(0, (int) (mu * (DataParameter.workerNum - 1)));

        for (Integer workerId : sybil_arr) {
            Worker worker = workerMap.get(workerId);

            // very few workers fail to complete tasks
            if (worker != null){
                worker.setSybil(1);
                worker.setAttack(new Random().nextInt(lambda));     // each worker controlled by one attacker
            }

        }


        // replacing independent workers with Sybil workers
        List<String> read_in_buffer = new ArrayList<>();
        try {
            BufferedReader r1 = new BufferedReader(new FileReader("data/" + DataParameter.datasetName +"/answer.csv"));
            String line = r1.readLine();
            read_in_buffer.add(line);
            for (int i = 0; i < DataParameter.taskNum * DataParameter.taskForWorkerNum; i += DataParameter.taskForWorkerNum) {


                List<Integer> attacker_ans = null;
                for (int j = i; j < DataParameter.taskForWorkerNum + i && j < DataParameter.taskNum * DataParameter.taskForWorkerNum; j++) {

                    // These 10 task are the same
                    line = r1.readLine();
                    String[] s = line.split(",");
                    int workerId = Integer.parseInt(s[1]);
                    Worker worker = workerMap.get(workerId);
                    int[] trueLabel = taskMap.get(Integer.parseInt(s[0])).trueLabel;

                    // attacker_ans is an Array, stored each attackers' shared answer
                    if ( i == j ){
                        attacker_ans = set_lambda_and_sybilAcc(trueLabel);      // set two Parameter: lambda and sybil_accuracy
                    }

                    // An independent worker
                    if (worker.getAttack() == -1){
                        read_in_buffer.add(line);       // write into file directly
                    }

                    // A Sybil worker
                    else{
                        int attackId = worker.getAttack();
                        int shared_label = attacker_ans.get(attackId);      // one attacker's shared label
                        int sybil_label = set_epsilon(shared_label);        // set Parameter: epsilon

                        String t = s[0] + "," + s[1] + "," + String.valueOf(sybil_label);   // write into file
                        read_in_buffer.add(t);
                    }

                }
            }

            r1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            BufferedWriter w1 = new BufferedWriter(new FileWriter("data/" + DataParameter.datasetName + "/sybil.csv"));
            for (int i = 0; i < read_in_buffer.size(); i++) {
                w1.append(read_in_buffer.get(i));
                if (i != read_in_buffer.size() - 1) {
                    w1.append("\n");
                }
            }
            w1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "sybil.csv";
    }

    public int set_epsilon(int shared_label){
        return (new Random().nextDouble() < 1 - epsilon) ? shared_label : new Random().nextInt(DataParameter.labelSize);
    }

    public List<Integer> set_lambda_and_sybilAcc(int[] true_label){
        // The length of error_ans is
        List<Integer> error_ans = new ArrayList<>();
        for (int i = 0; i < DataParameter.labelSize; i++) {
            if (i!=true_label[0] && i!=true_label[1]){
                error_ans.add(i);
            }
        }

        // store each attacker's answer, this answer share by sybil controlled by the same attacker
        List<Integer> attacker_ans = new ArrayList<>();
        for (int l = 0; l < lambda; l++) {
            attacker_ans.add( new Random().nextDouble()<sybil_accuracy ? true_label[0]:(error_ans.get(new Random().nextInt(error_ans.size()))) );
        }

        return attacker_ans;
    }

    public void readFile(String fileName) {
        // construct taskMap
        try {
            BufferedReader w1 = new BufferedReader(new FileReader("data/" +DataParameter.datasetName + "/truth.csv"));
            String line = w1.readLine();
            String[] data_parameter = line.split(",");

            DataParameter.labelSize = Integer.parseInt(data_parameter[0]);
            DataParameter.taskForWorkerNum = Integer.parseInt(data_parameter[1]);
            DataParameter.taskNum = Integer.parseInt(data_parameter[2]);
            DataParameter.workerNum = Integer.parseInt(data_parameter[3]);

            line = w1.readLine();
            while ((line = w1.readLine()) != null) {
                String[] s = line.split(",");
                int taskId = Integer.parseInt(s[0]);
                String[] answers = s[1].split(":");
                int[] trueLabel = new int[]{-1,-1};
                if (answers.length == 1){
                    trueLabel[0] = Integer.parseInt(answers[0]);
                }else if (answers.length == 2){
                    trueLabel[0] = Integer.parseInt(answers[0]);
                    trueLabel[1] = Integer.parseInt(answers[1]);
                }
                taskMap.put(taskId, new Task(taskId, trueLabel));
            }
            w1.close();
        } catch (Exception e) {
            System.out.println("read fail...");
            e.printStackTrace();
        }
        // construct workerMap
        try {
            BufferedReader w2 = new BufferedReader(new FileReader("data/"  + DataParameter.datasetName + "/" + fileName));
            String line;
            w2.readLine();
            while ((line = w2.readLine()) != null) {
                String[] s = line.split(",");
                int taskId = Integer.parseInt(s[0]);
                int workerId = Integer.parseInt(s[1]);
                int workerAns = Integer.parseInt(s[2]);

                taskMap.get(taskId).assigned_worker_and_answer.put(workerId, workerAns);

                if (workerMap.get(workerId) != null) {
                    Worker worker = workerMap.get(workerId);
                    worker.finished_task_and_answer.put(taskId, workerAns);
                } else {
                    Worker worker = new Worker(workerId);
                    worker.finished_task_and_answer.put(taskId, workerAns);
                    workerMap.put(workerId, worker);
                }
            }
            w2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}

class Output{
    public Map<Integer, Task> taskMap;
    public Map<Integer, Worker> workerMap;
    public double k;
    public double m;

    public Output(Map<Integer, Task> taskMap, Map<Integer, Worker> workerMap, double k, double m) {
        this.taskMap = taskMap;
        this.workerMap = workerMap;
        this.k = k;
        this.m = m;
    }
}
