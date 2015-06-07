package evoanalyzer;

import evoanalyzer.action.EvoluationDiffParser;


public class Main {
	public static void main(String[] args){
		//MCIDiffGlobalSettings.relativeThreshold = 0.5;
		//MCIDiffGlobalSettings.roughCompare = false;
		
		EvoluationDiffParser parser = new EvoluationDiffParser();
		parser.parseEvoluationDiffs("F:\\git_space\\eclipse.jdt.ui");
		//parser.parseEvoluationDiffs("F:\\git_space\\ATest\\ATest");
	}
}
