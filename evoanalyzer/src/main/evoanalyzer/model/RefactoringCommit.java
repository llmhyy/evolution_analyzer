package evoanalyzer.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jgit.revwalk.RevCommit;

import evoanalyzer.util.Settings;

public class RefactoringCommit {
	private ArrayList<CodeChangeMatch> matchList = new ArrayList<>();
	private HashMap<RefactoringCommit, ArrayList<MatchPair>> relatedRefactoringCommits = new HashMap<>();
	
	private boolean isMarked = false;
	
	/**
	 * previous commit
	 */
	private RevCommit prevCommit;
	
	/**
	 * this commit
	 */
	private RevCommit postCommit;

	public RefactoringCommit(ArrayList<CodeChangeMatch> matchList) {
		super();
		this.matchList = matchList;
	}

	public RefactoringCommit(ArrayList<CodeChangeMatch> matchList, RevCommit prevCommit, RevCommit postCommit) {
		super();
		this.matchList = matchList;
		this.prevCommit = prevCommit;
		this.postCommit = postCommit;
	}
	
	@Override
	public String toString(){
		return this.postCommit.toString();
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof RefactoringCommit){
			RefactoringCommit commit = (RefactoringCommit)obj;
			return commit.getPostCommit().toString().equals(this.getPostCommit().toString());
		}
		
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.getPostCommit().toString().hashCode();
	}
	
	public HashMap<RefactoringCommit, ArrayList<MatchPair>> getRelatedRefactoringCommits() {
		return relatedRefactoringCommits;
	}

	public void setRelatedRefactoringCommits(HashMap<RefactoringCommit, ArrayList<MatchPair>> relatedRefactoringCommits) {
		this.relatedRefactoringCommits = relatedRefactoringCommits;
	}
	
	public void addRelatedRefactoringCommit(RefactoringCommit commit, ArrayList<MatchPair> pairLists){
		this.relatedRefactoringCommits.put(commit, pairLists);
	}

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
	
	public boolean isMarked() {
		return isMarked;
	}

	public void setMarked(boolean isMarked) {
		this.isMarked = isMarked;
	}

	public ArrayList<MatchPair> findRelavantMatches(RefactoringCommit refactoringCommit) {
		ArrayList<MatchPair> pairList = new ArrayList<>();
		
		int seconds = this.postCommit.getCommitTime() - refactoringCommit.getPostCommit().getCommitTime();
		double hours = ((double)seconds)/60/60;
		
		if(hours <= Settings.commitTimeInterval){
			for(CodeChangeMatch thisMatch: this.matchList){
				for(CodeChangeMatch thatMatch: refactoringCommit.getMatchList()){
					if(thisMatch.isRelevanceTo(thatMatch)){
						MatchPair pair = new MatchPair(thisMatch, thatMatch);
						pairList.add(pair);
					}
				}
			}			
		}
		
		return pairList;
	}
	
	
}
