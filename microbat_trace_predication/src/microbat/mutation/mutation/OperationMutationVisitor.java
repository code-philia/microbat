package microbat.mutation.mutation;

import java.util.List;

import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.BinaryExpr.Operator;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.stmt.DoStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.WhileStmt;
import mutation.mutator.MutationVisitor;

public class OperationMutationVisitor extends MutationVisitor{
	private List<MutationType> mutationTypes;
	
	public OperationMutationVisitor(List<MutationType> mutationTypes){
		this.mutationTypes = mutationTypes;
	}
	
	@Override
	public boolean mutate(BinaryExpr n) {
		if(!mutationTypes.contains(MutationType.CHANGE_ARITHMETIC_OPERATOR) 
				&& !mutationTypes.contains(MutationType.SWAP_OPERANDS)) {
			return super.mutate(n);
		}
		
	    MutationNode muNode = newNode(n);
        if (n.getParentNode() instanceof ExpressionStmt) {
            muNode = newNode(n.getParentNode());
        } else {
            muNode = newNode(n);
        }
	    
		if(mutationTypes.contains(MutationType.CHANGE_ARITHMETIC_OPERATOR)) {
			BinaryExpr newBinaryExpr = (BinaryExpr) nodeCloner.visit(n, null);
		    if (newBinaryExpr.getOperator() == Operator.plus) {
		        newBinaryExpr.setOperator(Operator.minus);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		    else if (newBinaryExpr.getOperator() == Operator.minus) {
		        newBinaryExpr.setOperator(Operator.plus);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		    else if (newBinaryExpr.getOperator() == Operator.times) {
		        newBinaryExpr.setOperator(Operator.divide);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		    else if (newBinaryExpr.getOperator() == Operator.divide) {
		        newBinaryExpr.setOperator(Operator.times);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		    else if (newBinaryExpr.getOperator() == Operator.and) {
		        newBinaryExpr.setOperator(Operator.or);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		    else if (newBinaryExpr.getOperator() == Operator.or) {
		        newBinaryExpr.setOperator(Operator.and);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		    else if (newBinaryExpr.getOperator() == Operator.binAnd) {
		        newBinaryExpr.setOperator(Operator.binOr);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		    else if (newBinaryExpr.getOperator() == Operator.binOr) {
		        newBinaryExpr.setOperator(Operator.binAnd);
		        muNode.add(newBinaryExpr, MutationType.CHANGE_ARITHMETIC_OPERATOR.name());
		    }
		}
	    if (mutationTypes.contains(MutationType.SWAP_OPERANDS)) {
	        BinaryExpr newBinaryExpr = (BinaryExpr) nodeCloner.visit(n, null);
	        Expression left = newBinaryExpr.getLeft();
	        Expression right = newBinaryExpr.getRight();       
	        newBinaryExpr.setLeft(right);
	        newBinaryExpr.setRight(left);

	        muNode.add(newBinaryExpr, MutationType.SWAP_OPERANDS.name());
	    }
		return false;
	}
	
	@Override
	public boolean mutate(ForStmt n){
		if(mutationTypes.contains(MutationType.CHANGE_CONDITIONALS_BOUNDARY)) { // for(int i = 0;i<10;i++)
		    if (n.getCompare() instanceof BinaryExpr) {
				ForStmt newForStmt = (ForStmt) nodeCloner.visit(n, null);
		        BinaryExpr binaryExpr = (BinaryExpr) newForStmt.getCompare(); // i<10
		        if (binaryExpr.getOperator() == BinaryExpr.Operator.less) {
		            binaryExpr.setOperator(BinaryExpr.Operator.lessEquals);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.lessEquals) {
		            binaryExpr.setOperator(BinaryExpr.Operator.less);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.greater) {
		            binaryExpr.setOperator(BinaryExpr.Operator.greaterEquals);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.greaterEquals) {
		            binaryExpr.setOperator(BinaryExpr.Operator.greater);
		        }
		        
				MutationNode muNode = newNode(n);
		        muNode.add(newForStmt, MutationType.CHANGE_CONDITIONALS_BOUNDARY.name());
		    }
		}
	    return super.mutate(n);
	}
	
	@Override
	public boolean mutate(WhileStmt n){
		System.out.println("--Mutation-- catch a WhileStmt");		
		if(mutationTypes.contains(MutationType.CHANGE_CONDITIONALS_BOUNDARY)) { // while(i<10)
		    if (n.getCondition() instanceof BinaryExpr) {
				WhileStmt newWhileStmt = (WhileStmt) nodeCloner.visit(n, null);
		        BinaryExpr binaryExpr = (BinaryExpr) newWhileStmt.getCondition(); // i<10
		        if (binaryExpr.getOperator() == BinaryExpr.Operator.less) {
		            binaryExpr.setOperator(BinaryExpr.Operator.lessEquals);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.lessEquals) {
		            binaryExpr.setOperator(BinaryExpr.Operator.less);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.greater) {
		            binaryExpr.setOperator(BinaryExpr.Operator.greaterEquals);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.greaterEquals) {
		            binaryExpr.setOperator(BinaryExpr.Operator.greater);
		        }
		        
				MutationNode muNode = newNode(n);
		        muNode.add(newWhileStmt, MutationType.CHANGE_CONDITIONALS_BOUNDARY.name());
		    }
		}
	    return super.mutate(n);
	}
	
	@Override
	public boolean mutate(DoStmt n){
		if(mutationTypes.contains(MutationType.CHANGE_CONDITIONALS_BOUNDARY)) { // do{ }while(i<10);
		    if (n.getCondition() instanceof BinaryExpr) {
		    	DoStmt newDoStmt = (DoStmt) nodeCloner.visit(n, null);
		        BinaryExpr binaryExpr = (BinaryExpr) newDoStmt.getCondition(); // i<10
		        if (binaryExpr.getOperator() == BinaryExpr.Operator.less) {
		            binaryExpr.setOperator(BinaryExpr.Operator.lessEquals);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.lessEquals) {
		            binaryExpr.setOperator(BinaryExpr.Operator.less);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.greater) {
		            binaryExpr.setOperator(BinaryExpr.Operator.greaterEquals);
		        }
		        else if(binaryExpr.getOperator() == BinaryExpr.Operator.greaterEquals) {
		            binaryExpr.setOperator(BinaryExpr.Operator.greater);
		        }
		        
				MutationNode muNode = newNode(n);
		        muNode.add(newDoStmt, MutationType.CHANGE_CONDITIONALS_BOUNDARY.name());
		    }
		}
	    return super.mutate(n);
	}
}
