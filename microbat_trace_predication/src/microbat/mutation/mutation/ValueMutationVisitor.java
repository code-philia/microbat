package microbat.mutation.mutation;

import java.util.List;

import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.stmt.ReturnStmt;
import mutation.mutator.MutationVisitor;

public class ValueMutationVisitor extends MutationVisitor{
	private List<MutationType> mutationTypes;
	
	public ValueMutationVisitor(List<MutationType> mutationTypes){
		this.mutationTypes = mutationTypes;
	}
	
	@Override
	public boolean mutate(IntegerLiteralExpr n) {
		if(!mutationTypes.contains(MutationType.CHANGE_LITERAL)) {
			return super.mutate(n);
		}
		MutationNode muNode = newNode(n);
		muNode.add(new IntegerLiteralExpr("0"), MutationType.CHANGE_LITERAL.name());
		return false;
	}
	
	@Override
	public boolean mutate(CharLiteralExpr n) {
		if(!mutationTypes.contains(MutationType.CHANGE_LITERAL)) {
			return super.mutate(n);
		}
		MutationNode muNode = newNode(n);
		muNode.add(new CharLiteralExpr("\0"), MutationType.CHANGE_LITERAL.name());
		return false;
	}
	
	@Override
	public boolean mutate(DoubleLiteralExpr n) {
		if(!mutationTypes.contains(MutationType.CHANGE_LITERAL)) {
			return super.mutate(n);
		}
		MutationNode muNode = newNode(n);
		muNode.add(new DoubleLiteralExpr("0.0"), MutationType.CHANGE_LITERAL.name());
		return false;
	}
	
	@Override
	public boolean mutate(LongLiteralExpr n) {
		if(!mutationTypes.contains(MutationType.CHANGE_LITERAL)) {
			return super.mutate(n);
		}
		MutationNode muNode = newNode(n);
		muNode.add(new LongLiteralExpr("0L"), MutationType.CHANGE_LITERAL.name());
		return false;
	}
	
	@Override
	public boolean mutate(BooleanLiteralExpr n) {
		if(!mutationTypes.contains(MutationType.CHANGE_LITERAL)) {
			return super.mutate(n);
		}
		MutationNode muNode = newNode(n);
		if(n.getValue()==true) {
			muNode.add(new BooleanLiteralExpr(false), MutationType.CHANGE_LITERAL.name());
		}
		else {
			muNode.add(new BooleanLiteralExpr(true), MutationType.CHANGE_LITERAL.name());
		}
		return false;
	}
	
	@Override
	public boolean mutate(StringLiteralExpr n) {
		if(!mutationTypes.contains(MutationType.CHANGE_LITERAL)) {
			return super.mutate(n);
		}
		MutationNode muNode = newNode(n);
		muNode.add(new StringLiteralExpr(""), MutationType.CHANGE_LITERAL.name());
		return false;
	}
	
	@Override
	public boolean mutate(ReturnStmt n) {
		if(!mutationTypes.contains(MutationType.CHANGE_RETURN)) {
			return super.mutate(n);
		}
		ReturnStmt returnStmt = (ReturnStmt)nodeCloner.visit(n, null);
		Expression expr = returnStmt.getExpr();
	    // TODO get expr type and substitute with 0 or null
		
		return false;
	}
	
	
	
}
