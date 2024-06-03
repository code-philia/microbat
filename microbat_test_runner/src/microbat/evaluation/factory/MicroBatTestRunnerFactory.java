package microbat.evaluation.factory;

import microbat.evaluation.runners.MicroBatJUnit3And4TestRunner;
import microbat.evaluation.runners.MicroBatJUnit5TestRunner;
import microbat.evaluation.runners.MicroBatTestNGTestRunner;
import microbat.evaluation.runners.TestRunner;

// Decides which test runner to use e.g. junit 4/5, junit 3, testng
public class MicroBatTestRunnerFactory {
	
	private boolean forceJunit3Or4 = false;
	
	public MicroBatTestRunnerFactory(boolean forceJunit3Or4) {
		this.forceJunit3Or4 = forceJunit3Or4;
	}
	
    public TestRunner create(String className, String methodName) {
        JUnitTestFinder junitTestFinder = new JUnitTestFinder();
        boolean isJUnit3Or4Test = true;
        if (!forceJunit3Or4) {
        	isJUnit3Or4Test = junitTestFinder.junit3Or4TestExists(className, methodName);
        }
        if (isJUnit3Or4Test) {
            return new MicroBatJUnit3And4TestRunner();
        }
        boolean isJUnit5Test = junitTestFinder.junit5TestExists(className, methodName);
        if (isJUnit5Test) {
            return new MicroBatJUnit5TestRunner();
        }
        return new MicroBatTestNGTestRunner();
    }
}
