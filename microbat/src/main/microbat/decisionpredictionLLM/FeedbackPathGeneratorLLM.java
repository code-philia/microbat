package microbat.decisionpredictionLLM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import microbat.debugpilot.pathfinding.FeedbackPath;
import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.decisionpredictionLLM.DecisionPredictorLLM;
import microbat.model.trace.TraceNode;
import microbat.util.TraceUtil;
import microbat.views.PathView;

public class FeedbackPathGeneratorLLM {
	
	public List<FeedbackPath> generateFeedbackPath(List<DPUserFeedback> availableFeedbacks) {
		
		DecisionPredictorLLM decisionPredictor = new DecisionPredictorLLM();
		
		// confirmed steps
		availableFeedbacks.forEach(f->f.setConfirmed(true));
		FeedbackPath mustFollowPath = new FeedbackPath(availableFeedbacks);

		// last step of the confirmed path
		DPUserFeedback lastFeedback = availableFeedbacks.get(availableFeedbacks.size()-1);
		
		// store step to visit
		Stack<DPUserFeedback> stack = new Stack<DPUserFeedback>();
		
		// store construct paths
		List<FeedbackPath> paths = new ArrayList<>();
		
		// next nodes according to the confirmation
		Set<TraceNode> nextNodes = TraceUtil.findAllNextNodes(lastFeedback);
		for(TraceNode nextNode: nextNodes) {
			DPUserFeedback feedBackNode = new DPUserFeedback(DPUserFeedbackType.UNCLEAR, nextNode);
			feedBackNode.setParent(lastFeedback);
			stack.push(feedBackNode);
		}
		
		int step_count = 0;
		while(!stack.empty() && step_count < 20) {
			DPUserFeedback curStep = stack.pop();
			// prediction for current step
			boolean succeed = decisionPredictor.predictDecision(curStep);
			if(!succeed) { // an error happened when asking gpt, keep previous debugging plan
				return null;
			}
			step_count += 1;
			
			Set<TraceNode> allNextNodes = TraceUtil.findAllNextNodes(curStep);
			
			// reach an end
			if(allNextNodes.isEmpty()) {
				FeedbackPath newPath = constructOnePath(mustFollowPath,curStep);
				paths.add(newPath);
			}
			else {
				for(TraceNode n : allNextNodes) {
					DPUserFeedback feedBackNode = new DPUserFeedback(DPUserFeedbackType.UNCLEAR, n);
					feedBackNode.setParent(curStep);
					stack.push(feedBackNode);
				}
			}
		}
		
		if(paths.isEmpty()) {
			paths.add(mustFollowPath);
		}
		
		return paths;
	}

	public List<FeedbackPath> generateFeedbackPath(TraceNode currentNode) {
		
		DecisionPredictorLLM decisionPredictor = new DecisionPredictorLLM();

		// store step to visit
		Stack<DPUserFeedback> stack = new Stack<DPUserFeedback>();
		
		// store construct paths
		List<FeedbackPath> paths = new ArrayList<>();

		DPUserFeedback feedBackNode = new DPUserFeedback(DPUserFeedbackType.UNCLEAR, currentNode);
		stack.push(feedBackNode);
		
		int step_count = 0;
		while(!stack.empty() && step_count < 20) {
			DPUserFeedback curStep = stack.pop();
			// prediction for current step
			boolean succeed = decisionPredictor.predictDecision(curStep);
			if(!succeed) { // an error happened when asking gpt, keep previous debugging plan
				return null;
			}
			step_count += 1;

			Set<TraceNode> allNextNodes = TraceUtil.findAllNextNodes(curStep);
			
			// reach an end
			if(allNextNodes.isEmpty()) {				
				FeedbackPath newPath = constructOnePath(curStep);
				paths.add(newPath);
			}
			else {
				for(TraceNode n : allNextNodes) {
					DPUserFeedback fbNode = new DPUserFeedback(DPUserFeedbackType.UNCLEAR, n);
					fbNode.setParent(curStep);
					stack.push(fbNode);
				}
			}
		}
		
		return paths;
	}
	
	private FeedbackPath constructOnePath(FeedbackPath mustFollowPath, DPUserFeedback curStep) {
		List<DPUserFeedback> unConfirmedList = new ArrayList<DPUserFeedback>();
		for(DPUserFeedback fb = curStep;fb!=null && fb.isConfirmed()==false;fb = fb.getParent()) {
			unConfirmedList.add(fb);
		}
		Collections.reverse(unConfirmedList);
		FeedbackPath unConfirmedPath = new FeedbackPath(unConfirmedList);

		unConfirmedPath.getFeedbacks().forEach(f->f.setConfirmed(false));
		
		return FeedbackPath.concat(mustFollowPath, unConfirmedPath);
	}
	
	private FeedbackPath constructOnePath(DPUserFeedback curStep) {
		List<DPUserFeedback> unConfirmedList = new ArrayList<DPUserFeedback>();
		for(DPUserFeedback fb = curStep;fb!=null && fb.isConfirmed()==false;fb = fb.getParent()) {
			unConfirmedList.add(fb);
		}
		Collections.reverse(unConfirmedList);
		FeedbackPath unConfirmedPath = new FeedbackPath(unConfirmedList);

		unConfirmedPath.getFeedbacks().forEach(f->f.setConfirmed(false));
		
		return unConfirmedPath;
	}
}
