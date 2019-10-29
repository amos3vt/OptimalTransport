package optimalTransport;

import java.io.File; 
import java.io.FileNotFoundException; 
import java.util.Scanner;

// Make small test case
// Try with all supplies and demands as 1

// for arrays of length 2*n
// 0 to n-1 for B
// n to 2n-1 for A

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
	
	public OptimalTransport(int n, String fileDeficiencyB, String fileDeficiencyA, String fileCost) throws FileNotFoundException {
		this.n = n;
		iterations = 0;
		APLengths = 0;
		deficiencyB = loadArray(fileDeficiencyB);
		deficiencyA = loadArray(fileDeficiencyA);
		CAB = loadMatrix(fileCost);
		CBA = transpose(CAB);
		BFree = setFree(deficiencyB);
		AFree = setFree(deficiencyA);
		dualWeights = new double[2*n];
		capacityAB = new double[n][n];
		capacityBA = setCapacityBA();
		slackAB = new double[n][n];
		slackBA = new double[n][n];
		setSlacks();
		augmentingPathVertices = setAugmentingPathVertices();
		compute();
		capacity = formatCapacity();
	}
	
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
	
	private boolean[] setFree(double[] deficiency) {
		boolean[] Free = new boolean[n];
		for (int i = 0; i < n; i++) {
			if (deficiency[i] != 0) { Free[i] = true; }
		}
		return Free;
	}
	
	private double[][] setCapacityBA() {
		double[][] capacity = new double[n][n];
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				capacity[i][j] = Math.min(deficiencyB[j], deficiencyA[i]);
			}
		}
		return capacity;
	}
	
	private void setSlacks() {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				slackAB[j][i] = dualWeights[i+n] + dualWeights[j] - CAB[j][i];
				slackBA[j][i] = CBA[j][i] + 1 - dualWeights[i] - dualWeights[j+n];
			}
		}
	}
	
	private int[] setAugmentingPathVertices() {
		int[] AugmentingPathVertices = new int[2*n];
		for (int i = 0; i < 2*n; i++) {
			AugmentingPathVertices[i] = -1;
		}
		return AugmentingPathVertices;
	}
	
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
	
	private boolean anyFree(boolean[] b) {
		for (int i = 0; i < n; i++) {
			if (b[i]) { return true; }
		}
		return false;
	}
	
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
	
	private double dijkstra(double[] lv, boolean[] shortestpathset) {
		for (int i = 0; i < 2*n; i++) {
			int minIndex = DijkstraMinDistance(lv, shortestpathset);
			shortestpathset[minIndex] = true;
			if (minIndex < n) { // vertex of type B added to shortest path tree
				for (int j = 0; j < n; j++) {
					if (capacityBA[j][minIndex] > 0) {
						double slack = slackBA[j][minIndex];
						if (lv[j+n] > lv[minIndex] + slack) {
							lv[j+n] = lv[minIndex] + slack;
						}
					}
				}
			} 
			else {
				int a = minIndex - n;
				if (AFree[a]) { return lv[minIndex]; }
				for (int b = 0; b < n; b++) {
					if (capacityAB[b][a] > 0) {
						double slack = slackAB[b][a];
						if (lv[b] > lv[minIndex] + slack) {
							lv[b] = lv[minIndex] + slack;
						}
					}
				}
			}
		}
		return 0; // check if theres a better alternative
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
	
	private void updateSlacks() {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				slackAB[j][i] = dualWeights[i+n] + dualWeights[j] - CAB[j][i];
				slackBA[j][i] = CBA[j][i] + 1 - dualWeights[i] - dualWeights[j+n];
			}
		}
	}
	
	private void DFS() {
		int[] vertexVisited = setVertexVisited();
		for (int vertex = 0; vertex < n; vertex++) {
			if (BFree[vertex]) {
				while (deficiencyB[vertex] > 0 && vertexVisited[vertex] < n*2) {
					int AugPathVerIndex = DFSUtil(vertex, vertexVisited);
					if (AugPathVerIndex < 0) { break; } // no augmenting path found
					APLengths += AugPathVerIndex;
					double val = Math.min(deficiencyB[augmentingPathVertices[0]], deficiencyA[augmentingPathVertices[AugPathVerIndex]-n]);
					for (int j = 0; j < AugPathVerIndex; j++) {
						int vertex1 = augmentingPathVertices[j];
						int vertex2 = augmentingPathVertices[j+1];
						if (vertex1 >= n) { val = Math.min(val, capacityAB[vertex2][vertex1-n]); }
						else { val = Math.min(val,  capacityBA[vertex2-n][vertex1]); }
					}
					for (int j = 0; j < AugPathVerIndex; j++) {
						int vertex1 = augmentingPathVertices[j];
						int vertex2 = augmentingPathVertices[j+1];
						if (vertex1 >= n) {
							capacityAB[vertex2][vertex1 - n] = capacityAB[vertex2][vertex1 - n] - val;
							capacityBA[vertex1 - n][vertex2] = capacityBA[vertex1 - n][vertex2] + val;
							if (capacityAB[vertex2][vertex1 - n] > 0) {
								vertexVisited[vertex1] = vertex2 - 1;
							}
						}
						else {
							capacityBA[vertex2-n][vertex1] = capacityBA[vertex2-n][vertex1] - val;
							capacityAB[vertex1][vertex2-n] = capacityAB[vertex1][vertex2-n] + val;
							if (capacityBA[vertex2-n][vertex1] > 0) {
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
	
	private int[] setVertexVisited() {
		int[] vertexVisited = new int[2*n];
		for (int i = 0; i < n; i++) { vertexVisited[i] = n-1; }
		for (int i = n; i < 2*n; i++) { vertexVisited[i] = -1; }
		return vertexVisited;
	}
	
	private int DFSUtil(int vertex, int[] vertexVisited) {
		int AugPathVerIndex = 0;
		augmentingPathVertices[AugPathVerIndex] = vertex;
		while (AugPathVerIndex > -1) {
			vertex = augmentingPathVertices[AugPathVerIndex];
			if (vertex >= n && AFree[vertex-n]) { return AugPathVerIndex; }
			boolean backtrack = true;
			int range_var1 = vertexVisited[vertex] + 1;
			int range_var2;
			if (vertex < n) { range_var2 = n*2; } // vertex of B
			else { range_var2 = n; } // vertex of A
			for (int i = range_var1; i < range_var2; i++) {
				vertexVisited[vertex] = i;
				if (vertex < n) { // vertex type B
					int a = i - n;
					if (slackBA[a][vertex] == 0 && capacityBA[a][vertex] > 0) {
						backtrack = false;
						AugPathVerIndex = AugPathVerIndex + 1;
						augmentingPathVertices[AugPathVerIndex] = i;
						return AugPathVerIndex;
					}
				}
				else { // vertex type A
					int a = vertex - n + 1;
					if (slackAB[i][a] == 0 && capacityAB[i][a] > 0) {
						backtrack = false;
						AugPathVerIndex++;
						augmentingPathVertices[AugPathVerIndex] = i;
						return AugPathVerIndex;
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
	
	private double[][] formatCapacity() {
		double[][] capacity = new double[2*n][2*n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				capacity[i+n][j] = capacityAB[j][i];
				capacity[i][j+n] = capacityBA[j][i];
			}
		}
		return capacity;
	}
}
