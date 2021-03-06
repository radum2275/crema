package ch.idsia.crema.factor.bayesian;

import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.model.Domain;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.vertex.*;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Conversion of values to Log is hardcoded.
 * In the {@link ExplicitBayesianFactor} class
 * the conversion can be specified with an instance of {@link VertexOperation}.
 *
 * @author david
 */
public class BayesianFactor implements Factor<BayesianFactor> {

	private Strides domain;
	private double[] data;
	private final boolean log;

	public BayesianFactor(Domain domain, boolean log) {
		this.domain = Strides.fromDomain(domain);
		this.log = log;
		data = new double[this.domain.getCombinations()];
	}

	public BayesianFactor(int[] domain, int[] sizes, boolean log) {
		this.domain = new Strides(domain, sizes);
		this.log = log;
		data = new double[this.domain.getCombinations()];
	}

	public BayesianFactor(Strides stride, double[] data, boolean log) {
		this.domain = stride;
		this.data = data;
		this.log = log;
	}

	public BayesianFactor(Domain domain) {
		this(domain, false);
	}

	public BayesianFactor(int[] domain, int[] sizes) {
		this(domain, sizes, false);
	}

	public BayesianFactor(Strides stride, double[] data) {
		this(stride, data, false);
	}

	/**
	 * Factors are only mutable in their data. The domain should not change in
	 * time. We can therefore use the original domain.
	 */
	@Override
	public BayesianFactor copy() {
		return new BayesianFactor(domain, data.clone(), log);
	}

	public boolean isLog() {
		return log;
	}

	/**
	 * Set the CPT's data specifying a custom ordering used in the data
	 *
	 * @see ch.idsia.credo.model.bayesian.CPT#setData(int[], double[])
	 */
	public void setData(final int[] domain, double[] data) {
		int[] sequence = ArraysUtil.order(domain);

		// this are strides for the iterator so we do not need them one item
		// longer
		int[] strides = new int[domain.length];
		int[] sizes = new int[domain.length];

		int[] this_variables = this.domain.getVariables();
		int[] this_sizes = this.domain.getSizes();
		int[] this_strides = this.domain.getStrides();

		// with sequence we can now set sorted_domain[1] = domain[sequence[1]];
		for (int index = 0; index < this_variables.length; ++index) {
			int newindex = sequence[index];
			strides[newindex] = this_strides[index];
			sizes[newindex] = this_sizes[index];
		}

		final int combinations = this.domain.getCombinations();
		IndexIterator iterator = new IndexIterator(strides, sizes, 0, combinations);

		double[] target = data.clone();

		for (int index = 0; index < combinations; ++index) {
			int other_index = iterator.next();
			target[other_index] = data[index];
		}

		setData(target);
	}

	public double[] getInteralData() {
		return data;
	}

	public void setInteralData(double[] data) {
		this.data = data;
	}

	@Override
	public Strides getDomain() {
		return domain;
	}

	public void setData(double[] data) {
		if (log) {
			for (int index = 0; index < data.length; ++index) {
				this.data[index] = FastMath.log(data[index]);
			}
		} else {
			this.data = data;
		}
	}

	public double[] getData() {
		if (log) {
			double[] data = new double[this.data.length];
			for (int index = 0; index < data.length; ++index) {
				data[index] = FastMath.exp(this.data[index]);
			}
			return data;
		} else {
			return data;
		}
	}

	public double getValue(int... states) {
		return getValueAt(domain.getOffset(states));
	}

	public void setValueAt(double d, int index) {
		data[index] = log ? FastMath.log(d) : d;
	}

	public double getValueAt(int index) {
		double value = data[index];
		return log ? FastMath.exp(value) : value;
	}

	public class Helper extends Number {
		private int[] states ;

		public Helper(int left_var, int state) {

			and(left_var, state);
		}

		public Helper given(int var, int state) {
			// store the info
			return and(var, state);
		}
		public Helper and(int var, int state) {
			// store the info
			int index = getDomain().indexOf(var);
			states[index] = state;
			return this;
		}
		@Override
		public int intValue() {
			return (int) doubleValue();
		}
		@Override
		public long longValue() {
			return (long)doubleValue();
		}
		@Override
		public float floatValue() {
			return (float) doubleValue();
		}
		@Override
		public double doubleValue() {
			return BayesianFactor.this.getValue(states);
		}

		public void set(double v) {
			BayesianFactor.this.setValue(v, states);
		}

		public void add(double v) {
			double x = BayesianFactor.this.getValue(states);
			BayesianFactor.this.setValue(x + v, states);
		}
    }

	public Helper p(int var, int state) {
		return new Helper(var, state);
	}

	/**
	 * Get the value of a single parameter in the bayesian factor identified by the instantiation
	 * provided as a string. Ex: p("0=1 | 1=3, 2=4")
	 *
	 * @param def the instantiation string
	 * @return the parameter's values
	 */
	public double p(String def) {
		def = def.replaceAll("\\s", "");
		String[] steps = def.split("[\\|\\,]");
		if (steps.length != getDomain().getSize()) throw new IllegalArgumentException("need spec of a single probability");

		int[] states = new int[getDomain().getSize()];

		for (String step : steps) {
			String[] parts = step.split("=");
			int var = Integer.parseInt(parts[0]);
			int state = Integer.parseInt(parts[1]);
			int index = getDomain().indexOf(var);
			states[index] = state;
		}
		return getValue(states);
	}

	public void setValue(double value, int... states) {
		data[domain.getOffset(states)] = log ? FastMath.log(value) : value;
	}

	/*
	public void setValue(double value, int[] states, int[] variables) {
		int[] orderedStates = new int[states.length];

		//TODO

		setValue(value, orderedStates);
	}
	*/

	/**
	 * Reduce the domain by removing a variable and selecting the specified
	 * state.
	 *
	 * @param variable
	 *            the variable to be filtered out
	 * @param state
	 *            the state to be selected
	 */
	@Override
	public BayesianFactor filter(int variable, int state) {
		int offset = domain.indexOf(variable);
		return collect(offset, new Filter(domain.getStrideAt(offset), state));
	}

	/**
	 * <p>
	 * Marginalize a variable out of the factor. This corresponds to sum all
	 * parameters that differ only in the state of the marginalized variable.
	 * </p>
	 *
	 * <p>
	 * If this factor represent a Conditional Probability Table you should only
	 * marginalize variables on the right side of the conditioning bar. If so,
	 * there is no need for further normalization.
	 * </p>
	 *
	 * @param variable
	 *            the variable to be summed out of the CPT
	 * @return the new CPT with the variable marginalized out.
	 */
	@Override
	public BayesianFactor marginalize(int variable) {
		int offset = domain.indexOf(variable);
		if (offset == -1) return this;

		if (log)
			return collect(offset, new LogMarginal(domain.getSizeAt(offset), domain.getStrideAt(offset)));
		else
			return collect(offset, new Marginal(domain.getSizeAt(offset), domain.getStrideAt(offset)));
	}

	private BayesianFactor collect(final int offset, final Collector collector) {

		final int[] new_variables = new int[domain.getSize() - 1];
		final int[] new_sizes = new int[domain.getSize() - 1];

		System.arraycopy(domain.getVariables(), 0, new_variables, 0, offset);
		System.arraycopy(domain.getVariables(), offset + 1, new_variables, offset, new_variables.length - offset);

		System.arraycopy(domain.getSizes(), 0, new_sizes, 0, offset);
		System.arraycopy(domain.getSizes(), offset + 1, new_sizes, offset, new_variables.length - offset);

		final int stride = domain.getStrideAt(offset);
		final int size = domain.getSizeAt(offset);
		final int reset = size * stride;

		int source = 0;
		int next = stride;
		int jump = stride * (size - 1);

		Strides target_domain = new Strides(new_variables, new_sizes);
		final double[] new_data = new double[target_domain.getCombinations()];

		for (int target = 0; target < target_domain.getCombinations(); ++target, ++source) {
			if (source == next) {
				source += jump;
				next += reset;
			}

			new_data[target] = collector.collect(data, source);
		}

		return new BayesianFactor(target_domain, new_data, log);
	}

	/**
	 * Combine the provided factor with this one.
	 */
	public BayesianFactor combineIterator(BayesianFactor cpt) {

		Strides target = domain.union(cpt.domain);

		IndexIterator i1 = getDomain().getSupersetIndexIterator(target);
		IndexIterator i2 = cpt.getDomain().getSupersetIndexIterator(target);

		double[] result = new double[target.getCombinations()];

		if (log) {
			for (int i = 0; i < result.length; ++i) {
				result[i] = data[i1.next()] + cpt.data[i2.next()];
			}
		} else {
			for (int i = 0; i < result.length; ++i) {
				result[i] = data[i1.next()] * cpt.data[i2.next()];
			}
		}

		return new BayesianFactor(target, result, log);
	}


	/**
	 * The specialized method that avoids the cast of the input variable.
	 *
	 * <p>
	 * This implementation uses long values for strides, sizes and indices,
	 * allowing for combined operations. Increasing the index is, for instance,
	 * one single add instead of two. The values will contain in the Lower 32bit
	 * this factor's values and in the upper 32 the parameter's ones. Given that
	 * most architectures are 64 bit, this should give a tiny performance
	 * improvement.
	 * </p>
	 *
	 * @param factor
	 * @return
	 */
	@Override
	public BayesianFactor combine(BayesianFactor factor) {

		// domains should be sorted
		this.sortDomain();
		factor = factor.copy();
		factor.sortDomain();

		final Strides target = domain.union(factor.domain);
		final int length = target.getSize();

		final int[] limits = new int[length];
		final int[] assign = new int[length];

		final long[] stride = new long[length];
		final long[] reset = new long[length];

		for (int vindex = 0; vindex < domain.getSize(); ++vindex) {
			int offset = Arrays.binarySearch(target.getVariables(), domain.getVariables()[vindex]);
			// if (offset >= 0) {
			stride[offset] = domain.getStrides()[vindex];
			// }
		}

		for (int vindex = 0; vindex < factor.domain.getSize(); ++vindex) {
			int offset = ArraysUtil.indexOf(factor.domain.getVariables()[vindex], target.getVariables());
			// if (offset >= 0) {
			stride[offset] += ((long) factor.domain.getStrides()[vindex] << 32l);
			// }
		}

		for (int i = 0; i < length; ++i) {
			limits[i] = target.getSizes()[i] - 1;
			reset[i] = limits[i] * stride[i];
		}

		long idx = 0;
		double[] result = new double[target.getCombinations()];


		for (int i = 0; i < result.length; ++i) {
			if (log)
				result[i] = data[(int) (idx & 0xFFFFFFFF)] + factor.data[(int) (idx >>> 32l)];
			else
				result[i] = data[(int) (idx & 0xFFFFFFFF)] * factor.data[(int) (idx >>> 32l)];

			for (int l = 0; l < length; ++l) {
				if (assign[l] == limits[l]) {
					assign[l] = 0;
					idx -= reset[l];
				} else {
					++assign[l];
					idx += stride[l];
					break;
				}
			}
		}

		return new BayesianFactor(target, result, log);
	}

	/**
	 * divide this factor by the provided one. This assumes that the domain of
	 * the given factor is a subset of this one's.
	 *
	 * @param factor
	 * @return
	 */
	@Override
	public BayesianFactor divide(final BayesianFactor factor) {
		final int length = domain.getSize();

		final int[] limits = new int[length];
		final int[] assign = new int[length];

		final long[] stride = new long[length];
		final long[] reset = new long[length];

		for (int vindex = 0; vindex < length; ++vindex) {
			stride[vindex] = domain.getStrides()[vindex];
		}

		for (int vindex = 0; vindex < factor.domain.getSize(); ++vindex) {
			int offset = Arrays.binarySearch(domain.getVariables(), factor.domain.getVariables()[vindex]);
			stride[offset] += ((long) factor.domain.getStrides()[vindex] << 32l);
		}

		for (int i = 0; i < length; ++i) {
			limits[i] = domain.getSizes()[i] - 1;
			reset[i] = limits[i] * stride[i];
		}

		long idx = 0;
		double[] result = new double[domain.getCombinations()];

		for (int i = 0; i < result.length; ++i) {
			if (log)
				result[i] = data[(int) (idx & 0xFFFFFFFF)] - factor.data[(int) (idx >>> 32l)];
			else
				result[i] = data[(int) (idx & 0xFFFFFFFF)] / factor.data[(int) (idx >>> 32l)];

			for (int l = 0; l < length; ++l) {
				if (assign[l] == limits[l]) {
					assign[l] = 0;
					idx -= reset[l];
				} else {
					++assign[l];
					idx += stride[l];
					break;
				}
			}
		}

		return new BayesianFactor(domain, result, log);
	}

	/**
	 * Factor normalization
	 *
	 */
	public BayesianFactor normalize(int... given) {
		BayesianFactor div = this;
		for (int m : ArraysUtil.removeAllFromSortedArray(domain.getVariables(), given)) {
			div = div.marginalize(m);
		}

		return divide(div);
	}


	@Override
	public String toString() {
		return "P(" + Arrays.toString(domain.getVariables()) + ") "+Arrays.toString(this.getData());
	}

	public final double log(double val) {
		return FastMath.log(val);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BayesianFactor)) return false;

		BayesianFactor other = (BayesianFactor) obj;
		if (!Arrays.equals(domain.getVariables(), other.getDomain().getVariables())) return false;

		if (isLog() == other.isLog()) {
			return ArraysUtil.almostEquals(data, other.data, 0.00000001);
		} else {
			return ArraysUtil.almostEquals(getData(), other.getData(), 0.00000001);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(domain);
	}



	public BayesianFactor fixPrecission(int num_decimals, int... left_vars){

		Strides left = this.getDomain().intersection(left_vars);
		Strides right = this.getDomain().remove(left);
		BayesianFactor newFactor = this.reorderDomain(left.concat(right));
		double[][] newData = new double[right.getCombinations()][left.getCombinations()];
		double[][] oldData = ArraysUtil.reshape2d(newFactor.getData(), right.getCombinations());

		for(int i=0; i<right.getCombinations(); i++){
			newData[i] = ArraysUtil.roundNonZerosToTarget(oldData[i], 1.0, num_decimals);
		}

		newFactor.setData(Doubles.concat(newData));
		return newFactor.reorderDomain(this.getDomain());

	}

	/**
	 * Static method that builds a deterministic factor (values can only be ones or zeros).
	 * Thus, children variables are determined by the values of the parents
	 * @param left	Strides - children variables.
	 * @param right	Strides - parent variables
	 * @param assignments assignments of each combination of the parent
	 * @return
	 */
	public static BayesianFactor deterministic(Strides left, Strides right, int... assignments){

		if (assignments.length != right.getCombinations())
			throw new IllegalArgumentException("ERROR: length of assignments should be equal to the number of combinations of the parents");

		if (Ints.min(assignments)<0 || Ints.max(assignments)>= left.getCombinations())
			throw new IllegalArgumentException("ERROR: assignments of deterministic function should be in the inteval [0,"+left.getCombinations()+")");


		double[] values = new double[right.union(left).getCombinations()];
		for(int i=0; i< right.getCombinations(); i++){
			values[i * left.getCombinations() + assignments[i]] = 1.0;
		}
		return new BayesianFactor(left.concat(right), values, false);
	}
	/**
	 * Static method that builds a deterministic factor (values can only be ones or zeros)
	 * without parent variables.
	 * @param left	Strides - children variables.
	 * @param assignment int - single value to assign
	 * @return
	 */

	public static BayesianFactor deterministic(Strides left, int assignment){
		return BayesianFactor.deterministic(left, Strides.empty(), assignment);
	}

	public BayesianFactor get_deterministic(int var, int assignment){
		return BayesianFactor.deterministic(this.getDomain().intersection(var), assignment);
	}


	public boolean isMarginalNormalized(){
		if(this.getDomain().getVariables().length>1)
			return false;
		return this.marginalize(this.getDomain().getVariables()[0]).getValue() == 1;
	}


	public static BayesianFactor random(Strides left, Strides right, int num_decimals, boolean zero_allowed){
		double[][] data = new double[right.getCombinations()][];
		for(int i=0; i<data.length; i++){
			data[i] = RandomUtil.sampleNormalized(left.getCombinations(), num_decimals, zero_allowed);
		}
		return new BayesianFactor(left.concat(right), Doubles.concat(data), false);
	}

	public BayesianFactor reorderDomain(Strides newStrides){

		if( !(this.getDomain().isConsistentWith(newStrides) &&
				this.getDomain().getSize() == newStrides.getSize())){
			throw new IllegalArgumentException("ERROR: wrong input Strides");
		}

		// at position i, now we put the axis that were at varMap[i]
		int[] varMap = IntStream.of(newStrides.getVariables())
				.map(v-> ArraysUtil.indexOf(v, this.getDomain().getVariables()))
				.toArray();

		return new BayesianFactor(newStrides,
				ArraysUtil.swapVectorStrides(this.getData(), this.getDomain().getSizes(), varMap),false);

	}


	public BayesianFactor reorderDomain(int... vars){

		int[] all_vars = Ints.concat(vars,
				IntStream.of(this.getDomain().getVariables())
						.filter(v -> !ArrayUtils.contains(vars,v)).toArray()
		);

		return this.reorderDomain(
					new Strides(
						all_vars,
						IntStream.of(all_vars).map(v -> this.getDomain().getCardinality(v)).toArray()));

	}

	public void sortDomain() {
		if(!ArrayUtils.isSorted(this.getDomain().getVariables())) {
			BayesianFactor sorted = this.reorderDomain(this.getDomain().sort());
			this.domain = sorted.domain;
			this.data = sorted.getData();
		}
	}


	public BayesianFactor renameDomain(int... new_vars){
		BayesianFactor out = new BayesianFactor(new Strides(new_vars, this.getDomain().getSizes()));
		out.setData(this.getData());
		return out;
	}


}
