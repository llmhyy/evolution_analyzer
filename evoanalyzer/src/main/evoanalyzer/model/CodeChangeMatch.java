package evoanalyzer.model;

import evoanalyzer.util.Settings;

/**
 * A match represents the movement of a piece of code from one location to another 
 * @author ly
 *
 */
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

	/**
	 * Currently, I just consider two criteria for regarding two matches relevant:
	 * 1) the moved code (or member) is moved again;
	 * 2) difference code (or member) is moved from one same location to anther same location.
	 * 
	 * @param thatMatch
	 * @return
	 */
	public boolean isRelevanceTo(CodeChangeMatch thatMatch) {
		return isTheSameCodeMovedAgain(thatMatch) 
				|| isTheCodeMoveAmongSameLocation(thatMatch);
	}
	
	private boolean isTheSameCodeMovedAgain(CodeChangeMatch thatMatch){		
		return isTheSameCodeMovedAgain(this.addedChange, thatMatch.getRemovedChange()) ||
				isTheSameCodeMovedAgain(thatMatch.getAddedChange(), this.removedChange);
	}
	
	private boolean isTheSameCodeMovedAgain(CodeChange addedChange, CodeChange removedChange){
		if(addedChange.getFileChange().getNewPath().equals(removedChange.getFileChange().getOldPath())){
			double sim = addedChange.compareASTNodeTo(removedChange);
			if(sim >= Settings.ASTNodeSimilarity){
				return true;
			}
		}
		return false;
	}
	
	private boolean isTheCodeMoveAmongSameLocation(CodeChangeMatch thatMatch){
		if(this.getAddedChange().getFileChange().getNewPath().
				equals(thatMatch.getAddedChange().getFileChange().getNewPath()) &&
				this.getRemovedChange().getFileChange().getOldPath().
				equals(thatMatch.getRemovedChange().getFileChange().getOldPath())){
			return true;
		} 
		return false;
	}
}
