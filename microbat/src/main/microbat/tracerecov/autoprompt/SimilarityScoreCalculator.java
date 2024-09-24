package microbat.tracerecov.autoprompt;

import microbat.tracerecov.varskeleton.VariableSkeleton;

/**
 * This class is used to compute similarity score while searching for few-shot
 * example used in in-context learning.
 * 
 * @author hongshuwang
 */
public class SimilarityScoreCalculator {

	public SimilarityScoreCalculator() {
	}

	public double getJaccardCoefficient(VariableSkeleton var1, VariableSkeleton var2) {
		return 1 - var1.getDifferenceScore(var2);
	}
	
	public double getSimilarityRatioBasedOnLCS(String str1, String str2) {
		int lcs = getLongestCommonSequenceSize(str1, str2);
		return (double) (2 * lcs) / (double) (str1.length() + str2.length());
	}

	private int getLongestCommonSequenceSize(String str1, String str2) {
		int m = str1.length();
		int n = str2.length();

		if (m == 0 || n == 0) {
			return 0;
		}

		int[][] lens = new int[m][n];

		// initialize table
		for (int i = 0; i < m; i++) {
			lens[i][0] = str1.charAt(i) == str2.charAt(0) ? 1 : 0;
		}
		for (int j = 0; j < n; j++) {
			lens[0][j] = str1.charAt(0) == str2.charAt(j) ? 1 : 0;
		}

		for (int i = 1; i < m; i++) {
			for (int j = 1; j < n; j++) {
				if (str1.charAt(i) == str2.charAt(j)) {
					lens[i][j] = lens[i - 1][j - 1] + 1;
				} else {
					lens[i][j] = Math.max(lens[i - 1][j - 1], Math.max(lens[i - 1][j], lens[i][j - 1]));
				}
			}
		}

		return lens[m - 1][n - 1];
	}

	public double getCombinedScore(double[] entries, double[] weights) {
		int size = Math.min(entries.length, weights.length);
		double combinedScore = 0;
		for (int i = 0; i < size; i++) {
			combinedScore += entries[i] * weights[i];
		}
		return combinedScore;
	}

}
