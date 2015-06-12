package evoanalyzer;

import evoanalyzer.action.EvoluationDiffParser;
import evoanalyzer.util.Settings;


public class Main {
	public static void main(String[] args){
		//MCIDiffGlobalSettings.relativeThreshold = 0.5;
		//MCIDiffGlobalSettings.roughCompare = false;
		
		Settings.ASTNodeSimilarity = 0.8;
		Settings.commitTimeInterval = 72;
		
		EvoluationDiffParser parser = new EvoluationDiffParser();
		parser.parseEvoluationDiffs(args[0]);
		//parser.parseEvoluationDiffs("F:\\git_space\\ATest\\ATest");
	}
}
