package Entity;

import java.util.HashMap;
import java.util.Map;

public class Task {

    private int taskId = -1;
//    private int trueLabel = -1;
    public int[] trueLabel = {-1, -1};
    private int aggregateLabel = -1;
    private double info_entropy = -1;
    public Map<Integer, Integer> assigned_worker_and_answer = new HashMap<>();


    public double getInfo_entropy() {
        return info_entropy;
    }

    public void setInfo_entropy(double info_entropy) {
        this.info_entropy = info_entropy;
    }

    public Task(int taskId, int[] trueLabel) {
        this.taskId = taskId;
        this.trueLabel = trueLabel;
    }

    public int getAggregateLabel() {
        return aggregateLabel;
    }

    public void setAggregateLabel(int aggregateLabel) {
        this.aggregateLabel = aggregateLabel;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

//    public int getTrueLabel() {
//        return trueLabel;
//    }

//    public void setTrueLabel(int trueLabel) {
//        this.trueLabel = trueLabel;
//    }
}
