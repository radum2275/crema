package ch.idsia.crema.model.vertex;

import org.apache.commons.math3.util.FastMath;

public class LogVertexOperation implements VertexOperation {

	@Override
	public double[] combine(double[] t1, double[] t2, int size, long[] stride, long[] reset, int[] limits) {
		int length = limits.length;

		double[] result = new double[size];
		double[] assign = new double[length];

		long idx = 0;
		for (int i = 0; i < size; ++i) {
			result[i] = t1[(int) idx] + t2[(int) (idx >>> 32l)];

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

		return result;
	}

	@Override
	public double[] marginalize(double[] data, int size, int stride) {
		int reset = size * stride;
		int target_size = data.length / size;
		int source = 0;
		int next = stride;
		int jump = reset - stride;

		final double[] result = new double[target_size];

		for (int target = 0; target < target_size; ++target, ++source) {
			if (source == next) {
				source += jump;
				next += reset;
			}

			double value = FastMath.exp(data[source]);
			for (int idx = source + stride; idx < reset; ++idx) {
				value += FastMath.exp(data[idx]);
			}

			result[target] = FastMath.log(value);
		}

		return result;
	}

	@Override
	public double[] convert(double[] data) {
		double[] result = new double[data.length];
		for (int index = 0; index < data.length; ++index) {
			result[index] = FastMath.log(data[index]);
		}
		return result;
	}

	@Override
	public double[] revert(double[] data) {
		double[] result = new double[data.length];
		for (int index = 0; index < data.length; ++index) {
			result[index] = FastMath.exp(data[index]);
		}
		return result;
	}

	@Override
	public double convert(double val) {
		return FastMath.log(val);
	}

	@Override
	public double revert(double val) {
		return FastMath.exp(val);
	}
}
