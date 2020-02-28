package ch.irb.IgGenealogicTreeMaker;

public class MutationAtOnePosition {
	private boolean hasSingleMutation = false;
	private boolean hasReverseMutation = false;
	private boolean hasDoubleMutation = false;
	private String singleMutation = "";
	private String doubleMutation = "";
	private String reverseMutation = "";
	@SuppressWarnings("unused")
	private Integer position;
	private int numberOfMutations = 0;

	public MutationAtOnePosition(Integer position) {
		this.position = position;
	}

	public boolean hasSingleMutation() {
		return hasSingleMutation;
	}

	public void setSingleMutation(boolean hasSingleMutation, String singleMutation) {
		if (this.singleMutation.length() == 0) {
			this.hasSingleMutation = hasSingleMutation;
			this.singleMutation = singleMutation;
			numberOfMutations++;
		}
	}

	public boolean hasReverseMutation() {
		return hasReverseMutation;
	}

	public void setReverseMutation(boolean hasReverseMutation, String reverseMutation) {
		if (this.reverseMutation.length() == 0) {
			this.hasReverseMutation = hasReverseMutation;
			this.reverseMutation = reverseMutation;
			numberOfMutations++;
		}
	}

	public boolean hasDoubleMutation() {
		return hasDoubleMutation;
	}

	public void setDoubleMutation(boolean hasDoubleMutation, String doubleMutation) {
		if (this.doubleMutation.length() == 0) {
			this.hasDoubleMutation = hasDoubleMutation;
			this.doubleMutation = doubleMutation;
			numberOfMutations++;
		}
	}

	public String getSingleMutation() {
		return singleMutation;
	}

	public String getDoubleMutation() {
		return doubleMutation;
	}

	public String getReverseMutation() {
		return reverseMutation;
	}

	public int getNumberOfMutations() {
		return numberOfMutations;
	}

}
