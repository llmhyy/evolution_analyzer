package evoanalyzer.model;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

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
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		String newPath = this.addedChange.getFileChange().getNewPath();
		newPath = newPath.substring(newPath.lastIndexOf("/")+1, newPath.length());
		String oldPath = this.addedChange.getFileChange().getOldPath();
		oldPath = oldPath.substring(oldPath.lastIndexOf("/")+1, oldPath.length());
		
		buffer.append("move ");
		buffer.append(getSimpleName(this.addedChange.getNode()));
		buffer.append(" from ");
		buffer.append(newPath);
		buffer.append(" to ");
		buffer.append(oldPath);
		buffer.append("\n");
		
		return buffer.toString();
	}
	
	private String getSimpleName(ASTNode node){
		if(node instanceof TypeDeclaration){
			TypeDeclaration decl = (TypeDeclaration)node;
			return decl.getName().getIdentifier();
		}
		else if(node instanceof MethodDeclaration){
			MethodDeclaration decl = (MethodDeclaration)node;
			return decl.getName().getIdentifier();
		}
		else if(node instanceof FieldDeclaration){
			FieldDeclaration decl = (FieldDeclaration)node;
			return decl.fragments().get(0).toString();
		}
		
		return "";
	}
	
	public boolean isRenamingRefactoring(){
		FileChange codeAdded = getAddedChange().getFileChange();
		FileChange codeDeleted = getRemovedChange().getFileChange();
		
//		if(codeAdded.getType().equals(ChangeType.MODIFY) || codeDeleted.getType().equals(ChangeType.MODIFY)){
//			return false;
//		}

		if(codeAdded.getType().equals(ChangeType.ADD) && codeDeleted.getType().equals(ChangeType.DELETE)){
			return true;
		}
		
		return false;
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
	 * 2) difference code (or member) is moved from one relevant location to anther relevant location.
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
				equals(thatMatch.getAddedChange().getFileChange().getNewPath()) ||
				this.getRemovedChange().getFileChange().getOldPath().
				equals(thatMatch.getRemovedChange().getFileChange().getOldPath())){
			return true;
		} 
		return false;
	}
}
