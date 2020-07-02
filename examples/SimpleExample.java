

import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;

public class SimpleExample {
    public static void main(final String[] args) {
        double p = 0.2;
        double eps = 0.0001;
        
        /*  CN defined with vertex Factor  */
        
        // Define the model (with vertex factors)
        SparseModel model = new SparseModel();
        int u = model.addVariable(3);
        int x = model.addVariable(2);
        model.addParent(x,u);
        
        // Define a credal set of the partent node
        VertexFactor fu = new VertexFactor(model.getDomain(u), Strides.empty());
        fu.addVertex(new double[]{0., 1-p, p});
        fu.addVertex(new double[]{1-p, 0., p});
        model.setFactor(u,fu);
        
        
        System.out.println(p+" "+(1-p));
        
        // Define the credal set of the child
        VertexFactor fx = new VertexFactor(model.getDomain(x), model.getDomain(u));
        
        fx.addVertex(new double[]{1., 0.,}, 0);
        fx.addVertex(new double[]{1., 0.,}, 1);
        fx.addVertex(new double[]{0., 1.,}, 2);
        
        model.setFactor(x,fx);
        
        // Run exact inference inference
        VariableElimination ve = new FactorVariableElimination(model.getVariables());
        ve.setFactors(model.getFactors());
        System.out.println(ve.run(x));
    }
}
