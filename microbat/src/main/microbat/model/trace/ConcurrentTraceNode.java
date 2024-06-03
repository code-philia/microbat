package microbat.model.trace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import microbat.instrumentation.model.id.ThreadId;
import microbat.model.BreakPoint;
import microbat.model.BreakPointValue;
import microbat.model.UserInterestedVariables;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.util.JavaUtil;
import microbat.views.ReferenceAnnotation;

/**
 * Represents a concurrent trace node
 * 
 * Adds additional logic for determining dependencies
 * @author Gabau
 *
 */
public class ConcurrentTraceNode extends TraceNode {
	
	public static TraceNode getIniTraceNode(TraceNode traceNode) {
		if (traceNode instanceof ConcurrentTraceNode) {
			return ((ConcurrentTraceNode) traceNode).getInitialTraceNode();
		}
		return traceNode;
	}
	
	/**
	 * Obtains the original trace nodes given a list
	 * of a mix of concurrent trace nodes and trace nodes
	 * @param traceNodes
	 * @return
	 */
	public static List<TraceNode> convert(List<TraceNode> traceNodes) {
		List<TraceNode> result = new LinkedList<>();
		for (TraceNode traceNode: traceNodes) {
			if (result instanceof ConcurrentTraceNode) {
				result.add(((ConcurrentTraceNode) traceNode).getInitialTraceNode());
			} else {
				result.add(traceNode);
			}
		}
		return result;
	}
	
	
	/**
	 * The index of the trace that this trace node is in
	 */
	protected int currentTraceId = 0;
	protected boolean isAtomic = false;
	protected TraceNode initialTraceNode;
	// the sequential order it is in
	protected int concurrentOrder;
	/**
	 * The thread id
	 */
	protected long threadId = 0;
	private ConcurrentTrace concurrentTrace;
	
	public void setConcurrentTrace(ConcurrentTrace concurrentTrace) {
		this.concurrentTrace = concurrentTrace;
	}
	
	public ConcurrentTrace getConcurrentTrace() {
		return this.concurrentTrace;
	}
	
	public long getCurrentThread() {
		return initialTraceNode.getTrace().getThreadId();
	}
	
	

	
	@Override
	public ConcurrentTraceNode getBound() {
		return this;
	}




	@Override
	public TraceNode getStepInNext() {
		return initialTraceNode.getStepInNext();
	}




	@Override
	public TraceNode getStepInPrevious() {
		return initialTraceNode.getStepInPrevious();
	}




	@Override
	public TraceNode getStepOverNext() {
		return initialTraceNode.getStepOverNext();
	}




	@Override
	public TraceNode getStepOverPrevious() {
		return initialTraceNode.getStepOverPrevious();
	}



	/**
	 * Returns the order this node is in in the global order.
	 */
	@Override
	public int getOrder() {
		return concurrentOrder;
	}
	
	/**
	 * Returns the order of this trace node within the trace
	 * @return
	 */
	public int getInitialOrder() {
		return initialTraceNode.getOrder();
	}
	
	@Override
	public void setOrder(int concurrentOrder) {
		this.concurrentOrder = concurrentOrder;
	}
	
	
	/**
	 * Pointer to the trace node which writes to this trace node / reads to this trace node
	 * nullable
	 */
	private ConcurrentTraceNode linkedTraceNode;
	public int getCurrentTraceId() {
		return currentTraceId;
	}

	/** 
	 * Link the read and write trace nodes
	 * @return
	 */
	public ConcurrentTraceNode getLinkedTraceNode() {
		return linkedTraceNode;
	}
	
	
	public ConcurrentTraceNode(BreakPoint breakPoint, BreakPointValue programState, int order, Trace trace,
			String bytecode, long threadId) {
		super(breakPoint, programState, order, trace, bytecode);
		this.threadId = threadId;
		// TODO Auto-generated constructor stub
	}
	
	public ConcurrentTraceNode(TraceNode node, int currentTraceId, long threadId) {
		super(node.getBreakPoint(), node.getProgramState(), 
				node.getOrder(), node.getTrace(), node.getBytecode());
		this.setTimestamp(node.getTimestamp());
		this.currentTraceId = currentTraceId;
		this.threadId = threadId;
	}
	
	/**
	 * Due to the way concurrency works, we separate the read and the
	 * writes, unless there is a synchronisation step.
	 * @return
	 */
	public boolean isWrite() {
		return this.writtenVariables.size() > 0;
	}
	
	public boolean isAtomic() {
		return isAtomic;
	}


	/**
	 * This implementation currently means that if this
	 * node only has write, we get the written VarValues that match the selection.
	 * 
	 */
	@Override
	public List<VarValue> getWrongReadVars(UserInterestedVariables interestedVariables) {
		if (this.isAtomic()) {
			return super.getWrongReadVars(interestedVariables);
		}
		if (this.isWrite()) {
			
			List<VarValue> vars = new ArrayList<>();
			for(VarValue var: getWrittenVariables()){
				if(interestedVariables.contains(var)){
					vars.add(var);
				}
				List<VarValue> children = var.getAllDescedentChildren();
				for(VarValue child: children){
					if(interestedVariables.contains(child)){
						if(!vars.contains(child)){
							vars.add(child);
						}
					}
				}
			}
			
			return vars;
		}
		return super.getWrongReadVars(interestedVariables);
	}

	public TraceNode getInitialTraceNode() {
		return this.initialTraceNode;
	}
	
//
//	@Override
//	public TraceNode findProducer(VarValue varValue, TraceNode startNode) {
//
//		// check if it is on heap
//		String s = varValue.getAliasVarID();
//		boolean isLocalVar = varValue.isLocalVariable();
//		boolean isOnHeap = !isLocalVar;
//		if (!(startNode instanceof ConcurrentTraceNode)) {
//			throw new RuntimeException("Wrong checking node type");
//		}
//		ConcurrentTraceNode concNode = (ConcurrentTraceNode) startNode;
//		int j = concNode.getOrder();
//		if (!concNode.isAtomic() && concNode.getWrittenVariables().size() > 0) {
//			return concNode.getLinkedTraceNode();
//		}
//		
//		// find the individual trace to use the find data dependenccy
//		String varID = Variable.truncateSimpleID(varValue.getVarID());
//		String headID = Variable.truncateSimpleID(varValue.getAliasVarID());
//		
//			
//		for(int i=j-2; i>=1; i--) {
//			ConcurrentTraceNode node = this.getSequentialTrace().get(i);
//			for(VarValue writtenValue: node.getWrittenVariables()) {
//				
//				if (isOnHeap && writtenValue.getAliasVarID() != null
//						&& writtenValue.getAliasVarID().equals(varValue.getAliasVarID())) {
//					return node;
//				}
//				if (!isOnHeap && concNode.getCurrentThread() != node.getCurrentThread()) {
//					// when the value is on a different thread
//					continue;
//				}
//				
//				String wVarID = Variable.truncateSimpleID(writtenValue.getVarID());
//				String wHeadID = Variable.truncateSimpleID(writtenValue.getAliasVarID());
//				
//				if(wVarID != null && wVarID.equals(varID) && node.getCurrentTraceId() 
//						== concNode.getCurrentTraceId()) {
//					return node;						
//				}
//				
//				if(wHeadID != null && wHeadID.equals(headID)) {
//					return node;
//				}
//				
//				VarValue childValue = writtenValue.findVarValue(varID, headID);
//				if(childValue != null) {
//					return node;
//				}
//				
//			}
//		}
//		return null;
//	}
//
//
	
	
	
	/**
	 * Split a given trace node into read and write concurrent
	 * @param node
	 * @return
	 */
	public static List<ConcurrentTraceNode> splitTraceNode(TraceNode node, int traceId, long threadId) {
		ArrayList<ConcurrentTraceNode> result = new ArrayList<>();
		ConcurrentTraceNode r = new ConcurrentTraceNode(node, traceId, threadId);
		node.setBoundTraceNode(r);
		r.setReadVariables(node.getReadVariables());
		r.setWrittenVariables(node.getWrittenVariables());
		r.initialTraceNode = node;
		r.isAtomic = true;
		return List.of(r);
		
//		if (node.getReadVariables().size() == 0 && node.getWrittenVariables().size() == 0) {
//			ConcurrentTraceNode r = new ConcurrentTraceNode(node, threadId);
//			node.setConcurrentTraceNode(r);
//			return List.of(r);
//		}
//		if (node.getReadVariables().size() > 0) {
//			ConcurrentTraceNode readVariables = new ConcurrentTraceNode(node, threadId);
//			readVariables.setReadVariables(node.getReadVariables());
//			node.setConcurrentTraceNode(readVariables);
//			result.add(readVariables);
//		}
//		if (node.getWrittenVariables().size() > 0) {
//			ConcurrentTraceNode writeVariables = new ConcurrentTraceNode(node, threadId);
//			writeVariables.setWrittenVariables(node.getWrittenVariables());
//			result.add(writeVariables);
//			if (result.size() == 2) {
//				result.get(1).linkedTraceNode = result.get(0);
//				result.get(0).linkedTraceNode = result.get(1);
//			}
//		}
//		return result;
	}

	@Override
	public TraceNode getInvocationMethodOrDominator() {
		TraceNode invcationParent = super.getInvocationMethodOrDominator();
		if (invcationParent != null) return invcationParent;
		ConcurrentTrace concTrace = this.getConcurrentTrace();
		Trace currTrace = this.initialTraceNode.getTrace();
		ThreadId threadId = currTrace.getInnerThreadId();
		if (threadId == null) return null;
		if (threadId.getSpawnOrder() != -1) {
			Trace parentTrace = concTrace.getCorrespondingTrace(threadId.getParent());
			if (parentTrace != null) return parentTrace.getTraceNode(threadId.getSpawnOrder());
		}
		
		// find the node which invoked this thread
		return null;
		
	}

	@Override
	public TraceNode getDataDominator(VarValue readVar) {
		return this.getConcurrentTrace().findDataDependency(this, readVar);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Current threadId: " + this.currentTraceId + " " + super.toString();
	}
	
	
	
	
	
}