package ch.idsia.crema.model.io.uai;

import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import org.apache.commons.math3.optim.linear.Relationship;

public class HCredalUAIParser extends NetUAIParser<SparseModel>{


    private double[][] aCoeff = new double[numberOfVariables][];
    private double[][] bCoeff = new double[numberOfVariables][];

    public HCredalUAIParser(String file){
        TYPE = "H-CREDAL";
        this.fileName = file;
    }

    @Override
    protected void processFile() {
        parseType();
        parseVariablesInfo();
        parseDomains();
        parseCoefficients();
    }

    @Override
    protected SparseModel build() {
        SparseModel model = new SparseModel();

        // Add the variables
        for (int i = 0; i < numberOfVariables; i++) {
            model.addVariable(cardinalities[i]);
        }

        // Adding the parents to each variable
        for (int k = 0; k < numberOfVariables; k++) {
            model.addParents(k, parents[k]);
        }

        // Specifying the linear constraints for each variable
        SeparateHalfspaceFactor[] cpt = new SeparateHalfspaceFactor[numberOfVariables];
        for (int i = 0; i < numberOfVariables; i++) {
            int varsize = model.getDomain(i).getSizes()[0];

            if (parents[i].length == 0) {
                // reshaped coeff A
                double[][] A2d = ArraysUtil.reshape2d(aCoeff[i], aCoeff[i].length / varsize);
                // build the factor
                cpt[i] = new SeparateHalfspaceFactor(model.getDomain(i), A2d, bCoeff[i], Relationship.LEQ);
            } else {

                int par_comb = model.getDomain(model.getParents(i)).getCombinations();
                double[][] A2d_full = ArraysUtil.reshape2d(aCoeff[i], aCoeff[i].length / (varsize * par_comb));

                // Get the coefficients matrices for each combination of the parents
                double[][][] A = new double[par_comb][][];
                double[][] b = new double[par_comb][];

                for (int j = 0; j < par_comb; j++) {
                    double[][] Aj = ArraysUtil.copyOfRange(A2d_full, j * varsize, (j + 1) * varsize, 1);
                    int[] to_drop = ArraysUtil.rowsWhereAllZeros(Aj);
                    Aj = ArraysUtil.dropRows(Aj, to_drop);
                    double[] bj = ArraysUtil.dropColumns(new double[][]{bCoeff[i]}, to_drop)[0];
                    A[j] = Aj;
                    b[j] = bj;
                }

                // build the factor
                cpt[i] = new SeparateHalfspaceFactor(model.getDomain(i),
                        model.getDomain(model.getParents(i)),
                        A, b, Relationship.LEQ);

            }
        }

        model.setFactors(cpt);

        return model;
    }

    @Override
    protected void sanityChecks() {
        super.sanityChecks();
    }

    private void parseCoefficients(){

        // Parsing the a and b coefficient for each variable
        aCoeff = new double[numberOfVariables][];
        bCoeff = new double[numberOfVariables][];
        int n_a, n_b;
        for (int i = 0; i < numberOfVariables; i++) {
            n_a = Integer.parseInt(popElement());
            aCoeff[i] = new double[n_a];
            for (int k = 0; k < n_a; k++) {
                aCoeff[i][k] = Double.parseDouble(popElement());
            }

            n_b = Integer.parseInt(popElement());
            bCoeff[i] = new double[n_b];
            for (int k = 0; k < n_b; k++) {
                bCoeff[i][k] = Double.parseDouble(popElement());
            }
        }
    }

    public static void main(String[] args) {
        String fileName = "./examples/simple-hcredal.uai"; // .cn File to open
        HCredalUAIParser parser  = new HCredalUAIParser(fileName);
        SparseModel model = parser.parse();

        for (int i = 0; i < model.getVariables().length; i++) {
            System.out.println("Variable " + i);
            ((SeparateHalfspaceFactor) model.getFactor(i)).printLinearProblem();
        }
    }

}
