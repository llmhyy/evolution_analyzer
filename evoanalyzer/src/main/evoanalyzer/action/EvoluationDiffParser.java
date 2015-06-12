package evoanalyzer.action;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import evoanalyzer.model.CodeChangeMatch;
import evoanalyzer.model.FileChange;
import evoanalyzer.model.MatchPair;
import evoanalyzer.model.RefactoringCommit;
import evoanalyzer.util.RepositoryUtil;
import evoanalyzer.util.Settings;

public class EvoluationDiffParser {
	
	public void parseEvoluationDiffs(String repoPath) {
		
		long t1 = System.currentTimeMillis();
		
		ArrayList<RefactoringCommit> refactoringCommits = new ArrayList<>();
		try {
			Repository repository = RepositoryUtil.openRepository(repoPath);

			ObjectId lastCommitId = repository.resolve(Constants.HEAD);
			//ObjectId lastCommitId = repository.resolve("479abc");
			//ObjectId lastCommitId = repository.resolve("bbd95a");
			//ObjectId lastCommitId = repository.resolve("86ed1f");
			
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
				
				// match code changes to make sure that the refactoring exists
				RefactoringCommitDetector refactoringDetector = new RefactoringCommitDetector();
				RefactoringCommit refactoringCommit = refactoringDetector.detectRefactoring(fileChangeList);
				if(null != refactoringCommit){
					buildRelevance(refactoringCommits, refactoringCommit);
					refactoringCommits.add(refactoringCommit);
				}
				
//				if(refactoringCommits.size() > 100){
//					break;
//				}
				
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
		System.out.println("identifying refactoring commit time: " + (t2-t1)/1000/60);
		
		long t3 = System.currentTimeMillis();
		ArrayList<ArrayList<RefactoringCommit>> clusters = identifyRelevantRefactoringCommit(refactoringCommits);
		long t4 = System.currentTimeMillis();
		
		System.out.println("clustering time:" + (t4-t3)/1000/60 );
		
		ArrayList<ArrayList<RefactoringCommit>> cls = filterSingleElementCluster(clusters);
		
		System.currentTimeMillis();
	}

	private ArrayList<ArrayList<RefactoringCommit>> filterSingleElementCluster(ArrayList<ArrayList<RefactoringCommit>> clusters){
		ArrayList<ArrayList<RefactoringCommit>> cls = new ArrayList<ArrayList<RefactoringCommit>>();
		for(ArrayList<RefactoringCommit> cluster: clusters){
			if(cluster.size() > 1){
				cls.add(cluster);
			}
		}
		
		return cls;
	}
	
	/**
	 * find the connected components of the graph of refactoring commits
	 * @param refactoringCommits
	 * @return
	 */
	private ArrayList<ArrayList<RefactoringCommit>> identifyRelevantRefactoringCommit(ArrayList<RefactoringCommit> refactoringCommits){
		ArrayList<ArrayList<RefactoringCommit>> clusters = new ArrayList<>();
		RefactoringCommit commit = findUnmarkedCommits(refactoringCommits);
		while(commit != null){
			ArrayList<RefactoringCommit> cluster = new ArrayList<>();
			visit(commit, cluster);
			clusters.add(cluster);
			commit = findUnmarkedCommits(refactoringCommits);
		}
		
		
		return clusters;
	}

	private ArrayList<RefactoringCommit> visit(RefactoringCommit commit, ArrayList<RefactoringCommit> cluster) {
		cluster.add(commit);
		commit.setMarked(true);
		for(RefactoringCommit relatedCommit: commit.getRelatedRefactoringCommits().keySet()){
			if(!relatedCommit.isMarked()){
				visit(relatedCommit, cluster);
			}
		}
		
		return cluster;
	}

	private RefactoringCommit findUnmarkedCommits(ArrayList<RefactoringCommit> refactoringCommits) {
		for(RefactoringCommit commit: refactoringCommits){
			if(!commit.isMarked()){
				return commit;
			}
		}
		return null;
	}

	private void buildRelevance(ArrayList<RefactoringCommit> refactoringCommits, RefactoringCommit refactoringCommit) {
		for(RefactoringCommit commit: refactoringCommits){
//			ArrayList<MatchPair> pairList = commit.findRelavantMatches(refactoringCommit);
//			
//			if(pairList.size() > 0){
//				commit.addRelatedRefactoringCommit(refactoringCommit, pairList);
//				refactoringCommit.addRelatedRefactoringCommit(commit, pairList);
//			}
			int seconds = commit.getPostCommit().getCommitTime() - refactoringCommit.getPostCommit().getCommitTime();
			double hours = ((double)seconds)/60/60;
			
			if(Math.abs(hours) <= Settings.commitTimeInterval){
				commit.addRelatedRefactoringCommit(refactoringCommit, new ArrayList<MatchPair>());
				refactoringCommit.addRelatedRefactoringCommit(commit, new ArrayList<MatchPair>());
			}
		}
		
	}
	
	
}
