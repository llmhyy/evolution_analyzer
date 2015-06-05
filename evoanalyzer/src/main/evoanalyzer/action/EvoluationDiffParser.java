package evoanalyzer.action;

import java.io.IOException;
import java.util.ArrayList;

import evoanalyzer.model.RefactoringCommit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import evoanalyzer.RepositoryUtil;
import evoanalyzer.model.FileChange;

public class EvoluationDiffParser {
	
	public void parseEvoluationDiffs(String repoPath) {
		
		ArrayList<RefactoringCommit> refactoringCommits = new ArrayList<>();
		try {
			Repository repository = RepositoryUtil.openRepository(repoPath);

			ObjectId lastCommitId = repository.resolve(Constants.HEAD);

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
		
		System.currentTimeMillis();
	}
	
	
}
