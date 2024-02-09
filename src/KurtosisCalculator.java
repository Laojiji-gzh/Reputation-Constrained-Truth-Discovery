public class KurtosisCalculator {

    public double calculateExcessKurtosis(double[] data) {
        int n = data.length;


        double sum = 0.0;
        for (double value : data) {
            sum += value;
        }
        double mean = sum / n;


        double sumSquared = 0.0;
        double sumFourth = 0.0;
        for (double value : data) {
            double diff = value - mean;
            sumSquared += diff * diff;
            sumFourth += Math.pow(diff, 4);
        }


        double variance = sumSquared / n;
        double fourthMoment = sumFourth / n;


        double secondCentralMoment = variance;


        double excessKurtosis = fourthMoment / (secondCentralMoment * secondCentralMoment) - 3;

        return excessKurtosis;
    }

}