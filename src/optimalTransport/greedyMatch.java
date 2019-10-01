package optimalTransport;

import java.util.Arrays;

public class greedyMatch {
	// This function implements the Greedy match routine to match the leftover
	// supplies and demands from the GTTransport
	// implementations. 
	
	public static void main(String args[]) { // Testing code for this class
		int n = 10;
		double [] gSupplies = new double[] {0.0, 0.01, 0.01, 0.08, 0.02, 0.01, 0.01, 0.01, 0.01, 0.05};
		double [] gDemands = new double[] {0.1, 0.01, 0.01, 0.10, 0.02, 0.01, 0.03, 0.01, 0.01, 0.00};
		double[][] costGreedy = {{0.0, 0.9, 0.60, 0.00006, 0.042, 0.057, 0.99990, 0.0987, 0.0123456, 0.000000001},
				{0.0034, 0.0, 0.60, 0.00006, 0.042, 0.057, 0.00990, 0.0987, 0.0123400056, 0.000000001},
				{0.0035, 0.9, 0.0, 0.06, 0.042, 0.01111, 0.99990, 0.0987, 0.01230456, 0.0009001},
				{0.0034, 0.1, 0.60, 0.0, 0.042, 0.02, 0.000990, 0.07, 0.00056, 0.0090001},
				{0.0056560000008, 0.0005, 0.60, 0.00006, 0.0, 0.057, 0.00990, 0.0987, 0.567, 0.000000001},
				{0.34, 0.30, 0.60, 0.00006, 0.042, 0.0, 0.876, 0.0987, 0.0123400056, 0.5},
				{0.0034, 0.1230, 0.1602, 0.0001206, 0.042, 0.000121212121212, 0.0, 0.0987, 0.0123400056, 0.5},
				{0.012034, 0.30, 0.6012, 0.0001206, 0.042, 0.005, 0.876, 0.0, 0.0123400056, 0.5},
				{0.3124, 0.30, 0.1260, 0.000555, 0.042, 0.120, 0.81276, 0.0987, 0.0, 0.5},
				{0.5, 0.30, 0.60, 0.0032, 0.042, 0.012, 0.876, 0.0034, 0.0014, 0.0} };
		greedyMatch obj = new greedyMatch();
		System.out.println(Arrays.deepToString(obj.greedyMatcher(n, gSupplies, gDemands, costGreedy)));
	}
	
	public double[][] greedyMatcher(int n, double[] gSupplies, double[] gDemands, double[][] CostGreedy) {
		long time = System.currentTimeMillis();
		double[][] greedyCapacity = new double[n][n];
		
		int[][] I = indexSort(CostGreedy);//
		//System.out.println(Arrays.toString(I[15]));
		
		for(int i = 0; i < n; i++) {
			int j = 0;
			while(gSupplies[i] > 0 && j < n) {
				//int id2 = (int)CostGreedy[i][j];
				int id2 = I[i][j];
				//if(id2 > n) System.out.println(i + " " + j + " " + id2 + " "+ I[i][j]);
				double units = Math.min(gSupplies[i], gDemands[id2]);
				
				greedyCapacity[i][id2] = units;
				gSupplies[i] = gSupplies[i] - units;
				gDemands[id2] = gDemands[id2] - units;
				CostGreedy[i][id2] = Double.POSITIVE_INFINITY;
				j++;
			}
		}
		
		System.out.println(System.currentTimeMillis() - time);
		return greedyCapacity;
	}
	
	public int[][] indexSort(double[][] costs){
		Index[][] c = new Index[costs.length][costs[0].length];
		for(int i = 0; i < costs.length; i++) {
			for(int j = 0; j < costs[0].length; j++) {
				c[i][j] = new Index(costs[i][j], j);
				
			}
		}
		for(int i = 0; i < costs.length; i++) {
			Arrays.sort(c[i]);
		}
		int[][] indices = new int[costs.length][costs[0].length];
		for(int i = 0; i < costs.length; i++) {
			for(int j = 0; j < costs[0].length; j++) {
				indices[i][j] = c[i][j].idx;
			}
		}
		return indices;
	}
	private class Index implements Comparable<Index>{
		private double value;
		private int idx;
		
		private Index(double val, int i) {
			value = val;
			idx = i;
		}


		@Override
		public int compareTo(Index o) {
			//System.out.println("Test");
			return Double.compare(this.value, o.value);
		}
	}
	
}