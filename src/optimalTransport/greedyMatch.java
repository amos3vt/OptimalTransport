package optimalTransport;

import java.util.Arrays;

public class greedyMatch {
	// This function implements the Greedy match routine to match the leftover
	// supplies and demands from the GTTransport
	// implementations. 
	public double[][] greedyMatcher(int n, double[] gSupplies, double[] gDemands, double[][] CostGreedy) {
		long time = System.currentTimeMillis(); //time checking
		double[][] greedyCapacity = new double[n][n];
		long time0 = System.currentTimeMillis();
		int[][] I = indexSort(CostGreedy);//sorts the cost matrix by row and returns the old 
											//index values in their sorted (by data) order

		System.out.println("Sort Time: " + (System.currentTimeMillis() - time0));
		//greedily fills supplies and demands
		for(int i = 0; i < n; i++) {
			int j = 0;
			while(gSupplies[i] > 0 && j < n) {
				
				int id2 = I[i][j];
				
				double units = Math.min(gSupplies[i], gDemands[id2]);
				
				greedyCapacity[i][id2] = units;
				gSupplies[i] = gSupplies[i] - units;
				gDemands[id2] = gDemands[id2] - units;
				
				j++;
			}
		}
		
		
		return greedyCapacity;
	}
	/**
	 * sorts the cost matrix by row and returns the old 
	 * index values in their sorted (by data) order
	 * Uses an inner class Index object to assist the new index-based 
	 * Representation of the costs
	 * @param costs
	 * @return the sorted indices
	 */
	public int[][] indexSort(double[][] costs){
		//create Index matrix based on costs
		Index[][] c = new Index[costs.length][costs[0].length];
		for(int i = 0; i < costs.length; i++) {
			for(int j = 0; j < costs[0].length; j++) {
				c[i][j] = new Index(costs[i][j], j);
				
			}
		}
		//sorts each row with Arrays.sort
		for(int i = 0; i < costs.length; i++) {
			Arrays.sort(c[i]);
		}
		//creates int matrix with the index values
		int[][] indices = new int[costs.length][costs[0].length];
		for(int i = 0; i < costs.length; i++) {
			for(int j = 0; j < costs[0].length; j++) {
				indices[i][j] = c[i][j].idx;
			}
		}
		return indices;
	}
	/**
	 *  This inner class is for bundling the index and its data
	 *  Into one object
	 *
	 */
	private class Index implements Comparable<Index>{
		private double value;
		private int idx;
		
		private Index(double val, int i) {
			value = val;
			idx = i;
		}

		/**
		 * Compares based on the data value
		 */
		@Override
		public int compareTo(Index o) {
			//System.out.println("Test");
			return Double.compare(this.value, o.value);
		}
	}
	
}