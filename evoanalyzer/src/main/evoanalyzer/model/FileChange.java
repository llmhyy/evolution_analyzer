package evoanalyzer.model;

import java.util.ArrayList;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

public class FileChange {
	private String oldPath;
	private String newPath;
	private ChangeType type;
	
	private ArrayList<CodeChange> codeChangeList = new ArrayList<>();
	
	public FileChange(String oldPath, String newPath, ChangeType type) {
		super();
		this.oldPath = oldPath;
		this.newPath = newPath;
		this.type = type;
	}
	
	public String toString(){
		return type + ": " + newPath; 
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((newPath == null) ? 0 : newPath.hashCode());
		result = prime * result + ((oldPath == null) ? 0 : oldPath.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileChange other = (FileChange) obj;
		if (newPath == null) {
			if (other.newPath != null)
				return false;
		} else if (!newPath.equals(other.newPath))
			return false;
		if (oldPath == null) {
			if (other.oldPath != null)
				return false;
		} else if (!oldPath.equals(other.oldPath))
			return false;
		if (type != other.type)
			return false;
		return true;
	}



	public String getOldPath() {
		return oldPath;
	}

	public void setOldPath(String oldPath) {
		this.oldPath = oldPath;
	}

	public String getNewPath() {
		return newPath;
	}

	public void setNewPath(String newPath) {
		this.newPath = newPath;
	}

	public ChangeType getType() {
		return type;
	}

	public void setType(ChangeType type) {
		this.type = type;
	}

	public ArrayList<CodeChange> getCodeChangeList() {
		return codeChangeList;
	}

	public void setCodeChangeList(ArrayList<CodeChange> codeChangeList) {
		this.codeChangeList = codeChangeList;
	}
	
	
}
