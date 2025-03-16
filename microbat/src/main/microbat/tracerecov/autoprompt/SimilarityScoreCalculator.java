package microbat.tracerecov.autoprompt;

import microbat.tracerecov.varskeleton.VariableSkeleton;

/**
 * This class is used to compute similarity score while searching for few-shot
 * example used in in-context learning.
 * 
 * @author hongshuwang
 */
public class SimilarityScoreCalculator {

	private static double LCS_LEN_THRESHOLD = 500;

	public SimilarityScoreCalculator() {
	}

	public double getJaccardCoefficient(VariableSkeleton var1, VariableSkeleton var2) {
		return 1 - var1.getDifferenceScore(var2);
	}

	public boolean isBelowLengthThreshold(String str1, String str2) {
		return str1.length() < LCS_LEN_THRESHOLD && str2.length() < LCS_LEN_THRESHOLD;
	}

	public double getSimilarityRatioBasedOnLCS(String str1, String str2) {
		if (!isBelowLengthThreshold(str1, str2)) {
			return 0;
		}
		int lcs = getLongestCommonSequenceSize(str1, str2);
		return (double) (2 * lcs) / (double) (str1.length() + str2.length());
	}

	private int getLongestCommonSequenceSize(String str1, String str2) {
		if (str1.length() < str2.length()) {
			String temp = str1;
			str1 = str2;
			str2 = temp;
		}

		int m = str1.length();
		int n = str2.length();
		int[] dp = new int[n + 1];

		for (int i = 1; i <= m; i++) {
			int prev = 0;
			for (int j = 1; j <= n; j++) {
				int temp = dp[j];
				if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
					dp[j] = prev + 1;
				} else {
					dp[j] = Math.max(dp[j], dp[j - 1]);
				}
				prev = temp;
			}
		}
		return dp[n];
	}

	public double[] normalize(double[] initialWeights, boolean[] activationStatus) {
		int size = initialWeights.length;
		if (activationStatus.length != size) {
			throw new IllegalArgumentException("arrays have different sizes");
		}

		double[] updatedWeights = new double[3];
		double total = 0;

		for (int i = 0; i < size; i++) {
			total += initialWeights[i] * (activationStatus[i] ? 1 : 0);
		}

		for (int i = 0; i < size; i++) {
			updatedWeights[i] = activationStatus[i] ? (double) (initialWeights[i] / total) : (double) 0;
		}

		return updatedWeights;
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
