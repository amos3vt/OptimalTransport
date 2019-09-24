
package optimalTransport;




public class GTTransport {
	
	
	public static double[][] Transport(double[][] C, double[] supplies, double[] demands, int n) {
		
		double sSum = 0;
		double dSum = 0;
		
		for(int i = 0; i < n; i++) {
			sSum += supplies[i];
			dSum += demands[i];
		}
		
		assert(sSum == dSum);
		
		double[][] CBA = transpose(C);
		double[][] CAB = C;
		
		int[] bFree = new int[n];
		int[] aFree = new int[n];
		
		for(int i = 0; i < n; i++) {
			bFree[i] = 1;
			aFree[i] = 1;
		}
		
		int[] dualWeights = new int[2*n];
		
		double[] deficiencyB = supplies;
		double[] deficiencyA = demands;
		
		for(int i = 0; i < n; i++) {
			if(deficiencyB[i] == 0) {
				bFree[i] = 0;
			}
			if(deficiencyA[i] == 0) {
				aFree[i] = 0;
			}
		}
		
		double[][] capacityAB = new double[n][n];
		double[][] capacityBA = new double[n][n];
		
		for(int j = 0; j < n; j++) {
			for(int i = 0; i < n; i++) {
				capacityBA[i][j] = Math.min(deficiencyB[j], deficiencyA[i]);
			}
		}
		
		int iterationCount = 0;
		
		double[][] slackAB = new double[n][n];
		double[][] slackBA = new double[n][n];
		double[] vertexVisited = new double[2*n];
		double[] augmentingPathVertices = new double[2*n];
		for(int i = 0; i < n; i++) {
			for(int j = 0; i < n; i++) {
				slackAB[j][i] = dualWeights[i + n] + dualWeights[j] - CAB[j][i];
				slackBA[j][i] = CBA[j][i] + 1 - dualWeights[i] - dualWeights[j + n];
			}
		}
		
		
		return null;
	}
	
	public static double[][] transpose(double[][] matrix){
		double[][] t = new double[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++) {
			for(int j = 0; j < matrix[0].length; j++) {
				t[j][i] = matrix[i][j];
			}
		}
		return t;
	}
}