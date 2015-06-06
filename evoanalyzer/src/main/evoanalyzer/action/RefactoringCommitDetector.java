package evoanalyzer.action;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jgit.revwalk.RevCommit;

import evoanalyzer.model.CodeChange;
import evoanalyzer.model.CodeChangeMatch;
import evoanalyzer.model.FileChange;
import evoanalyzer.model.RefactoringCommit;
import evoanalyzer.util.Settings;

public class RefactoringCommitDetector {

	public RefactoringCommit detectRefactoring(ArrayList<FileChange> fileChangeList) {
		
		ArrayList<CodeChange> allCodeChanges = collectAllCodeChanges(fileChangeList);
		ArrayList<CodeChangeMatch> matches = identifyChangeMatches(allCodeChanges);
		
		if(matches.size() > 0){
			RevCommit prevCommit = allCodeChanges.get(0).getPrevCommit();
			RevCommit postCommit = allCodeChanges.get(0).getPostCommit();
			RefactoringCommit commit = new RefactoringCommit(matches, prevCommit, postCommit);
			
			return commit;
		}
		
		return null;
	}

	private ArrayList<CodeChangeMatch> identifyChangeMatches(ArrayList<CodeChange> allCodeChanges) {
		ArrayList<CodeChangeMatch> matchList = new ArrayList<>();
		HashSet<Integer> visitedIndexes = new HashSet<>();
		
		for(int i=0; i<allCodeChanges.size(); i++){
			if(visitedIndexes.contains(i))continue;
			
			CodeChange change = allCodeChanges.get(i);
			int j = findBestMatch(i+1, allCodeChanges, visitedIndexes);
			
			if(j != -1){
				CodeChange matchedChange = allCodeChanges.get(j);
				if(!change.getFileChange().equals(matchedChange.getFileChange())){
					visitedIndexes.add(i);
					visitedIndexes.add(j);
					
					CodeChange addedChange = (change.getChangeType().equals(CodeChange.ADD))? change : matchedChange;
					CodeChange removedChange = (change.getChangeType().equals(CodeChange.REMOVE))? change : matchedChange;
					CodeChangeMatch match = new CodeChangeMatch(addedChange, removedChange);
					matchList.add(match);
				}				
			}
		}

		return matchList;
	}

	private int findBestMatch(int k, ArrayList<CodeChange> allCodeChanges, HashSet<Integer> visitedIndexes) {
		CodeChange change = allCodeChanges.get(k-1);
		
		double sim = 0;
		int matchedIndex = -1;
		for(int i=k; i<allCodeChanges.size(); i++){
			if(visitedIndexes.contains(i))continue;
			
			CodeChange matChange = allCodeChanges.get(i);
			
			if(change.getChangeType().equals(matChange.getChangeType()))continue;
			
			double tempSim = change.compareASTNodeTo(matChange);
			
			if(tempSim > sim && tempSim >= Settings.ASTNodeSimilarity){
				
				sim = tempSim;
				matchedIndex = i;
				
				change.compareASTNodeTo(matChange);
			}
			
		}
		
		return matchedIndex;
	}

	private ArrayList<CodeChange> collectAllCodeChanges(ArrayList<FileChange> fileChangeList) {
		ArrayList<CodeChange> codeChangeList = new ArrayList<>();
		for(FileChange fileChange: fileChangeList){
			codeChangeList.addAll(fileChange.getCodeChangeList());
		}
		
		return codeChangeList;
	}
	
}
