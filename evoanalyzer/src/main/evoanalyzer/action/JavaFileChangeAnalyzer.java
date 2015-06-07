package evoanalyzer.action;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mcidiff.main.SeqMCIDiff;
import mcidiff.model.CloneInstance;
import mcidiff.model.CloneSet;
import mcidiff.model.SeqMultiset;
import mcidiff.model.TokenSeq;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import evoanalyzer.model.CodeChange;
import evoanalyzer.model.FileChange;
import evoanalyzer.util.Utils;

public class JavaFileChangeAnalyzer {
	public static String PREV_SUFFIX = "_prev";
	public static String POST_SUFFIX = "_post";
	
	class MemberRetriever extends ASTVisitor{
		private ArrayList<ASTNode> nodeList = new ArrayList<>();
		
		public boolean visit(TypeDeclaration type){
			boolean isInnerClass = false;
			ASTNode parent = type.getParent();
			while(!(parent instanceof CompilationUnit)){
				if(parent instanceof TypeDeclaration){
					isInnerClass = true;
					break;
				}
				parent = parent.getParent();
			}
			
			if(isInnerClass){
				nodeList.add(type);
				return false;
			}
			else{
				return true;
			}
		}
		
		public boolean visit(MethodDeclaration method){
			this.nodeList.add(method);
			return false;
		}
		
		public boolean visit(FieldDeclaration field){
			this.nodeList.add(field);
			return false;
		}
		
		public ArrayList<ASTNode> getMemberList(){
			return this.nodeList;
		}
	}

	public ArrayList<FileChange> analyzeJavaFileChanges(RevCommit prevCommit, RevCommit postCommit,
			Repository repository) throws Exception {
		ObjectId prevId = prevCommit.getTree().getId();
		ObjectId postId = postCommit.getTree().getId();

		ArrayList<FileChange> fileChangeList = new ArrayList<>();
		ObjectReader reader = repository.newObjectReader();
		CanonicalTreeParser prevTree = new CanonicalTreeParser();
		prevTree.reset(reader, prevId);
		CanonicalTreeParser postTree = new CanonicalTreeParser();
		postTree.reset(reader, postId);

		DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
		diffFormatter.setRepository(repository);
		diffFormatter.setContext(0);
		List<DiffEntry> entries = diffFormatter.scan(prevTree, postTree);
		//int count = 0;
		for (DiffEntry entry : entries) {
			FileChange fileChange;
			if (entry.getOldPath().equals(entry.getNewPath())) {
				fileChange = new FileChange(entry.getOldPath(), entry.getNewPath(), ChangeType.MODIFY);
			} else {
				fileChange = new FileChange(entry.getOldPath(), entry.getNewPath(), entry.getChangeType());
			}
			FileChange existingFileChange = findFileChange(fileChange, fileChangeList);
			if (null == existingFileChange) {
				fileChangeList.add(fileChange);
				existingFileChange = fileChange;
			}

			//for rename and modify
			if (fileChange.getNewPath().endsWith("java") && fileChange.getOldPath().endsWith("java")) {
				ArrayList<CodeChange> codeChangeList = parseModifyOrRenameCodeChanges(entry, diffFormatter, repository, existingFileChange, 
						prevCommit, postCommit);
				existingFileChange.getCodeChangeList().addAll(codeChangeList);
			}
			//for add
			else if(fileChange.getNewPath().endsWith("java") && fileChange.getOldPath().equals("/dev/null")){
				ArrayList<CodeChange> codeChangeList = parseAddOrRemoveCodeChanges(repository, existingFileChange, 
						prevCommit, postCommit, CodeChange.ADD);
				existingFileChange.getCodeChangeList().addAll(codeChangeList);
			}
			//for remove
			else if(fileChange.getNewPath().equals("/dev/null") && fileChange.getOldPath().endsWith("java")){
				ArrayList<CodeChange> codeChangeList = parseAddOrRemoveCodeChanges(repository, existingFileChange, 
						prevCommit, postCommit, CodeChange.REMOVE);
				existingFileChange.getCodeChangeList().addAll(codeChangeList);
			}
			
		}

		diffFormatter.close();

		return fileChangeList;
	}
	
	private ArrayList<CodeChange> parseAddOrRemoveCodeChanges(Repository repository, FileChange fileChange, 
			RevCommit prevCommit, RevCommit postCommit, String changeType) throws Exception{
		
		RevCommit commit = changeType.equals(CodeChange.ADD) ? postCommit : prevCommit;
		String path = changeType.equals(CodeChange.ADD) ? fileChange.getNewPath() : fileChange.getOldPath();
		
		TreeWalk walk = TreeWalk.forPath(repository, path, commit.getTree());
		String fileContent = parseFileContent(walk, repository);
		CompilationUnit cu = Utils.parseCompliationUnit(fileContent);
		MemberRetriever retriever = new MemberRetriever();
		
		cu.accept(retriever);
		
		ArrayList<CodeChange> codeChanges = new ArrayList<>();
		ArrayList<ASTNode> memberList = retriever.getMemberList();
		for(ASTNode member: memberList){
			CodeChange codeChange = new CodeChange(prevCommit, postCommit, fileChange, member, changeType);
			codeChanges.add(codeChange);
		}
		
		return codeChanges;
	}
	
	private ArrayList<CodeChange> parseModifyOrRenameCodeChanges(DiffEntry entry, DiffFormatter diffFormatter, Repository repository, FileChange fileChange, 
			RevCommit prevCommit, RevCommit postCommit) throws Exception{
		ArrayList<CodeChange> codeChangeList = new ArrayList<>();

		FileHeader fileHeader = diffFormatter.toFileHeader(entry);
		List<? extends HunkHeader> hunks = fileHeader.getHunks();
		for (HunkHeader hunk : hunks) {
			EditList editList = hunk.toEditList();
			for (Edit edit : editList) {
				TreeWalk prevWalk = TreeWalk.forPath(repository, fileChange.getOldPath(), prevCommit.getTree());
				TreeWalk postWalk = TreeWalk.forPath(repository, fileChange.getNewPath(), postCommit.getTree());

				ArrayList<SeqMultiset> diffList = null; 
				try{
					diffList = generateCodeDiff(repository, fileChange, edit, prevWalk, postWalk);
					
					// parse changes into codeChanges
					ArrayList<CodeChange> codeChanges = identifyMemberLevelChanges(diffList, fileChange, prevCommit, postCommit);
					codeChangeList.addAll(codeChanges);
				}
				catch(Exception e){
					e.printStackTrace();
				}

			}
		}
		
		return codeChangeList;
	}
	
	private ArrayList<CodeChange> identifyMemberLevelChanges(ArrayList<SeqMultiset> diffList, FileChange fileChange, 
			RevCommit prevCommit, RevCommit postCommit){
		ArrayList<CodeChange> codeChangeList = new ArrayList<>();
		for(SeqMultiset diff: diffList){
			if(diff.isGapped()){
				TokenSeq prevSeq = getPrevSeq(diff);
				TokenSeq postSeq = getPostSeq(diff);
				
				String changeType = prevSeq.isEpisolonTokenSeq() ? CodeChange.ADD : CodeChange.REMOVE;
				TokenSeq targetSeq = prevSeq.isEpisolonTokenSeq() ? postSeq : prevSeq;
				
				ArrayList<ASTNode> containedMembers = targetSeq.findContainedMembers();
				for(ASTNode node: containedMembers){
					CodeChange codeChange = new CodeChange(prevCommit, postCommit, fileChange, node, changeType);
					codeChangeList.add(codeChange);
				}
			}
		}
		
		return codeChangeList;
	}

	private TokenSeq getPrevSeq(SeqMultiset diff) {
		TokenSeq seq1 = diff.getSequences().get(0);
		TokenSeq seq2 = diff.getSequences().get(1);
		
		if(seq1.getCloneInstance().getFileName().endsWith(PREV_SUFFIX)){
			return seq1;
		}
		else{
			return seq2;
		}
	}
	
	private TokenSeq getPostSeq(SeqMultiset diff) {
		TokenSeq seq1 = diff.getSequences().get(0);
		TokenSeq seq2 = diff.getSequences().get(1);
		
		if(seq1.getCloneInstance().getFileName().endsWith(POST_SUFFIX)){
			return seq1;
		}
		else{
			return seq2;
		}
	}

	private ArrayList<SeqMultiset> generateCodeDiff(Repository repository, FileChange fileChange, Edit edit,
			TreeWalk prevWalk, TreeWalk postWalk) throws IOException, Exception {
		CloneSet cloneSet = new CloneSet("0");

		CloneInstance instanceA = new CloneInstance(fileChange.getOldPath() + PREV_SUFFIX, edit.getBeginA(),
				edit.getEndA() + 1);
		String prevContent = parseFileContent(prevWalk, repository);
		instanceA.setFileContent(prevContent);
		instanceA.setSet(cloneSet);

		CloneInstance instanceB = new CloneInstance(fileChange.getNewPath() + POST_SUFFIX, edit.getBeginB(),
				edit.getEndB() + 1);
		String postContent = parseFileContent(postWalk, repository);
		instanceB.setFileContent(postContent);
		instanceB.setSet(cloneSet);

		//I skip some very large diff content for efficiency, it is usually caused by encoding problem.
		if(instanceA.getLength() > 80 && instanceB.getLength() > 80){
			return new ArrayList<SeqMultiset>();
		}
		
		cloneSet.addInstance(instanceA);
		cloneSet.addInstance(instanceB);
		ArrayList<SeqMultiset> diffList = new SeqMCIDiff().diff(cloneSet, null);

		System.currentTimeMillis();

		return diffList;
	}

	private String parseFileContent(TreeWalk prevWalk, Repository repository)
			throws IOException {
		ObjectId objId = prevWalk.getObjectId(0);
		ObjectLoader loader = repository.open(objId);

		InputStream stream = loader.openStream();
		byte[] byteArray = IOUtils.toByteArray(stream);

		String content = new String(byteArray, "UTF-8");

		// int firstIndex = Utils.nthOccurrence(content, "\n", startLine-1);
		// int endIndex = Utils.nthOccurrence(content, "\n", endLine);

		return content;
	}

	private FileChange findFileChange(FileChange change, ArrayList<FileChange> list) {
		for (FileChange c : list) {
			if (c.equals(change)) {
				return c;
			}
		}
		return null;
	}
}
