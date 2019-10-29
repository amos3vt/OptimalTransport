package optimalTransport;

public class GTTransportMapping {
	// This function implements the mapping for the 1-feasibility
	// implementation of the Gabow-Tarjan transportation algorithm, which
	// acts on integral supplies, demands, and costs.
	// This function calls the GTTransport function with the appropriate
	// arguments. It also calls the GreedyMatch function to match the remaining
	// supplies and demands not computed by GTTransport.


	public GTTransportMapping(int n, double[] supplies, double[] demands, double[][] cost, double delta) {
		double maxC = max(cost);
		int[][] newCost = mapCost(cost, delta);
		int[] newSupplies = mapSupplies(supplies, delta);
		int[] newDemands = mapDemands(demands, delta);
		
		
		OptimalTransport otObj = null;
		//otObj = new OptimalTransport(n, newSupplies, newDemands, newCost);
		//otObj.compute();
		double[][] capacity = otObj.capacity;
		
		
		double[] actualSupplies = new double[n];
		double[] actualDemands = new double[n];
		
		for(int i = 0; i < n; i++) { //I believe this loop is correct, but I'm not 100% sure I understand the
										//MATLAB syntax for this part
			for(int j = 0; j < n*2; j++) {
				actualDemands[i] += capacity[i + n][j];
				actualSupplies[i] += capacity[j][i];
				
			}
		}
		
		original(actualDemands, n, maxC, delta);
		original(actualSupplies, n, maxC, delta);
		original(capacity, n, maxC, delta);
		
		
		double[] remSupplies = new double[n];
		double[] remDemands = new double[n];
		
		for(int i = 0; i < n; i++) {
			remSupplies[i] = supplies[i] - actualSupplies[i];
			remDemands[i] = actualDemands[i] - demands[i];
		}
		
		
		for(int i = n; i < 2*n; i++) {
			int j = 1;
			while(remDemands[i - n] > 0 && j < n) {
				if(capacity[i][j] >= remDemands[i - n]) {
					capacity[i][j] -= remDemands[i - n];
					remSupplies[j] += remDemands[i - n];
					remDemands[i - n] = 0;
				}
				else if(capacity[i][j] > 0) {
					remSupplies[j] += capacity[i][j];
					remDemands[i - n] -= capacity[i][j];
					capacity[i][j] = 0;
				}
				j++;
			}
		}
		
		absolute(remDemands);
		
		greedyMatch gm = new greedyMatch();
		double[][] greedyCapacity = gm.greedyMatcher(n, remSupplies, remDemands, cost);
		
		
		for(int i = 0; i < n; i++) {
			for(int j = n; n < n * 2; j++) {
				capacity[j][i] += greedyCapacity[i][j-n]; 
			}
		}
		
		System.out.println("DISPLAY SOLUTION");
		double totalTransportCost = 0;
		double fulfilledCapacity = 0;
		
		for(int i = 0; i < n; i++) {
			for(int j = n; j < 2*n; j++) {
				if(capacity[j][i] > 0) {
					fulfilledCapacity += capacity[j][i];
					totalTransportCost += (cost[i][j-n] * capacity[j][i]);
				}
			}
		}
		
		double tolerance = 0.0000001;
		double[] finalDemandsUsed = new double[n];
		double[] finalSuppliesUsed = new double[n];
		for(int i = 0; i < n; i++) {
			for(int j = 0; j < n*2; j++) {
				finalDemandsUsed[i] += capacity[i + n][j];
				finalSuppliesUsed[i]+= capacity[j][i];
				
			}
		}
		
		double[] residualS = new double[n];
		double[] residualD = new double[n];
		for(int i = 0; i < n; i++) {
			residualS[i] = supplies[i] - finalSuppliesUsed[i];
			residualD[i] = demands[i] - finalDemandsUsed[i];
			
		}
		absolute(residualS);
		absolute(residualD);
		
		if(!allLessOrEqual(residualS, tolerance)) {
			System.out.println("Error: GT did not return a valid transport; a supply constraint was violated.");
		}
		if(!allLessOrEqual(residualD, tolerance)) {
			System.out.println("Error2: GT did not return a valid transport; a demand constraint was violated.");
		}
		assert(Math.abs(fulfilledCapacity - 1) <= tolerance);
		
		System.out.println("Result  = " + totalTransportCost);
		
	}
	
	public static boolean allLessOrEqual(double[] arr, double tol) {
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] > tol)return false;
		}
		return true;
	}
	
	public static void absolute(double[] arr) {
		for(int i = 0; i < arr.length; i++) {
			arr[i] = Math.abs(arr[i]);
		}
	}
	
	public static void original(double[] arr, int n, double maxC, double delta) {
		double denom = 4 * n * maxC;
		for(int i = 0; i < n; i++) {
			arr[i] = ((arr[i] * delta) / denom);
		}
		
	}
	
	public static void original(double[][] arr, int n, double maxC, double delta) {
		double denom = 4 * n * maxC;
		for(int i = 0; i < n; i++) {
			for(int j = 0; j < n; j++) {
				if(arr[i][j] != 0.0) {
					arr[i][j] = ((arr[i][j] * delta) / denom);
				}
			}
		}
	}
	
	public static int[] mapDemands(double[] dem, double delta) {
		int[] newDem = new int[dem.length];
		for(int i = 0; i< dem.length; i++) {
			double temp = 4 * dem[i] / delta;
			if(temp % 1 == 0) newDem[i] = (int) temp;
			else newDem[i] = ((int) temp) + 1;
		}
		return newDem;
		
	}
	
	public static int[] mapSupplies(double[] supp, double delta) {
		int[] newSupp = new int[supp.length];
		for(int i = 0; i < supp.length; i++) {
			double temp = 4 * supp[i] / delta;
			newSupp[i] = (int) temp;
		}
		return newSupp;
	}
	
	
	public static int[][] mapCost(double[][] cost, double delta) {
		int[][] nCost = new int[cost.length][cost[0].length];
		for(int i = 0; i < cost.length; i++) {
			for(int j = 0; j < cost[0].length; j++) {
				double temp = 4 * cost[i][j] / delta;
				nCost[i][j] = (int) temp;
			}
		}
		return nCost;
	}

	public static double max(double[][] c) {
		double maxC = -1;
		for(int i = 0; i < c.length; i++) {
			for(int j = 0; j < c[0].length; j++) {
				if(c[i][j] > maxC) maxC = c[i][j];
			}
		}
		return maxC;
	}
}