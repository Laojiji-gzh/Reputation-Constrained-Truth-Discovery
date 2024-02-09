public class Run {

    public static void main(String[] args) {


        /**
         * Our Algorithm RDTD, "Reputation-Driven Truth  Discovery"
         *
         * mu:0-0.6
         * epsilon:0-0.5
         * lambda:1,2,3,4,5
         * sybil_accuracy:0.15-0.45
         * p:0.8-1
         * step:1,2,3,4,5
         *
         * dataset(4): DOG, NLP, Weather Sentiment, Sentiment Popularity
         */



        RCTD.start(0,0.1,1,0.25,0.9,2,10,10,"DOG", 1.5, 0.15, true);




//        for (String data : dataset) {
//        int[] step = new int[]{1,2,3,4};
//        double[] p = new double[]{0.7,0.75,0.8,0.85};
//        String[] dataset = new String[]{"DOG","NLP","Sentiment Popularity","Weather Sentiment"};
//            for (int sp : step) {
//                for (double pp : p) {
//                    RDTD.start(0.4,0.1,1,0.25,pp,sp,10,data);
//                }
//            }
//        }


        /**
         * The CRH Algorithm, call by the following statement
         * Reference: "Resolving Conflicts in Heterogeneous Data by Truth Discovery and Source Reliability Estimation"
         */



//        CRH.start(0,0.1,1,0.2,100,30000,"DOG", true);


//        System.out.println("CRH just run one iteration!!!");
//        String name = "Weather Sentiment";
//        double acc = 0.25;
//
//        System.out.println("-----------------------" + name + "! mu:0-0.6--------------------------------");
//        for (int i = 0; i <= 6; i++) {
////            MV.start(i * 0.1, 0.1, 1, acc,  name);
////            CRH.start(i * 0.1, 0.1, 1, acc,50,  name);
//            CATD.start(i * 0.1,0.1,1,acc,20,name);
//
//        }
//
//        System.out.println("-----------------------" + name + "! epsilon 0-0.5 --------------------------------");
//
//        for (int i = 0; i <= 5; i++) {
////            MV.start(0.4, 0.1 * i, 1, acc,  name);
////            CRH.start(0.4, 0.1 * i, 1, acc, 50, name);
//            CATD.start(0.4,0.1 * i,1,acc,20,name);
//
//        }
//
//        System.out.println("-----------------------" + name + "! lambda 1-4 --------------------------------");
//
//        for (int i = 1; i <= 4; i++) {
////            MV.start(0.4, 0.1, i, acc,  name);
////            CRH.start(0.4, 0.1, i, acc, 50, name);
//            CATD.start(0.4,0.1,i,acc,20,name);
//
//        }
//
//        System.out.println("-----------------------" + name + "! theta 0.15-0.45 ------------------------");
//
//        double[] doubles = {0.15, 0.25, 0.35, 0.45};
//        for (int i = 0; i < 4; i++) {
////            MV.start(0.4, 0.1, 1, doubles[i],  name);
////            CRH.start(0.4, 0.1, 1, doubles[i], 50, name);
//            CATD.start(0.4,0.1,1,doubles[i],20,name);
//
//        }


        /**
         * MV, Majority Voting, call by the following statement
         */
//        MV.start(0.4, 0.1, 1, 0.2, "Toloka5");

        /**
         * CATD algorithm, resolve the long tail phenomenon of crowdsourcing
         * Reference: "A Confidence-Aware Approach for Truth Discovery on Long-Tail Data", Sentiment Popularity
         */
//        CATD.start(0,0.1,1,0.2,50,50,"Toloka5",false);


    }
}
