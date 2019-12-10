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
	private static double total_cost;
	private static long tTime;
	private static int iterations;
	/**
	 * How to call from MATLAB:
	 * 
	 * javaaddpath('PATH\Optimal Transport\bin\');
	 * GTTransport_time = tic;
     * arr = optimalTransport.GTTransportMapping.callFromMATLAB(n, a, b', C, delta);
     * total_cost_transport = arr(1);
     * iterationCountTransport = arr(2);
     * GTTransport_time = toc(GTTransport_time);
     * 
     * 
	 * @param n
	 * @param supplies
	 * @param demands
	 * @param costs
	 * @param delta
	 * @return
	 */
	public static double[] callFromMATLAB(int n, double[] supplies, double[] demands, double[][] costs, double delta) {
		long t1 = System.currentTimeMillis();
		mapping(n, supplies, demands, costs, delta);
		tTime = System.currentTimeMillis() - t1;
		double[] ret = new double[2];
		ret[0] = total_cost;
		ret[1] = iterations;
		return ret;//new double[((total_cost, ((double)(iterations)))];
	}
	public static long getTime() {return tTime;}
	
	
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
  
  /**
   * Read in nxn matrix from file
   */
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
  
  /**
   * Read in array of length n from file
   */
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
  /**
   * mapping() is the main functionality of this class
   * It 1) maps the supplies, demands, and costs
   * 2) computes the optimal transport with these mapped values
   * 3) un-maps the supplies, demands, and costs
   * 4) runs the greedyMatch
   * and 5) computes and displays final solution
  */
	public static void mapping(int n, double[] supplies, double[] demands, double[][] cost, double delta) {
		double maxC = max(cost);
    // use adjusted costs/supplies/demands
		double[][] newCost = mapCost(cost, delta);
		double[] newSupplies = mapSupplies(supplies, n, maxC, delta);
		double[] newDemands = mapDemands(demands, n, maxC, delta);
    
    // compute optimal transport using adjusted values
		OptimalTransport otObj = null;
		try {
			otObj = new OptimalTransport(n, newSupplies, newDemands, newCost);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		//otObj.compute();
		iterations = otObj.iterations;
		double[][] capacity = otObj.capacity;
    
    // calculate matched supplies/demands
		double[] actualSupplies = new double[n];
		double[] actualDemands = new double[n];
		for(int i = 0; i < n; i++) { //I believe this loop is correct, but I'm not 100% sure I understand the
										//MATLAB syntax for this part
			for(int j = 0; j < n*2; j++) {
				actualDemands[i] += capacity[i + n][j];
				actualSupplies[i] += capacity[j][i];
			}
		}
    // convert to original inputs for comparison
		original(actualDemands, n, maxC, delta);
		original(actualSupplies, n, maxC, delta);
		original(capacity, n, maxC, delta);
		
    // determine the remaining supplies and demands
		double[] remSupplies = new double[n];
		double[] remDemands = new double[n];
		for(int i = 0; i < n; i++) {
			remSupplies[i] = supplies[i] - actualSupplies[i];
			remDemands[i] = actualDemands[i] - demands[i];
		}
		
    // push back extra demands
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
		
    // match remaining supplies and demands
		greedyMatch gm = new greedyMatch();
		double[][] greedyCapacity = gm.greedyMatcher(n, remSupplies, remDemands, cost);
		
    // determine final capacities
		for(int i = 0; i < n; i++) {
			for(int j = n; j < n * 2; j++) {
				capacity[j][i] += greedyCapacity[i][j-n]; 
			}
		}
		
		System.out.println("DISPLAY SOLUTION");
		double totalTransportCost = 0;
		double fulfilledCapacity = 0;
		
    //Sums totalTransportCost
		for(int i = 0; i < n; i++) {
			for(int j = n; j < 2*n; j++) {
				if(capacity[j][i] > 0) {
					fulfilledCapacity += capacity[j][i];
					totalTransportCost += (cost[i][j-n] * capacity[j][i]);
				}
			}
		}
		
		double tolerance = 0.0000001; //if any value is off by less than this it's ok
		double[] finalDemandsUsed = new double[n];
		double[] finalSuppliesUsed = new double[n];
		for(int i = 0; i < n; i++) {
			for(int j = 0; j < n*2; j++) {
				finalDemandsUsed[i] += capacity[i + n][j];
				finalSuppliesUsed[i]+= capacity[j][i];
				
			}
		}
		
    //For testing the valididty of the solution, the residual supplies and demands are calculated
		double[] residualS = new double[n];
		double[] residualD = new double[n];
		for(int i = 0; i < n; i++) {
			residualS[i] = supplies[i] - finalSuppliesUsed[i];
			residualD[i] = demands[i] - finalDemandsUsed[i];
			
		}
    
    //Assert that the transport is valid 
		absolute(residualS);
		absolute(residualD);
		
		if(!allLessOrEqual(residualS, tolerance)) {
			System.out.println("Error: GT did not return a valid transport; a supply constraint was violated.");
		}
		if(!allLessOrEqual(residualD, tolerance)) {
			System.out.println("Error2: GT did not return a valid transport; a demand constraint was violated.");
		}
		assert(Math.abs(fulfilledCapacity - 1) <= tolerance);
		
    
    
    //Display solution
		System.out.println("Result  = " + totalTransportCost);
		total_cost = totalTransportCost;
		
	}
	/**
   * Checks if all elements of arr are less than or equal to tol
  */
	public static boolean allLessOrEqual(double[] arr, double tol) {
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] > tol)return false;
		}
		return true;
	}
	/**
   * Returns abs(arr)
  */
	public static void absolute(double[] arr) {
		for(int i = 0; i < arr.length; i++) {
			arr[i] = Math.abs(arr[i]);
		}
	}
	/**
   * Un-maps an array
   * Does arr *= delta / (4 * n * maxC)
  */
	public static void original(double[] arr, int n, double maxC, double delta) {
		double denom = 4 * n * maxC;
		for(int i = 0; i < n; i++) {
			arr[i] = ((arr[i] * delta) / denom);
		}
		
	}
	/**
   * Overloaded method that un-maps a matrix
   * does the same thing as the original method, but in the
   * MATLAB implementation, it checks if the value to be un-mapped
   * is not zero before continuing 
  */
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
	/**
   * Maps dem[] using n, maxC, and delta
   * Does dem *= 4 * n * maxC / delta. then ceils dem
  */
	public static double[] mapDemands(double[] dem, int n, double maxC, double delta) {
		double[] newDem = new double[dem.length];
		for(int i = 0; i< dem.length; i++) {
			double temp = 4 * n * maxC * dem[i] / delta;
			if(temp % 1 == 0) newDem[i] = (int) temp; //ceil demands, there is a 
      																						//chance that the demand value is already an int
                                                  //i.e. 4.000, so, just in case we must check 
			else newDem[i] = ((int) temp) + 1;
		}
		return newDem;
		
	}
	/**
   * Maps supp[] using n, maxC, and delta
   * Does supp *= 4 * n * maxC / delta; for every element in supp then floors supp
  */
	public static double[] mapSupplies(double[] supp, int n, double maxC, double delta) {
		double[] newSupp = new double[supp.length];
		for(int i = 0; i < supp.length; i++) {
			double temp = 4 * n * maxC *supp[i] / delta;
			newSupp[i] = (int) temp; //floors temp
		}
		return newSupp;
	}
	
	/**
   * Maps cost[][] using delta
   * Simply does cost *= 4 / delta; for every element of cost[][]
  */ 
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
	/**
   * Returns the greatest value in c by iterating through every value of c
  */
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