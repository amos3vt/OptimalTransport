package optimalTransport;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class GTTransportMapping {
	// This function implements the mapping for the 1-feasibility
	// implementation of the Gabow-Tarjan transportation algorithm, which
	// acts on integral supplies, demands, and costs.
	// This function calls the GTTransport function with the appropriate
	// arguments. It also calls the GreedyMatch function to match the remaining
	// supplies and demands not computed by GTTransport.


	
	public static void main(String args[]) {
		int n = Integer.parseInt(args[0]);
		double delta = Double.parseDouble(args[4]);
		
		try {
			long time = System.currentTimeMillis();
			double[] supplies = loadArray(args[1], n);
			double[] demands = loadArray(args[2], n);
			double[][] costs = loadMatrix(args[3], n);
			long time1 = System.currentTimeMillis();
			System.out.println("Load File time = " + (System.currentTimeMillis() - time));
			mapping(n, supplies, demands, costs, delta);
			long time2 = System.currentTimeMillis();
			System.out.println("Total Time = " + (time2 - time) + " (" + (time2 - time1 + ")"));
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} //a
		
		
	}
	private static double[][] loadMatrix(String filename, int n) throws FileNotFoundException {
		double[][] matrix = new double[n][n];
		File file = new File(filename);
		Scanner scanner = new Scanner(file);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				matrix[i][j] = scanner.nextDouble();
			}
		}
		scanner.close();//Does file need to be closed too?
		return matrix;
	}
	private static double[] loadArray(String filename, int n) throws FileNotFoundException {
		double[] array = new double[n];
		File file = new File(filename);
		Scanner scanner = new Scanner(file);
		for (int i = 0; i < n; i++) {
			array[i] = scanner.nextDouble();
		}
		scanner.close();//Does file need to be closed too?
		return array;
	}
	public static void mapping(int n, double[] supplies, double[] demands, double[][] cost, double delta) {
		double maxC = max(cost);
		double[][] newCost = mapCost(cost, delta);
		double[] newSupplies = mapSupplies(supplies, n, maxC, delta);
		double[] newDemands = mapDemands(demands, n, maxC, delta);
		
		
		OptimalTransport otObj = null;
		try {
			otObj = new OptimalTransport(n, newSupplies, newDemands, newCost);
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
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
			for(int j = n; j < n * 2; j++) {
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
		for(int i = 0; i < arr.length; i++) {
			for(int j = 0; j < arr[0].length; j++) {
				if(arr[i][j] != 0.0) {
					arr[i][j] = ((arr[i][j] * delta) / denom);
				}
			}
		}
	}
	
	public static double[] mapDemands(double[] dem, int n, double maxC, double delta) {
		double[] newDem = new double[dem.length];
		for(int i = 0; i< dem.length; i++) {
			double temp = 4 * n * maxC * dem[i] / delta;
			if(temp % 1 == 0) newDem[i] = (int) temp;
			else newDem[i] = ((int) temp) + 1;
		}
		return newDem;
		
	}
	
	public static double[] mapSupplies(double[] supp, int n, double maxC, double delta) {
		double[] newSupp = new double[supp.length];
		for(int i = 0; i < supp.length; i++) {
			double temp = 4 * n * maxC *supp[i] / delta;
			//double temp = 
			newSupp[i] = (int) temp;
		}
		return newSupp;
	}
	
	
	public static double[][] mapCost(double[][] cost, double delta) {
		double[][] nCost = new double[cost.length][cost[0].length];
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