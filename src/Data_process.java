import Entity.Worker;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Data_process {

    Map<Integer, Worker> workerMap = null;

    public Data_process(Map<Integer, Worker> workerMap) {
        this.workerMap = workerMap;
    }

    public Map<Integer, double[]> process(String data_name){

        Map<Integer, Integer> task_id_and_label = new HashMap<>();

        Map<Integer, double[]> worker_id_acc_total = new HashMap<>();

        try {
            BufferedReader r1 = new BufferedReader(new FileReader("data/" + data_name + "/truth.csv"));
            String line;
            r1.readLine();
            r1.readLine();
            while ((line= r1.readLine())!=null){
                String[] s = line.split(",");
                task_id_and_label.put(Integer.parseInt(s[0]),Integer.parseInt(s[1]));
            }
            r1.close();

            BufferedReader r2 = new BufferedReader(new FileReader("data/" + data_name + "/sybil.csv"));
            r2.readLine();
            while ((line = r2.readLine())!=null){

                String[] s = line.split(",");
                int task_id = Integer.parseInt(s[0]);
                int worker_id = Integer.parseInt(s[1]);
                int label = Integer.parseInt(s[2]);

                if(!worker_id_acc_total.containsKey(worker_id)){
                    double[] temp  = {0,1};
                    if (task_id_and_label.containsKey(task_id) && task_id_and_label.get(task_id)==label){
                        temp[0]++;
                    }
                    worker_id_acc_total.put(worker_id,temp);
                }else{
                    double[] temp = worker_id_acc_total.get(worker_id);
                    temp[1]++;
                    if (task_id_and_label.containsKey(task_id) && task_id_and_label.get(task_id)==label){
                        temp[0]++;
                    }
                }
            }
            r2.close();


        } catch (IOException e) {
            e.printStackTrace();
        }



        double ave_acc = 0;
        for (Integer worker_id : worker_id_acc_total.keySet()) {
            double acc = worker_id_acc_total.get(worker_id)[0] / worker_id_acc_total.get(worker_id)[1];
            worker_id_acc_total.get(worker_id)[0] = acc;
            ave_acc += acc;
        }
        ave_acc /= worker_id_acc_total.size();

        // Sybil worker have an average approval rate
        for (Integer worker_id : worker_id_acc_total.keySet()) {
            if (workerMap.get(worker_id).isSybil()==1){
                worker_id_acc_total.get(worker_id)[0] = ave_acc;
            }
        }



        return worker_id_acc_total;

    }

}
