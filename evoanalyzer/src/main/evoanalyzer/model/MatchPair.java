package evoanalyzer.model;

/**
 * This class is used to specify the instance of why two matches are relevant.
 * Note that two matches should origin from different refactoring commit.
 * @author ly
 *
 */
public class MatchPair {
	private CodeChangeMatch matchA;
	private CodeChangeMatch matchB;
	
	public MatchPair(CodeChangeMatch matchA, CodeChangeMatch matchB) {
		super();
		this.matchA = matchA;
		this.matchB = matchB;
	}
	
	public CodeChangeMatch getMatchA() {
		return matchA;
	}
	public void setMatchA(CodeChangeMatch matchA) {
		this.matchA = matchA;
	}
	public CodeChangeMatch getMatchB() {
		return matchB;
	}
	public void setMatchB(CodeChangeMatch matchB) {
		this.matchB = matchB;
	}
	
	
}
