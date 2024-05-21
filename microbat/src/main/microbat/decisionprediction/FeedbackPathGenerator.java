package microbat.decisionprediction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import microbat.debugpilot.pathfinding.FeedbackPath;
import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.TraceUtil;

public class FeedbackPathGenerator {
	
	public FeedbackPath generateFeedbackPath(List<DPUserFeedback> availableFeedbacks) {
		
		DecisionPredictor decisionPredictor = new DecisionPredictor();
		 
		// confirmed steps
		FeedbackPath mustFollowPath = new FeedbackPath(availableFeedbacks);
		availableFeedbacks.forEach(f->f.setReachPossibility(1));
		availableFeedbacks.forEach(f->f.setConfidence(1));

		// last step of the confirmed path
		DPUserFeedback lastFeedback = availableFeedbacks.get(availableFeedbacks.size()-1);
		
		// store step to visit
		Stack<DPUserFeedback> stack = new Stack<DPUserFeedback>();
		
		// next nodes according to the confirmation
		Set<TraceNode> nextNodes = TraceUtil.findAllNextNodes(lastFeedback);
		for(TraceNode nextNode: nextNodes) {
			DPUserFeedback feedBackNode = new DPUserFeedback(DPUserFeedbackType.UNCLEAR, nextNode);
			feedBackNode.setParentType(lastFeedback.getType());
			feedBackNode.setParent(lastFeedback);
			feedBackNode.setReachPossibility(lastFeedback.getReachPossibility());

			stack.push(feedBackNode);
		}
		
		// explore data and control dependencies and select end node of debugging plan
		DPUserFeedback tempEnd = null;
		double maxConfidence = 0;
		
		while(!stack.empty()) {
			DPUserFeedback curStep = stack.pop();
			
			// prediction for current step
			try {
				decisionPredictor.predictDecision(curStep);
			} catch (IOException e) {
				System.out.println("--ERROR-- IOException in predicting decision...");
				e.printStackTrace();
			}
			
			Set<DPUserFeedback> allNextFeedbackNodes = getAllNextFeedbackNodes(curStep);
			
			if(allNextFeedbackNodes.isEmpty()) {
				// end of a path, check debug step along the path
				for(DPUserFeedback f = curStep;f!=null && f.isConfirmed()==false;f=f.getParent()) {
					// correct
					if(f.getReachPossibility()*f.getPrediction()[1] > maxConfidence) {
						maxConfidence = f.getReachPossibility()*f.getPrediction()[1];
						tempEnd = f;
						f.setType(DPUserFeedbackType.CORRECT);
						System.out.println("New maxConfidence: "+maxConfidence);
					}
					// root
					if(f.getReachPossibility()*f.getPrediction()[0] > maxConfidence) {
						maxConfidence = f.getReachPossibility()*f.getPrediction()[0];
						tempEnd = f;
						f.setType(DPUserFeedbackType.ROOT_CAUSE);
						System.out.println("New maxConfidence: "+maxConfidence);
					}
				}
			}
			else {
				for(DPUserFeedback f : allNextFeedbackNodes) {
					System.out.println(curStep.getNode().getOrder()+"-->"+f.getNode().getOrder());
					stack.push(f);
				}
			}
		}
		
		// construct path
		if(tempEnd == null) {
			mustFollowPath.getFeedbacks().forEach(f->f.getNode().confirmed = true);
			return mustFollowPath;
		}
		List<DPUserFeedback> unConfirmedList = new ArrayList<DPUserFeedback>();
		for(DPUserFeedback fb = tempEnd;fb!=null && fb.isConfirmed()==false;fb = fb.getParent()) {
			fb.setConfidence();
			DPUserFeedback p = fb.getParent();
			if(p!=null && p.isConfirmed()==false) {
				p.setType(fb.getParentType());
			}
			unConfirmedList.add(fb);
		}
		Collections.reverse(unConfirmedList);
		FeedbackPath unConfirmedPath = new FeedbackPath(unConfirmedList);

		mustFollowPath.getFeedbacks().forEach(f->f.getNode().confirmed = true);
		unConfirmedPath.getFeedbacks().forEach(f->f.getNode().confirmed = false);
		
		return FeedbackPath.concat(mustFollowPath, unConfirmedPath);
	}
	
	
	private Set<DPUserFeedback> getAllNextFeedbackNodes(DPUserFeedback curStep) {
		TraceNode node = curStep.getNode();
		Trace trace = node.getTrace();
		Set<DPUserFeedback> allNextFeedbackNodes = new HashSet<>();
		
		// Control dependency
		if(node.getControlDominator() != null) {
			DPUserFeedback fb = new DPUserFeedback(DPUserFeedbackType.UNCLEAR,node.getControlDominator());
			fb.setParent(curStep);
			fb.setParentType(DPUserFeedbackType.WRONG_PATH);
			fb.setReachPossibility(curStep.getReachPossibility()*curStep.getPrediction()[2]);
			allNextFeedbackNodes.add(fb);
		}
		
		// Data dependency
		List<VarValue> readVariables = node.getReadVariables();
		for(int i = 0;i<readVariables.size();i++) {
			TraceNode dataDom = trace.findDataDependency(node, readVariables.get(i));
			if(dataDom!=null) {
				DPUserFeedback fb = new DPUserFeedback(DPUserFeedbackType.UNCLEAR,dataDom);
				fb.setParent(curStep);
				fb.setParentType(DPUserFeedbackType.WRONG_VARIABLE);
				fb.setReachPossibility(curStep.getReachPossibility()*curStep.getPrediction()[3]*curStep.getVarPrediction()[i][1]);
				allNextFeedbackNodes.add(fb);
			}
		}
		
		return allNextFeedbackNodes;
	}

}
