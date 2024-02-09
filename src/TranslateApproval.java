import Entity.Worker;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.util.*;

public class TranslateApproval {

    int random_seed = -1;

    public TranslateApproval(int random_seed) {
        this.random_seed = random_seed;
    }

    private Result translate(Map<Integer, double[]> worker_id_Acc_total, double t, double C) {



        Map<Integer, double[]> worker_id_App_total = new HashMap<>();


        double logC_mu = 0;

        for (double[] value : worker_id_Acc_total.values()) {
            logC_mu += Math.log(value[0] + 1);
        }
        logC_mu /= worker_id_Acc_total.size();

        Random random = new Random(this.random_seed);
        for (Integer worker_id : worker_id_Acc_total.keySet()) {
            double noise =  random.nextGaussian() * 0.02 + (t + (1- C) * logC_mu);
            double approval =  Math.log(worker_id_Acc_total.get(worker_id)[0] + 1) * C  + noise;
            worker_id_App_total.put(worker_id, new double[]{approval, worker_id_Acc_total.get(worker_id)[1]});
        }


        double[] a = new double[worker_id_Acc_total.size()];
        double[] b = new double[worker_id_App_total.size()];

        int i =0;
        for (double[] value : worker_id_Acc_total.values()) {
            double acc = value[0];
            a[i] = acc;
            i++;
        }

        i=0;
        for (double[] value : worker_id_App_total.values()) {
            double app = value[0];
            b[i] = app;
            i++;
        }

        double kurt1 = new KurtosisCalculator().calculateExcessKurtosis(a);
        double m1 = new Mean().evaluate(a);

        double kurt2 = new KurtosisCalculator().calculateExcessKurtosis(b);
        double m2 = new Mean().evaluate(b);


        double delta_k = kurt2 - kurt1;
        double delta_m = m2 - m1;


//        System.out.println("k=" + delta_k);
//        System.out.println("m=" + delta_m);
//        // truncation
        for (double[] app_total : worker_id_App_total.values()) {
            if (app_total[0] > 1) app_total[0] = 1;
            if (app_total[0] < 0) app_total[0] = 0;
        }

        return new Result(delta_k, delta_m, worker_id_App_total);


    }

    public Result getApproval(double k, double m, String dataset,Map<Integer, Worker> workerMap) {


        Map<Integer, double[]> worker_id_acc = new Data_process(workerMap).process(dataset);




        double C_true = -1;
        double t_true = -1;
        double k_true = -1;
        double m_true = -1;

        boolean flag_k = false;
        boolean flag_m = false;


        Result result = null;

        double C = 0.01;
        for (double t = 0; t < 30; t += 0.001) {
            result = translate(worker_id_acc, t, C);
            double delta_k = result.delta_k;
            double delta_m = result.delta_m;

            if (Math.abs(delta_m - m) < 0.01) {
                m_true = delta_m;
                t_true = t;
                flag_m=true;
                break;
            }
        }

        for (double C_temp = 0; C_temp < 200; C_temp += 0.1) {
            result = translate(worker_id_acc, t_true, C_temp);
            double delta_k = result.delta_k;
            double delta_m = result.delta_m;

            if (Math.abs(delta_k - k) < 0.03) {
                C_true = C_temp;
                k_true = delta_k;
                flag_k=true;
                break;
            }
        }



        if (flag_k && flag_m) {
            return result;
        }else{
            return null;
        }
    }



}
class Result{

    public Result(double delta_k, double delta_m, Map<Integer, double[]> worker_id_App_total) {
        this.delta_k = delta_k;
        this.delta_m = delta_m;
        this.worker_id_App_total = worker_id_App_total;
    }


    public double delta_k;
    public double delta_m;
    public Map<Integer, double[]> worker_id_App_total;


}
