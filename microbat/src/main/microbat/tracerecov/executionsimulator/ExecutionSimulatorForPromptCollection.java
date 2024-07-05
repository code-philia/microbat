package microbat.tracerecov.executionsimulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.varskeleton.VarSkeletonBuilder;
import microbat.tracerecov.varskeleton.VariableSkeleton;

public class ExecutionSimulatorForPromptCollection extends ExecutionSimulator {
	
	public ExecutionSimulatorForPromptCollection() {
		this.logger = new ExecutionSimulationFileLogger();
	}

	@Override
	public void expandVariable(VarValue selectedVar, TraceNode step) throws IOException {

		if (selectedVar.isExpanded()) {
			return;
		}

		List<VariableSkeleton> variableSkeletons = new ArrayList<>();

		/*
		 * Expand the selected variable.
		 */
		VariableSkeleton parentSkeleton = VarSkeletonBuilder.getVariableStructure(selectedVar.getType(),
				step.getTrace().getAppJavaClassPath());
		variableSkeletons.add(parentSkeleton);

		// assume var layer == 1, then only elementArray will be recorded in ArrayList
		if (!selectedVar.getChildren().isEmpty()) {
			VarValue child = selectedVar.getChildren().get(0);
			String childType = child.getType();
			if (childType.contains("[]")) {
				childType = childType.substring(0, childType.length() - 2); // remove [] at the end
			}
			VariableSkeleton childSkeleton = VarSkeletonBuilder.getVariableStructure(childType,
					step.getTrace().getAppJavaClassPath());
			variableSkeletons.add(childSkeleton);
		}

		String background = VariableExpansionUtils.getBackgroundContent();
		String content = VariableExpansionUtils.getQuestionContent(selectedVar, variableSkeletons, step);

		this.logger.printInfoBeforeQuery("Variable Expansion", selectedVar, step, background + content);

//		for (int i = 0; i < 2; i++) {
//			try {
//				String response = sendRequest(background, content);
//				this.logger.printResponse(i, response);
//				VariableExpansionUtils.processResponse(selectedVar, response);
//				break;
//			} catch (org.json.JSONException | java.lang.StringIndexOutOfBoundsException e) {
//				this.logger.printError(e.getMessage());
//			}
//		}
	}
	
}
