package microbat.instrumentation.instr.aggreplay;

import java.util.List;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.SWAP;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;

import microbat.instrumentation.AgentConstants;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.filter.UserFilters;
import microbat.instrumentation.instr.GeneratedMethods;
import microbat.instrumentation.instr.LocalVariableSupporter;
import microbat.instrumentation.instr.TraceInstrumenter;
import microbat.instrumentation.instr.aggreplay.agents.AggrePlayReplayAgent;
import microbat.instrumentation.instr.instruction.info.ArrayInstructionInfo;
import microbat.instrumentation.instr.instruction.info.FieldInstructionInfo;
import microbat.instrumentation.instr.instruction.info.LineInstructionInfo;
import microbat.instrumentation.instr.instruction.info.LocalVarInstructionInfo;
import microbat.instrumentation.instr.instruction.info.RWInstructionInfo;
import microbat.instrumentation.model.id.AggrePlayMethods;
import microbat.instrumentation.runtime.IExecutionTracer;
import microbat.instrumentation.utils.MicrobatUtils;

public class AggrePlayTraceInstrumenter extends TraceInstrumenter {

	private UserFilters userFilters;
	private int tempVarIdx;
	private Class<?> instrumentationClass = AggrePlayReplayAgent.class;

	public AggrePlayTraceInstrumenter(AgentParams params) {
		super(params);
	}

//	@Override
//	protected byte[] instrument(String classFName, String className, JavaClass jc) {
//		ClassGen cg = new ClassGen(jc);
//		ConstantPoolGen cpg = cg.getConstantPool();
//		for (Method method : cg.getMethods()) {
//			if (method.isNative() || method.isAbstract() || method.getCode() == null) {
//				continue;
//			}
//			MethodGen mg = new MethodGen(method, classFName, cpg);
//			mg.getInstructionList();
//			/* fill up missing variables in localVariableTable */
//			LocalVariableSupporter.fillUpVariableTable(mg, method, cpg);
//			
//			List<LineInstructionInfo> lineInsnInfos = LineInstructionInfo.buildLineInstructionInfos(cg, cpg,
//					mg, method, true, mg.getInstructionList());
//			
//		}
//		return cg.getJavaClass().getBytes();
//	}
	
	
	
	
	
	@Override
	protected byte[] instrument(String classFName, String className, JavaClass jc) {
		ClassGen classGen = new ClassGen(jc);
		ConstantPoolGen constPool = classGen.getConstantPool();
		JavaClass newJC = null;
		boolean entry = entryPoint == null ? false : className.equals(entryPoint.getClassName());
//		boolean isAppClass = GlobalFilterChecker.isAppClass(classFName) || entry;
		boolean isAppClass = true;
		if (userFilters != null && !userFilters.isInstrumentable(className)) {
			return null;
		}
		for (Method method : jc.getMethods()) {
			if (method.isNative() || method.isAbstract() || method.getCode() == null) {
				continue; // Only instrument methods with code in them!
			}
			try {
				MethodGen methodGen = new MethodGen(method, classFName, constPool);
				boolean isMainMethod = false;
				if (entry && entryPoint.matchMethod(method.getName(), method.getSignature())) {
					isMainMethod = true;
				}
				
				boolean isEntry = false;
				if(method.getName().equals("run") && isThread(jc)) {
					isEntry = true;
				}
				
				
				GeneratedMethods generatedMethods = runMethodInstrumentation(classGen, constPool, methodGen, 
						method, isAppClass, isMainMethod, isEntry);
				if (generatedMethods != null) {
					if (doesBytecodeExceedLimit(generatedMethods)) {
						AgentLogger.info(String.format("Warning: %s exceeds bytecode limit!",
								MicrobatUtils.getMicrobatMethodFullName(classGen.getClassName(), method)));
					} else {
						for (MethodGen newMethod : generatedMethods.getExtractedMethods()) {
							newMethod.setMaxStack();
							newMethod.setMaxLocals();
							classGen.addMethod(newMethod.getMethod());
						}
						methodGen = generatedMethods.getRootMethod();
						// All changes made, so finish off the method:
						InstructionList instructionList = methodGen.getInstructionList();
						instructionList.setPositions();
						methodGen.setMaxStack();
						methodGen.setMaxLocals();
						classGen.replaceMethod(method, methodGen.getMethod());
					}
				}
				newJC = classGen.getJavaClass();
				newJC.setConstantPool(constPool.getFinalConstantPool());
			} catch (Exception e) {
				String message = e.getMessage();
				if (e.getMessage() != null && e.getMessage().contains("offset too large")) {
					message = "offset too large";
				}
				AgentLogger.info(String.format("Warning: %s [%s]",
						MicrobatUtils.getMicrobatMethodFullName(classGen.getClassName(), method), message));
				AgentLogger.error(e);
				e.printStackTrace();
			}
		}
		if (newJC != null) {
			byte[] data = newJC.getBytes();
			return data;
		}

		return null;
	}




	@Override
	protected InstructionList getInjectCodePutField(ConstantPoolGen constPool, LocalVariableGen tracerVar,
			FieldInstructionInfo info, LocalVariableGen classNameVar, LocalVariableGen methodSigVar) {
		InstructionList result = new InstructionList();
		result.append(new SWAP()); // val, obj
		result.append(new DUP()); // val, obj, obj
		result.append(new LDC(constPool.addString(info.getFieldName()))); // v, o, o, s
		result.append(AggrePlayMethods.BEFORE_OBJECT_WRITE.toInvokeStatic(constPool, instrumentationClass)); // v, o
		result.append(new SWAP()); // o, v
		result.append(super.getInjectCodePutField(constPool, tracerVar, info, classNameVar, methodSigVar));
		return result;
	}

	@Override
	protected InstructionList getInjectCodeGetField(ConstantPoolGen constPool, LocalVariableGen tracerVar,
			FieldInstructionInfo info, LocalVariableGen classNameVar, LocalVariableGen methodSigVar) {
		InstructionList beforeGetFieldInstructionList = new InstructionList();
		InstructionList afterGetFieldInstructionList = new InstructionList();
		// obj
		beforeGetFieldInstructionList.append(new DUP());
		// obj, obj
		beforeGetFieldInstructionList.append(new LDC(constPool.addString(info.getFieldName())));
		beforeGetFieldInstructionList.append(AggrePlayMethods.BEFORE_OBJECT_READ.toInvokeStatic(constPool, instrumentationClass));
		afterGetFieldInstructionList.append(AggrePlayMethods.AFTER_OBJECT_READ.toInvokeStatic(constPool, instrumentationClass));
		InstructionList getFieldInstructions = 
				super.getInjectCodeGetField(constPool, tracerVar, info, classNameVar, methodSigVar);
		beforeGetFieldInstructionList.append(getFieldInstructions);
		beforeGetFieldInstructionList.append(afterGetFieldInstructionList);
		return beforeGetFieldInstructionList;
		// return super.getInjectCodeGetField(constPool, tracerVar, info, classNameVar, methodSigVar);
	}
	
	
	

	

	private InstructionList afterWrite(ConstantPoolGen cpg) {
		InstructionList result = new InstructionList();
		INVOKESTATIC afterPutFieldInvokestatic = 
				AggrePlayMethods.AFTER_OBJECT_WRITE.toInvokeStatic(cpg, instrumentationClass);
		result.append(afterPutFieldInvokestatic);
		return result;
	}
	

	
	protected InstructionList appendFieldInstructions(FieldInstructionInfo info, ConstantPoolGen cpg) {
		// only need to deal with putfield and putstatic
		Instruction instruction = info.getInstruction();
		if (instruction.getOpcode() == Const.PUTFIELD) {
			return afterWrite(cpg);
		} 
		if (instruction.getOpcode() == Const.PUTSTATIC) {
			return afterWrite(cpg);
		}
		return null;
	}
	
	// TODO:
	/**
	 * Currently store instruction, i.e. write instructions, we only instrument the before stage,
	 * We do not instrument the after stage -> should modify to instrument the after stage
	 * with the minimum amount of change.
	 * TODO: reduce the amount of change in this code
	 */
	@Override
	protected boolean instrumentMethod(ClassGen classGen, ConstantPoolGen constPool, MethodGen methodGen, Method method,
			boolean isAppClass, boolean isMainMethod, boolean isEntry) {
		// TODO Auto-generated method stub
		if (userFilters != null && !userFilters.isInstrumentable(classGen.getClassName(), method, methodGen.getLineNumbers())) {
			return false;
		}
		tempVarIdx = 0;
		InstructionList insnList = methodGen.getInstructionList();
		InstructionHandle startInsn = insnList.getStart();
		if (startInsn == null) {
			// empty method
			return false;
		}
		
		/* fill up missing variables in localVariableTable */
		LocalVariableSupporter.fillUpVariableTable(methodGen, method, constPool);
		
		List<LineInstructionInfo> lineInsnInfos = LineInstructionInfo.buildLineInstructionInfos(classGen, constPool,
				methodGen, method, isAppClass, insnList);
		int startLine = Integer.MAX_VALUE;
		int endLine = AgentConstants.UNKNOWN_LINE;
		for (LineInstructionInfo lineInfo : lineInsnInfos) {
			int line = lineInfo.getLine();
			if (line < startLine) {
				startLine = line;
			}
			if (line > endLine) {
				endLine = line;
			}
		}
		if (startLine == Integer.MAX_VALUE) {
			startLine = AgentConstants.UNKNOWN_LINE;
		}
		LocalVariableGen classNameVar = createLocalVariable(CLASS_NAME, methodGen, constPool);
		LocalVariableGen methodSigVar = createLocalVariable(METHOD_SIGNATURE, methodGen, constPool);
		LocalVariableGen tracerVar = methodGen.addLocalVariable(TRACER_VAR_NAME, Type.getType(IExecutionTracer.class),
				insnList.getStart(), insnList.getEnd());
		
		if (userFilters != null) userFilters.filter(lineInsnInfos, classGen.getClassName(), method);
		for (LineInstructionInfo lineInfo : lineInsnInfos) {
			/* instrument RW instructions */
			List<RWInstructionInfo> rwInsns = lineInfo.getRWInstructions();
//			if (lineInfo.hasNoInstrumentation()) {
			injectCodeTracerHitLine(insnList, constPool, tracerVar, lineInfo.getLine(), lineInfo.getLineNumberInsn(),
					classNameVar, methodSigVar, lineInfo.hasExceptionTarget(), lineInfo.getReadWriteInsnTotal(false),
					lineInfo.getReadWriteInsnTotal(true), lineInfo);
//			}
			for (RWInstructionInfo rwInsnInfo : rwInsns) {
				InstructionList newInsns = null;
				// instructions to append --> only on write instructions
				InstructionList appendInsnsInstructionList = null;
				if (rwInsnInfo instanceof FieldInstructionInfo) {
					newInsns = getInjectCodeTracerRWriteField(constPool, tracerVar, (FieldInstructionInfo) rwInsnInfo, classNameVar, methodSigVar);
					appendInsnsInstructionList = appendFieldInstructions((FieldInstructionInfo) rwInsnInfo, constPool);
				} else if (rwInsnInfo instanceof ArrayInstructionInfo) {
					newInsns = getInjectCodeTracerRWriteArray(methodGen, constPool, tracerVar,
							(ArrayInstructionInfo) rwInsnInfo, classNameVar, methodSigVar);
				} else if (rwInsnInfo instanceof LocalVarInstructionInfo) {
					if (rwInsnInfo.getInstruction() instanceof IINC) {
						newInsns = getInjectCodeTracerIINC(constPool, tracerVar,
								(LocalVarInstructionInfo) rwInsnInfo, classNameVar, methodSigVar);
					} else {
						newInsns = getInjectCodeTracerRWLocalVar(constPool, tracerVar,
								(LocalVarInstructionInfo) rwInsnInfo, classNameVar, methodSigVar);
					}
				}
				if ((newInsns != null) && (newInsns.getLength() > 0)) {
					InstructionHandle insnHandler = rwInsnInfo.getInstructionHandler();
					if (rwInsnInfo.isStoreInstruction()) {
						insertInsnHandler(insnList, newInsns, insnHandler);
						if (appendInsnsInstructionList != null) appendInstruction(insnList, appendInsnsInstructionList, insnHandler);
						newInsns.dispose();
					} else {
						insertInsnHandler(insnList, newInsns, insnHandler);
						try {
							updateTarget(insnHandler, insnHandler.getPrev(), insnHandler.getNext());
							insnList.delete(insnHandler);
						} catch (TargetLostException e) {
							e.printStackTrace();
						}
						newInsns.dispose();
					}
				}
			}
			int line = lineInfo.getLine();
			/* instrument Invocation instructions */
			InstructionFactory instructionFactory = new InstructionFactory(classGen, constPool);
			for (InstructionHandle insn : lineInfo.getInvokeInstructions()) {
				injectCodeTracerInvokeMethod(methodGen, insnList, constPool, instructionFactory, tracerVar, insn, line,
						classNameVar, methodSigVar, isAppClass);
			}
			/* instrument Return instructions */
			for (InstructionHandle insn : lineInfo.getReturnInsns()) {
				injectCodeTracerReturn(insnList, constPool, tracerVar, insn, line, classNameVar, methodSigVar);
			}

			/**
			 * instrument exit instructions
			 */
			for (InstructionHandle exitInsHandle : lineInfo.getExitInsns()) {
				injectCodeTracerExit(exitInsHandle, insnList, constPool, tracerVar, line, classNameVar, methodSigVar, isMainMethod, isEntry);
			}
			
			/**
			 * Instrument new instructions
			 */
			for (InstructionHandle newInsnHandle: lineInfo.getNewInstructions()) {
				injectOnNewInstructions(insnList, newInsnHandle, constPool);
			}
			

			lineInfo.dispose();
		}
		injectCodeInitTracer(methodGen, constPool, startLine, endLine, isAppClass, classNameVar,
				methodSigVar, isMainMethod, tracerVar);
		return true;	
	}

	
	
	@Override
	protected InstructionList getInjectCodePutStatic(ConstantPoolGen constPool, LocalVariableGen tracerVar,
			FieldInstructionInfo info, LocalVariableGen classNameVar, LocalVariableGen methodSigVar) {
		InstructionList before = new InstructionList();
		PUTSTATIC insPutstatic = (PUTSTATIC) info.getInstruction();
		String classNameString = insPutstatic.getReferenceType(constPool).getSignature();
		
		before.append(new LDC(constPool.addString(classNameString)));
		before.append(new LDC(constPool.addString(info.getFieldName())));
		before.append(AggrePlayMethods.BEFORE_STATIC_WRITE.toInvokeStatic(constPool, instrumentationClass));
		InstructionList inBetween = super.getInjectCodePutStatic(constPool, tracerVar, info, classNameVar, methodSigVar);
		before.append(inBetween);
		
		return before;
	}

	@Override
	protected InstructionList getInjectCodeGetStatic(ConstantPoolGen constPool, LocalVariableGen tracerVar,
			FieldInstructionInfo info, LocalVariableGen classNameVar, LocalVariableGen methodSigVar) {
		InstructionList before = new InstructionList();
		GETSTATIC insPutstatic = (GETSTATIC) info.getInstruction();
		String classNameString = insPutstatic.getReferenceType(constPool).getSignature();

		before.append(new LDC(constPool.addString(classNameString)));
		before.append(new LDC(constPool.addString(info.getFieldName())));
		before.append(AggrePlayMethods.BEFORE_STATIC_READ.toInvokeStatic(constPool, instrumentationClass));
		
		
		InstructionList tracerCode = super.getInjectCodeGetStatic(constPool, tracerVar, info, classNameVar, methodSigVar);
		before.append(tracerCode);
		before.append(AggrePlayMethods.AFTER_OBJECT_READ.toInvokeStatic(constPool, instrumentationClass));
		return before;
	}

	protected void injectOnNewInstructions(InstructionList instructionList, 
			InstructionHandle instructionHandle,
			ConstantPoolGen cpg) {
		InstructionList toAppend = new InstructionList();
		toAppend.append(new DUP());
		toAppend.append(AggrePlayMethods.ON_NEW_OBJECT.toInvokeStatic(cpg, instrumentationClass));
		appendInstruction(instructionList, toAppend, instructionHandle);
	}
	

}
