package evoanalyzer.model;

import mcidiff.util.FastASTNodeComparator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * This class aims to specify in which file of what commit add/remove/modify what AST node
 * @author ly
 *
 */
public class CodeChange {
	public static String REMOVE = "remove";
	public static String ADD = "add";
	public static String MODIFY = "modify";
	
	private String changeType;
	private FileChange fileChange;
	private RevCommit prevCommit;
	private RevCommit postCommit;
	private ASTNode node;
	
	public CodeChange(RevCommit prevCommit, RevCommit postCommit, FileChange fileChange, ASTNode node, String changeType) {
		super();
		this.changeType = changeType;
		this.fileChange = fileChange;
		this.prevCommit = prevCommit;
		this.postCommit = postCommit;
		this.node = node;
	}
	
	public String toString(){
		return changeType + ": " + node.toString();
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public FileChange getFileChange() {
		return fileChange;
	}

	public void setFileChange(FileChange fileChange) {
		this.fileChange = fileChange;
	}

	public RevCommit getPrevCommit() {
		return prevCommit;
	}

	public void setPrevCommit(RevCommit prevCommit) {
		this.prevCommit = prevCommit;
	}

	public RevCommit getPostCommit() {
		return postCommit;
	}

	public void setPostCommit(RevCommit postCommit) {
		this.postCommit = postCommit;
	}

	public ASTNode getNode() {
		return node;
	}

	public void setNode(ASTNode node) {
		this.node = node;
	}

	public double compareASTNodeTo(CodeChange matChange) {
		ASTNode thatNode = matChange.getNode();
		ASTNode thisNode = this.node;
		
		return FastASTNodeComparator.computeNodeSim(thisNode, thatNode);
	}
	
}
