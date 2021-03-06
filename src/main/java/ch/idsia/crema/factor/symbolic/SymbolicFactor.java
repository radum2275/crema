package ch.idsia.crema.factor.symbolic;

import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.model.Strides;


public abstract class SymbolicFactor implements Factor<SymbolicFactor> {
	private Strides domain;
	
	public SymbolicFactor(Strides domain) {
		this.domain = domain;
	}
	
	@Override
	public CombinedFactor combine(SymbolicFactor other) {
		return new CombinedFactor(this, other);
	}

	@Override
	public MarginalizedFactor marginalize(int variable) {
		return new MarginalizedFactor(this, variable);
	}
	
	@Override
	public Strides getDomain() {
		return domain;
	}


	// XXX copy not this way
	@Override
	public SymbolicFactor copy() {
		return null;
	}

	@Override
	public FilteredFactor filter(int variable, int state) {
		return new FilteredFactor(this, variable, state);
	}


	
	@Override
	public DividedFactor divide(SymbolicFactor factor) {
		return new DividedFactor(this, factor);
	}
}
