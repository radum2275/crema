package ch.idsia.crema.inference.ve;

import ch.idsia.crema.factor.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.symbolic.PriorFactor;
import ch.idsia.crema.factor.symbolic.SymbolicFactor;
import ch.idsia.crema.factor.symbolic.serialize.NLSerializer;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.order.OrderingStrategy;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.math.Operation;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class VariableElimination<F extends GenericFactor> implements JoinInference<F, F> {
	private int[] sequence;

	private List<F> factors;

	private TIntIntMap evidence;

	private Operation<F> operator;
	
	private boolean normalize = true;
	
	/**
	 * Constructs a variable elimination specifying the algebra.
	 * Factors, evidence and elimnation sequence must be specified with setters.
	 * 
	 * @param ops 
	 */
	public VariableElimination(Operation<F> ops) {
		this.operator = ops;
	}

	/**
	 * Constructs a variable elimination specifying the algebra to be used for the
	 * factors and the elimination order
	 * 
	 * @param ops
	 * @param sequence
	 */
	public VariableElimination(Operation<F> ops, int[] sequence) {
		setSequence(sequence);
		this.operator = ops;
	}

	/**
	 * Set the elimination sequence to be used. Variables will be eliminated in this order. 
	 * The sequence may include the query! 
	 * <p>Elimination sequencies can be generated with an {@link OrderingStrategy}.
	 * </p>
	 * @param sequence
	 */
	public void setSequence(int[] sequence) {
		this.sequence = sequence;
	}
	
	/**
	 * Populate the problem with the factors to be considered.
	 * Collection version.
	 * 
	 * @param factors
	 */
	public void setFactors(Collection<? extends F> factors) {
		this.factors = new ArrayList<>(factors);
	}

	/**
	 * Populate the problem with the factors to be considered.
	 * Array version.
	 * 
	 * @param factors
	 */
	public void setFactors(F[] factors) {
		this.factors = Arrays.asList(factors);
	}

	/**
	 * Fix some evidence. The provided argument is a map of variable - state 
	 * associations.
	 * 
	 * @param evidence
	 */
	public void setEvidence(TIntIntMap evidence) {
		this.evidence = evidence;
	}

	
	/**
	 * Specify if the resulting value should be normalized. 
	 * Will result in asking K(Q|e) vs K(Qe)
	 * @param norm
	 */
	public void setNormalize(boolean norm) {
		normalize = norm;
	}

	/**
	 * Execute the variable elimination asking for the marginal or posterior of the specified
	 * variables. If multiple variables are specified the joint over the query is computed.
	 * 
	 * 
	 * The elimination sequence is to be specified via {@link VariableElimination#setSequence(int[])}.
	 * 
	 * @param query
	 * @return
	 */
	public F run(int... query) {
		FactorQueue<F> queue = new FactorQueue<>(sequence);
		queue.addAll(factors);
		boolean normalize = false;
		F last = null;
		while (queue.hasNext()) {
			int variable = queue.getVariable();
			Collection<F> var_factors = queue.next();

			if (var_factors.size() > 0) {
				last = FactorUtil.combine(operator, var_factors);

				if (Arrays.binarySearch(query, variable) >= 0) {
					// query var // nothing to do
				} else if (evidence != null && evidence.containsKey(variable)) {
					int state = evidence.get(variable);
					last = operator.filter(last, variable, state);
					normalize = true;
				} else {
					last = operator.marginalize(last, variable);
				}
				queue.add(last);
			}
		}

		if (normalize && this.normalize) {
			last = FactorUtil.normalize(operator, last);
		}

		return last;

	}

	@Override
	public F apply(GraphicalModel<F> model, int[] query, TIntIntMap observations) throws InterruptedException {
		setEvidence(observations);
		setFactors(model.getFactors());
		return run(query);
	}

	public static void main(String[] a) {

		IntervalFactor pa = new IntervalFactor(new Strides(new int[] { 2 }, new int[] { 4 }), new Strides(new int[] {}, new int[] {}));
		IntervalFactor pba = new IntervalFactor(new Strides(new int[] { 1 }, new int[] { 3 }), new Strides(new int[] { 2 }, new int[] { 4 }));
		IntervalFactor pcba = new IntervalFactor(new Strides(new int[] { 3 }, new int[] { 3 }), new Strides(new int[] { 1, 2 }, new int[] { 3, 4 }));
		
		SymbolicFactor fa = new PriorFactor(pa);
		SymbolicFactor fb = new PriorFactor(pba);
		SymbolicFactor fc = new PriorFactor(pcba);

		FactorVariableElimination<SymbolicFactor> ve = new FactorVariableElimination<>(new int[] { 1, 3, 2 });
		ve.setFactors(Arrays.asList(fa, fb, fc));
		ve.setEvidence(new TIntIntHashMap(new int[] { 1 }, new int[] { 1 }));

		SymbolicFactor f = ve.run(1);
		NLSerializer serial = new NLSerializer();
		System.out.println(serial.serialize(f));
	}
}
