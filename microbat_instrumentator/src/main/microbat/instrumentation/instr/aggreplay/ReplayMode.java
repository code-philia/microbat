package microbat.instrumentation.instr.aggreplay;

public enum ReplayMode {
	STRICT_RW("strict_rw"),
	RELAX_RW("relax_rw"),
	AGGR("aggr_rw");
	
	private String idString;
	private ReplayMode(String name) {
		idString = name;
	}
	
	public String getIdString() {
		return this.idString;
	}
	
	public static ReplayMode parse(String value) {
		try {
			return ReplayMode.valueOf(value);
		} catch (IllegalArgumentException arg) {
			return AGGR;
		}
		
	}
}
