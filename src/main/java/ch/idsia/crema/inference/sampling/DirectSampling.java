package ch.idsia.crema.inference.sampling;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.Updating;
import ch.idsia.crema.model.GraphicalModel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: CreMA
 * Date:    05.02.2018 13:32
 */
public class DirectSampling extends StochasticSampling implements Updating<BayesianFactor, BayesianFactor> {

	/**
	 * Algorithm 44 from "Modeling and Reasoning with BN", Dawiche, p.380
	 *
	 * @return a map with the computed sampled states over all the variables.
	 */
	private TIntIntMap simulateBN() {

		int[] roots = model.getRoots();

		TIntIntMap map = new TIntIntHashMap();
		TIntSet nodes = new TIntHashSet();

		// sample root nodes
		for (int root : roots) {
			map.put(root, sample(model.getFactor(root)));

			int[] children = model.getChildren(root);
			nodes.addAll(children);
		}

		// sample child nodes
		do {
			TIntSet slack = new TIntHashSet();

			for (int node : nodes.toArray()) {
				// check for evidence in this child node
				int[] parents = model.getParents(node);

				// filter out parents state
				BayesianFactor factor = model.getFactor(node);
				for (int parent : parents) {
					factor = factor.filter(parent, map.get(parent));
				}

				map.put(node, sample(factor));

				int[] children = model.getChildren(node);
				slack.addAll(children);
			}

			nodes = slack;
		} while (!nodes.isEmpty());

		return map;
	}

	/**
	 * Algorithm 45 from "Modeling and Reasoning with BN", Dawiche, p.385
	 * <p>
	 * Use Monte Carlo simulation to estimate the expectation of the direct sampling function.
	 *
	 * @param query variable to query
	 * @return the distribution of probability on the query
	 */
	public Collection<BayesianFactor> run(int... query) {
		TIntObjectMap<double[]> distributions = new TIntObjectHashMap<>();

		for (int variable : model.getVariables()) {
			int states = model.getDomain(variable).getCombinations();

			distributions.put(variable, new double[states]);
		}

		for (int i = 0; i < iterations; i++) {
			TIntIntMap x = simulateBN();

			for (int variable : x.keys()) {
				distributions.get(variable)[x.get(variable)] += 1. / iterations;
			}
		}

		List<BayesianFactor> factors = new ArrayList<>();

		for (int q : query) {
			factors.add(new BayesianFactor(model.getDomain(q), distributions.get(q), false));
		}

		return factors;
	}

	@Override
	public Collection<BayesianFactor> apply(GraphicalModel<BayesianFactor> model, int[] query) {
		setModel(model);
		return run(query);
	}

	@Override
	public Collection<BayesianFactor> apply(GraphicalModel<BayesianFactor> model, int[] query, TIntIntMap observations) {
		throw new IllegalArgumentException("DirectSampling doesn't allow observations!");
	}
}
