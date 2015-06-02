package evoanalyzer.model;

import java.util.ArrayList;

import org.eclipse.jgit.revwalk.RevCommit;

public class RefactoringCommit {
	private ArrayList<CodeChangeMatch> matchList = new ArrayList<>();
	
	/**
	 * previous commit
	 */
	private RevCommit prevCommit;
	
	/**
	 * this commit
	 */
	private RevCommit postCommit;

	public ArrayList<CodeChangeMatch> getMatchList() {
		return matchList;
	}

	public void setMatchList(ArrayList<CodeChangeMatch> matchList) {
		this.matchList = matchList;
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
	
	
}
