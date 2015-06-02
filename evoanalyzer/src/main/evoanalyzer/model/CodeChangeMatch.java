package evoanalyzer.model;

public class CodeChangeMatch {
	private CodeChange addedChange;
	private CodeChange removedChange;
	
	public CodeChangeMatch(CodeChange addedChange, CodeChange removedChange) {
		super();
		this.addedChange = addedChange;
		this.removedChange = removedChange;
	}
	
	public CodeChange getAddedChange() {
		return addedChange;
	}
	
	public void setAddedChange(CodeChange addedChange) {
		this.addedChange = addedChange;
	}
	
	public CodeChange getRemovedChange() {
		return removedChange;
	}
	
	public void setRemovedChange(CodeChange removedChange) {
		this.removedChange = removedChange;
	}
	
	
}
