package mcidiff.util;

import mcidiff.model.Token;

import org.eclipse.jdt.core.dom.ASTNode;

public class TokenSimilarityComparator{
	public double compute(Token token1, Token token2){
		
		if(token1.isEpisolon() && token2.isEpisolon()){
			return 1;
		}
		
		ASTNode node1 = token1.getNode();
		ASTNode node2 = token2.getNode();
		
		if(node1 != null && node2 != null){
			
			double positionSim = 1-Math.abs(token1.getRelativePositionRatio() - token2.getRelativePositionRatio());
			
			double textualSim = 0;
			double contextualSim = 0;
			
			// the idea is that if the two to-be-compared synonym tokens are very far way considering
			// their relative position, they are highly likely to be unmatched
			if(positionSim > GlobalSettings.relativeThreshold){
				textualSim = FastASTNodeComparator.computeNodeSim(token1.getNode(), token2.getNode());
				
				contextualSim = textualSim;
				
				if(!isGodParent(node1) && !isGodParent(node2)){
					contextualSim = FastASTNodeComparator.computeNodeSim(node1.getParent(), node2.getParent());
				}				
			}
			else{
				System.currentTimeMillis();
			}
			
			//double avgWeight = (1.0)/3;
			return 0.5*contextualSim + 0.4*textualSim + 0.0*positionSim;
		}
		
		return 0;
	};	
	
	private boolean isGodParent(ASTNode child){
		ASTNode parent = child.getParent();
		if(parent == null){
			return true;
		}
		else{
			int parentLen = parent.getLength();
			int childLen = child.getLength();
		
			return parentLen > GlobalSettings.godParentRatio*childLen;
		}
	}
}
