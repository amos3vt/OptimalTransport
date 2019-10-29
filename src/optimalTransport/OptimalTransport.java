package optimalTransport;

import java.io.File; 
import java.io.FileNotFoundException; 
import java.util.Scanner;

public class OptimalTransport {
    int n; // number of supply vertices ( == number of demand vertices)
    boolean[] AFree; // array of booleans representing whether demand vertex is free
    boolean[] BFree; // array of booleans representing whether supply vertex is free
    double[] deficiencyA; // amount of unmet demands at each demand vertex
    double[] deficiencyB; // amount of unused supplies at each supply vertex
    int iterations; // RETURN
    double APLengths; // RETURN
    double[] dualWeights;
    double[][] capacityAB;
    double[][] capacityBA;
    int[][] CAB;
    int[][] CBA;
    int[] vertexVisited;
    double[][] slackAB;
    double[][] slackBA;
    int[] augmentingPathVertices;
    double[][] capacity;
    
    
    
    public static void main(String args[]) {
    	
    	try {
			OptimalTransport o = new OptimalTransport(Integer.parseInt(args[0]), args[1], args[2], args[3]);
			o.compute();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}
    	
    	
    }
    /**
     * Performs Gabow-Tarjan optimal transport
     * Constructor sets up all initial values
     * @param n Number of supply vertices (equals number of demand vertices)
     */
    public OptimalTransport(int n, String deficiencyBFile, String deficiencyAFile, String fileCost) throws FileNotFoundException {
    	System.out.println("GO");
        iterations = 0;
        APLengths = 0;
        this.n = n;
        this.deficiencyA = loadArray(n, deficiencyAFile);
        this.deficiencyB = loadArray(n, deficiencyBFile);
        this.CAB = loadMatrix(n, fileCost);
        AFree = new boolean[n];
        BFree = new boolean[n];
        for(int i = 0; i < n; i++) {
        	AFree[i] = true;
        	BFree[i] = true;
        }
        CBA = transpose(CAB);
        
        slackAB = new double[n][n];
        slackBA = new double[n][n];
        capacityAB = new double[n][n];
        capacityBA = new double[n][n];
        dualWeights = new double[2*n];
        vertexVisited = new int[2*n];
        augmentingPathVertices = new int[2*n];
        for (int i = 0; i < n; i++) {
            if (deficiencyA[i] == 0.0) { AFree[i] = false; }
            if (deficiencyB[i] == 0.0) { BFree[i] = false; }
            augmentingPathVertices[i] = -1;
            for (int j = 0; j < n; j++) {
            	//System.out.println("Hello?");
                capacityAB[j][i] = Math.min(deficiencyB[j], deficiencyA[i]);
                //CBA[j][i] = CAB[i][j];
            }
        }
        /*for (int i = n; i < 2*n; i++) {
            for (int j = 0; j < n; j++) {
                slackAB[j][i-n] = dualWeights[i] + dualWeights[j] - CAB[j][i-n];
                slackBA[j][i-n] = CBA[j][i-n] + 1 - dualWeights[i-n] - dualWeights[j+n];
            	
            }
        }*/
        for(int i = 0; i < n; i++) {
        	for(int j = 0; j < n; j++) {
        		slackAB[j][i] = -1*CAB[j][i];
        		slackBA[j][i] = CBA[j][i] +1;
        	}
        }
        //System.out.println("Setup COmplete");
    }
    
    public static int[][] transpose(int[][] matrix){
		int[][] t = new int[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++) {
			for(int j = 0; j < matrix[0].length; j++) {
				t[j][i] = matrix[i][j];
			}
		}
		return t;
	}
    private int[][] loadMatrix(int n, String filename) throws FileNotFoundException {
        int[][] matrix = new int[n][n];
        File file = new File(filename);
        Scanner scanner = new Scanner(file);
        //scanner.useDelimiter(" ");
        for (int i = 0; i < n; i++) {
        	//System.out.println("Loading matrix " + i);
            for (int j = 0; j < n; j++) {
            	
            	//if(scanner.hasNextInt()) {
            		//System.out.println("d");
            		matrix[i][j] = scanner.nextInt();
            	//System.out.println(scanner.nextLine());
            	//}
            }
        }
        scanner.close();
        return matrix;
    }
    
    private double[] loadArray(int n, String filename) throws FileNotFoundException {
        double[] array = new double[n];
        File file = new File(filename);
        Scanner scanner = new Scanner(file);
        
        for (int i = 0; i < n; i++) {
            array[i] = scanner.nextInt();
            //System.out.println("loading array");
            if(scanner.hasNextLine())scanner.nextLine();
        }
        scanner.close();
        return array;
    }
    
    /**
     * Computes the optimal transport
     */
    private void compute() {
        while (anyFree(BFree)) {
        	//System.out.println("main phase");
            iterations++;
            double[] lv = new double[2*n];
            boolean[] shortestpathset = new boolean[2*n];
            for (int i = 0; i < n; i++) {
                if (BFree[i]) { lv[i] = 0; }
                else { lv[i] = Double.POSITIVE_INFINITY; }
                shortestpathset[i] = false;
            }
            for (int i = n; i < 2*n; i++) {
                lv[i] = Double.POSITIVE_INFINITY;
                shortestpathset[i] = false;
            }
            mainPhase(lv, shortestpathset);
            getCapacities();
        }
    }
    
    /**
     * Determine if there are any free vertices
     * @param f Array of booleans indicating whether a vertex is free
     * @return
     */
    private boolean anyFree(boolean[] f) {
        for (int i = 0; i < n; i++) {
            if (f[i]) { return true; } 
        }
        return false;
    }
    
    private void mainPhase(double[] lv, boolean[] shortestpathset) {
        double distAF = dijkstra(lv, shortestpathset); // min distance to a free vertex
        updateDualWeights(lv, distAF);
        updateSlacks();
        DFS();
    }
    
    private double dijkstra(double[] lv, boolean[] shortestpathset) {
        for (int i = 0; i < n*2; i++) {
            int minIndex = dijkstraMinIndex(lv, shortestpathset);
            shortestpathset[minIndex] = true;
            if (minIndex <= n) { // added vertex of type b to shortest path tree
                for (int j = 0; j < n; j++) {
                    if (capacityBA[j][minIndex] > 0) {
                        double slack = slackBA[j][minIndex];
                        if (lv[j+n] > lv[minIndex] + slack) {
                            lv[j+n] = lv[minIndex] + slack;
                        }
                    }
                }
            } else {
                int a = minIndex - n;
                if (AFree[a]) {
                    return lv[minIndex];
                }
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
        return -1;
    }
    
    private int dijkstraMinIndex(double[] lv, boolean[] shortestpathset) {
        double minValue = Double.POSITIVE_INFINITY;
        int minIndex = 0;//was -1
        for (int i = 0; i < n*2; i++) {
            if (lv[i] < minValue && !shortestpathset[i]) {
                minValue = lv[i];
                minIndex = i;
            }
        }
        return minIndex;
    }
    
    private void updateDualWeights(double[] lv, double distAF) {
        for (int i = 0; i < n*2; i++) {
            if (lv[i] < distAF) {
                if (i <= n) { // i is a vertex of B, increase dual weight
                    dualWeights[i] = dualWeights[i] + distAF - lv[i];
                } else { //i is a vertex of A, decrease dual weight
                    dualWeights[i] = dualWeights[i] - distAF + lv[i];
                }
            }
        }
    }
    
    private void updateSlacks() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double slack = dualWeights[i+n] + dualWeights[j] - CAB[j][i];
                slackAB[j][i] = slack;
                slack = CBA[j][i] + 1 - dualWeights[i] - dualWeights[j+n];
                slackBA[j][i] = slack;
            }
        }
    }
    
    private void getCapacities() {
        capacity = new double[2*n][2*n];
        for(int i = 0; i < n; i++) {
			for(int j = 0; j < n; j++) {
				capacity[n + i][j] = capacityAB[j][i];
				capacity[i][j + n] = capacityBA[j][i];
			}
		}
    }
    
    private void DFS() {
    	
    	for(int i = 0; i < n; i++) {
    		vertexVisited[i] = n;
    	}
    	for(int i = n; i < 2*n; i++) {
    		vertexVisited[i] = 0;
    	}
        for(int vertex = 1; vertex < n; vertex++) {
            if(BFree[vertex]) {
                while(deficiencyB[vertex] > 0 && vertexVisited[vertex] < 2*n) {
                    int AugPathVerIndex = DFSUtil(vertex);
                    if(AugPathVerIndex <= 0) { break; }
                    else {
                        double val = Math.min(deficiencyB[augmentingPathVertices[0]], deficiencyA[augmentingPathVertices[AugPathVerIndex] - n]);
                        for(int j = 0; j < AugPathVerIndex; j++) {
                            int vertex1 = augmentingPathVertices[j];
                            int vertex2 = augmentingPathVertices[j + 1];
                            if(vertex1 > n) {
                                val = Math.min(val, capacityAB[vertex2][vertex1 - n]);      
                            }
                            else {
                                val = Math.min(val, capacityBA[vertex2 - n][vertex1]);
                            }
                        }
                        for(int j = 0; j < AugPathVerIndex; j++) {
                            int vertex1 = augmentingPathVertices[j];
                            int vertex2 = augmentingPathVertices[j + 1];   
                            if(vertex1 > n) {
                                capacityAB[vertex2] [vertex1 - n] = capacityAB[vertex2] [vertex1 - n] - val;
                                capacityBA[vertex1 - n][ vertex2] = capacityBA[vertex1 - n] [vertex2] + val;
                                if(capacityAB[vertex2][ vertex1 - n] > 0) {   
                                    vertexVisited[vertex1] = vertex2 - 1; 
                                } 
                            }
                            else {
                                capacityBA[vertex2 - n][ vertex1] = capacityBA[vertex2 - n][ vertex1] - val;
                                capacityAB[vertex1][ vertex2 - n] = capacityAB[vertex1][ vertex2 - n] + val;
                                if(capacityBA[vertex2 - n] [vertex1] > 0) {          
                                    vertexVisited[vertex1] = vertex2 - 1; 
                                }
                            }
                        }
                        //update deficiencies                     
                        deficiencyB[vertex] = deficiencyB[vertex] - val;
                        if(deficiencyB[vertex] == 0) {
                            BFree[vertex] = false;
                        }
                        int last = augmentingPathVertices[AugPathVerIndex] - n;
                        deficiencyA[last] = deficiencyA[last] - val;
                        if(deficiencyA[last] == 0){
                            AFree[last] = false;
                        }
                    }
                    
                }
            }
        }
    }
    
    private int DFSUtil(int vertex) {
    	//System.out.println("DFSUTIL " + vertex);
        int AugPathVerIndex = 0;
        augmentingPathVertices[AugPathVerIndex] = vertex;
        while(AugPathVerIndex >= 0) {
            vertex = augmentingPathVertices[AugPathVerIndex];     
            if(vertex > n && AFree[vertex - n - 1]) { // THERE MAY BE AN ISSUE WITH THIS CONDITION
                                                            // LOOK HERE WHEN IT DOESN'T WORK
                System.out.println("!!!!augpathverindex = " + AugPathVerIndex);
            	return AugPathVerIndex;
                
            }
            boolean backtrack = true;
            int range_var1 = vertexVisited[vertex] + 1;//this +1 may be wrong too
            int range_var2 = 0;
            
            if(vertex <= n) {//
                range_var2 = 2*n;
            }
            else {
                range_var2 = n;
            }         
            for(int i = range_var1; i < range_var2; i++) {
                vertexVisited[vertex] = i;   
                if(vertex < n) {
                    int a = i - n -1;
                    if(a == 146 && vertex == 202)
                    System.out.println("slackba " + slackBA[a][vertex]);
                    if(slackBA[a][vertex] == 0 && capacityBA[a][vertex] > 0) {
                        backtrack = false;
                        augmentingPathVertices[++AugPathVerIndex] = i;
                        break;
                    }
                }
                else {
                    int a = vertex - n;
                    if(slackAB[i][a] == 0 && capacityAB[i][a] > 0) {
                        backtrack = false;
                        augmentingPathVertices[++AugPathVerIndex] = i;
                        break;
                    }
                }
            }
            if(backtrack) {
                augmentingPathVertices[AugPathVerIndex--] = 0;
            }
            
        }
        //System.out.println("augpathverindex = " + AugPathVerIndex);
        return AugPathVerIndex;
    }
    
}