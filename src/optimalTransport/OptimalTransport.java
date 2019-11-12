package optimalTransport;

import java.io.File; 
import java.io.FileNotFoundException; 
import java.util.Scanner;

public class OptimalTransport {
	int iterations;
	int n; // number of supply vertices (== number of demand vertices)
	double APLengths; // total length of all augmenting paths
	double[] deficiencyA; // length n array of unmet demands
	double[] deficiencyB; // length n array of unused supplies
	double[][] CAB; // nxn cost matrix, vertices of A are rows, B are columns
	double[][] CBA; // cost matrix transpose (to minimize cache misses)
	boolean[] AFree; // length n array indicating if vertex of A is free
	boolean[] BFree; // length n array indicating if vertex of B is free
	double[] dualWeights; // length 2*n array of dual weights
	double[][] capacityAB; // nxn matrix of capacities from A to B, A are columns
	double[][] capacityBA; // capacities from B to A, B are columns
	double[][] slackAB;
	double[][] slackBA;
	int[] augmentingPathVertices;
	double[][] capacity;
	
  /**
   * Set initial values for optimal transport, compute, and reformat capacities
   * Determines deficiencies and costs by reading files specified
   */
	public OptimalTransport(int n, String fileDeficiencyB, String fileDeficiencyA, String fileCost) throws FileNotFoundException {
		this.n = n;
		iterations = 0;
		APLengths = 0;
		deficiencyB = loadArray(fileDeficiencyB);
		deficiencyA = loadArray(fileDeficiencyA);
		CBA = loadMatrix(fileCost);
		CAB = transpose(CBA);
		BFree = setFree(deficiencyB);
		AFree = setFree(deficiencyA);
		dualWeights = new double[2*n];
		capacityAB = new double[n][n];
		capacityBA = setCapacityBA();
		slackAB = new double[n][n];
		slackBA = new double[n][n];
		updateSlacks();
		augmentingPathVertices = setAugmentingPathVertices();
		compute();
		capacity = formatCapacity();
	}
  
  /**
   * Set initial values for optimal transport, compute, and reformat capacities
   * Receives deficiencies and costs as input
   */
	public OptimalTransport(int n, double[] fileDeficiencyB, double[] fileDeficiencyA, double[][] fileCost) throws FileNotFoundException {
		this.n = n;
		iterations = 0;
		APLengths = 0;
		deficiencyB = (fileDeficiencyB);
		deficiencyA = (fileDeficiencyA);
		CBA = (fileCost);
		CAB = transpose(CBA);
		BFree = setFree(deficiencyB);
		AFree = setFree(deficiencyA);
		dualWeights = new double[2*n];
		capacityAB = new double[n][n];
		capacityBA = setCapacityBA();
		slackAB = new double[n][n];
		slackBA = new double[n][n];
		updateSlacks();
		augmentingPathVertices = setAugmentingPathVertices();
		compute();
		capacity = formatCapacity();
	}
	
  /**
   * read file, returning an array of the first n values
   */
	private double[] loadArray(String filename) throws FileNotFoundException {
		double[] array = new double[n];
		File file = new File(filename);
		Scanner scanner = new Scanner(file);
		for (int i = 0; i < n; i++) {
			array[i] = scanner.nextDouble();
		}
		scanner.close();
		return array;
	}
	
  /**
   * read file, returning an nxn matrix of first n^2 values
   */
	private double[][] loadMatrix(String filename) throws FileNotFoundException {
		double[][] matrix = new double[n][n];
		File file = new File(filename);
		Scanner scanner = new Scanner(file);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				matrix[i][j] = scanner.nextDouble();
			}
		}
		scanner.close();
		return matrix;
	}
	
  /**
   * get the transpose of a matrix
   */
	private double[][] transpose(double[][] matrix) {
		int n = matrix.length;
		double[][] t = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				t[i][j] = matrix[j][i];
			}
		}
		return t;
	}
	
  /**
   * determine which vertices are initially free
   */
	private boolean[] setFree(double[] deficiency) {
		boolean[] Free = new boolean[n];
		for (int i = 0; i < n; i++) {
			if (deficiency[i] != 0) { Free[i] = true; }
		}
		return Free;
	}
	
  /**
   * Determine the initial capacities from B to A
   */
	private double[][] setCapacityBA() {
		double[][] capacity = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				capacity[i][j] = Math.min(deficiencyB[i], deficiencyA[j]);
			}
		}
		return capacity;
	}
	
  /**
   * initialize an array of length 2*n with -1
   */
	private int[] setAugmentingPathVertices() {
		int[] AugmentingPathVertices = new int[2*n];
		for (int i = 0; i < 2*n; i++) {
			AugmentingPathVertices[i] = -1;
		}
		return AugmentingPathVertices;
	}
	
  /**
   * compute the optimal transport
   */
	private void compute() {
		while (anyFree(BFree)) {
			iterations++;
			double[] lv = setLV();
			boolean[] shortestpathset = new boolean[2*n];
			double distAF = dijkstra(lv, shortestpathset); // minimum distance to a free vertex
			updateDualWeights(lv, distAF);
			updateSlacks();
			DFS();
		}
	}
	
  /**
   * determine if any entry in any array of booleans is true
   */
	private boolean anyFree(boolean[] b) {
		for (int i = 0; i < n; i++) {
			if (b[i]) { return true; }
		}
		return false;
	}
	
  /**
   * set distances to vertices
   * total slack from nearest free vertex of B
   */
	private double[] setLV() {
		double[] lv = new double[n*2];
		for (int i = 0; i < n; i++) {
			if (!BFree[i]) {
				lv[i] = Double.POSITIVE_INFINITY;
			}
		}
		for (int i = n; i < 2*n; i++) {
			lv[i] = Double.POSITIVE_INFINITY;
		}
		return lv;
	}
	
  /**
   * perform dijkstra
   * each iteration adds vertex to shortest path tree
   * until it finds a free vertex of A
   */
	private double dijkstra(double[] lv, boolean[] shortestpathset) {
		for (int i = 0; i < 2*n; i++) {
      // determine next vertex to add to shortest path tree
			int minIndex = DijkstraMinDistance(lv, shortestpathset);
			shortestpathset[minIndex] = true;
			if (minIndex < n) { // vertex of type B added to shortest path tree
        // update distance to each neighbor of this vertex
				for (int j = 0; j < n; j++) {
					if (capacityBA[minIndex][j] > 0) {
						double slack = slackBA[minIndex][j];
						if (lv[j+n] > lv[minIndex] + slack) {
							lv[j+n] = lv[minIndex] + slack;
						}
					}
				}
			} 
			else { // vertex of type A added
				int a = minIndex - n;	
				if (AFree[a]) { return lv[minIndex]; } // augmenting path found
        // update distance to each neighbor of this vertex
				for (int b = 0; b < n; b++) {
					if (capacityAB[a][b] > 0) {
						double slack = slackAB[a][b];
						if (lv[b] > lv[minIndex] + slack) {
							lv[b] = lv[minIndex] + slack;
						}
					}
				}
			}
		}
		return -1; // this isn't supposed to happen... verify with asserts?
	}
	
	private int DijkstraMinDistance(double[] lv, boolean[] shortestpathset) {
		double minVal = Double.POSITIVE_INFINITY;
		int minIndex = -1;
		for (int i = 0; i < 2*n; i++) {
			if (lv[i] < minVal && !shortestpathset[i]) {
				minVal = lv[i];
				minIndex = i;
			}
		}
		return minIndex;
	}
	
	private void updateDualWeights(double[] lv, double distAF) {
		for (int i = 0; i < 2*n; i++) {
			if (lv[i] < distAF) {
				if (i < n) { // i is vertex of B
					dualWeights[i] = dualWeights[i] + distAF - lv[i];
				}
				else { // is vertex of A
					dualWeights[i] = dualWeights[i] - distAF + lv[i];
				}
			}
		}
	}
	
  /**
   * update slacks and compute admissible graph for use in DFS
   */
	private void updateSlacks() {//invert i & j
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				slackAB[i][j] = dualWeights[i+n] + dualWeights[j] - CAB[i][j]; //old
				slackBA[i][j] = CBA[i][j] + 1 - dualWeights[i] - dualWeights[j+n];
				//slackAB[i][j] = dualWeights[j+n] + dualWeights[i] - CAB[i][j];
				//slackBA[i][j] = CBA[i][j] + 1 - dualWeights[j] - dualWeights[i + n];
			}
		}
	}
	
	private void DFS() {
		int[] vertexVisited = setVertexVisited();
		for (int vertex = 0; vertex < n; vertex++) {
			if (BFree[vertex]) {
        // find augmenting paths from this vertex until vertex is
        // no longer free or no more paths are found
				while (deficiencyB[vertex] > 0 && vertexVisited[vertex] < n*2) {
          // perform partial DFS to 
					int AugPathVerIndex = DFSUtil(vertex, vertexVisited);
					if (AugPathVerIndex < 0) { break; } // no augmenting path found
					APLengths += AugPathVerIndex;
          // compute maximum flow augmenting path can carry
					double val = Math.min(deficiencyB[augmentingPathVertices[0]], deficiencyA[augmentingPathVertices[AugPathVerIndex]-n]); // +1 or no?
					for (int j = 0; j < AugPathVerIndex; j++) {
						int vertex1 = augmentingPathVertices[j];
						int vertex2 = augmentingPathVertices[j+1];
						if (vertex1 >= n) { val = Math.min(val, capacityAB[vertex1-n][vertex2]); } // edge is A to B
						else { val = Math.min(val,  capacityBA[vertex1][vertex2-n]); } // edge is B to A
					}
          // augment along path
					for (int j = 0; j < AugPathVerIndex; j++) {
						int vertex1 = augmentingPathVertices[j];
						int vertex2 = augmentingPathVertices[j+1];
						if (vertex1 >= n) { // edge is A to B
							capacityAB[vertex1 - n][vertex2] -= val;
							capacityBA[vertex2][vertex1 - n] += val;
							if (capacityAB[vertex1 - n][vertex2] > 0) {
                // allow edge to be reused in future augmenting paths
								vertexVisited[vertex1] = vertex2 - 1;
							}
						}
						else { // edge is B to A
							capacityBA[vertex1][vertex2-n] -= val;
							capacityAB[vertex2-n][vertex1] += val;
							if (capacityBA[vertex1][vertex2-n] > 0) {
                // allow edge to be reused in future augmenting paths
								vertexVisited[vertex1] = vertex2 - 1;
							}
						}
					}
					deficiencyB[vertex] = deficiencyB[vertex] - val;
					if (deficiencyB[vertex] == 0) { BFree[vertex] = false; }
					int last = augmentingPathVertices[AugPathVerIndex] - n;
					deficiencyA[last] -= val;
					if (deficiencyA[last] == 0) { AFree[last] = false; }
				}
			}
		}
	}
	
  /**
   * Track index of largest explored neighbor of each vertex 
   */
	private int[] setVertexVisited() {
		int[] vertexVisited = new int[2*n];
		for (int i = 0; i < n; i++) { vertexVisited[i] = n-1; }
		for (int i = n; i < 2*n; i++) { vertexVisited[i] = -1; }
		return vertexVisited;
	}
	
	/**
   * Main computation for DFS
   * Only has 2 parameters, but uses other globals
  */
	private int DFSUtil(int vertex, int[] vertexVisited) {
		int AugPathVerIndex = 0;
		augmentingPathVertices[AugPathVerIndex] = vertex;
    
		while (AugPathVerIndex > -1) {
			vertex = augmentingPathVertices[AugPathVerIndex];
			if (vertex >= n && AFree[vertex-n]) { return AugPathVerIndex; }
			boolean backtrack = true;
			int range_var1 = vertexVisited[vertex] + 1;
			int range_var2;
      
      
			if (vertex < n) { range_var2 = n*2; } // is vertex of B
			else { range_var2 = n; } // vertex of A
      
      
			for (int i = range_var1; i < range_var2; i++) {
				vertexVisited[vertex] = i;
        
				if (vertex < n) { // vertex type B
					int a = i - n;
          
					if (slackBA[vertex][a] == 0 && capacityBA[vertex][a] > 0) {
						backtrack = false; //no need to go back
						AugPathVerIndex = AugPathVerIndex + 1;
						augmentingPathVertices[AugPathVerIndex] = i;
						break;
					}
				}
        
				else { // vertex type A
					int a = vertex - n; 
          
					if (slackAB[a][i] == 0 && capacityAB[a][i] > 0) {
						backtrack = false;
						AugPathVerIndex++;
						augmentingPathVertices[AugPathVerIndex] = i;
						break;
					}
				}
			}
			if (backtrack) {
				augmentingPathVertices[AugPathVerIndex] = -1;
				AugPathVerIndex--;
			}
		}
		return AugPathVerIndex;
	}
	/**
   * Uses capacityAB and capacityBA to create a matrix capacity 
   * which contains the data from both AB and BA
  */
	private double[][] formatCapacity() {
		double[][] capacity = new double[2*n][2*n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				capacity[i+n][j] = capacityAB[i][j];
				capacity[i][j+n] = capacityBA[i][j];
			}
		}
		return capacity;
	}
}
