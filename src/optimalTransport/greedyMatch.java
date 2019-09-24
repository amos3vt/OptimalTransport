package optimalTransport;

import java.util.Arrays;

public class greedyMatch {
	// This function implements the Greedy match routine to match the leftover
	// supplies and demands from the GTTransport
	// implementations. 
	
	public int testFunction(int x) {
		return x + 1;
	}
	
	
	//function greedyCapacity = greedyMatch(n, gsupplies, gdemands, CostGreedy)
	
	public double[][] greedyMatcher(int n, double[] gSupplies, double[] gDemands, double[][] CostGreedy) {
		
		double[][] greedyCapacity = new double[n][n];
		
		int[][] I = indexSort(CostGreedy);//
		
		for(int i = 0; i < n; i++) {
			int j = 0;
			while(gSupplies[i] > 0 && j < n) {
				int id2 = (int)CostGreedy[i][j];
				double units = Math.min(gSupplies[i], gDemands[id2]);
				
				greedyCapacity[i][id2] = units;
				gSupplies[i] = gSupplies[i] - units;
				gDemands[id2] = gSupplies[id2] - units;
				CostGreedy[i][id2] = Double.POSITIVE_INFINITY;
				j++;
			}
		}
		
		
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
	private class Index implements Comparable{
		private double value;
		private int idx;
		
		private Index(double val, int i) {
			value = val;
			idx = i;
		}

		@Override
		public int compareTo(Object o) {
			return ((int)(this.value - ((Index) o).value));
		}
	}
	
}