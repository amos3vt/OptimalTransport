package optimalTransport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

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
    double[][] CAB;
    double[][] CBA;
    int[] vertexVisited;
    double[][] slackAB;
    double[][] slackBA;
    int[] augmentingPathVertices;
    double[][] capacity;
    
    /**
     * Performs Gabow-Tarjan optimal transport
     * Constructor sets up all initial values
     * @param n Number of supply vertices (equals number of demand vertices)
     */
    public OptimalTransport(int n, double[] deficiencyA, double[] deficiencyB, double[][] CAB) {
    	
        // assert sum(supplies) <= sum(demands)
        iterations = 0;
        APLengths = 0;
        this.n = n;
        this.deficiencyA = deficiencyA;
        this.deficiencyB = deficiencyB;
        this.CAB = CAB;
        AFree = new boolean[n];
        BFree = new boolean[n];
        CBA = new double[n][n];
        slackAB = new double[n][n];
        slackBA = new double[n][n];
        capacityAB = new double[n][n];
        capacityBA = new double[n][n];
        dualWeights = new double[2*n];
        vertexVisited = new int[2*n];
        augmentingPathVertices = new int[2*n];
        for (int i = 0; i < n; i++) {
            if (deficiencyA[i] != 0) { AFree[i] = true; }
            if (deficiencyB[i] != 0) { BFree[i] = true; }
            augmentingPathVertices[i] = -1;
            for (int j = 0; j < n; j++) {
                capacityAB[j][i] = Math.min(deficiencyB[j], deficiencyA[i]);
                CBA[j][i] = CAB[i][j];
            }
        }
        for (int i = n; i < 2*n; i++) {
            for (int j = 0; j < n; j++) {
                slackAB[j][i-n] = dualWeights[i] + dualWeights[j] - CAB[j][i-n];
                slackBA[j][i-n] = CBA[j][i-n] + 1 - dualWeights[i-n] - dualWeights[j+n];
            }
        }
    }
    
    /**
     * Computes the optimal transport
     */
    private void compute() {
        while (anyFree(BFree)) {
            iterations++;
            int f = 2;
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
        int minIndex = -1;
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
        for (int i = 0; i < n; i++) {
            capacity[n+i][i] = capacityAB[i][n+i];
            capacity[i][n+i] = capacityBA[n+i][i];
        }
    }
    
    private void DFS() {
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
        int AugPathVerIndex = 0;
        augmentingPathVertices[AugPathVerIndex] = vertex;
        while(AugPathVerIndex >= 0) {
            vertex = augmentingPathVertices[AugPathVerIndex];     
            if(vertex > n && AFree[vertex - n]) { // THERE MAY BE AN ISSUE WITH THIS CONDITION
                                                            // LOOK HERE WHEN IT DOESN'T WORK
                return AugPathVerIndex;
            }
            boolean backtrack = true;
            int range_var1 = vertexVisited[vertex]+1; //this +1 may be wrong too
            int range_var2 = 0;
            
            if(vertex < n) {
                range_var2 = 2*n;
            }
            else {
                range_var2 = n;
            }         
            for(int i = range_var1; i <= range_var2; i++) {
                vertexVisited[vertex] = i;   
                if(vertex < n) {
                    int a = i - n;
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
        return AugPathVerIndex;
    }
    
}
