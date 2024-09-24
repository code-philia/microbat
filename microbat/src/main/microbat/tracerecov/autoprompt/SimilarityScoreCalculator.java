package microbat.tracerecov.autoprompt;

import microbat.tracerecov.varskeleton.VariableSkeleton;

/**
 * This class is used to compute similarity score while searching for few-shot
 * example used in in-context learning.
 * 
 * @author hongshuwang
 */
public class SimilarityScoreCalculator {

	public static double getSimScoreBetweenVarSkeletons(VariableSkeleton var1, VariableSkeleton var2) {
		return 1 - var1.getDifferenceScore(var2);
	}

}
