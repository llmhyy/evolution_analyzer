package evoanalyzer.action;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mcidiff.main.SeqMCIDiff;
import mcidiff.model.CloneInstance;
import mcidiff.model.CloneSet;
import mcidiff.model.SeqMultiset;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import evoanalyzer.RepositoryUtil;
import evoanalyzer.model.CodeChange;
import evoanalyzer.model.FileChange;

public class EvoluationDiffParser {
	public void parseEvoluationDiffs(String repoPath){
		try {
			Repository repository = RepositoryUtil.openRepository(repoPath);
			
			ObjectId lastCommitId = repository.resolve(Constants.HEAD);
			
			RevWalk walk = new RevWalk(repository);
			RevCommit commit = walk.parseCommit(lastCommitId); 
			
			walk.markStart(commit);
						
			RevCommit postCommit = commit;
			for(RevCommit prevCommit: walk){
				if(prevCommit == postCommit){
					continue;
				}
				
				// return the code change in terms of code syntax 
				ArrayList<FileChange> fileChangeList = analyzeJavaFileChanges(prevCommit, postCommit, repository);
				
				
			}
			walk.close();
			repository.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	private ArrayList<FileChange> analyzeJavaFileChanges(
			RevCommit prevCommit, RevCommit postCommit, Repository repository) throws Exception {
		ObjectId prevId = prevCommit.getTree().getId();
		ObjectId postId = postCommit.getTree().getId();
		
		ArrayList<FileChange> fileChangeList = new ArrayList<>();
		ObjectReader reader = repository.newObjectReader();
		CanonicalTreeParser prevTree = new CanonicalTreeParser();
		prevTree.reset(reader, prevId);
		CanonicalTreeParser postTree = new CanonicalTreeParser();
		postTree.reset(reader, postId);
				
		// Use a DiffFormatter to compare new and old tree and return a list of changes
		DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
		diffFormatter.setRepository(repository);
		diffFormatter.setContext(0);
		List<DiffEntry> entries = diffFormatter.scan(prevTree, postTree);
		for(DiffEntry entry: entries){
			FileChange fileChange;
			if(entry.getOldPath().equals(entry.getNewPath())){
				fileChange = new FileChange(entry.getOldPath(), entry.getNewPath(), ChangeType.MODIFY);
			}
			else{
				fileChange = new FileChange(entry.getOldPath(), entry.getNewPath(), entry.getChangeType());
			}
			
			if(fileChange.getNewPath().endsWith("java") && fileChange.getOldPath().endsWith("java")){
				ArrayList<CodeChange> codeChanges = new ArrayList<>();
				
				FileHeader fileHeader = diffFormatter.toFileHeader( entry );
				List<? extends HunkHeader> hunks = fileHeader.getHunks();
				for(HunkHeader hunk: hunks){
					EditList editList = hunk.toEditList();
					for(Edit edit: editList){
						TreeWalk prevWalk = TreeWalk.forPath(repository, fileChange.getOldPath(), prevCommit.getTree());
						TreeWalk postWalk = TreeWalk.forPath(repository, fileChange.getNewPath(), postCommit.getTree());
						
						ArrayList<SeqMultiset> diffList = generateCodeDiff(repository, fileChange, edit, prevWalk, postWalk);
						
						//TODO parse changes into codeChanges
						
						
						System.currentTimeMillis();
					}
				}
				
				
				FileChange existingChange = findFileChange(fileChange, fileChangeList);
				if(null == existingChange){
					fileChangeList.add(fileChange);	
					existingChange = fileChange;
				}
				
				existingChange.setCodeChangeList(codeChanges);
			}
		}
		
		diffFormatter.close();
		
		return fileChangeList;
	}

	private ArrayList<SeqMultiset> generateCodeDiff(Repository repository,
			FileChange fileChange, Edit edit, TreeWalk prevWalk,
			TreeWalk postWalk) throws IOException, Exception {
		CloneSet cloneSet = new CloneSet("0");
		
		CloneInstance instanceA = new CloneInstance(fileChange.getOldPath()+"_prev", edit.getBeginA(), edit.getEndA()+1);
		String prevContent = parseFileContent(prevWalk, repository, instanceA.getStartLine(), instanceA.getEndLine());
		instanceA.setFileContent(prevContent);
		instanceA.setSet(cloneSet);
		
		CloneInstance instanceB = new CloneInstance(fileChange.getNewPath()+"_post", edit.getBeginB(), edit.getEndB()+1);
		String postContent = parseFileContent(postWalk, repository, instanceB.getStartLine(), instanceB.getEndLine());
		instanceB.setFileContent(postContent);
		instanceB.setSet(cloneSet);
		
		cloneSet.addInstance(instanceA);
		cloneSet.addInstance(instanceB);
		ArrayList<SeqMultiset> diffList = new SeqMCIDiff().diff(cloneSet, null);
		
		System.currentTimeMillis();
		
		return diffList;
	}
	
	private String parseFileContent(TreeWalk prevWalk, Repository repository, int startLine, int endLine) throws IOException {
		ObjectId objId = prevWalk.getObjectId(0);
		ObjectLoader loader = repository.open(objId);
		
		InputStream stream = loader.openStream();
		byte[] byteArray = IOUtils.toByteArray(stream);
		
		String content = new String(byteArray, "UTF-8");
		
		//int firstIndex = Utils.nthOccurrence(content, "\n", startLine-1);
		//int endIndex = Utils.nthOccurrence(content, "\n", endLine);
		
		
		return content;
	}
	
	private FileChange findFileChange(FileChange change, ArrayList<FileChange> list){
		for(FileChange c: list){
			if(c.equals(change)){
				return c;
			}
		}
		return null;
	}
}
