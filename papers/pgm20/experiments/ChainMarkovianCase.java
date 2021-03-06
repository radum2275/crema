package pgm20.experiments;

import ch.idsia.crema.models.causal.RandomChainMarkovian;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.causality.CausalInference;
import ch.idsia.crema.inference.causality.CausalVE;
import ch.idsia.crema.inference.causality.CredalCausalAproxLP;
import ch.idsia.crema.inference.causality.CredalCausalVE;
import ch.idsia.crema.model.graphical.specialized.StructuralCausalModel;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.hash.TIntIntHashMap;

public class ChainMarkovianCase {
    public static void main(String[] args) throws InterruptedException {

        /** Number of endogenous variables in the chain (should be 3 or greater)*/
        int N = 4;

        /** Number of states in endogenous variables */
        int endoVarSize = 3;

        /** Number of states in the exogenous variables */
        int exoVarSize = 9;

        /** epsilon value for ApproxLP  */
        double eps = 0.00001;


        long seed = 1234;




        /////////////////////////////////





        RandomUtil.getRandom().setSeed(seed);
        // Load the chain model
        StructuralCausalModel model = RandomChainMarkovian.buildModel(N, endoVarSize, exoVarSize);


        // Query: P(X[N/2] | X[N-1]=0, do(X[0])=0)

        int[] X = model.getEndogenousVars();

        TIntIntHashMap evidence = new TIntIntHashMap();

        int obsvar = X[0];
        //obsvar = -1;
        int dovar = X[N-1];

       if(obsvar!=-1) evidence.put(obsvar, 1);

        TIntIntHashMap intervention = new TIntIntHashMap();
        if(dovar!=-1) intervention.put(X[0], 0);


        int target = X[N/2];
        target = X[1];



        System.out.println("\nChainMarkovian\n   N=" + N + " endovarsize=" + endoVarSize + " exovarsize=" + exoVarSize + " target=" + target + " obsvar=" + obsvar + " dovar=" + dovar + " seed=" + seed);
        System.out.println("=================================================================");

        model.printSummary();

        // Run inference

        CausalInference inf1 = new CausalVE(model);
        BayesianFactor result1 = (BayesianFactor) inf1.query(target, evidence, intervention);
        System.out.println(result1);

        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);


        CausalInference inf3 = new CredalCausalAproxLP(model).setEpsilon(eps);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);


    }
}
