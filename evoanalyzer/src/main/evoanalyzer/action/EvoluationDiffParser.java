package evoanalyzer.action;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import evoanalyzer.RepositoryUtil;
import evoanalyzer.model.FileChange;
import evoanalyzer.model.MatchPair;
import evoanalyzer.model.RefactoringCommit;

public class EvoluationDiffParser {
	
	public void parseEvoluationDiffs(String repoPath) {
		
		long t1 = System.currentTimeMillis();
		
		ArrayList<RefactoringCommit> refactoringCommits = new ArrayList<>();
		try {
			Repository repository = RepositoryUtil.openRepository(repoPath);

			ObjectId lastCommitId = repository.resolve(Constants.HEAD);
			//ObjectId lastCommitId = repository.resolve("bbd95ad");
			
			RevWalk walk = new RevWalk(repository);
			RevCommit commit = walk.parseCommit(lastCommitId);

			walk.markStart(commit);

			RevCommit postCommit = commit;
			for (RevCommit prevCommit : walk) {
				if (prevCommit == postCommit) {
					continue;
				}

				System.out.println("I am parsing the commit: " + postCommit.toString());
				
				// return the code change in terms of code syntax
				JavaFileChangeAnalyzer analyzer = new JavaFileChangeAnalyzer();
				ArrayList<FileChange> fileChangeList = analyzer.analyzeJavaFileChanges(prevCommit, postCommit, repository);
				
				//TODO match code changes to make sure that the refactoring exists
				RefactoringCommitDetector refactoringDetector = new RefactoringCommitDetector();
				RefactoringCommit refactoringCommit = refactoringDetector.detectRefactoring(fileChangeList);
				if(null != refactoringCommit){
					buildRelevance(refactoringCommits, refactoringCommit);
					refactoringCommits.add(refactoringCommit);
				}
				
				postCommit = prevCommit;
			}
			walk.close();
			repository.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long t2 = System.currentTimeMillis();
		
		System.out.println("total time: " + (t2-t1)/1000/60);
	}
	
	private ArrayList<ArrayList<RefactoringCommit>> identifyRelevantRefactoringCommit(ArrayList<RefactoringCommit> refactoringCommits){
		//TODO
		return null;
	}

	private void buildRelevance(ArrayList<RefactoringCommit> refactoringCommits, RefactoringCommit refactoringCommit) {
		for(RefactoringCommit commit: refactoringCommits){
			ArrayList<MatchPair> pairList = commit.findRelavantMatches(refactoringCommit);
			
			if(pairList.size() > 0){
				commit.addRelatedRefactoringCommit(refactoringCommit, pairList);
				refactoringCommit.addRelatedRefactoringCommit(commit, pairList);
			}
		}
		
	}
	
	
}
