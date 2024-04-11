package microbat.views.utils.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import microbat.Querier;
import microbat.QueryRequestGenerator;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class DependencyRecovery {

	public DependencyRecovery() {

	}

	public boolean recoverWrongVar(final Trace trace, final TraceNode currentNode, final VarValue wrongValue) {
		
		// find dependency order
		int nowOrder = currentNode.getOrder();
		int lastOrder;
		
		TraceNode lastNode = trace.findDataDependency(currentNode, wrongValue);
		if (lastNode == null) {
			lastOrder = 0;
		}
		else{
			lastOrder = lastNode.getOrder();
		}
		
		//find all related node
		List<TraceNode> relatedNodes = new ArrayList<>();
		TraceNode node = new TraceNode();;
		for (int i = nowOrder - 1 ; i > lastOrder ; i--) {
			node = trace.getTraceNode(i);
			for (VarValue readVariable : node.getReadVariables()) {
				if (readVariable.getVarID() == wrongValue.getVarID()) {
					relatedNodes.add(node);
					break;
				}
			}
			
		}

		//ask GPT
		Querier querier = new Querier(false);
		String query = QueryRequestGenerator.getQueryRequest(relatedNodes, wrongValue);
		String respones = querier.getDependency(query);
		boolean[] recoveriedWrittenVariables = QueryRequestGenerator.getResult(relatedNodes.size(), respones);
		
		//add dependency
		for (int i = 0; i < relatedNodes.size(); i++) {
			if(recoveriedWrittenVariables[i]) {
				node =  relatedNodes.get(i);
				node.addWrittenVariable(wrongValue);
				trace.setTraceNode(node.getOrder(), node);
			}
		}
		
		
		return true;

	}
	
};
