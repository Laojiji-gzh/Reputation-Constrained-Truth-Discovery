public class KurtosisCalculator {

    public double calculateExcessKurtosis(double[] data) {
        int n = data.length;

        // 计算均值
        double sum = 0.0;
        for (double value : data) {
            sum += value;
        }
        double mean = sum / n;

        // 计算四阶中心矩和二阶中心矩的分子部分
        double sumSquared = 0.0;
        double sumFourth = 0.0;
        for (double value : data) {
            double diff = value - mean;
            sumSquared += diff * diff;
            sumFourth += Math.pow(diff, 4);
        }

        // 计算二阶中心矩和四阶中心矩
        double variance = sumSquared / n; // 注意：这里计算的是方差，不是二阶中心矩的完整形式
        double fourthMoment = sumFourth / n;

        // 修正方差为二阶中心矩
        double secondCentralMoment = variance; // 对于样本数据，方差已经是二阶中心矩的无偏估计

        // 计算 excess kurtosis
        double excessKurtosis = fourthMoment / (secondCentralMoment * secondCentralMoment) - 3;

        return excessKurtosis;
    }

}