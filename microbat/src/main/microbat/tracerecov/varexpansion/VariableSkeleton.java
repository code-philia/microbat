package microbat.tracerecov.varexpansion;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a representation of the structure of a variable.
 * 
 * @author hongshuwang
 */
public class VariableSkeleton {

	private String type;
	private String name;
	private VariableSkeleton parent;
	private List<VariableSkeleton> children;

	public VariableSkeleton(String type) {
		this.type = type;
		this.name = "";
		this.parent = null;
		this.children = new ArrayList<>();
	}

	public VariableSkeleton(String type, String name) {
		this.type = type;
		this.name = name;
		this.parent = null;
		this.children = new ArrayList<>();
	}
	
	public void addChild(VariableSkeleton child) {
		this.children.add(child);
		child.parent = this;
	}

}
