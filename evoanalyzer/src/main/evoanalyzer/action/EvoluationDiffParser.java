package evoanalyzer.action;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import evoanalyzer.model.FileChange;
import evoanalyzer.model.MatchPair;
import evoanalyzer.model.RefactoringCommit;
import evoanalyzer.util.RepositoryUtil;
import evoanalyzer.util.Settings;

public class EvoluationDiffParser {

	public void parseEvoluationDiffs(String repoPath) {
		long t1 = System.currentTimeMillis();

		//ArrayList<RefactoringCommit> refactoringCommits = new ArrayList<>();
		ArrayList<RefactoringCommit> refactoringCommits;

		ArrayList<String> IDs = readRefactoringCommit();
		if (IDs.size() != 0) {
			refactoringCommits = retrieveRefactoringCommitsFromLog(repoPath, IDs);
		}
		else {
			refactoringCommits = retrieveRefactoringCommitsFromStart(repoPath);
		}

		long t2 = System.currentTimeMillis();
		System.out.println("identifying refactoring commit time: " + (t2 - t1) / 1000 / 60);

		logRefactoringCommit(refactoringCommits);

		long t3 = System.currentTimeMillis();
		ArrayList<ArrayList<RefactoringCommit>> clusters = identifyRelevantRefactoringCommit(refactoringCommits);
		long t4 = System.currentTimeMillis();

		System.out.println("clustering time:" + (t4 - t3) / 1000 / 60);

		ArrayList<ArrayList<RefactoringCommit>> cls = filterSingleElementCluster(clusters);

//		System.currentTimeMillis();
	}
	
	private void checkAndAddRefactoringCommits(ArrayList<RefactoringCommit> refactoringCommits, Repository repository, 
			RevCommit postCommit, RevCommit prevCommit) throws Exception{
		System.out.println("I am parsing the commit: " + postCommit.toString());

		// return the code change in terms of code syntax
		JavaFileChangeAnalyzer analyzer = new JavaFileChangeAnalyzer();
		ArrayList<FileChange> fileChangeList = analyzer.analyzeJavaFileChanges(prevCommit, postCommit,
				repository);

		// match code changes to make sure that the refactoring exists
		RefactoringCommitDetector refactoringDetector = new RefactoringCommitDetector();
		RefactoringCommit refactoringCommit = refactoringDetector.detectRefactoring(fileChangeList);
		if (null != refactoringCommit) {
			buildRelevance(refactoringCommits, refactoringCommit);
			refactoringCommits.add(refactoringCommit);
		}
	}
	
	private ArrayList<RefactoringCommit> retrieveRefactoringCommitsFromLog(String repoPath, ArrayList<String> IDs){
		ArrayList<RefactoringCommit> refactoringCommits = new ArrayList<>();
		try {
			Repository repository = RepositoryUtil.openRepository(repoPath);
//			ObjectId lastCommitId = repository.resolve(IDs.get(0));
			RevWalk walk = new RevWalk(repository);
//			RevCommit commit = walk.parseCommit(lastCommitId);
			
//			RevCommit postCommit = commit;
						
			for (String associatedID : IDs) {
				String[] assoIDs = associatedID.split(":");
				ObjectId prevCommitId = repository.resolve(assoIDs[1]);
				RevCommit prevCommit = walk.parseCommit(prevCommitId);
				
				ObjectId postCommitId = repository.resolve(assoIDs[0]);
				RevCommit postCommit = walk.parseCommit(postCommitId);
				
				checkAndAddRefactoringCommits(refactoringCommits, repository, postCommit, prevCommit);
				//postCommit = prevCommit;
				
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return refactoringCommits;
	}

	private ArrayList<RefactoringCommit> retrieveRefactoringCommitsFromStart(String repoPath) {

		ArrayList<RefactoringCommit> refactoringCommits = new ArrayList<>();

		try {

			Repository repository = RepositoryUtil.openRepository(repoPath);
			ObjectId lastCommitId = repository.resolve(Constants.HEAD);
			// ObjectId lastCommitId = repository.resolve("479abc");
			// ObjectId lastCommitId = repository.resolve("bbd95a");
			// ObjectId lastCommitId = repository.resolve("86ed1f");
			// ObjectId lastCommitId = repository.resolve("852d59b");
			
			RevWalk walk = new RevWalk(repository);
			RevCommit commit = walk.parseCommit(lastCommitId);
			walk.markStart(commit);
			RevCommit postCommit = commit;
			for (RevCommit prevCommit : walk) {
				if (prevCommit == postCommit) {
					continue;
				}

				checkAndAddRefactoringCommits(refactoringCommits, repository, postCommit, prevCommit);

//				if (refactoringCommits.size() > 2) {
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

		return refactoringCommits;
	}

	@SuppressWarnings("resource")
	private ArrayList<String> readRefactoringCommit() {
		ArrayList<String> list = new ArrayList<>();
		InputStream stream;
		try {
			stream = new FileInputStream("log.txt");
			InputStreamReader reader = new InputStreamReader(stream);
			BufferedReader br = new BufferedReader(reader);

			String line = br.readLine();
			while (line != null) {
				list.add(line);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}

	private void logRefactoringCommit(ArrayList<RefactoringCommit> refactoringCommits) {
		StringBuffer buffer = new StringBuffer();
		for (RefactoringCommit commit : refactoringCommits) {
			String id = commit.toString();
			id = id.substring(7, 19);
			
			String preId = commit.getPrevCommit().toString();
			preId = preId.substring(7, 19);

			buffer.append(id);
			buffer.append(":");
			buffer.append(preId);
			buffer.append("\n");
		}
		String logContent = buffer.toString();

		PrintWriter out;
		try {
			out = new PrintWriter("log.txt");
			out.print(logContent);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<ArrayList<RefactoringCommit>> filterSingleElementCluster(
			ArrayList<ArrayList<RefactoringCommit>> clusters) {
		ArrayList<ArrayList<RefactoringCommit>> cls = new ArrayList<ArrayList<RefactoringCommit>>();
		for (ArrayList<RefactoringCommit> cluster : clusters) {
			if (cluster.size() > 1) {
				cls.add(cluster);
			}
		}

		return cls;
	}

	/**
	 * find the connected components of the graph of refactoring commits
	 * 
	 * @param refactoringCommits
	 * @return
	 */
	private ArrayList<ArrayList<RefactoringCommit>> identifyRelevantRefactoringCommit(
			ArrayList<RefactoringCommit> refactoringCommits) {
		ArrayList<ArrayList<RefactoringCommit>> clusters = new ArrayList<>();
		RefactoringCommit commit = findUnmarkedCommits(refactoringCommits);
		while (commit != null) {
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
		for (RefactoringCommit relatedCommit : commit.getRelatedRefactoringCommits().keySet()) {
			if (!relatedCommit.isMarked()) {
				visit(relatedCommit, cluster);
			}
		}

		return cluster;
	}

	private RefactoringCommit findUnmarkedCommits(ArrayList<RefactoringCommit> refactoringCommits) {
		for (RefactoringCommit commit : refactoringCommits) {
			if (!commit.isMarked()) {
				return commit;
			}
		}
		return null;
	}

	private void buildRelevance(ArrayList<RefactoringCommit> refactoringCommits, RefactoringCommit refactoringCommit) {
		for (RefactoringCommit commit : refactoringCommits) {
			// ArrayList<MatchPair> pairList =
			// commit.findRelavantMatches(refactoringCommit);
			//
			// if(pairList.size() > 0){
			// commit.addRelatedRefactoringCommit(refactoringCommit, pairList);
			// refactoringCommit.addRelatedRefactoringCommit(commit, pairList);
			// }
			int seconds = commit.getPostCommit().getCommitTime() - refactoringCommit.getPostCommit().getCommitTime();
			double hours = ((double) seconds) / 60 / 60;

			if (Math.abs(hours) <= Settings.commitTimeInterval) {
				commit.addRelatedRefactoringCommit(refactoringCommit, new ArrayList<MatchPair>());
				refactoringCommit.addRelatedRefactoringCommit(commit, new ArrayList<MatchPair>());
			}
		}

	}

}
