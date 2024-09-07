package microbat.codeanalysis.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import microbat.model.ClassLocation;

public class PreCheckInformation {
	private int threadNum;
	private int stepNum;
	private boolean isOverLong;
	private List<ClassLocation> visitedLocations = new ArrayList<>();
	private List<String> overLongMethods = new ArrayList<>();
	private boolean isPassTest;
	private boolean timeout = false;
	private List<String> loadedClasses = new ArrayList<>();
	private boolean undeterministic = false;
	private Set<String> libraryCalls = new HashSet<>();
	
	public PreCheckInformation() {
	}

	public PreCheckInformation(int threadNum, int stepNum, boolean isOverLong, 
			List<ClassLocation> visitedLocations, List<String> overLongMethods, 
			List<String> loadedClasses, Set<String> libraryCalls) {
		super();
		this.threadNum = threadNum;
		this.stepNum = stepNum;
		this.isOverLong = isOverLong;
		this.visitedLocations = visitedLocations;
		this.overLongMethods = overLongMethods;
		this.loadedClasses = loadedClasses;
		this.libraryCalls = libraryCalls;
	}
	
	public int getThreadNum() {
		return threadNum;
	}

	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}

	public int getStepNum() {
		return stepNum;
	}

	public void setStepNum(int stepNum) {
		this.stepNum = stepNum;
	}

	public List<ClassLocation> getVisitedLocations() {
		return visitedLocations;
	}

	public void setVisitedLocations(List<ClassLocation> visitedLocations) {
		this.visitedLocations = visitedLocations;
	}

	public boolean isOverLong() {
		return isOverLong;
	}

	public void setOverLong(boolean isOverLong) {
		this.isOverLong = isOverLong;
	}

	public List<String> getOverLongMethods() {
		return overLongMethods;
	}

	public void setOverLongMethods(List<String> overLongMethods) {
		this.overLongMethods = overLongMethods;
	}
	public boolean isPassTest() {
		return isPassTest;
	}
	
	public void setPassTest(boolean isPassTest) {
		this.isPassTest = isPassTest;
	}

	public boolean isTimeout() {
		return timeout;
	}

	public void setTimeout(boolean timeout) {
		this.timeout = timeout;
	}

	public List<String> getLoadedClasses() {
		return loadedClasses;
	}

	public void setLoadedClasses(List<String> loadedClasses) {
		this.loadedClasses = loadedClasses;
	}

	public boolean isUndeterministic() {
		return undeterministic;
	}

	public void setUndeterministic(boolean undeterministic) {
		this.undeterministic = undeterministic;
	}
	
	public Set<String> getLibraryCalls() {
		return libraryCalls;
	}
	
	public void setLibraryCalls(Set<String> libraryCalls) {
		this.libraryCalls = libraryCalls;
	}

	@Override
	public String toString() {
		return "PreCheckInformation [isOverLong=" + isOverLong + ", isPassTest=" + isPassTest + "]";
	}
	
}
